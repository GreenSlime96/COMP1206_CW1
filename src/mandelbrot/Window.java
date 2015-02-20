package mandelbrot;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Window extends JFrame {
	
	
	public Window() {
		super();
		
		setTitle("Mandelbrot Set");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
        final Model model = new Model();

        final Controls controls = new Controls(model);
        final View view = new View(model);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(controls, BorderLayout.EAST);
        panel.add(view, BorderLayout.CENTER);
        setContentPane(panel);

        pack();

        // ensure that the view updates the model size
        view.setVisible(true);
	}

}
