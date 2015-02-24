public class Algorithm {

	public static int escapeTime(double x, double y, double maxRadius, int maxIter) {
		double x0 = x;
		double y0 = y;
		int iteration = 0;
		double maxRadiusSquared = maxRadius * maxRadius;
		
		// compute sequence terms until one "escapes"
		while (x * x + y * y < maxRadiusSquared && iteration < maxIter) {
			double xt = x * x - y * y + x0;
			double yt = 2 * x * y + y0;
			
			// implement wikipedia's periodic checking
			if (x == xt && y == yt) {
				iteration = maxIter;
				break;
			}
			
			x = xt;
			y = yt;
			
			iteration += 1;
		}
						
		return iteration;
	}
	
	public static double normalisedIterationCount(double x, double y, double maxRadius, int maxIter) {
		double x0 = x;
		double y0 = y;
		double iteration = 0;
		double maxRadiusSquared = maxRadius * maxRadius;
		
		// compute sequence terms until one "escapes"
		while (x * x + y * y < maxRadiusSquared && iteration < maxIter) {
			double xt = x * x - y * y + x0;
			double yt = 2 * x * y + y0;
			
			// implement Wikipedia's periodic checking
			if (x == xt && y == yt) {
				iteration = maxIter;
				break;
			}
			
			x = xt;
			y = yt;
			
			iteration += 1;
		}
		
        if (iteration < maxIter) {
            double zn_abs = Math.sqrt(x * x + y * y);
            double u = Math.log(Math.log(zn_abs) / Math.log(maxRadiusSquared)) /
                Math.log(2);
            iteration += 1 - Math.min(u, 1);
        }
        
		return Math.min(iteration, maxIter);
	}
	
	public static int burningShipFractal(double x, double y, double maxRadius, int maxIter) {
		double x0 = x;
		double y0 = y;
		int iteration = 0;
		double maxRadiusSquared = maxRadius * maxRadius;
		
		// compute sequence terms until one "escapes"
		while (x * x + y * y < maxRadiusSquared && iteration < maxIter) {            
			double xt = x * x - y * y - x0;
			double yt = 2 * Math.abs(x * y) - y0;
			
			// implement wikipedia's periodic checking
			if (x == xt && y == yt) {
				iteration = maxIter;
				break;
			}
			
			x = xt;
			y = yt;
			
			iteration += 1;
		}
		
		return 0;
	}
	
//	public static int bigDecimalEscapeTime(double x, double y, double maxRadius, int maxIter) {
//		BigDecimal x1 = BigDecimal.valueOf(x);
//		BigDecimal y1 = BigDecimal.valueOf(y);
//		
//		BigDecimal x0 = BigDecimal.valueOf(x);
//		BigDecimal y0 = BigDecimal.valueOf(y);
//		
//		BigDecimal x2 = x1.multiply(x1);
//		BigDecimal y2 = y1.multiply(y1);
//		
//		int iteration = 0;
//		double maxRadiusSquared = maxRadius * maxRadius;
//		
//		BigDecimal mR = BigDecimal.valueOf(maxRadiusSquared);
//		
//		while (x2.add(y2).compareTo(mR) < 0 && iteration < maxIter) {
//			x2 = x1.multiply(x1).setScale(20, BigDecimal.ROUND_HALF_UP);
//			y2 = y1.multiply(y1).setScale(20, BigDecimal.ROUND_HALF_UP);
//			
//			BigDecimal xt = x2.subtract(y2).add(x0).setScale(20, BigDecimal.ROUND_HALF_UP);
//
//			y1 = BigDecimal.valueOf(2).multiply(x1).multiply(y1).add(y0).setScale(20, BigDecimal.ROUND_HALF_UP);
//			x1 = xt;
//			
//			iteration += 1;
//		}
//		
//		return iteration;
//	}	
}
