/**
 * 
 */
package org.fog.gui.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;

public class ApplicationGraph {

	private Graph graph;
	private SpriteManager sman;

	public ApplicationGraph(String name, Application application) {
		graph = new MultiGraph(name);

		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.setAutoCreate(true);
		graph.setStrict(false);
		graph.addAttribute("ui.antialias");
		graph.addAttribute("ui.quality");
		sman = new SpriteManager(graph);
		// graph.display();

		List<AppModule> modules = application.getModules();
		List<AppEdge> edges = application.getEdges();

		addNodes(modules);
		addEdges(edges);
		addTuppleMapping(modules, edges);
		Viewer viewer = graph.display(false);
		viewer.enableAutoLayout();

	}

	/**
	 * @param modules
	 */
	private void addTuppleMapping(List<AppModule> modules, List<AppEdge> edges) {
		// TODO Auto-generated method stub
		for (int j = 0; j < modules.size(); j++) {
			int k = 1;
			for (Pair<String, String> pair : modules.get(j).getSelectivityMap().keySet()) {
				AppEdge edge1, edge2;
				int i = 0;
				while (true) {
					if (edges.get(i).getDestination().equals(modules.get(j).getName())
							&& edges.get(i).getTupleType().equals(pair.getFirst())) {
						edge1 = edges.get(i);
						break;
					} else
						i++;
				}
				i = 0;
				while (true) {
					if (edges.get(i).getSource().equals(modules.get(j).getName())
							&& edges.get(i).getTupleType().equals(pair.getValue())) {
						edge2 = edges.get(i);
						break;
					}
					i++;
				}
				Random rand = new Random();
				int r = rand.nextInt(255) + 0;
				int g = rand.nextInt(255) + 0;
				int b = rand.nextInt(255) + 0;

				BigDecimal b1, b2;

				int temp = modules.get(j).getSelectivityMap().size() + 1;
				b2 = new BigDecimal("" + temp);
				b2 = new BigDecimal(k).divide(b2, 2, RoundingMode.HALF_UP);
				b2 = b2.multiply(new BigDecimal(0.5));
				b1 = new BigDecimal(0.5);

				Sprite s1 = sman.addSprite(edge1.getSource() + "->" + edge1.getDestination() + ",2," + k);
				s1.attachToEdge(edge1.getSource() + "->" + edge1.getDestination());
				s1.addAttribute("ui.style", "fill-color: rgb(" + r + "," + g + "," + b + ");");
				s1.setPosition(b1.add(b2).doubleValue());

				Sprite s2 = sman.addSprite(edge2.getSource() + "->" + edge2.getDestination() + "1," + k);
				s2.attachToEdge(edge2.getSource() + "->" + edge2.getDestination());
				s2.addAttribute("ui.style", "fill-color: rgb(" + r + "," + g + "," + b + ");");
				s2.setPosition(b1.subtract(b2).doubleValue());

				k++;
			}
		}
	}

	private void addEdges(List<AppEdge> edges) {
		// TODO Auto-generated method stub
		for (AppEdge edge : edges) {
			graph.addEdge(edge.getSource() + "->" + edge.getDestination(), edge.getSource(), edge.getDestination(),
					true);

			Edge edge2 = graph.getEdge(edge.getSource() + "->" + edge.getDestination());
			edge2.addAttribute("ui.label", edge.getTupleType());

			if (edge.getEdgeType() == AppEdge.SENSOR) {
				graph.addNode(edge.getSource());
				Node node = graph.getNode(edge.getSource());
				node.addAttribute("ui.label", edge.getSource());
			} else if (edge.getEdgeType() == AppEdge.ACTUATOR) {
				graph.addNode(edge.getDestination());
				Node node = graph.getNode(edge.getDestination());
				node.addAttribute("ui.label", edge.getDestination());
			}
		}
	}

	private void addNodes(List<AppModule> modules) {
		// TODO Auto-generated method stub
		for (AppModule module : modules) {
			graph.addNode(module.getName());
			Node node = graph.getNode(module.getName());
			node.addAttribute("ui.label", module.getName());

			Sprite s = sman.addSprite(module.getName() + " sprite");
			s.attachToNode(module.getName());
			String requirement = "<" + module.getMips() + ", " + module.getRam() + ", " + module.getBw() + ">";
			s.addAttribute("ui.label", requirement);
			s.setPosition(Units.PX, 25, 0, 270);
		}
	}

	protected String styleSheet = "edge{text-size:25px;} node {" + "size: 20px;text-size: 25px;	fill-color: black;"
			+ "}" + "node.marked {" + "	fill-color: red;" + "} sprite {fill-color:rgba(0,0,0,0);text-size: 25px;}";
}