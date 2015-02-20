package mandelbrot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;

public class View extends JComponent implements Observer, ActionListener {

    // ==== Constructor ====
    
	/**
	 * 
	 * @param aModel
	 */
    public View(final Model aModel) {
        super();
    }
    
    // ==== ActionListener Implementation ====

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	// ==== Observer Implementation ====

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}

}
