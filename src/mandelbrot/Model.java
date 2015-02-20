package mandelbrot;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Observable;

/**
 * My go at implementing MVC for the Mandelbrot set.
 * 
 * TODO: Comment other methods
 * 
 * @author khengboonpek
 *
 */
public class Model extends Observable implements ActionListener {
	
    // ==== Constants ====

    /**
     * Escape Time Algorithm to compute the iteration count bounded by
     * maxRadius and maxIterations.
     */
    public static final int ALGORITHM_ESCAPE_TIME = 0;

    /**
     * Normalized Iteration Count Algorithm to compute the iteration count
     * bounded by maxRadius and maxIterations.
     */
    public static final int ALGORITHM_NORMALIZED_ITERATION_COUNT = 1;
	
	// ==== Properties ====
	
	private final GraphicsConfiguration config = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration();
    
	private int threadCount = Runtime.getRuntime().availableProcessors();
	
    private int maxIter = 1000;
    private double maxRadius = 2;
    
	private BufferedImage image;
	
	
	// ==== Constructor ====
	
    /**
     * Create a new {@code Model} instance and specify sane defaults.
     */
    public Model() {
        super();
        //setActive(false);
        //setSize(new Dimension(1, 1));
        //setFps(25);
        //setMaxIterations(200);
        //setMaxRadius(10);
        //setActive(true);
    }
    
    // ==== Accessors ====
    
    /**
     * 
     * @return
     */
    public synchronized final BufferedImage getImage() {
    	return image;
    }
    
    /**
     * 
     * @return
     */
    public synchronized final int getThreadCount() {
    	return threadCount;
    }
    
    /**
     * 
     * @param threadCount
     */
    public synchronized void setThreadCount(int threadCount) {
    	if (threadCount < 1) {
    		throw new IllegalArgumentException("threadCount must be larger than zero");
    	}
    	
    	if (this.threadCount != threadCount) {
    		stopDrawing();
    		this.threadCount = threadCount;
    		startDrawing();
    	}
    }
    
    /**
     * 
     * @return
     */
    public synchronized final Dimension getSize() {
    	return new Dimension(this.image.getWidth(), this.image.getHeight());
    }
    
	public synchronized void setSize(Dimension size) {
		// trigger if the image has been altered
		if (image == null || size.width != image.getWidth()
				|| size.height != image.getHeight()) {
			stopDrawing();

			BufferedImage newImage;

			if (image != null) {
				newImage.getGraphics().drawImage(image, 0, 0, image.getWidth(),
						image.getHeight(), null);
			}
			
			image = newImage;
			
			//TODO: updateIndexes();
			
			startDrawing();
		}
	}
	
	// ==== Private Helper Methods ====
	
	private void stopDrawing() {
		
	}
	
	private void startDrawing() {
		
	}
    

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
