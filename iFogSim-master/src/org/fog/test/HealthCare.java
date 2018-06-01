/**
 * 
 */
package org.fog.test;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.ApplicationJson;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.gui.example.ApplicationGraph;
import org.fog.gui.example.FogGuiDemo;
import org.fog.placement.Controller;
import org.fog.placement.FirstFitPlacement;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TopologyToJson;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * @author Thanh An
 *
 */
public class HealthCare {

	/**
	 * @param args
	 */

	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static int numOfFogGate = 1;
	static int numOfRouter = 2;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Log.printLine("Starting Smart Health Care...");
		final boolean CLOUD = true;
		// final boolean CLOUD = true;

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "HealthCare";

			FogBroker broker = new FogBroker("broker");

			// get application from JSON
			ApplicationJson application_json = new ApplicationJson("topologies/healthcare_app.json", appId,
					broker.getId());
			Application application = application_json.getApplication();
			application.setUserId(broker.getId());

			createFogDevices(broker.getId(), appId);

			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

			// create GUI
			TopologyToJson toJson = new TopologyToJson();

			toJson.createJson(fogDevices, sensors, actuators,
					"topologies/" + appId + "-" + numOfFogGate + "-" + numOfRouter);
			Frame jFrame = new FogGuiDemo("topologies/" + appId + "-" + numOfFogGate + "-" + numOfRouter);
			jFrame.setVisible(true);

			ApplicationGraph applicationGraph = new ApplicationGraph("HealthCare", application);

			// TopologyGraph topologicalGraph = new TopologyGraph("HealthCare", fogDevices,
			// sensors, actuators);
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

			if (CLOUD) {
				// Fixing all instance client to mobile
				for (FogDevice device : fogDevices) {
					if (device.getName().startsWith("mobile")) {
						moduleMapping.addModuleToDevice("client", device.getName());
					}
					if (device.getName().startsWith("cloud")) {
						moduleMapping.addModuleToDevice("data_filtering", device.getName());
						moduleMapping.addModuleToDevice("data_processing", device.getName());
						moduleMapping.addModuleToDevice("event_handler", device.getName());

					}
				}
			} else {
				for (FogDevice device : fogDevices) {
					if (device.getName().startsWith("mobile")) {
						moduleMapping.addModuleToDevice("client", device.getName());
					}
				}
			}

			controller.submitApplication(application, 0,
					(CLOUD) ? new ModulePlacementMapping(fogDevices, application, moduleMapping)
							: new FirstFitPlacement(fogDevices, sensors, actuators, application, moduleMapping));

			// TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			// File temp = new File("output/healthcare/" + appId + "-" + numOfFogGate + "-"
			// + numOfRouter + ".txt");
			// if (!temp.exists())
			// temp.createNewFile();
			// PrintStream o = new PrintStream(
			// new File("output/healthcare/cloud" + appId + "-" + numOfFogGate + "-" +
			// numOfRouter + ".txt"));
			//
			// PrintStream console = System.out;
			// System.setOut(o);

			CloudSim.startSimulation();

			System.out.println("something");
			CloudSim.stopSimulation();

			Log.printLine("Simulation finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}

	}

	private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level,
			double ratePerMips, double busyPower, double idlePower) {

		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage,
				peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
		// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost,
				costPerMem, costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList,
					10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}

		fogdevice.setLevel(level);
		return fogdevice;
	}

	private static void createFogDevices(int userId, String appId) {
		// TODO Auto-generated method stub
		// (String nodeName, long mips, int ram, long upBw, long downBw, int level,
		// double ratePerMips, double busyPower, double idlePower)
		FogDevice cloud = createFogDevice("cloud", 40000, 40960, 1000, 10000, 0, 0.01, 500.00, 350.00);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		for (int j = 0; j < numOfFogGate; j++) {
			FogDevice fogGateway = createFogDevice("fog-gateway-" + j, 7000, 8192, 10000, 10000, 1, 0.0, 200.00,
					100.00);
			fogGateway.setParentId(cloud.getId());
			fogGateway.setUplinkLatency(200);
			fogDevices.add(fogGateway);
			for (int i = 0; i < numOfRouter; i++) {
				addRouter(j + "-" + i, userId, appId, fogGateway.getId());
			}
		}
	}

	private static void addRouter(String id, int userId, String appId, int parentId) {
		// TODO Auto-generated method stub
		FogDevice router = createFogDevice("router-" + id, 6000, 6144, 10000, 10000, 2, 0.0, 107, 83);
		fogDevices.add(router);
		router.setParentId(parentId);
		router.setUplinkLatency(20);
		for (int i = 0; i < 2; i++) {
			String deviceId = id + "-" + i;
			FogDevice device = addMobile(deviceId, userId, appId, router.getId());
			device.setUplinkLatency(5);
			fogDevices.add(device);
		}

	}

	private static FogDevice addMobile(String id, int userId, String appId, int parentId) {
		// TODO Auto-generated method stub
		FogDevice device = createFogDevice("mobile-" + id, 3000, 2048, 100, 250, 3, 0, 87, 82);
		device.setParentId(parentId);

		Sensor heart_sensor = new Sensor("s-" + id, "Heart_Sensor", userId, appId, new DeterministicDistribution(5.0));
		sensors.add(heart_sensor);

		Actuator ptz = new Actuator("a-" + id, userId, appId, "Display");
		actuators.add(ptz);

		heart_sensor.setGatewayDeviceId(device.getId());
		heart_sensor.setLatency(2.0);

		ptz.setGatewayDeviceId(device.getId());
		ptz.setLatency(2.0);

		return device;
	}

	@SuppressWarnings("serial")
	private static Application createApplication(String appId, int userId) {
		// TODO Auto-generated method stub
		Application application = Application.createApplication(appId, userId);

		// add app module
		application.addAppModule("client", 1024, 1000, 500);
		application.addAppModule("data_filtering", 2048, 2500, 100);
		application.addAppModule("data_processing", 2048, 3000, 100);
		application.addAppModule("event_handler", 4096, 4000, 200);

		// add app edge
		// sensor to client
		application.addAppEdge("Heart_Sensor", "client", 3000, 500, "Heart_Sensor", Tuple.UP, AppEdge.SENSOR);
		// client to data_filtering
		application.addAppEdge("client", "data_filtering", 2000, 500, "raw_heart", Tuple.UP, AppEdge.MODULE);
		// data_filtering to data_processing
		application.addAppEdge("data_filtering", "data_processing", 1500, 1500, "filtered_heart", Tuple.UP,
				AppEdge.MODULE);
		// data_processing to event_handler
		application.addAppEdge("data_processing", "event_handler", 1000, 1000, "analyzed_heart", Tuple.UP,
				AppEdge.MODULE);
		// event_handler to client
		application.addAppEdge("event_handler", "client", 1000, 500, "response_heart", Tuple.DOWN, AppEdge.MODULE);
		// client to actuator
		application.addAppEdge("client", "Display", 500, 500, "display_data", Tuple.DOWN, AppEdge.ACTUATOR);
		// Tuple.DOWN, AppEdge.ACTUATOR);

		// add tuple mapping, relationship between tuples emitted by modules
		application.addTupleMapping("client", "Heart_Sensor", "raw_heart", new FractionalSelectivity(1.0));
		application.addTupleMapping("data_filtering", "raw_heart", "filtered_heart", new FractionalSelectivity(1.0));
		application.addTupleMapping("data_processing", "filtered_heart", "analyzed_heart",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("event_handler", "analyzed_heart", "response_heart",
				new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "response_heart", "display_data", new FractionalSelectivity(1.0));

		// end-to-end latancy app loop

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("Heart_Sensor");
				add("client");
				add("data_filtering");
				add("data_processing");
				add("event_handler");
				add("client");
				add("Display");
			}
		});

		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
			}
		};

		application.setLoops(loops);

		return application;
	}

}
