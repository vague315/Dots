package graphics;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import graphics.opengl.Shader;
import graphics.opengl.ShaderProgram;
import graphics.opengl.Texture;
import graphics.opengl.VertexArrayObject;
import graphics.opengl.VertexBufferObject;

import java.awt.Color;
import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import math.Matrix4f;
import math.Vector2f;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import text.Font;

public class GameRenderer {
	private static final int BUFFER_SIZE = 4096;
	
	private VertexArrayObject vao;
    private VertexBufferObject vbo;
    private Shader vertexShader;
    private Shader fragmentShader;
    private ShaderProgram program;

    private FloatBuffer vertices;
    private int numVertices;
    
    private FloatBuffer lineVertices;
    private int numLineVertices;
    private boolean drawing;

    private Font font;
	private Font debugFont;
	
    /**
     * Initializes the renderer.
     *
     * @param defaultContext Specifies if the OpenGL context is 3.2 compatible
     */
    public void init() {
    	numVertices = 0;
    	numLineVertices = 0;
        drawing = false;
        
        // create our vertex array object
        vao = new VertexArrayObject();
        vao.bind();

        // create out vertex buffer object
        vbo = new VertexBufferObject();
        vbo.bind(GL_ARRAY_BUFFER);

        // create our buffer of vertices to draw with
        vertices = BufferUtils.createFloatBuffer(BUFFER_SIZE);
        lineVertices = BufferUtils.createFloatBuffer(BUFFER_SIZE);

        // allocate storage for the vbo by sending null data to the gpu
        long size = BUFFER_SIZE * Float.BYTES;
        vbo.uploadData(GL_ARRAY_BUFFER, size, GL_STREAM_DRAW);

        // load our default shaders
        vertexShader = Shader.loadShader(GL_VERTEX_SHADER, "res/test_vertex.glsl");
        fragmentShader = Shader.loadShader(GL_FRAGMENT_SHADER, "res/test_fragment.glsl");

        // create the shader programe
        program = new ShaderProgram();
        program.attachShader(vertexShader);
        program.attachShader(fragmentShader);

        program.bindFragmentDataLocation(0, "fragColor");

        program.link();
        program.use();

        // get the window width and height for our orthographic matrix
        long window = GLFW.glfwGetCurrentContext();
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
        int width = widthBuffer.get();
        int height = heightBuffer.get();

        // specify the vertex attributes. describe the order/structure of our vertices buffer
        specifyVertexAttributes();

        // set the texture uniform in the shader
        int uniTex = program.getUniformLocation("texImage");
        program.setUniform(uniTex, 0);

        // create the orthographic matrix and set the uniform in the shader
        Matrix4f ortho = Matrix4f.orthographic(0f, width, 0f, height, -1f, 1f);
        int uniOrtho = program.getUniformLocation("ortho");
        program.setUniform(uniOrtho, ortho);

        // enable blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        try {
            font = new Font(new FileInputStream("res/Inconsolata.otf"), 16);
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(GameRenderer.class.getName()).log(Level.CONFIG, null, ex);
            font = new Font();
        }
		debugFont = new Font(16);
    }
    
    /**
     * Dispose renderer and clean up its used data.
     */
    public void dispose() {
        if (vao != null) {
            vao.delete();
        }
        vbo.delete();
        vertexShader.delete();
        fragmentShader.delete();
        program.delete();
        font.dispose();
		debugFont.dispose();
    }

    /**
     * Clears the drawing area.
     */
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Begin rendering.
     */
    public void begin() {
        if (drawing) {
            throw new IllegalStateException("Renderer is already drawing!");
        }
        drawing = true;
        numVertices = 0;
        numLineVertices = 0;
    }

    /**
     * End rendering.
     */
    public void end() {
        if (!drawing) {
            throw new IllegalStateException("Renderer isn't drawing!");
        }
        drawing = false;
        flush();
        flushLines();
    }

    /**
     * Flushes the data to the GPU to let it get rendered.
     */
    public void flush() {
        if (numVertices > 0) {
            vertices.flip();

            vao.bind();

            program.use();

            /* Upload the new vertex data */
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadSubData(GL_ARRAY_BUFFER, 0, vertices);

            /* Draw batch */
            glDrawArrays(GL_TRIANGLES, 0, numVertices);

            /* Clear vertex data for next batch */
            vertices.clear();
            numVertices = 0;
        }
    }
    
    /**
     * Flushes line data to the GPU to be rendered
     */
    public void flushLines(){
    	if (numLineVertices > 0) {
            lineVertices.flip();

            vao.bind();

            program.use();

            /* Upload the new vertex data */
            vbo.bind(GL_ARRAY_BUFFER);
            vbo.uploadSubData(GL_ARRAY_BUFFER, 0, lineVertices);

            /* Draw batch */
            glDrawArrays(GL_LINES, 0, numLineVertices);

            /* Clear vertex data for next batch */
            lineVertices.clear();
            numLineVertices = 0;
        }
    }

    /**
     * Draws the currently bound texture on specified coordinates.
     *
     * @param texture Used for getting width and height of the texture
     * @param x X position of the texture
     * @param y Y position of the texture
     */
    public void drawTexture(Texture texture, float x, float y) {
        drawTexture(texture, x, y, Color.WHITE);
    }

    /**
     * Draws the currently bound texture on specified coordinates and with
     * specified color.
     *
     * @param texture Used for getting width and height of the texture
     * @param x X position of the texture
     * @param y Y position of the texture
     * @param c The color to use
     */
    public void drawTexture(Texture texture, float x, float y, Color c) {
        /* Vertex positions */
        float x1 = x;
        float y1 = y;
        float x2 = x1 + texture.getWidth();
        float y2 = y1 + texture.getHeight();

        /* Texture coordinates */
        float s1 = 0f;
        float t1 = 0f;
        float s2 = 1f;
        float t2 = 1f;

        drawTextureRegion(x1, y1, x2, y2, s1, t1, s2, t2, c);
    }

    /**
     * Draws a texture region with the currently bound texture on specified
     * coordinates.
     *
     * @param texture Used for getting width and height of the texture
     * @param x X position of the texture
     * @param y Y position of the texture
     * @param regX X position of the texture region
     * @param regY Y position of the texture region
     * @param regWidth Width of the texture region
     * @param regHeight Height of the texture region
     */
    public void drawTextureRegion(Texture texture, float x, float y, float regX, float regY, float regWidth, float regHeight) {
        drawTextureRegion(texture, x, y, regX, regY, regWidth, regHeight, Color.WHITE);
    }

    /**
     * Draws a texture region with the currently bound texture on specified
     * coordinates.
     *
     * @param texture Used for getting width and height of the texture
     * @param x X position of the texture
     * @param y Y position of the texture
     * @param regX X position of the texture region
     * @param regY Y position of the texture region
     * @param regWidth Width of the texture region
     * @param regHeight Height of the texture region
     * @param c The color to use
     */
    public void drawTextureRegion(Texture texture, float x, float y, float regX, float regY, float regWidth, float regHeight, Color c) {
        /* Vertex positions */
        float x1 = x;
        float y1 = y;
        float x2 = x + regWidth;
        float y2 = y + regHeight;

        /* Texture coordinates */
        float s1 = regX / texture.getWidth();
        float t1 = regY / texture.getHeight();
        float s2 = (regX + regWidth) / texture.getWidth();
        float t2 = (regY + regHeight) / texture.getHeight();

        drawTextureRegion(x1, y1, x2, y2, s1, t1, s2, t2, c);
    }

    /**
     * Draws a texture region with the currently bound texture on specified
     * coordinates.
     *
     * @param x1 Bottom left x position
     * @param y1 Bottom left y position
     * @param x2 Top right x position
     * @param y2 Top right y position
     * @param s1 Bottom left s coordinate
     * @param t1 Bottom left t coordinate
     * @param s2 Top right s coordinate
     * @param t2 Top right t coordinate
     */
    public void drawTextureRegion(float x1, float y1, float x2, float y2, float s1, float t1, float s2, float t2) {
        drawTextureRegion(x1, y1, x2, y2, s1, t1, s2, t2, Color.WHITE);
    }

    /**
     * Draws a texture region with the currently bound texture on specified
     * coordinates.
     *
     * @param x1 Bottom left x position
     * @param y1 Bottom left y position
     * @param x2 Top right x position
     * @param y2 Top right y position
     * @param s1 Bottom left s coordinate
     * @param t1 Bottom left t coordinate
     * @param s2 Top right s coordinate
     * @param t2 Top right t coordinate
     * @param c The color to use
     */
    public void drawTextureRegion(float x1, float y1, float x2, float y2, float s1, float t1, float s2, float t2, Color c) {
        if (vertices.remaining() < 7 * 6) {
            /* We need more space in the buffer, so flush it */
            flush();
        }

        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;

        vertices.put(x1).put(y1).put(r).put(g).put(b).put(s1).put(t1);
        vertices.put(x1).put(y2).put(r).put(g).put(b).put(s1).put(t2);
        vertices.put(x2).put(y2).put(r).put(g).put(b).put(s2).put(t2);

        vertices.put(x1).put(y1).put(r).put(g).put(b).put(s1).put(t1);
        vertices.put(x2).put(y2).put(r).put(g).put(b).put(s2).put(t2);
        vertices.put(x2).put(y1).put(r).put(g).put(b).put(s2).put(t1);

        numVertices += 6;
    }

    // Specifies vertex attributes for position, color, and texture coordinates
//    private void specifyVertexAttributes() {
//    	// Specify vertex pointer
//        int posAttrib = program.getAttributeLocation("position");
//        program.enableVertexAttribute(posAttrib);
//        program.pointVertexAttribute(posAttrib, 2, 7 * Float.BYTES, 0);
//
//        // Specify color pointer
//        int colAttrib = program.getAttributeLocation("color");
//        program.enableVertexAttribute(colAttrib);
//        program.pointVertexAttribute(colAttrib, 3, 7 * Float.BYTES, 2 * Float.BYTES);
//
//        // Specify texture pointer
//        int texAttrib = program.getAttributeLocation("texcoord");
//        program.enableVertexAttribute(texAttrib);
//        program.pointVertexAttribute(texAttrib, 2, 7 * Float.BYTES, 5 * Float.BYTES);
//    }

    // Specifies vertex attributes for position and color
	private void specifyVertexAttributes() {
        /* Specify Vertex Pointer */
        int posAttrib = program.getAttributeLocation("position");
        program.enableVertexAttribute(posAttrib);
        program.pointVertexAttribute(posAttrib, 2, 5 * Float.BYTES, 0);

        /* Specify Color Pointer */
        int colAttrib = program.getAttributeLocation("color");
        program.enableVertexAttribute(colAttrib);
        program.pointVertexAttribute(colAttrib, 3, 5 * Float.BYTES, 2 * Float.BYTES);
    }
	
	public void drawLine(float x1, float y1, float x2, float y2, Color color){
		if(lineVertices.remaining()< 2*5)
			flushLines();
		
		float r = color.getRed();
		float g = color.getGreen();
		float b = color.getBlue();
		
		lineVertices.put(x1).put(y1).put(r).put(g).put(b);
		lineVertices.put(x2).put(y2).put(r).put(g).put(b);
		numLineVertices+=2;
	}
    
	public void drawCircle(float cx, float cy, float radius, Color color) {
		float increment = 0.075f;
		// Since drawing circles utilizes line loops rather than triangles
		// clear the buffer if any vertices are a
		if(numVertices>0)
			flush();
		
		
		float r = color.getRed();
		float g = color.getGreen();
		float b = color.getBlue();
		
		for(float i=0; i<2*Math.PI; i+=increment){
			float x = cx + (float) (radius * Math.cos(i));
			float y = cy + (float) (radius * Math.sin(i));
			vertices.put(x).put(y).put(r).put(g).put(b);
			numVertices++;
		}		
		vertices.flip();
		
		vao.bind();
		program.use();
		
		// upload the new vertex data
		vbo.bind(GL_ARRAY_BUFFER);
		vbo.uploadSubData(GL_ARRAY_BUFFER, 0, vertices);

		glDrawArrays(GL_LINE_LOOP, 0, numVertices);
		
		vertices.clear();
		numVertices = 0;		
	}
	
	public void drawSquare(float x, float y, float width, float height, Color color){
		if(vertices.remaining() < 6*5){
			flush();
		}
	
		float r = color.getRed();
		float g = color.getGreen();
		float b = color.getBlue();
		
		Vector2f tl = new Vector2f(x-width/2,y+height/2);
		Vector2f bl = new Vector2f(x-width/2,y-height/2);
		Vector2f tr = new Vector2f(x+width/2, y+height/2);
		Vector2f br = new Vector2f(x+width/2, y-height/2);
		
		vertices.put(bl.x).put(bl.y).put(r).put(g).put(b);
		vertices.put(tl.x).put(tl.y).put(r).put(g).put(b);
		vertices.put(tr.x).put(tr.y).put(r).put(g).put(b);
		
		vertices.put(bl.x).put(bl.y).put(r).put(g).put(b);
		vertices.put(tr.x).put(tr.y).put(r).put(g).put(b);
		vertices.put(br.x).put(br.y).put(r).put(g).put(b);
		
		numVertices += 6;
	}
}
