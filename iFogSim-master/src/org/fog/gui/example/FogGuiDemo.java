package org.fog.gui.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.gui.core.Bridge;
import org.fog.gui.core.Graph;
import org.fog.gui.core.GraphView;

public class FogGuiDemo extends JFrame {
	private static final long serialVersionUID = -2238414769964738933L;

	private JPanel contentPane;

	/** Import file names */
	private String physicalTopologyFile = ""; // physical
	private String applicationFile = "";
	//
	// private JPanel panel;
	private JPanel graph;

	private Graph physicalGraph;
	// private Graph virtualGraph;
	private GraphView physicalCanvas;
	// private GraphView virtualCanvas;

	private JButton btnRun;

	private String mode; // 'm':manual; 'i':import

	public FogGuiDemo() {

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1280, 800));
		// setLocationRelativeTo(null);
		// setResizable(false);

		setTitle("Fog Topology Creator");
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());
		//
		initUI();
		initGraph();
		//
		pack();
		setVisible(true);

	}

	public FogGuiDemo(String location) {
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				System.out.println("clmm");

			}
		});
		this.physicalTopologyFile = location;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(1280, 700));
		setLocationRelativeTo(null);
		// setResizable(false);

		setTitle("Fog Topology Creator");
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());

		initUI();
		initGraph();

		pack();
		setVisible(true);
		Graph phyGraph = Bridge.jsonToGraph(this.physicalTopologyFile, 0);
		physicalGraph = phyGraph;
		physicalCanvas.setGraph(physicalGraph);

		physicalCanvas.repaint();
	}

	public final void initUI() {
		// setUIFont(new javax.swing.plaf.FontUIResource("Serif", Font.BOLD, 18));

		// panel = new JPanel();
		// panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		graph = new JPanel(new java.awt.GridLayout(1, 2));

		initBar();

		doPosition();

	}

	/** position window */
	private void doPosition() {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height;
		int width = screenSize.width;

		int x = (width / 2 - 1280 / 2);
		int y = (height / 2 - 800 / 2);
		// One could use the dimension of the frame. But when doing so, one have to call
		// this method !BETWEEN! pack and
		// setVisible. Otherwise the calculation will go wrong.

		this.setLocation(x, y);
	}

	/** Initialize project menu and tool bar */
	private final void initBar() {
	}

	/** initialize Canvas */
	private void initGraph() {
		physicalGraph = new Graph();
		// virtualGraph = new Graph();

		physicalCanvas = new GraphView(physicalGraph);
		// virtualCanvas = new GraphView(virtualGraph);
		graph.add(physicalCanvas);
		// graph.add(virtualCanvas);
		contentPane.add(graph, BorderLayout.CENTER);

		physicalCanvas.canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();

				Map<String, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> temp = physicalCanvas.getButtonMap();
				for (String s : temp.keySet()) {
					if (x >= temp.get(s).getFirst().getFirst() && x <= temp.get(s).getSecond().getFirst()
							&& y >= temp.get(s).getFirst().getSecond() && y <= temp.get(s).getSecond().getSecond()) {
						if (CloudSim.getEntity(s).getClass() == FogDevice.class) {
							FogDevice fogDevice = (FogDevice) CloudSim.getEntity(s);
							ChangeFogDeviceDialog fogDeviceDialog = new ChangeFogDeviceDialog(fogDevice, physicalGraph,
									FogGuiDemo.this);
						} else if (CloudSim.getEntity(s).getClass() == Sensor.class) {
							Sensor sensor = (Sensor) CloudSim.getEntity(s);
							ChangeSensorDialog sensorDialog = new ChangeSensorDialog(sensor, physicalGraph,
									FogGuiDemo.this);
						} else {

						}

					}
				}
			}
		});

	}

	public GraphView getPhysicalCanvas() {
		return physicalCanvas;
	}

	/**
	 * @param physicalCanvas
	 *            the physicalCanvas to set
	 */
	public void setPhysicalCanvas(GraphView physicalCanvas) {
		this.physicalCanvas = physicalCanvas;
	}

	/** Application entry point */
	public static void main(String args[]) throws InterruptedException {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				FogGuiDemo sdn = new FogGuiDemo();
				sdn.setVisible(true);
			}
		});
	}

}
