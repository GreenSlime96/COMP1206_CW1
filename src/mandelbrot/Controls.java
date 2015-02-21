package mandelbrot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Controls extends Box implements Observer, ActionListener,
		ChangeListener, ItemListener {

	// ==== Constants ====

	private final double ZOOM_FACTOR = .6;
	private final double MOVE_FACTOR = .4;

	// ==== Properties ====

	private final Model model;

	private final JSpinner threadsSpinner = new JSpinner(
			new SpinnerNumberModel(Runtime.getRuntime().availableProcessors(), 1, 
					Runtime.getRuntime().availableProcessors() * 2, 1));
	private final JSpinner fpsSpinner = new JSpinner(
			new SpinnerNumberModel(25, 1, 60, 1));
    private final JComboBox<String> algorithmComboBox = new JComboBox<String>(
            new String[] {"main"});
	private final JSpinner maxIterSpinner = new JSpinner(
			new SpinnerNumberModel(1000, 0, 10000000, 10));
	private final JSpinner maxRadiusSpinner = new JSpinner(
			new SpinnerNumberModel(2, 0, 100000, 0.1));
    private final JCheckBox histogramCheckBox = new JCheckBox(
            Localization.get("main.histogram.checkbox"));
    private final JButton leftButton = createControlButton("main.left");
    private final JButton rightButton = createControlButton("main.right");
    private final JButton upButton = createControlButton("main.up");
    private final JButton downButton = createControlButton("main.down");
    private final JButton inButton = createControlButton("main.in");
    private final JButton outButton = createControlButton("main.out");
    private final JButton fitButton = createControlButton("main.fit");
    private final JLabel renderingLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar();
	
	

	// ==== Constructor ====

	/**
	 * 
	 * @param aModel
	 */
	public Controls(Model aModel) {
		super(BoxLayout.Y_AXIS);

		setPreferredSize(new Dimension(280, 800));
		setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 1, 0,
				0, new Color(150, 150, 150)), new EmptyBorder(20, 20, 20, 20)));

		model = aModel;
		model.addObserver(this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub

	}

	// ==== ActionListener Implementation ====

	@Override
	public void actionPerformed(ActionEvent e) {
		final Object source = e.getSource();
		final Dimension size = model.getSize();


	}

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub

	}
	
    // ==== Private Helper Methods ====

    private void addSetting(String key, JComponent control) {
        JLabel label = new JLabel(Localization.get(key + ".title"));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setLabelFor(maxRadiusSpinner);

        control.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JTextArea help = createHelpLabel(Localization.get(key + ".help"));
        help.setFont(label.getFont().deriveFont(Font.ITALIC).deriveFont(10.f));

        add(label);
        add(Box.createRigidArea(new Dimension(0, 3)));
        add(control);
        add(Box.createRigidArea(new Dimension(0, 2)));
        add(help);
    }

    private static JTextArea createHelpLabel(String text) {
        JTextArea textArea = new JTextArea();
        textArea.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(false);
        textArea.setText(text);
        textArea.setMaximumSize(new Dimension(300, 400));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private static JButton createControlButton(String key) {
        JButton button = new JButton(Localization.get(key + ".title"));
        button.setToolTipText(Localization.get(key + ".help"));
        button.setFont(button.getFont().deriveFont(16.f));
        return button;
    }

}
