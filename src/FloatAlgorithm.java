import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import com.amd.aparapi.Kernel;

public class FloatAlgorithm extends Kernel {
	// Complex Number representations
	private float[] iter;
	private float[] real;
	private float[] imag;
	
	// parameters
	private float[] para;
	
	private float maxRadiusSquared;
	private int maxIteration;
	
	AtomicInteger processed;
	BufferedImage image;
	
	public FloatAlgorithm(float[] para, float[] iter, float[] real, float[] imag, float maxRadius, int maxIteration) { 	
		this.para = para;
		
		this.iter = iter;
		this.real = real;
		this.imag = imag;
		
		this.maxIteration = maxIteration;
		this.maxRadiusSquared = maxRadius * maxRadius;
	}
	
	public void getCount(float x, float y, int gid) {
		float x0 = x;
		float y0 = y;
		int iteration = 0;

		while (x * x + y * y < maxRadiusSquared && iteration < maxIteration) {
			float xt = x * x - y * y + x0;
			float yt = 2 * x * y + y0;
			
//			// implement wikipedia's periodic checking
//			if (x == xt && y == yt) {
//				iteration = maxIteration;
//				break;
//			}
			
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
		
		float x0 = para[0] + x * para[2];
		float y0 = para[1] - y * para[2];
		
		getCount(x0, y0, gid);
	}
}
