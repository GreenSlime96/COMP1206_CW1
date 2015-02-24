import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Observable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Timer;
/**
 * TODO: Implement Colour Palette TODO: Implement Dynamic Precision (float ->
 * double -> BigDecimal(?)) TODO: Implement Historgram Use TODO: Refresh only
 * when necessary
 * TODO: SuperFractalThing (understand code)
 * 
 * @author khengboonpek
 *
 */
public class Model extends Observable implements ActionListener {
	
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

	// create a timer that calls its own ActionListener every 1000ms
	// set the threadCount to a non-final variable defaulting to the number of
	// processors
	// store the array of threads in an ArrayList size threadCount
	// use a CountDownLatch to ensure synchrony between threads
	private final Timer timer = new Timer(1000, this);
	private int threadCount = Runtime.getRuntime().availableProcessors();
	private final ArrayList<Thread> threads = new ArrayList<Thread>(threadCount);
	private CountDownLatch runLatch;

	// indexes used for colouring
	private int[] indexes;
	private double[] iterations;
	private AtomicInteger[] histogram;
	private final AtomicInteger processed = new AtomicInteger();
	private final AtomicInteger firstIndex = new AtomicInteger();
	private final AtomicInteger secondIndex = new AtomicInteger();

	// view-related modelling
	private boolean isReady = true;
	private int refreshRate;
	private int algorithm = 1;
	private int maxIteration = 100;
	private double maxRadius = 2;
	private boolean dragToZoom = true;
	private boolean histogramColouring = false;
	private boolean cardoidBulbCheck = true;
	private boolean shuffleIndexes = true;
	private boolean openCLRendering = false;
	private boolean arbitraryPrecision = false;
	private long renderStart = 0;
	private volatile long renderTime = 0;

	// image and coordinates
	private Point2D point = new Point2D.Double(-2.0, 1.6);
	private double scale = 1 / 200;
	private BufferedImage image;

	// ==== Constructor ====

	public Model() {
		super();
		setReady(false);
		setSize(new Dimension(1, 1));
		setRefreshRate(25);
		setMaxIteration(200);
		setMaxRadius(10);
		setReady(true);
	}

	// ==== Accessors ====

	public synchronized final BufferedImage getImage() {
		return image;
	}

	public synchronized final Dimension getSize() {
		return new Dimension(image.getWidth(), image.getHeight());
	}
	
	public synchronized final boolean getReady() {
		return isReady;
	}
	
	public synchronized final void setReady(boolean isReady) {
		stopDrawing();
		this.isReady = isReady;
		startDrawing();
	}

	public synchronized final void setSize(Dimension size) {
		// trigger if image altered
		if (image == null || image.getWidth() != size.width
				|| image.getHeight() != size.height) {
			stopDrawing();

			// create new BufferedImage with new Dimensions
			BufferedImage newImage = new BufferedImage(size.width, size.height,
					BufferedImage.TYPE_INT_RGB);

			// map old image (if existing) onto the new image
			if (image != null)
				newImage.createGraphics().drawImage(image, 0, 0, size.width,
						size.height, null);

			// update the image reference
			image = newImage;

			// refresh the indexes
			refreshIndexes();

			// start drawing
			startDrawing();
		}
	}

	public synchronized final int getThreadCount() {
		return threadCount;
	}

	public synchronized final void setThreadCount(int threadCount) {
		if (threadCount < 1)
			throw new IllegalArgumentException(
					"threadCount cannot be less than 1");

		// restart drawing if threadCount is different
		if (this.threadCount != threadCount) {
			stopDrawing();
			this.threadCount = threadCount;
			startDrawing();
		}
	}

	public synchronized final int getRefreshRate() {
		return refreshRate;
	}

	public synchronized final void setRefreshRate(int refreshRate) {
		// updates the refresh rate
		this.refreshRate = refreshRate;

		// propagate this change to the timer
		timer.setDelay(1000 / refreshRate);
		timer.setInitialDelay(1000 / refreshRate);
	}

	public synchronized final int getAlgorithm() {
		return algorithm;
	}

	public synchronized final void setAlgorithm(int algorithm) {
		// TODO: implement checks for algorithm validity
		if (this.algorithm != algorithm) {
			stopDrawing();
			this.algorithm = algorithm;
			startDrawing();
		}
	}

	public synchronized final int getMaxIteration() {
		return maxIteration;
	}

	public synchronized final void setMaxIteration(int maxIteration) {
		if (maxIteration < 0)
			throw new IllegalArgumentException(
					"maxIterations cannot be less than 0");

		// restart drawing if maxIterations is different
		if (this.maxIteration != maxIteration) {
			stopDrawing();
			this.maxIteration = maxIteration;
			
			histogram = new AtomicInteger[maxIteration + 1];
            for (int i = 0; i <= maxIteration; ++i) {
                histogram[i] = new AtomicInteger();
            }
			
			startDrawing();
		}
	}

	public synchronized final double getMaxRadius() {
		return maxRadius;
	}

	public synchronized final void setMaxRadius(double maxRadius) {
		if (maxRadius < 0)
			throw new IllegalArgumentException(
					"maxRadius cannot be less than 0");

		// re-render if maxRadius is different
		if (this.maxRadius != maxRadius) {
			stopDrawing();
			this.maxRadius = maxRadius;
			startDrawing();
		}
	}

	public synchronized final boolean getHistogramColouring() {
		return histogramColouring;
	}

	public synchronized final void setHistogramColouring(
			boolean histogramColouring) {
		if (this.histogramColouring != histogramColouring) {
			stopDrawing();
			this.histogramColouring = histogramColouring;
			startDrawing();
		}
	}

	public synchronized final boolean getCardoidBulbCheck() {
		return cardoidBulbCheck;
	}

	public synchronized final void setCardoibBulbCheck(boolean cardoidBulbCheck) {
		// TODO: implement cardoid bulbs for Mandelbrot only
		if (this.cardoidBulbCheck != cardoidBulbCheck) {
			stopDrawing();
			this.cardoidBulbCheck = cardoidBulbCheck;
			startDrawing();
		}
	}

	public synchronized final boolean getShuffleIndexes() {
		return shuffleIndexes;
	}

	public synchronized final void setShuffleIndexes(boolean shuffleIndexes) {
		if (this.shuffleIndexes != shuffleIndexes) {
			stopDrawing();
			this.shuffleIndexes = shuffleIndexes;
			startDrawing();
		}
	}

	public synchronized final float getProgress() {
		// the number of processed indexes over the number of indexes
		// if we are using a histogram, we will have double the indexes to
		// process
		return (float) processed.get() / indexes.length / (histogramColouring ? 2 : 1);
	}

	public synchronized final long getRenderTime() {
		return renderTime;
	}

	// ==== Public Methods ====

	public synchronized void show(Rectangle rectangle) {
		stopDrawing();

		// store the aspect ratio of the image
		final double ratio = (double) image.getWidth() / image.getHeight();

		// if the new rectangle is fatter than the aspect ratio
		if ((double) rectangle.width / rectangle.height > ratio) {
			// the amount to shift the height by
			final double delta = rectangle.width / ratio - rectangle.height;

			// shift the y-position by half the delta to keep image centered
			// increment height by delta to maintain aspect ratio
			rectangle.y -= delta / 2;
			rectangle.height += delta;
		} else {
			// the amount to shift the width by
			final double delta = rectangle.height * ratio - rectangle.width;

			// shift the x-position by half the delta to keep image centered
			// increment the height by delta to maintain the aspect ratio
			rectangle.x -= delta / 2;
			rectangle.width += delta;
		}

		// update the points to the current scale, and updates scale to the
		// ratio of rectangle widths
		point.setLocation(point.getX() + rectangle.x * scale, point.getY()
				- rectangle.y * scale);
		scale = rectangle.width * scale / image.getWidth();

		// scale image to provide a pixelated preview while we render
		BufferedImage newImage = new BufferedImage(rectangle.width,
				rectangle.height, BufferedImage.TYPE_INT_RGB);
		newImage.createGraphics().drawImage(image, 0, 0, rectangle.width,
				rectangle.height, rectangle.x, rectangle.y,
				rectangle.x + rectangle.width, rectangle.y + rectangle.height,
				null);
		image.getGraphics().drawImage(newImage, 0, 0, image.getWidth(),
				image.getHeight(), null);

		setChanged();
		notifyObservers();

		startDrawing();
	}
	
	public synchronized void fit() {
		stopDrawing();
		
		point.setLocation(-2.5, 1);
		scale = 1d / 200d;
		
		show(new Rectangle(0, 0, (int) (3.5 / scale), (int) (2d / scale)));
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
	
	public synchronized Point2D getPoint(int x, int y) {
		Point2D p = new Point2D.Double();
		p.setLocation(point.getX() + x * scale, point.getY() - y
				* scale);

		return p;
	}
	
	public synchronized Point2D getPoint(Point2D p) {
		return getPoint((int) p.getX(), (int) p.getY());
	}

	// ==== Private Helper Methods ====

	private void refreshIndexes() {
		// total number of pixels to process
		final int total = image.getWidth() * image.getHeight();

		// create and populate indexes with values
		indexes = new int[total];
		for (int i = 0; i < total; i++)
			indexes[i] = i;

		if (shuffleIndexes) {
			// use the Fisher-Yates shuffle
			for (int i = 0; i < total; i++) {
				int j = (int) (Math.random() * total);

				int t = indexes[i];
				indexes[i] = indexes[j];
				indexes[j] = t;
			}
		}
		
		iterations = new double[total];
	}

	private void stopDrawing() {
		// interrupt each thread
		for (Thread t : threads) {
			t.interrupt();
		}

		// wait for threads to die
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		}

		// clear list of threads
		threads.clear();

		// no more need to fire events
		timer.stop();
	}

	private void startDrawing() {
		if (isReady) {
			// create a CountDownLatch for the number of threads
			runLatch = new CountDownLatch(threadCount);

			// reset all indexes to 0
			firstIndex.set(0);
			secondIndex.set(0);
			processed.set(0);

			// reset the histograms
			if (histogramColouring) {
                for (int i = 0; i <= maxIteration; ++i) {
                    histogram[i].set(0);
                }
			}

			// start timer and initialise time
			timer.start();
			renderStart = System.currentTimeMillis();
			
			if (cardoidBulbCheck) {
				new GPUThread();
//				final int width = image.getWidth();
//				double[] vars = JOCLAlgorithm.getArray(point.getX(), point.getY(), width, scale, maxRadius, maxIteration);
//
//				for (int i = 0; i < indexes.length; i ++) {
//					final int x = i % width;
//					final int y = i / width;
//					
//					int rgb = Color.HSBtoRGB((float) vars[i] / maxIteration, 1, (float) (1 - vars[i] / maxIteration)); 
//					
//					image.setRGB(x, y, (vars[i] >= maxIteration) ? Color.BLACK.getRGB() : rgb);
//					processed.getAndIncrement();
//				}
//				
//				renderTime = System.currentTimeMillis() - renderStart;
			} else {
				// create threadCount threads and add them to our arraylist
				for (int i = 0; i < threadCount; i++) {
					Thread t = new RenderThread();
					threads.add(t);
					t.start();
				}
			}
		}
	}

	// ==== ActionListener Implementation ====

	@Override
	public void actionPerformed(ActionEvent e) {
		// the only thing that should be firing an event must be the timer
		if (e.getSource() == timer) {
			// tell all observers to refresh
			setChanged();
			notifyObservers();

			// don't stop timer if thread still alive
			for (Thread t : threads) {
				if (t.isAlive())
					return;
			}

			// stop timer as no processing is being done
			timer.stop();
		}
	}
	
	// ==== GPU Thread ====
	
	private class GPUThread {
		final int total = indexes.length;
		final int width = image.getWidth();
		
		
		public GPUThread() {
			boolean useDouble = true;
			
			if (useDouble) {
				// parameters for the real, imaginary and iterations
				double[] iter = new double[total];
				double[] real = new double[total];
				double[] imag = new double[total];
				
				// parameters for other things
				double[] para = new double[5];
				
				para[0] = point.getX();
				para[1] = point.getY();
				para[2] = scale;
				para[3] = width;
				para[4] = total;
				
				OpenCLAlgorithm openCLAlgorithm;
				
				openCLAlgorithm = new OpenCLAlgorithm(para, iter, real, imag, maxRadius, maxIteration);
				openCLAlgorithm.execute(total);
				openCLAlgorithm.dispose();

				for (int i = 0; i < total && isActive(); i ++) {
					final int x = i % width;
					final int y = i / width;

					int rgb = 0;
					
			        if (iter[i] < maxIteration) {
			            double zn_abs = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
			            double u = Math.log(Math.log(zn_abs) / Math.log(maxRadius * maxRadius)) /
			                Math.log(2);
			            iter[i] += 1 - Math.min(u, 1);
			            iter[i] = Math.min(iter[i], maxIteration);
			        }
					
					rgb = Color.HSBtoRGB((float) iter[i] / maxIteration, 1, 1 - (float) iter[i] / maxIteration);				
						
					image.setRGB(x, y, (iter[i] >= maxIteration) ? Color.BLACK.getRGB() : rgb);
					
					// logic for histogram colouring
					if (histogramColouring) {
						iterations[i] = iter[i];
						histogram[(int) Math.floor(iter[i])].incrementAndGet();
					}
					
					processed.incrementAndGet();
				}
			} else {
				// parameters for the real, imaginary and iterations
				float[] iter = new float[total];
				float[] real = new float[total];
				float[] imag = new float[total];
				
				// parameters for other things
				float[] para = new float[5];
				
				para[0] = (float) point.getX();
				para[1] = (float) point.getY();
				para[2] = (float) scale;
				para[3] = width;
				para[4] = total;
				
				FloatAlgorithm floatAlgorithm;
				
				floatAlgorithm = new FloatAlgorithm(para, iter, real, imag, (float) maxRadius, maxIteration);
				floatAlgorithm.execute(total);
				floatAlgorithm.dispose();
				
				for (int i = 0; i < total && isActive(); i ++) {
					final int x = i % width;
					final int y = i / width;
					
					int rgb = 0;
					
			        if (iter[i] < maxIteration) {
			            double zn_abs = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
			            double u = Math.log(Math.log(zn_abs) / Math.log(maxRadius * maxRadius)) /
			                Math.log(2);
			            iter[i] += 1 - Math.min(u, 1);
			            iter[i] = Math.min(iter[i], maxIteration);
			        }
					
					try {
						 rgb = Color.HSBtoRGB((float) iter[i] / maxIteration, 1, 1 - (float) iter[1] / maxIteration);
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println(i + " " + total);
					}					
						
					image.setRGB(x, y, (iter[i] >= maxIteration) ? Color.BLACK.getRGB() : rgb);
					
					// logic for histogram colouring
					if (histogramColouring) {
						iterations[i] = iter[i];
						histogram[(int) Math.floor(iter[i])].incrementAndGet();
					}
					
					processed.incrementAndGet();
				}
			}
			
			if (histogramColouring) {
				secondRun();
			}
			
			renderTime = System.currentTimeMillis() - renderStart;
		}
		
		private void secondRun() {
            // compute the cumulative distribution function
            double cdf[] = new double[maxIteration + 1];
            double t = 0;
            int min = maxIteration, max = 0;
            for (int j = 0; j <= maxIteration && isActive(); ++j) {
                t += Math.pow((double) histogram[j].get() / total, 1d / 4d);
                cdf[j] = t;
            }

            for (int i = 0; i < total; ++i) {
                int x = (int)Math.floor(iterations[i]);
                if (x < min) min = x;
                if (x > max) max = x;
            }
			
			for (int i = 0; i < total; i ++) {
				final double iter = iterations[i];
                final int d = (int)Math.floor(iter);

                // the relative gradient key point
                double r = cdf[d] - (cdf[d] - (d > 0 ? cdf[d-1] : 0)) * (1 - iter % 1);
                r = (r - cdf[min]) / (cdf[max] - cdf[min]);
                r = Math.min(Math.max(r, 0), 1);

                // compute the interpolated color
                double colorIter = r * maxIteration;
				int colour = Color.HSBtoRGB((float) (colorIter / maxIteration), 1, 1 - (float) colorIter / maxIteration);

				int xy = i;
                image.setRGB(xy % width, xy / width, iter >= maxIteration ? Color.BLACK.getRGB() : colour);
				
				processed.getAndIncrement();
			}			
		}
		
		private boolean isActive() {
			// a thread is active if it is not interrupted
			return true;
		}
	}

	// ==== Rendering Thread ====

	private class RenderThread extends Thread {
		final int total = indexes.length;
		final int width = image.getWidth();

		@Override
		public void run() {
			firstRun();

			// decrements the CountDownLatch
			runLatch.countDown();
			while (runLatch.getCount() > 0 && isActive()) {
				try {
					runLatch.await(10, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
				}
			}

			if (histogramColouring && isActive()) {
				secondRun();
			}

			// update rendering time at completion of thread
			renderTime = System.currentTimeMillis() - renderStart;
		}

		private void firstRun() {
			int index;			

			while ((index = firstIndex.getAndIncrement()) < total && isActive()) {
				// initialise iter to -1 as uninitialised
				double iter = -1;

				// converting indexes to coordinates
				final int x = indexes[index] % width;
				final int y = indexes[index] / width;

				// convert index to points on the plane
				// TODO: implement arbitrary precision
				final double cx = point.getX() + x * scale;
				final double cy = point.getY() - y * scale;

				// TODO: every new algorithm needs a check against this
				if (cardoidBulbCheck && algorithm != 3) {
					double p = Math.sqrt(Math.pow(cx - (1d / 4d), 2) + cy * cy);

					// cardoid bulb checking courtesy Wikipedia
					if (cx < (p - 2 * p * p + (1d / 4d))
							|| Math.pow(cx + 1, 2) + (cy * cy) < (1d / 16d))
						iter = maxIteration;
				}

				if (iter == -1) {
					switch (algorithm) {
					case ALGORITHM_ESCAPE_TIME:
						iter = Algorithm.escapeTime(cx, cy, maxRadius,
								maxIteration);
						break;
					case ALGORITHM_NORMALISED_ITERATION_COUNT:
						iter = Algorithm.normalisedIterationCount(cx, cy,
								maxRadius, maxIteration);
						break;
					case ALGORITHM_BURNING_SHIP_FRACTAL:
						iter = Algorithm.burningShipFractal(cx, cy, maxRadius,
								maxIteration);
						break;
					}
				}

				// TODO: implement palette selection
				int colour = Color.HSBtoRGB((float) (iter / maxIteration), 1, 1
						- (float) iter / maxIteration);

				// if maximum iteration reached, always black
				image.setRGB(x, y, iter >= maxIteration ? Color.BLACK.getRGB()
						: colour);

				// logic for histogram colouring
				if (histogramColouring) {
					iterations[index] = iter;
					histogram[(int) Math.floor(iter)].incrementAndGet();
				}

				processed.incrementAndGet();
			}
		}
		
		private void secondRun() {
            // compute the cumulative distribution function
            double cdf[] = new double[maxIteration + 1];
            double t = 0;
            int min = maxIteration, max = 0;
            for (int j = 0; j <= maxIteration && isActive(); ++j) {
                t += Math.pow((double) histogram[j].get() / total, 1d / 4d);
                cdf[j] = t;
            }

            for (int i = 0; i < total; ++i) {
                int x = (int)Math.floor(iterations[i]);
                if (x < min) min = x;
                if (x > max) max = x;
            }
			
			int index;			
			while ((index = secondIndex.getAndIncrement()) < total && isActive()) {
				final double iter = iterations[index];
                final int d = (int)Math.floor(iter);

                // the relative gradient key point
                double r = cdf[d] - (cdf[d] - (d > 0 ? cdf[d-1] : 0)) * (1 - iter % 1);
                r = (r - cdf[min]) / (cdf[max] - cdf[min]);
                r = Math.min(Math.max(r, 0), 1);

                // compute the interpolated color
                double colorIter = r * maxIteration;
				int colour = Color.HSBtoRGB((float) (colorIter / maxIteration), 1, 1 - (float) colorIter / maxIteration);
				
//				t = 0;
//				for (int i = 0; i < maxIteration; i++) {
//					t += histogram[i].get();
//				}
//				
//				double hue = 0;
//				for (int i = 0; i < iter; i ++) {
//					hue += (histogram[i].get() / t);
//				}
//				
//				double hue2 = 0;
//				for (int i = 0; i < iter - 1; i ++) {
//					hue2 += (histogram[i].get() / t);
//				}
//				
//				double hue1 = hue - (hue - hue2) * (1 - iter % 1);
//							
//				//hue = hue - (hue - (histogram[(int) Math.floor(iter - 1)].get() / t)) * (1 - iter % 1);
//				int colour = Color.HSBtoRGB((float) hue1, 1, 1 - (float) hue1);

				int xy = indexes[index];
                image.setRGB(xy % width, xy / width, iter >= maxIteration ? Color.BLACK.getRGB() : colour);
				
				processed.getAndIncrement();
			}
		
//			if (threadCount == 1) {
//				System.out.println(blah.size());
//				for (Double d : blah) {
//					System.out.println(d);
//				}
//			}
			
		}

		// used for future methods where we want to be able to
		// interrupt the thread
		private boolean isActive() {
			// a thread is active if it is not interrupted
			return !currentThread().isInterrupted();
		}
	}

}
