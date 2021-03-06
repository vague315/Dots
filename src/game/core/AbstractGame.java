package game.core;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import graphics.GameRenderer;
import graphics.TextRenderer;
import graphics.Window;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;

public abstract class AbstractGame {
	public static final int TARGET_FPS = 120;
    public static final int TARGET_UPS = 30;
    
	protected Timer timer;
	protected Window window;
	protected GameRenderer renderer;
	protected TextRenderer textRenderer;
	protected boolean running;
	
	private GLFWErrorCallback errorCallback;
	
	
	
	public AbstractGame(){
		timer = new Timer();
		renderer = new GameRenderer();	
		textRenderer = new TextRenderer();
	}
	
	public void start(){
		init();
		gameloop();
		dispose();
	}
	
	public void init(){
		errorCallback = Callbacks.errorCallbackPrint();
		glfwSetErrorCallback(errorCallback);
		
		// Initialize glfw
		if (glfwInit() != GL_TRUE) {
            throw new IllegalStateException("Unable to initialize GLFW!");
        }
		
		window = new Window(1024, 768, "Dots", false);
		timer.init();
		renderer.init();
		textRenderer.init();

		initGameObjects();
		
		running = true;
	}
	
	public abstract void initGameObjects();
	public abstract void updateGameObjects(float delta);
	public abstract void renderGameObjects(float alpha);
	public abstract void disposeGameObjects();
	public abstract void gameloop();
	public abstract void input();
	public abstract void renderText();
	
	public void update(){
		update(1f / TARGET_UPS);
	}
	
	public void update(float delta){
		updateGameObjects(delta);
	}	
	
	public void render(){
		render(1f);
	}
	
	public void render(float alpha){
		renderer.clear();
		renderer.begin();
		renderGameObjects(alpha);
		renderer.end();
	}	
	
	public void dispose(){
		window.destroy();
		renderer.dispose();
		textRenderer.dispose();
		disposeGameObjects();
		glfwTerminate();
		errorCallback.release();
	}
	
	/**
     * Synchronizes the game at specified frames per second.
     *
     * @param fps Frames per second
     */
    public void sync(int fps) {
        double lastLoopTime = timer.getLastLoopTime();
        double now = timer.getTime();
        float targetTime = 1f / fps;

        while (now - lastLoopTime < targetTime) {
            Thread.yield();

            /* This is optional if you want your game to stop consuming too much
             CPU but you will loose some accuracy because Thread.sleep(1) could
             sleep longer than 1 millisecond */
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(AbstractGame.class.getName()).log(Level.SEVERE, null, ex);
            }

            now = timer.getTime();
        }
    }
	
	public boolean isRunning(){
		return running;
	}
	
	
}
