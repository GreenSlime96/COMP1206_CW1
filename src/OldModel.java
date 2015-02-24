import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Timer;

/**
 * My go at implementing MVC for the Mandelbrot set.
 * 
 * NOTES: Vector > ArrayList bc. synchronisation
 * 
 * TODO: Comment other methods
 * 
 * @author khengboonpek
 *
 */
public class OldModel extends Observable implements ActionListener {

	// ==== Constants ====

	/**
	 * Escape Time Algorithm
	 */
	public static final int ALGORITHM_ESCAPE_TIME = 0;

	/**
	 * Normalized Iteration Count Algorithm
	 */
	public static final int ALGORITHM_NORMALISED_ITERATION_COUNT = 1;

	/**
	 * Burning Ship Fractal Algorithm
	 */
	public static final int ALGORITHM_BURNING_SHIP_FRACTAL = 2;

	// ==== Properties ====

	// create Timer to fire events every x milliseconds, used to generate image
	// user-configurable threadCount which defaults to the number of processedors
	// store threads in an ArrayList whose default size is the threadCount
	// use a CountDownLatch to make sure all threads finish at the same time
	private final Timer timer = new Timer(1000, this);
	private int threadCount = Runtime.getRuntime().availableProcessors();
	private final ArrayList<Thread> threads = new ArrayList<Thread>(threadCount);
	private CountDownLatch firstRun;

	// indexes the pixels on the BufferedImage, size of the array will be pixel count
	// store the number of processed pixels in a thread safe AtomicInteger()
    private int[] indexes;
    private double[] iterations;
    private AtomicInteger[] histogram;
	private final AtomicInteger processed = new AtomicInteger();
	private final AtomicInteger firstIndex = new AtomicInteger();
	private final AtomicInteger secondIndex = new AtomicInteger();

	private boolean active = true;
	private Point2D location = new Point2D.Double(-2.5, 1);
	private double scale = 1 / 200.;
	private int fps = 25;
	private int algorithm = ALGORITHM_NORMALISED_ITERATION_COUNT;
	private int maxIter = 1000;
	private double maxRadius = 2;
    private boolean useHistogram = false;
	private boolean cardoidBulbCheck = true;
	private long renderingTime = 0;
	private long renderingStart = 0;

	private BufferedImage image;

	// ==== Constructor ====

	public OldModel() {
		super();
		setActive(false);
		setSize(new Dimension(1, 1));
		setFps(25);
		setMaxIterations(200);
		setMaxRadius(10);
		setActive(true);
	}

	// ==== Accessors ====

	public synchronized final BufferedImage getImage() {
		return image;
	}

	public synchronized final boolean isActive() {
		return active;
	}

	public synchronized void setActive(boolean active) {
		stopDrawing();
		this.active = active;
		startDrawing();
	}

	public synchronized final int getThreadCount() {
		return threadCount;
	}

	public synchronized void setThreadCount(int threadCount) {
		if (threadCount < 1)
			throw new IllegalArgumentException("threadCount must be larger than zero");

		if (this.threadCount != threadCount) {
			stopDrawing();
			this.threadCount = threadCount;
			startDrawing();
		}
	}

	public synchronized final Dimension getSize() {
		// returns the image height and width
		return new Dimension(image.getWidth(), image.getHeight());
	}

	public synchronized void setSize(Dimension size) {
		// trigger if the image has been altered
		if (image == null || size.width != image.getWidth()	|| size.height != image.getHeight()) {
			stopDrawing();

			// create a new BufferedImage with the new dimensions
			BufferedImage newImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

			// resize the previous image onto the current one
			if (image != null) {
				newImage.getGraphics().drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
			}

			// update the reference to the image
			image = newImage;

			// since the image is resized, update the indexes
			updateIndexes();

			// update the image
			startDrawing();
		}
	}

	public synchronized final int getFps() {
		return fps;
	}

	public synchronized void setFps(int fps) {
		this.fps = fps;
		timer.setDelay(1000 / fps);
	}

	public synchronized final int getAlgorithm() {
		return algorithm;
	}

	public synchronized void setAlgorithm(int algorithm) {
		if (algorithm != ALGORITHM_ESCAPE_TIME
				&& algorithm != ALGORITHM_NORMALISED_ITERATION_COUNT
				&& algorithm != ALGORITHM_BURNING_SHIP_FRACTAL) {
			throw new IllegalArgumentException("algorithm not recognised");
		}

		if (this.algorithm != algorithm) {
			stopDrawing();
			this.algorithm = algorithm;
			startDrawing();
		}
	}

	public synchronized final int getMaxIterations() {
		return maxIter;
	}

	public synchronized void setMaxIterations(int maxIter) {
		// can't have a maxIter value less than 0
		if (maxIter < 0) {
			throw new IllegalArgumentException("maxIter has to be greater than 0");
		}

		// if the maximum iterations have changed we need to update the image
		if (this.maxIter != maxIter) {
			stopDrawing();
			this.maxIter = maxIter;
			
            histogram = new AtomicInteger[maxIter + 1];
            for (int i = 0; i <= maxIter; ++i) {
                histogram[i] = new AtomicInteger();
            }
			
			startDrawing();
		}
	}

	public synchronized final double getMaxRadius() {
		return maxRadius;
	}

	public synchronized void setMaxRadius(double maxRadius) {
		if (this.maxRadius != maxRadius) {
			stopDrawing();
			this.maxRadius = maxRadius;
			startDrawing();
		}
	}
	
	public synchronized final boolean getHistogramUse() {
		return useHistogram;
	}
	
	public synchronized void setHistogramUse(boolean useHistogram) {
		if (this.useHistogram != useHistogram) {
			stopDrawing();
			this.useHistogram = useHistogram;
			startDrawing();
		}
	}
	
	public synchronized final boolean getCardoidBulbCheck() {
		return cardoidBulbCheck;
	}
	
	public synchronized void setCardoidBulbCheck(boolean cardoidBulbCheck) {
		if (this.cardoidBulbCheck != cardoidBulbCheck) {
			stopDrawing();
			this.cardoidBulbCheck = cardoidBulbCheck;
			startDrawing();
		}
	}

	public synchronized final float getProgress() {
		// the number of progressed items over the total of items to process		
		return (float) processed.get() / indexes.length / (useHistogram ? 2 : 1);
	}

	public synchronized final long getRenderingTime() {
		return renderingTime;
	}

	// ==== Public Methods ====

	public synchronized void show(Rectangle rectangle) {
		stopDrawing();

		final double ratio = (double) image.getWidth() / image.getHeight();

		// ensure that everything within the rectangle is shown
		if ((double) rectangle.width / rectangle.height > ratio) {
			System.out.println("here");
			final double delta = rectangle.width / ratio - rectangle.height;
			rectangle.y -= delta / 2.;
			rectangle.height += delta;
		} else {
			System.out.println("there");
			final double delta = rectangle.height * ratio - rectangle.width;
			rectangle.x -= delta / 2;
			rectangle.width += delta;
		}

		// update Mandelbrot coordinates
		location.setLocation(location.getX() + rectangle.x * scale,
				location.getY() - rectangle.y * scale);
		scale = rectangle.width * scale / image.getWidth();

		// scale image region to provide a fast yet not sharp preview
		BufferedImage s = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_INT_RGB);
		s.createGraphics().drawImage(image, 0, 0, rectangle.width,
				rectangle.height, rectangle.x, rectangle.y,
				rectangle.x + rectangle.width, rectangle.y + rectangle.height,
				null);
		image.getGraphics().drawImage(s, 0, 0, image.getWidth(), image.getHeight(), null);

		setChanged();
		notifyObservers();

		startDrawing();
	}

	public synchronized Point2D getPoint(int x, int y) {
		Point2D p = new Point2D.Double();
		p.setLocation(location.getX() + x * scale, location.getY() - y
				* scale);

		return p;
	}
	
	public synchronized Point2D getPoint(Point2D p) {
		return getPoint((int) p.getX(), (int) p.getY());
	}

	public synchronized void fit() {
		stopDrawing();

		location = new Point2D.Double(-2.5, 1);
		scale = 1 / 200.;

		show(new Rectangle(0, 0, (int) (3.5 / scale), (int) (2. / scale)));
	}

	public synchronized void scale(int x, int y, double scale) {
		final int width = image.getWidth(), height = image.getHeight();

		final double w = width * scale;
		final double h = height * scale;
		final int nx = (int) Math.round((width - w) * x / width);
		final int ny = (int) Math.round((height - h) * y / height);

		show(new Rectangle(nx, ny, (int) Math.round(w), (int) Math.round(h)));
	}

	public synchronized void translate(int dx, int dy) {
		show(new Rectangle(dx, dy, image.getWidth(), image.getHeight()));
	}

	// ==== ActionListener Implementation ====

	public void actionPerformed(ActionEvent e) {
		// events are fired by the timer depending on refresh rate
		if (e.getSource() == timer) {
			setChanged();
			notifyObservers();

			// if any threads are still running return
			// don't want to prematurely stop the timer
			for (Thread thread : threads) {
				if (thread.isAlive()) {
					return;
				}
			}

			// stop the timer if all threads are finished to prevent
			// any more update events from being fired and sent to the observer
			timer.stop();
		}

	}

	// ==== Private Helper Methods ====

	private void updateIndexes() {
		// the total number of indexes would be the number of pixels
		final int total = image.getWidth() * image.getHeight();

		// create a new index containing references to all the pixels
		indexes = new int[total];
		for (int i = 0; i < total; i++)
			indexes[i] = i;

		// shuffles the index to give the illusion of speed
		for (int i = 0; i < total; ++i) {
			int j = (int) (Math.random() * total);

			int t = indexes[i];
			indexes[i] = indexes[j];
			indexes[j] = t;
		}
		
		iterations = new double[total];
	}

	private void stopDrawing() {
		// tells all threads to interrupt
		for (Thread thread : threads) {
			thread.interrupt();
		}

		// wait for all threads to die
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		// since all threads have died, clear the list
		threads.clear();

		// stop the timer from firing, drawing done
		timer.stop();
	}

	private void startDrawing() {
		if (active) {
			// create and set countdown for threads
			firstRun = new CountDownLatch(threadCount);
			
			// reset indexes to zero
			firstIndex.set(0);
			secondIndex.set(0);			
			processed.set(0);
			
			System.out.println(1 / scale);
			
            // set histograms to zero
            if (useHistogram) {
                for (int i = 0; i <= maxIter; ++i) {
                    histogram[i].set(0);
                }
            }

			timer.start();
			renderingStart = System.currentTimeMillis();

			// spawn new threads to perform the calculations
			for (int i = 0; i < threadCount; i++) {
				Thread thread = new Calculation();
				thread.start();
				threads.add(thread);
			}
		}
	}

	// ==== Calculation Task ====

	private class Calculation extends Thread {
		final int total = indexes.length;
		final int width = image.getWidth();

		@Override
		public void run() {
			// calculate iterations and colour pixels
			firstRun();			
			
			firstRun.countDown();
            while (firstRun.getCount() > 0 && isActive()) {
                try {
                    firstRun.await();
                } catch (InterruptedException e) {
                	System.out.println("interrupted");
                }
            }
			
			if (useHistogram && isActive()) {
				secondRun();
			}	

			// update rendering time
			renderingTime = System.currentTimeMillis() - renderingStart;
		}
		
		private void firstRun() {
			int index;
			while ((index = firstIndex.getAndIncrement()) < total && isActive()) {
				// converting indexes to coordinates
				final int xy = indexes[index];
				final int x = xy % width;
				final int y = xy / width;

				// map coordinates into Mandelbrot space
				final double mx = location.getX() + x * scale;
				final double my = location.getY() - y * scale;

				final double iter;
				
				if (cardoidBulbCheck) {
					double p = Math.sqrt(Math.pow(mx - (1d / 4d), 2) + my * my);
	
					if (mx < (p - 2 * p * p + (1d / 4d))) {
						iter = maxIter;
					} else if (Math.pow(mx + 1, 2) + (my * my) < (1d / 16d)) {
						iter = maxIter;
					} else {
						switch (algorithm) {
						case ALGORITHM_ESCAPE_TIME:
							iter = Algorithm.escapeTime(mx, my, maxRadius, maxIter);
							break;
						case ALGORITHM_NORMALISED_ITERATION_COUNT:
							iter = Algorithm.normalisedIterationCount(mx, my,
									maxRadius, maxIter);
							break;
						case ALGORITHM_BURNING_SHIP_FRACTAL:
							iter = Algorithm.burningShipFractal(mx, my, maxRadius,
									maxIter);
							break;
						default:
							iter = maxIter;
							break;
						}
					}
				} else {
					switch (algorithm) {
					case ALGORITHM_ESCAPE_TIME:
						iter = Algorithm.escapeTime(mx, my, maxRadius, maxIter);
						break;
					case ALGORITHM_NORMALISED_ITERATION_COUNT:
						iter = Algorithm.normalisedIterationCount(mx, my, maxRadius, maxIter);
						break;
					case ALGORITHM_BURNING_SHIP_FRACTAL:
						iter = Algorithm.burningShipFractal(mx, my, maxRadius, maxIter);
						break;
					default:
						iter = maxIter;
						break;
					}
				}
				
				int colour = Color.HSBtoRGB((float) (iter / maxIter), 1, 1 - (float) iter / maxIter);

				image.setRGB(x, y, iter >= maxIter ? Color.BLACK.getRGB() : colour);
				
                if (useHistogram) {
                    iterations[index] = iter;
                    histogram[(int) Math.floor(iter)].incrementAndGet();
                }
                
                processed.incrementAndGet();
			}
		}
		
        private void secondRun() {
            // compute the cumulative distribution function
            double cdf[] = new double[maxIter + 1];
            double t = 0;
            int min = maxIter, max = 0;
            for (int j = 0; j <= maxIter && isActive(); ++j) {
                t += Math.pow((double)histogram[j].get()/total, 1/4.);
                cdf[j] = t;
            }
           

            for (int i = 0; i < total; ++i) {
                int x = (int)Math.floor(iterations[i]);
                if (x < min) min = x;
                if (x > max) max = x;
            }

            int idx;
            while ((idx = secondIndex.getAndIncrement()) < total && isActive()) {
                final double iter = iterations[idx];
                final int d = (int)Math.floor(iter);

                // the relative gradient key point
                double r = cdf[d] - (cdf[d] - (d > 0 ? cdf[d-1] : 0)) * (1 - iter % 1);
                r = (r - cdf[min]) / (cdf[max] - cdf[min]);
                r = Math.min(Math.max(r, 0), 1);

                // compute the interpolated color
                double colorIter = r * maxIter;
				int colour = Color.HSBtoRGB((float) (colorIter / maxIter), 1, 1 - (float) colorIter / maxIter);

                // write color
                final int xy = indexes[idx];
                image.setRGB(xy % width, xy / width,
                    iter >= maxIter ? 0xff000000 : colour);

                processed.incrementAndGet();
            }
        }
		
		// used for future methods where we want to be able to
		// interrupt the thread
		private boolean isActive() {
			// a thread is active if it is not interrupted
			return !currentThread().isInterrupted();
		}
	}
}
