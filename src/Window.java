import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class Window extends JFrame {
	
	public Window() {
		super();
		
		setTitle("Mandelbrot Set");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
        final Model model = new Model();
        
        final Controls controls = new Controls(model);
        final View view = new View(model);
        
        final JScrollPane scroll = new JScrollPane(controls);
        scroll.setPreferredSize(new Dimension(400, 800));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(view, BorderLayout.CENTER);
        panel.add(scroll, BorderLayout.EAST);
        setContentPane(panel);

        pack();

        view.setVisible(true);
	}

}
