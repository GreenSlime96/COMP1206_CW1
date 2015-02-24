import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// extends Box
public class Controls extends Box implements Observer, ActionListener,
		ChangeListener, ItemListener {

	// ==== Constants ====
	
	private static final long serialVersionUID = -6991007317617413573L;
	
	private final double ZOOM_FACTOR = .6;
	private final double MOVE_FACTOR = .4;

	// ==== Properties ====

	private final Model model;

	private final JSpinner threadsSpinner = new JSpinner(
			new SpinnerNumberModel(Runtime.getRuntime().availableProcessors(),
					1, Runtime.getRuntime().availableProcessors() * 2, 1));
	private final JSpinner fpsSpinner = new JSpinner(new SpinnerNumberModel(25,
			1, 60, 1));
	private final JComboBox<String> algorithmComboBox = new JComboBox<String>(
			new String[] {
					Localization.get("main.algorithm.escape_time"),
					Localization
							.get("main.algorithm.normalized_iteration_count"),
					Localization.get("main.algorithm.burning_ship_fractal") });
	private final JSpinner maxIterSpinner = new JSpinner(
			new SpinnerNumberModel(1000, 0, 10000000, 10));
	private final JSpinner maxRadiusSpinner = new JSpinner(
			new SpinnerNumberModel(2, 0, 100000, 0.1));
	private final JCheckBox histogramCheckBox = new JCheckBox(
			Localization.get("main.histogram.checkbox"));
	private final JCheckBox cardoidCheckBox = new JCheckBox(
			Localization.get("main.cardoidbulb.checkbox"));
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

	public Controls(Model model) {
		super(BoxLayout.Y_AXIS);

		setPreferredSize(new Dimension(280, 800));
		setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 1, 0,
				0, new Color(150, 150, 150)), new EmptyBorder(20, 20, 20, 20)));

		this.model = model;
		model.addObserver(this);

		// add listeners
		threadsSpinner.addChangeListener(this);
		fpsSpinner.addChangeListener(this);
		algorithmComboBox.addActionListener(this);
		maxIterSpinner.addChangeListener(this);
		maxRadiusSpinner.addChangeListener(this);
		histogramCheckBox.addItemListener(this);
		cardoidCheckBox.addItemListener(this);
		leftButton.addActionListener(this);
		rightButton.addActionListener(this);
		upButton.addActionListener(this);
		downButton.addActionListener(this);
		inButton.addActionListener(this);
		outButton.addActionListener(this);
		fitButton.addActionListener(this);

		// settings
		addSetting("main.threads", threadsSpinner);
		add(Box.createRigidArea(new Dimension(0, 15)));
		addSetting("main.fps", fpsSpinner);
		add(Box.createRigidArea(new Dimension(0, 15)));
		addSetting("main.algorithm", algorithmComboBox);
		add(Box.createRigidArea(new Dimension(0, 15)));
		addSetting("main.iter", maxIterSpinner);
		add(Box.createRigidArea(new Dimension(0, 15)));
		addSetting("main.radius", maxRadiusSpinner);
		add(Box.createRigidArea(new Dimension(0, 15)));
		addSetting("main.histogram", histogramCheckBox);
		add(Box.createRigidArea(new Dimension(0, 15)));
		addSetting("main.cardoidbulb", cardoidCheckBox);
		add(Box.createRigidArea(new Dimension(0, 15)));

		// controls
		JPanel moving = new JPanel(new GridLayout(3, 3, 2, 2));
		moving.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		moving.add(outButton);
		moving.add(upButton);
		moving.add(inButton);
		moving.add(leftButton);
		moving.add(fitButton);
		moving.add(rightButton);
		moving.add(createGlue());
		moving.add(downButton);
		moving.add(createGlue());
		addSetting("main.viewport", moving);

		// vertical spacing in a really weird way ..
		add(new JPanel(new GridBagLayout()));

		// rendering time
		renderingLabel.setToolTipText(Localization.get("main.rendering.help"));
		add(renderingLabel);
		add(Box.createRigidArea(new Dimension(0, 8)));

		// progress bar
		progressBar.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		add(progressBar);
	}

	// ==== ActionListener Implementation ====

	@Override
	public void actionPerformed(ActionEvent e) {
		final Object source = e.getSource();
		final Dimension size = model.getSize();

		// fit best
		if (source == fitButton) {
			model.fit();

			// zooming and moving
		} else if (source == inButton || source == outButton) {
			model.scale(size.width / 2, size.height / 2,
					source == inButton ? ZOOM_FACTOR : 1 / ZOOM_FACTOR);

			// moving
		} else if (source == leftButton) {
			model.translate((int) Math.round(-size.width * MOVE_FACTOR), 0);
		} else if (source == rightButton) {
			model.translate((int) Math.round(size.width * MOVE_FACTOR), 0);
		} else if (source == upButton) {
			model.translate(0, (int) Math.round(-size.height * MOVE_FACTOR));
		} else if (source == downButton) {
			model.translate(0, (int) Math.round(size.height * MOVE_FACTOR));

			// algorithm
		} else if (source == algorithmComboBox) {
			if (algorithmComboBox.getSelectedIndex() == 0) {
				model.setAlgorithm(Model.ALGORITHM_ESCAPE_TIME);
			} else if (algorithmComboBox.getSelectedIndex() == 1) {
				model.setAlgorithm(Model.ALGORITHM_NORMALISED_ITERATION_COUNT);
			} else {
				model.setAlgorithm(Model.ALGORITHM_BURNING_SHIP_FRACTAL);
			}
		}
	}

	// ==== ChangeListener Implementation ====

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == threadsSpinner) {
			model.setThreadCount((int) threadsSpinner.getModel().getValue());
		} else if (e.getSource() == fpsSpinner) {
			model.setRefreshRate((int) fpsSpinner.getModel().getValue());
		} else if (e.getSource() == maxIterSpinner) {
			model.setMaxIteration((int) maxIterSpinner.getModel().getValue());
		} else if (e.getSource() == maxRadiusSpinner) {
			model.setMaxRadius((double) maxRadiusSpinner.getModel().getValue());
		}
	}
	
    // ==== ItemListener Implementation ====

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getItemSelectable() == histogramCheckBox) {
            model.setHistogramColouring(histogramCheckBox.isSelected());
        } else if (e.getItemSelectable() == cardoidCheckBox) {
        	model.setCardoibBulbCheck(cardoidCheckBox.isSelected());
        }
    }

	// ==== Observer Implementation ====

	@Override
	public void update(Observable o, Object arg) {
		if (o == model) {
			threadsSpinner.getModel().setValue(model.getThreadCount());
			fpsSpinner.getModel().setValue(model.getRefreshRate());
			algorithmComboBox.setSelectedIndex(model.getAlgorithm());
			maxIterSpinner.getModel().setValue(model.getMaxIteration());
			maxRadiusSpinner.getModel().setValue(model.getMaxRadius());
			histogramCheckBox.setSelected(model.getHistogramColouring());
			cardoidCheckBox.setSelected(model.getCardoidBulbCheck());
			renderingLabel.setText(model.getProgress() < 1.f ? Localization
					.get("main.rendering.title") : String.format(
					Localization.get("main.rendered.title"),
					model.getRenderTime() / 1000.f));
			progressBar.setValue((int) (model.getProgress() * 100));
		}
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
