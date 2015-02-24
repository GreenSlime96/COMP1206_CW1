import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
	public static void main(String[] args) {
		// run the program within the UI thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final Window window = new Window();
				window.setVisible(true);
				window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);				
			}			
		});
	}
}
