public class Complex {
	private double x, y;

	/**
	 * Constructs a Complex Number in the form z = u + iv
	 * Plots this on an Argand Diagram
	 * 
	 * @param u is the Real part
	 * @param v is the Imaginary part
	 */
	public Complex(double u, double v) {
		x = u;
		y = v;
	}
	
	/**
	 * Returns the real part of the Complex Number
	 * This is represented by the x-axis on the Argand Diagram
	 * 
	 * @return Re[z] where z represents the Complex Number
	 */
	public double getReal() {
		return x;
	}
	
	/**
	 * Returns the imaginary part of the Complex Number
	 * This is represented by the y-axis on the Argand Diagram
	 * 
	 * @return Im[z] where z represents the Complex Number
	 */
	public double getImaginary() {
		return y;
	}
	
	/**
	 * Returns the modulus of the Complex Number
	 * This is the distance of the point from the origin
	 * 
	 * @return |z| where z represents the Complex Number
	 */
	public double getModulus() {
		if (x != 0 || y != 0) {
			return Math.sqrt(x * x + y * y);
		} else {
			return 0;
		}
	}
	
	/**
	 * Returns the modulus squared of this Complex Number
	 * 
	 * @return |z|^2 where z represents the Complex Number
	 */
	public double modulusSquared() {
		return x * x + y * y;
	}
	
	/**
	 * Adds two Complex Numbers together
	 * Creates and returns a new Complex Number
	 * (a + bi) + (c + di) = (a + c) + (b + d)i
	 * 
	 * @param w is the Complex Number to add
	 * @return z + w where z is this Complex Number
	 */
	public Complex add(Complex w) {
		return new Complex(x + w.getReal(), y + w.getImaginary());
	}
	
	/**
	 * Multiplies two Complex Numbers together
	 * Creates and returns a new Complex Number
	 * (a + bi)(c + di) = (ac - bd) + (ad + bc)i 
	 * 
	 * @param w is the Complex Number to add
	 * @return z * w where z is this Complex Number
	 */
	public Complex multiply(Complex w) {
		return new Complex(x * w.getReal() - y * w.getImaginary(), x * w.getImaginary() + y * w.getReal());
	}
	
	/**
	 * Multiplies the current Complex Number by itself
	 * Creates and returns a new Complex Number
	 * 
	 * @return z * z where z is this Complex Number
	 */
	public Complex square() {
		// TODO Optimise this Power Method		
		return this.multiply(this);
	}
}
