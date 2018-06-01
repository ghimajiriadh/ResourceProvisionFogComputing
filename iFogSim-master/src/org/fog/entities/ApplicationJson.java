/**
 * 
 */
package org.fog.entities;

import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * @author tptha
 *
 */
public class ApplicationJson {
	private Application application;

	@SuppressWarnings("unchecked")
	public ApplicationJson(String file, String appId, int userId) {
		// TODO Auto-generated constructor stub
		application = Application.createApplication(appId, userId);
		try {
			JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(file));

			// get ApppModule
			JSONArray appModule = (JSONArray) doc.get("modules");
			Iterator<JSONObject> iter = appModule.iterator();
			while (iter.hasNext()) {
				JSONObject node = iter.next();
				String name = (String) node.get("name");
				int mips = new BigDecimal((Long) node.get("mips")).intValueExact();
				int ram = new BigDecimal((Long) node.get("ram")).intValueExact();
				long bw = new BigDecimal((Long) node.get("bw")).intValueExact();
				application.addAppModule(name, ram, mips, bw);
			}

			JSONArray appEdge = (JSONArray) doc.get("edge");
			Iterator<JSONObject> edgeIter = appEdge.iterator();
			while (edgeIter.hasNext()) {
				JSONObject jsonObject = (JSONObject) edgeIter.next();
				String source = (String) jsonObject.get("src");
				String destination = (String) jsonObject.get("des");
				double tupleCpuLength = (double) jsonObject.get("mips");
				double tupleNwLength = (double) jsonObject.get("nw");
				String tupleType = (String) jsonObject.get("tupleName");
				String direction_s = (String) jsonObject.get("direction");
				int direction;
				switch (direction_s) {
				case "up":
					direction = Tuple.UP;
					break;
				default:
					direction = Tuple.DOWN;
					break;
				}

				String type_s = (String) jsonObject.get("type");
				int edgeType;
				switch (type_s) {
				case "sensor":
					edgeType = AppEdge.SENSOR;
					break;
				case "module":
					edgeType = AppEdge.MODULE;
					break;
				default:
					edgeType = AppEdge.ACTUATOR;
					break;
				}
				if (jsonObject.containsKey("periodicity")) {
					double periodicity = (double) jsonObject.get("periodicity");
					application.addAppEdge(source, destination, periodicity, tupleCpuLength, tupleNwLength, tupleType,
							direction, edgeType);
				} else {
					application.addAppEdge(source, destination, tupleCpuLength, tupleNwLength, tupleType, direction,
							edgeType);
				}
			}

			JSONArray tupleMapping = (JSONArray) doc.get("tupleMapping");

			Iterator<JSONObject> tupleMappingIter = tupleMapping.iterator();
			while (tupleMappingIter.hasNext()) {
				JSONObject jsonObject = (JSONObject) tupleMappingIter.next();
				String moduleName = (String) jsonObject.get("moduleName");
				String inputTupleType = (String) jsonObject.get("input");
				String outputTupleType = (String) jsonObject.get("output");
				double pediodcity = (double) jsonObject.get("periodcity");

				application.addTupleMapping(moduleName, inputTupleType, outputTupleType,
						new FractionalSelectivity(pediodcity));
			}

			JSONArray loop_1 = (JSONArray) doc.get("loop");
			Iterator<JSONObject> loop_2 = loop_1.iterator();
			List<AppLoop> loops = new ArrayList<AppLoop>();
			while (loop_2.hasNext()) {
				JSONObject jsonObject = (JSONObject) loop_2.next();

				ArrayList<String> temp = new ArrayList<String>();

				for (int i = 1; i <= jsonObject.keySet().size(); i++) {
					String value = (String) jsonObject.get("name" + i);
					temp.add(value);
				}
				AppLoop loop1 = new AppLoop(temp);
				loops.add(loop1);
			}

			application.setLoops(loops);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	/**
	 * @return the application
	 */
	public Application getApplication() {
		return application;
	}

	/**
	 * @param application
	 *            the application to set
	 */
	public void setApplication(Application application) {
		this.application = application;
	}
}
