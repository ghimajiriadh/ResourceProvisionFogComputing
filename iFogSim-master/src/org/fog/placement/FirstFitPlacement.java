/**
 * 
 */
package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;

/**
 * @author tptha
 *
 */
public class FirstFitPlacement extends ModulePlacement {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.fog.placement.ModulePlacement#mapModules()
	 */

	protected ModuleMapping moduleMapping;
	protected List<Sensor> sensors;
	protected List<Actuator> actuators;

	protected Map<Integer, List<String>> currentModuleMap;
	protected Map<Integer, Double> currentCpuLoad;
	protected Map<Integer, Integer> currentRam;

	public FirstFitPlacement(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
			Application application, ModuleMapping moduleMapping) {
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		setSensors(sensors);
		setActuators(actuators);
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		setCurrentCpuLoad(new HashMap<Integer, Double>());
		setCurrentRam(new HashMap<Integer, Integer>());

		for (FogDevice dev : getFogDevices()) {
			getCurrentCpuLoad().put(dev.getId(), 0.0);
			getCurrentRam().put(dev.getId(), 0);
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
		}

		mapModules();

	}

	@Override
	protected void mapModules() {
		// TODO Auto-generated method stub

		for (String deviceName : getModuleMapping().getModuleMapping().keySet()) {
			for (String moduleName : getModuleMapping().getModuleMapping().get(deviceName)) {
				int deviceId = CloudSim.getEntityId(deviceName);
				getCurrentModuleMap().get(deviceId).add(moduleName);
				double moduleMips = getApplication().getModuleByName(moduleName).getMips();
				int moduleRam = getApplication().getModuleByName(moduleName).getRam();
				getCurrentCpuLoad().put(deviceId, getCurrentCpuLoad().get(deviceId) + moduleMips);
				getCurrentRam().put(deviceId, getCurrentRam().get(deviceId) + moduleRam);
			}
		}

		List<List<Integer>> leafToRootPaths = getLeafToRootPaths();

		for (List<Integer> path : leafToRootPaths) {
			placeModulesInPath(path);
		}

		for (int deviceId : getCurrentModuleMap().keySet()) {
			for (String module : getCurrentModuleMap().get(deviceId)) {
				createModuleInstanceOnDevice(getApplication().getModuleByName(module), getFogDeviceById(deviceId));
			}
		}

	}

	private void placeModulesInPath(List<Integer> path) {
		// TODO Auto-generated method stub
		if (path.size() == 0)
			return;

		List<String> placedModules = new ArrayList<String>();

		for (Integer deviceId : path) {
			FogDevice device = getFogDeviceById(deviceId);
			Map<String, Integer> sensorsAssociated = getAssociatedSensors(device);
			Map<String, Integer> actuatorsAssociated = getAssociatedActuators(device);
			placedModules.addAll(sensorsAssociated.keySet()); // ADDING ALL SENSORS TO PLACED LIST
			placedModules.addAll(actuatorsAssociated.keySet()); // ADDING ALL ACTUATORS TO PLACED LIST

			List<String> modulesToPlace = getModulesToPlace(placedModules);

			while (modulesToPlace.size() > 0) {
				String moduleName = modulesToPlace.get(0);

				int upstreamDeviceId = isPlacedUpstream(moduleName, path);
				// if module already place upstream
				if (upstreamDeviceId > 0) {
					if (deviceId == upstreamDeviceId) {
						// check if device can host module
						if (currentCpuLoad.get(deviceId) > device.getHost().getTotalMips()
								|| currentRam.get(deviceId) > device.getHost().getRam()) {
							// TODO move to upstream
						} else {
							System.out.println("Placement of module " + moduleName + " on device " + device.getName()
									+ " successful.");
							// modulesToPlace = getModulesToPlace(placedModules);
							if (!placedModules.contains(moduleName))
								placedModules.add(moduleName);
						}
					}
				} else {
					// check if device can host module
					if (canBePlace(deviceId, moduleName)) {
						System.out.println("Placement of module " + moduleName + " on device " + device.getName()
								+ " successful.");
						AppModule module = getApplication().getModuleByName(moduleName);
						currentModuleMap.get(deviceId).add(moduleName);
						currentCpuLoad.put(deviceId, currentCpuLoad.get(deviceId) + module.getMips());
						currentRam.put(deviceId, currentRam.get(deviceId) + module.getRam());
						placedModules.add(moduleName);
						modulesToPlace = getModulesToPlace(placedModules);
					}
				}

				modulesToPlace.remove(moduleName);
			}
		}

	}

	private boolean canBePlace(Integer deviceId, String moduleName) {
		// TODO Auto-generated method stub
		double cpuLoad = currentCpuLoad.get(deviceId);
		int ramLoad = currentRam.get(deviceId);

		FogDevice device = getFogDeviceById(deviceId);

		AppModule module = getApplication().getModuleByName(moduleName);

		if (cpuLoad + module.getMips() <= device.getHost().getTotalMips()
				&& ramLoad + module.getRam() <= device.getHost().getRam())
			return true;
		else
			return false;
	}

	/**
	 * Gets all sensors associated with fog-device <b>device</b>
	 * 
	 * @param device
	 * @return map from sensor type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedSensors(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for (Sensor sensor : getSensors()) {
			if (sensor.getGatewayDeviceId() == device.getId()) {
				if (!endpoints.containsKey(sensor.getTupleType()))
					endpoints.put(sensor.getTupleType(), 0);
				endpoints.put(sensor.getTupleType(), endpoints.get(sensor.getTupleType()) + 1);
			}
		}
		return endpoints;
	}

	/**
	 * Gets all actuators associated with fog-device <b>device</b>
	 * 
	 * @param device
	 * @return map from actuator type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedActuators(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for (Actuator actuator : getActuators()) {
			if (actuator.getGatewayDeviceId() == device.getId()) {
				if (!endpoints.containsKey(actuator.getActuatorType()))
					endpoints.put(actuator.getActuatorType(), 0);
				endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType()) + 1);
			}
		}
		return endpoints;
	}

	/**
	 * Get the list of modules that are ready to be placed
	 * 
	 * @param placedModules
	 *            Modules that have already been placed in current path
	 * @return list of modules ready to be placed
	 */
	private List<String> getModulesToPlace(List<String> placedModules) {
		Application app = getApplication();
		List<String> modulesToPlace_1 = new ArrayList<String>();
		List<String> modulesToPlace = new ArrayList<String>();
		for (AppModule module : app.getModules()) {
			if (!placedModules.contains(module.getName()))
				modulesToPlace_1.add(module.getName());
		}
		/*
		 * Filtering based on whether modules (to be placed) lower in physical topology
		 * are already placed
		 */
		for (String moduleName : modulesToPlace_1) {
			boolean toBePlaced = true;

			for (AppEdge edge : app.getEdges()) {
				// CHECK IF OUTGOING DOWN EDGES ARE PLACED
				if (edge.getSource().equals(moduleName) && edge.getDirection() == Tuple.DOWN
						&& !placedModules.contains(edge.getDestination()))
					toBePlaced = false;
				// CHECK IF INCOMING UP EDGES ARE PLACED
				if (edge.getDestination().equals(moduleName) && edge.getDirection() == Tuple.UP
						&& !placedModules.contains(edge.getSource()))
					toBePlaced = false;
			}
			if (toBePlaced)
				modulesToPlace.add(moduleName);
		}

		return modulesToPlace;
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
			if (device.getName().equals("cloud"))
				cloud = device;
		}
		return getPaths(cloud.getId());
	}

	/**
	 * @return deviceid hosting module
	 */
	private int isPlacedUpstream(String operatorName, List<Integer> path) {
		for (int deviceId : path) {
			if (currentModuleMap.containsKey(deviceId) && currentModuleMap.get(deviceId).contains(operatorName))
				return deviceId;
		}
		return -1;
	}

	/**
	 * @return the moduleMapping
	 */
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	/**
	 * @param moduleMapping
	 *            the moduleMapping to set
	 */
	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
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

	/**
	 * @return the currentModuleMap
	 */
	public Map<Integer, List<String>> getCurrentModuleMap() {
		return currentModuleMap;
	}

	/**
	 * @param currentModuleMap
	 *            the currentModuleMap to set
	 */
	public void setCurrentModuleMap(Map<Integer, List<String>> currentModuleMap) {
		this.currentModuleMap = currentModuleMap;
	}

	/**
	 * @return the currentCpuLoad
	 */
	public Map<Integer, Double> getCurrentCpuLoad() {
		return currentCpuLoad;
	}

	/**
	 * @param currentCpuLoad
	 *            the currentCpuLoad to set
	 */
	public void setCurrentCpuLoad(Map<Integer, Double> currentCpuLoad) {
		this.currentCpuLoad = currentCpuLoad;
	}

	/**
	 * @return the currentRam
	 */
	public Map<Integer, Integer> getCurrentRam() {
		return currentRam;
	}

	/**
	 * @param currentRam
	 *            the currentRam to set
	 */
	public void setCurrentRam(Map<Integer, Integer> currentRam) {
		this.currentRam = currentRam;
	}

}
