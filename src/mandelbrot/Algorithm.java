package mandelbrot;

import java.awt.Color;

/**
 * 
 * @author khengboonpek
 *
 */
public class Algorithm {
	
	/**
	 * Returns a colour
	 * 
	 * @param x
	 * @param y
	 * @param maxRadius
	 * @param maxIter
	 * @return
	 */
	public static int escapeTime(double x, double y, double maxRadius, int maxIter) {
		double x0 = x;
		double y0 = y;
		int iteration = 0;
		double maxRadiusSquared = maxRadius * maxRadius;
		
		// compute sequence terms until one "escapes"
		while (x * x + y * y < maxRadiusSquared && iteration < maxIter) {
			double xt = x * x - y * y + x0;
			double yt = 2 * x * y + y0;
			
			x = xt;
			y = yt;
			
			iteration += 1;
		}
		
		double smoothColouring = iteration - Math.log(Math.log(Math.sqrt(x * x + y * y))) / Math.log(2);
		float hsvFloat = (float) (smoothColouring / maxIter);						
		int HSVtoRGB = (iteration >= maxIter) ? Color.BLACK.getRGB() : Color.HSBtoRGB(hsvFloat, 1, 1 - hsvFloat);
				
		return HSVtoRGB;
	}
	
	public static int burningShip(double x, double y, double maxRadius, int maxIter) {
		double x0 = x;
		double y0 = y;
		int iteration = 0;
		double maxRadiusSquared = maxRadius * maxRadius;
		
		// compute sequence terms until one "escapes"
		while (x * x + y * y < maxRadiusSquared && iteration < maxIter) {            
			double xt = x * x - y * y - x0;
			double yt = 2 * Math.abs(x * y) - y0;
			
			x = xt;
			y = yt;
			
			iteration += 1;
		}
		
		double smoothColouring = iteration - Math.log(Math.log(Math.sqrt(x * x + y * y))) / Math.log(2);
		float hsvFloat = (float) (smoothColouring / maxIter);						
		int HSVtoRGB = iteration >= maxIter ? Color.BLACK.getRGB() : Color.HSBtoRGB(hsvFloat, 1, 1 - hsvFloat);
				
		return HSVtoRGB;
	}

}
