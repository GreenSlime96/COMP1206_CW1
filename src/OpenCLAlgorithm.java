import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import com.amd.aparapi.Kernel;

public class OpenCLAlgorithm extends Kernel {
	// Complex Number representations
	private double[] iter;
	private double[] real;
	private double[] imag;
	
	// parameters
	private double[] para;
	
	private double maxRadiusSquared;
	private int maxIteration;
	
	AtomicInteger processed;
	BufferedImage image;
	
	public OpenCLAlgorithm(double[] para, double[] iter, double[] real, double[] imag, double maxRadius, int maxIteration) { 	
		this.para = para;
		
		this.iter = iter;
		this.real = real;
		this.imag = imag;
		
		this.maxIteration = maxIteration;
		this.maxRadiusSquared = maxRadius * maxRadius;
	}
	
	public void getCount(double x, double y, int gid) {
		double x0 = x;
		double y0 = y;
		int iteration = 0;

		while (x * x + y * y < maxRadiusSquared && iteration < maxIteration) {
			double xt = x * x - y * y + x0;
			double yt = 2 * x * y + y0;
			
//			// implement wikipedia's periodic checking
			if (x == xt && y == yt) {
				iteration = maxIteration;
			}
			
			x = xt;
			y = yt;
			
			iteration ++;
		}
		
		//System.out.println(iteration);
		
		iter[gid] = iteration;
		real[gid] = x;
		imag[gid] = y;	
	}


	@Override
	public void run() {
		int gid = getGlobalId();
		
		int x = gid % (int) para[3];
		int y = gid / (int) para[3];
		
		double x0 = para[0] + x * para[2];
		double y0 = para[1] - y * para[2];
		
		getCount(x0, y0, gid);
	}
}
