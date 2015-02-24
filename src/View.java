import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.Timer;

public class View extends JComponent implements Observer, ActionListener {

	// ==== Constants ====

	private static final long serialVersionUID = -8690150230262167823L;
	
	private final double ZOOM_FACTOR = .6;
	private final double PAN_THRESHOLD = 8.;

	// ==== Properties ====

	private final Model model;

	private final Timer timer = new Timer(250, this);
	
	private String complex = "";
	
	private BufferedImage julia = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);

	// ==== Constructor ====

	/**
	 * 
	 * @param aModel
	 */
	public View(final Model aModel) {
		super();

		setPreferredSize(new Dimension((int) (1.25 * 800), 800));
		setVisible(false);

		model = aModel;
		model.addObserver(this);

		timer.setRepeats(false);

		// resizing
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				timer.restart();
			}

			@Override
			public void componentShown(ComponentEvent e) {
				final Timer timer = new Timer(1000, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.setSize(getSize());
						model.fit();
					}
				});
				timer.setRepeats(false);
				timer.start();
			}

		});

		// zooming and panning
		MouseAdapter mouseAdapter = new MouseAdapter() {
			private Point pressed;
			private boolean panning;

			@Override
			public void mousePressed(MouseEvent e) {
				pressed = e.getPoint();
				panning = false;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (panning) {
					panning = false;
					model.setReady(true);
				}
			}
			
			// labeling
			public void mouseMoved(MouseEvent e) {
				if (!panning) {
					Point2D p = model.getPoint(e.getX(), e.getY());	
					complex = p.getX() + " + " + p.getY() + "i";
					
					for(int y = 0; y < 150; y ++) {
						for(int x = 0; x < 200; x ++) {
							double x0 = -2.0 + x * (4.0 / 200);
							double y0 = 1.6 - y * (3.2 / 150);
							
							int iteration = 0;
									
							while (x0 * x0 + y0 * y0 < 4 && iteration < 100) {
								double xt = x0 * x0 - y0 * y0 + p.getX();
								double yt = 2 * x0 * y0 + p.getY();
								
								x0 = xt;
								y0 = yt;
								
								iteration ++;
							}
														
							float hsvFloat = (float) ((double) iteration / 100);						
							int HSVtoRGB = (iteration >= 100) ? Color.BLACK.getRGB() : Color.HSBtoRGB(hsvFloat, 1, 1 - hsvFloat);
							
							julia.setRGB(x, y, HSVtoRGB);
						}
					}
					
					repaint();
				}
			}

			// panning
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!panning) {
					double d = Math.sqrt(Math.pow(pressed.getX() - e.getX(), 2)
							+ Math.pow(pressed.getY() - e.getY(), 2));

					if (d >= PAN_THRESHOLD) {
						panning = true;
						pressed = e.getPoint();
						model.setReady(false);
					}

					return;
				}

				model.translate(pressed.x - e.getX(), pressed.y - e.getY());
				pressed = e.getPoint();
			}

			// zoom on double click
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					model.scale(e.getX(), e.getY(),
							e.getButton() == MouseEvent.BUTTON1 ? ZOOM_FACTOR
									: 1 / ZOOM_FACTOR);
				} else {
					final Point2D point = model.getPoint(e.getPoint());
					double x0 = point.getX(), y0 = point.getY();
					
					double p = Math.sqrt(Math.pow(x0 - (1.0 / 4.0), 2) + y0 * y0);

					if (x0 < (p - 2 * p * p + (1.0 / 4.0))) {
						System.out.println("firstCond");
					}
						
					if (Math.pow(x0 + 1, 2) + (y0 * y0) < (1.0 / 16.0))
						System.out.println("secondCond");
//					System.out.println(Algorithm.escapeTimer(point.getX(), point.getY(), model.getMaxRadius(), model.getMaxIterations()));
				}
			}

			// zoom through scrolling
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				model.scale(e.getX(), e.getY(),
						e.getWheelRotation() < 0 ? ZOOM_FACTOR
								: 1 / ZOOM_FACTOR);
			}
		};
		
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);
		addMouseWheelListener(mouseAdapter);
	}
	
	// ==== JComponent Overrides ====
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		// draw the image
		BufferedImage image = model.getImage();
		int w = Math.min(image.getWidth(), getWidth()),
				h = Math.min(image.getHeight(), getHeight());
		g.drawImage(image, 0, 0, null);
		g.drawImage(julia, 5, 5, null);
		
		// draw the text
		g.setColor(Color.WHITE);
		g.drawRect(5, 5, julia.getWidth(), julia.getHeight());
		g.drawString(complex, (w - g.getFontMetrics().stringWidth(complex)) / 2,
				h - g.getFontMetrics().getHeight());
	}

	// ==== Observer Implementation ====

	@Override
	public void update(Observable o, Object arg) {
		if (o == model) {
			repaint();
		}
	}
	
	// ==== ActionListener Implementation ====

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			model.setSize(getSize());
		}
	}
}
