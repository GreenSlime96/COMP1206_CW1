package mandelbrot;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Timer;

/**
 * My go at implementing MVC for the Mandelbrot set.
 * 
 * NOTES: Vector > ArrayList bc. synchronisation
 * 
 * TODO: Comment other methods TODO: Look for ArrayList synchronisation
 * 
 * @author khengboonpek
 *
 */
public class Model extends Observable implements ActionListener {

	// ==== Constants ====

	/**
	 * Escape Time Algorithm to compute the iteration count bounded by maxRadius
	 * and maxIterations.
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

	private final Timer timer = new Timer(1000, this);
	private int threadCount = Runtime.getRuntime().availableProcessors();
	private final Vector<Thread> threads = new Vector<Thread>(threadCount);
	private CountDownLatch firstRun;
	
    private int[] indexes;
    private double[] iterations;
    private AtomicInteger[] histogram;
    private final AtomicInteger firstIndex = new AtomicInteger();
    private final AtomicInteger secondIndex = new AtomicInteger();
    private final AtomicInteger processed = new AtomicInteger();

	private boolean active = true;
	private Point2D location = new Point2D.Double(-2.5, 1);
	private double scale = 1 / 200.;
	private int fps = 25;
	private int maxIter = 1000;
	private double maxRadius = 2;
    private boolean histEqualization = true;
	private volatile long renderingTime = 0;
	private long renderingStart = 0;

	private BufferedImage image;

	// ==== Constructor ====

	/**
	 * Create a new {@code Model} instance and specify sane defaults.
	 */
	public Model() {
		super();
		setActive(false);
		setSize(new Dimension(1, 1));
		setFps(25);
		setMaxIterations(200);
		setMaxRadius(10);
		setActive(true);
	}

	// ==== Accessors ====

	/**
	 * Get the image that the algorithm renders to. The image isn't necessarily
	 * fully rendered.
	 * 
	 * @return The rendered image.
	 */
	public synchronized final BufferedImage getImage() {
		return image;
	}

	/**
	 * Whether the model should re-render the image if necessary.
	 * 
	 * @return The current active state
	 * @see #setActive(boolean)
	 */
	public synchronized final boolean isActive() {
		return active;
	}

	/**
	 * Set the re-render state
	 * 
	 * @param active
	 *            The re-render behaviour.
	 * @see #isActive()
	 */
	public synchronized void setActive(boolean active) {
		stopDrawing();
		this.active = active;
		startDrawing();
	}

	/**
	 * Get the number of render threads running in parallel.
	 * 
	 * @return Number of threads.
	 */
	public synchronized final int getThreadCount() {
		return threadCount;
	}

	/**
	 * Sets the number of threads running in parallel and redraws.
	 * 
	 * @param threadCount
	 *            The new number of threads.
	 */
	public synchronized void setThreadCount(int threadCount) {
		if (threadCount < 1) {
			throw new IllegalArgumentException(
					"threadCount must be larger than zero");
		}

		if (this.threadCount != threadCount) {
			stopDrawing();
			this.threadCount = threadCount;
			startDrawing();
		}
	}

	/**
	 * Get the size of the created image.
	 * 
	 * @return The size of the image.
	 * @see #setSize(java.awt.Dimension)
	 */
	public synchronized final Dimension getSize() {
		return new Dimension(this.image.getWidth(), this.image.getHeight());
	}

	/**
	 * Sets the size of the rendered image, redrawing if necessary.
	 * 
	 * @param size
	 *            The new image size
	 * @see #getSize()
	 */
	public synchronized void setSize(Dimension size) {
		// trigger if the image has been altered
		if (image == null || size.width != image.getWidth()
				|| size.height != image.getHeight()) {
			stopDrawing();

			BufferedImage newImage = config.createCompatibleImage(size.width,
					size.height, Transparency.OPAQUE);

			if (image != null) {
				newImage.getGraphics().drawImage(image, 0, 0, image.getWidth(),
						image.getHeight(), null);
			}

			image = newImage;

			updateIndexes();

			startDrawing();
		}
	}

	/**
	 * Gets the number of update notifications per second sent to the viewer.
	 * 
	 * @return The frames per second while rendering.
	 * @see #setFps(int)
	 */
	public synchronized final int getFps() {
		return fps;
	}

	public synchronized void setFps(int fps) {
		this.fps = fps;
		timer.setDelay(1000 / fps);
		timer.setInitialDelay(1000 / fps);
	}

	/**
	 * Gets the maximum iteration used to determine whether a number has escaped
	 * the Mandelbrot set.
	 * 
	 * @return The maximum number of iterations.
	 */
	public synchronized final int getMaxIterations() {
		return maxIter;
	}

	/**
	 * Sets the maximum number of iterations.
	 * 
	 * @param maxIter
	 *            The new maximum iterations.
	 */
	public synchronized void setMaxIterations(int maxIter) {
		if (maxIter < 0) {
			throw new IllegalArgumentException(
					"maxIter must be equal to or larger than 0");
		}

		if (this.maxIter != maxIter) {
			stopDrawing();
			this.maxIter = maxIter;

			// TODO: implement a new render
			// change the colour palette

			startDrawing();
		}
	}

	/**
	 * Gets the maximum radius around the origin after which the
	 * 
	 * @return
	 */
	public synchronized final double getMaxRadius() {
		return maxRadius;
	}

	/**
	 * 
	 * @param maxRadius
	 */
	public synchronized void setMaxRadius(double maxRadius) {
		stopDrawing();
		this.maxRadius = maxRadius;
		startDrawing();
	}

	/**
	 * 
	 * @return
	 */
	public synchronized final float getProgress() {
		return Math.min(1.f, (float) processed.get() / indexes.length);
	}

	/**
	 * 
	 * @return
	 */
	public synchronized final long getRenderingTime() {
		// TODO: implement rendering time
		return renderingTime;
	}

	// ==== Public Methods ====

	/**
	 * 
	 * @param rectangle
	 */
	public synchronized void show(Rectangle rectangle) {
		stopDrawing();

		final double ratio = (double) image.getWidth() / image.getHeight();

		// ensure that everything within the rectangle is shown
		if ((double) rectangle.width / rectangle.height > ratio) {
			final double delta = rectangle.width / ratio - rectangle.height;
			rectangle.y -= delta / 2.;
			rectangle.height += delta;
		} else {
			final double delta = rectangle.height * ratio - rectangle.width;
			rectangle.x -= delta / 2;
			rectangle.width += delta;
		}

		// update Mandelbrot coordinates
		System.out.println(location.getX() + "\t" + location.getY());
		location.setLocation(location.getX() + rectangle.x * scale,
				location.getY() - rectangle.y * scale);
		scale = rectangle.width * scale / image.getWidth();

		// scale image region to provide a fast yet not sharp preview
		BufferedImage s = config.createCompatibleImage(rectangle.width,
				rectangle.height);
		s.getGraphics().drawImage(image, 0, 0, rectangle.width,
				rectangle.height, rectangle.x, rectangle.y,
				rectangle.x + rectangle.width, rectangle.y + rectangle.height,
				null);
		image.getGraphics().drawImage(s, 0, 0, image.getWidth(),
				image.getHeight(), null);

		setChanged();
		notifyObservers();

		startDrawing();
	}
	
	public synchronized Point2D getPoint(int x, int y) {
		Point2D point = new Point2D.Double();
		System.out.println(location.getX() + "\t" + location.getY());
		point.setLocation(location.getX() + x * scale, location.getY() - y * scale);
		
		return point;
	}

	/**
	 * 
	 */
	public synchronized void fit() {
		stopDrawing();

		location = new Point2D.Double(-2.5, 1);
		scale = 1 / 200.;

		show(new Rectangle(0, 0, (int) (3.5 / scale), (int) (2. / scale)));
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param scale
	 */
	public synchronized void scale(int x, int y, double scale) {
		final int width = image.getWidth(), height = image.getHeight();
		
		final double w = width * scale;
		final double h = height * scale;
		final int nx = (int) Math.round((width - w) * x / width);
		final int ny = (int) Math.round((height - h) * y / height);
		
		show(new Rectangle(nx, ny, (int) Math.round(w), (int) Math.round(h)));	
	}
	
	/**
	 * 
	 * @param dx
	 * @param dy
	 */
	public synchronized void translate(int dx, int dy) {
		show(new Rectangle(dx, dy, image.getWidth(), image.getHeight()));
	}

	// ==== ActionListener Implementation ====

	public void actionPerformed(ActionEvent e) {
		// run a timer to update the user on render progress
		if (e.getSource() == timer) {
			setChanged();
			notifyObservers();

			// if any threads are still running return
			for (Thread thread : threads) {
				if (thread.isAlive()) {
					return;
				}
			}

			// stop the timer if all threads are finished
			timer.stop();
		}

	}

	// ==== Private Helper Methods ====
	
	private void updateIndexes() {
		final int total = image.getWidth() * image.getHeight();
		
		// create increasing pixel indexes
		indexes = new int[total]; 
		for (int i = 0; i < total; i++)
			indexes[i] = i;
		
        // apply Knuth shuffle for random permutation
        for (int i = 0; i < total; ++i) {
            int j = (int)(Math.random() * total);

            int t = indexes[i];
            indexes[i] = indexes[j];
            indexes[j] = t;
        }
		
		iterations = new double[total];
		
		firstIndex.set(0);		
	}

	private void stopDrawing() {
		// interrupts all threads
		for (Thread thread : threads) {
			thread.interrupt();
		}

		// wait for all threads to die
		for (Thread thread : threads) {
			while (thread.isAlive()) {
				try {
					thread.join();
				} catch (InterruptedException e) {
				}
			}
		}

		// clear the list of threads
		threads.removeAllElements();

		// stop the timer from firing
		timer.stop();
	}

	private void startDrawing() {
		if (active) {
            firstRun = new CountDownLatch(threadCount);
            firstIndex.set(0);
            secondIndex.set(0);
            processed.set(0);

//            // set histogram bins to zero
//            if (histEqualization) {
//                for (int i = 0; i <= maxIter; ++i) {
//                    histogram[i].set(0);
//                }
//            }
            
			timer.start();
			renderingStart = System.currentTimeMillis();
			
			// spawn new threads to perform the calculations
			for (int i = 0; i < threadCount; i ++) {
				Thread thread = new Calculation();
				thread.start();
				threads.add(thread);
			}
		}
	}
	
	// ==== Calculation Task ====
	
	private class Calculation extends Thread {
		final int width = image.getWidth();
		final int total = indexes.length;

		@Override
		public void run() {
			// compute iterations and eventually store them and histogram info
			firstRun();
			
			// wait until all threads finish the first run
			firstRun.countDown();
			while (firstRun.getCount() > 0 && active()) {
				try {
					firstRun.await(10, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {}
			}
			
            // if hist. equalization enabled, perform second run for coloring
            if (histEqualization && active()) {
                secondRun();
            }
			
            // update rendering time
            renderingTime = System.currentTimeMillis() - renderingStart;
        }
		
		private void firstRun() {
			// consume pixels until exhausted or thread is interrupted
			int idx;
			while ((idx = firstIndex.getAndIncrement()) < total && active()) {
				// 1D to 2D coordinates
				final int xy = indexes[idx];
				final int x = xy % width;
				final int y = xy / width;
				
				// map coordinates into Mandelbrot space
				final double mx = x * scale + location.getX();
				final double my = - y * scale + location.getY();
				
				final int iter = Algorithm.escapeTime(mx, my, maxRadius, maxIter);
				final double fraction = iter % 1;
				
	            image.setRGB(x, y, iter);
	            
	            processed.incrementAndGet();
			}
			
		}
		
		private void secondRun() {
			
		}
		
		private boolean active() {
			return !currentThread().isInterrupted();
		}
	}
}
