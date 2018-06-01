/**
 * 
 */
package org.fog.utils;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author tptha
 *
 */
public class TopologyToJson {
	@SuppressWarnings("unchecked")
	public boolean createJson(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
			String location) {

		JSONArray nodes = new JSONArray();
		JSONArray links = new JSONArray();
		for (FogDevice fogDevice : fogDevices) {
			JSONObject object = new JSONObject();
			object.put("name", fogDevice.getName());
			object.put("level", fogDevice.getLevel());
			object.put("ratePerMips", fogDevice.getRatePerMips());
			object.put("downBw", (long) Math.round(fogDevice.getDownlinkBandwidth()));
			object.put("upBw", (long) Math.round(fogDevice.getUplinkBandwidth()));
			object.put("ram", fogDevice.getHost().getRam());
			object.put("mips", fogDevice.getHost().getTotalMips());
			object.put("type", "FOG_DEVICE");
			nodes.add(object);
			if (!fogDevice.getName().equals("cloud")) {
				FogDevice parentDevice = (FogDevice) CloudSim.getEntity(fogDevice.getParentId());
				JSONObject link = new JSONObject();
				link.put("latency", fogDevice.getUplinkLatency());
				link.put("source", fogDevice.getName());
				link.put("destination", parentDevice.getName());
				links.add(link);
			}
		}

		for (Sensor sensor : sensors) {
			JSONObject object = new JSONObject();

			object.put("distribution", sensor.getTransmitDistribution().getDistributionType());
			object.put("sensorType", sensor.getTupleType());
			object.put("name", sensor.getName());
			object.put("type", "SENSOR");
			if (sensor.getTransmitDistribution().getDistributionType() == Distribution.DETERMINISTIC) {
				DeterministicDistribution distribution = (DeterministicDistribution) sensor.getTransmitDistribution();
				object.put("value", distribution.getNextValue());
			} else if (sensor.getTransmitDistribution().getDistributionType() == Distribution.NORMAL) {
				NormalDistribution distribution = (NormalDistribution) sensor.getTransmitDistribution();
				object.put("mean", distribution.getMean());
				object.put("stdDev", distribution.getStdDev());
			} else {
				UniformDistribution distribution = (UniformDistribution) sensor.getTransmitDistribution();
				object.put("min", distribution.getMin());
				object.put("max", distribution.getMax());
			}
			nodes.add(object);

			FogDevice parentDevice = (FogDevice) CloudSim.getEntity(sensor.getGatewayDeviceId());
			JSONObject link = new JSONObject();
			link.put("latency", sensor.getLatency());
			link.put("source", sensor.getName());
			link.put("destination", parentDevice.getName());
			links.add(link);

		}
		for (Actuator actuator : actuators) {
			JSONObject object = new JSONObject();
			object.put("name", actuator.getName());
			object.put("type", "ACTUATOR");
			object.put("actuatorType", actuator.getActuatorType());
			nodes.add(object);

			FogDevice parentDevice = (FogDevice) CloudSim.getEntity(actuator.getGatewayDeviceId());
			JSONObject link = new JSONObject();
			link.put("latency", actuator.getLatency());
			link.put("source", actuator.getName());
			link.put("destination", parentDevice.getName());
			links.add(link);
		}
		JSONObject result = new JSONObject();
		result.put("nodes", nodes);
		result.put("links", links);
		String jsonString = result.toString();
		try {
			FileWriter fileWriter = new FileWriter(location, false);
			fileWriter.write(jsonString);
			fileWriter.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}
}
