/**
 * 
 */
package org.fog.gui.example;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;

/**
 * @author tptha
 *
 */
public class TopologyGraph {
	private Graph graph;
	private SpriteManager sman;

	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;

	public TopologyGraph(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {

		this.setFogDevices(fogDevices);
		this.setSensors(sensors);
		this.setActuators(actuators);
		graph = new MultiGraph(name);

		graph.setAutoCreate(true);
		graph.setStrict(false);
		graph.addAttribute("ui.antialias");
		graph.addAttribute("ui.quality");
		sman = new SpriteManager(graph);

		// viewer.enableAutoLayout();

		createNode(fogDevices, sensors, actuators);
		// graph.display();
		Viewer viewer = graph.display(true);
		// HierarchicalLayout hl = new HierarchicalLayout();
		// viewer.enableAutoLayout(hl);
	}

	/**
	 * @param fogDevices
	 * @param sensors
	 * @param actuators
	 */
	private void createNode(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
		// TODO Auto-generated method stub
		List<List<Integer>> paths = getLeafToRootPaths();
		for (List<Integer> path : paths) {
			for (int fogDeviceId : path) {
				// add node
				FogDevice device = (FogDevice) CloudSim.getEntity(fogDeviceId);

				graph.addNode(device.getName());
				Node node = graph.getNode(device.getName());
				node.addAttribute("ui.label", device.getName());

				if (!device.getName().equals("cloud")) {
					// add parent node
					int parentId = device.getParentId();
					FogDevice parentDevice = (FogDevice) CloudSim.getEntity(parentId);
					Node parentNode;
					if (graph.getNode(parentDevice.getName()) != null) {
						graph.addNode(parentDevice.getName());
						parentNode = graph.getNode(parentDevice.getName());
						parentNode.addAttribute("ui.label", parentDevice.getName());
					} else
						parentNode = graph.getNode(parentDevice.getName());

					// add egde
					graph.addEdge(parentDevice.getName() + "->" + device.getName(), parentDevice.getName(),
							device.getName(), true);
				}
			}
		}
		System.out.print("tABC");

	}

	protected List<List<Integer>> getPaths(final int fogDeviceId) {
		FogDevice device = (FogDevice) CloudSim.getEntity(fogDeviceId);
		if (device.getChildrenIds().size() == 0) {
			final List<Integer> path = (new ArrayList<Integer>() {
				{
					add(fogDeviceId);
				}
			});
			List<List<Integer>> paths = (new ArrayList<List<Integer>>() {
				{
					add(path);
				}
			});
			return paths;
		}
		List<List<Integer>> paths = new ArrayList<List<Integer>>();
		for (int childId : device.getChildrenIds()) {
			List<List<Integer>> childPaths = getPaths(childId);
			for (List<Integer> childPath : childPaths)
				childPath.add(fogDeviceId);
			paths.addAll(childPaths);
		}
		return paths;
	}

	protected List<List<Integer>> getLeafToRootPaths() {
		FogDevice cloud = null;
		for (FogDevice device : getFogDevices()) {
			if (device.getName().equals("cloud")) {
				cloud = device;
			}
		}
		return getPaths(cloud.getId());
	}

	/**
	 * @return the graph
	 */
	public Graph getGraph() {
		return graph;
	}

	/**
	 * @param graph
	 *            the graph to set
	 */
	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	/**
	 * @return the sman
	 */
	public SpriteManager getSman() {
		return sman;
	}

	/**
	 * @param sman
	 *            the sman to set
	 */
	public void setSman(SpriteManager sman) {
		this.sman = sman;
	}

	/**
	 * @return the fogDevices
	 */
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	/**
	 * @param fogDevices
	 *            the fogDevices to set
	 */
	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	/**
	 * @return the sensors
	 */
	public List<Sensor> getSensors() {
		return sensors;
	}

	/**
	 * @param sensors
	 *            the sensors to set
	 */
	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	/**
	 * @return the actuators
	 */
	public List<Actuator> getActuators() {
		return actuators;
	}

	/**
	 * @param actuators
	 *            the actuators to set
	 */
	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

}
