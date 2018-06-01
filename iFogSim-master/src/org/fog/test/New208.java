package org.fog.test;

import java.awt.Frame;
import java.io.File;
import java.io.PrintStream;
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
import org.fog.utils.TimeKeeper;
import org.fog.utils.TopologyToJson;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for Resource Aware
 * 
 * @author Thanh An
 *
 */
public class New208 {

	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static int numOfFogGate = 4;

	public static void main(String[] args) {

		Log.printLine("Starting Example...");
		// final boolean CLOUD = false;
		final boolean CLOUD = true;

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "Example";

			FogBroker broker = new FogBroker("broker");

			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());

			createFogDevices(broker.getId(), appId);

			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

			// create GUI
			TopologyToJson toJson = new TopologyToJson();
			toJson.createJson(fogDevices, sensors, actuators, "topologies/" + appId + "-" + numOfFogGate);
			Frame jFrame = new FogGuiDemo("topologies/" + appId + "-" + numOfFogGate);
			jFrame.setVisible(true);

			ApplicationGraph applicationGraph = new ApplicationGraph("Example", application);

			if (CLOUD) {

				// Fixing all instance module_1 to device
				for (FogDevice device : fogDevices) {
					if (device.getName().startsWith("Device")) {
						moduleMapping.addModuleToDevice("module_1", device.getName());
					}
					if (device.getName().startsWith("cloud")) {
						moduleMapping.addModuleToDevice("module_2", device.getName());
						moduleMapping.addModuleToDevice("module_3", device.getName());
						moduleMapping.addModuleToDevice("module_4", device.getName());
						moduleMapping.addModuleToDevice("module_5", device.getName());
						moduleMapping.addModuleToDevice("module_6", device.getName());

					}
				}
			} else {
				for (FogDevice device : fogDevices) {
					if (device.getName().startsWith("Device")) {
						moduleMapping.addModuleToDevice("module_1", device.getName());

					}
				}
			}

			controller.submitApplication(application, 0,
					(CLOUD) ? new ModulePlacementMapping(fogDevices, application, moduleMapping)
							: new FirstFitPlacement(fogDevices, sensors, actuators, application, moduleMapping));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			// if (CloudSim.terminateSimulation(10000))

			File temp = new File("output/example/" + appId + "-" + numOfFogGate + ".txt");
			if (!temp.exists())
				temp.createNewFile();
			PrintStream o = new PrintStream(new File("output/example/cloud" + appId + "-" + numOfFogGate + ".txt"));

			PrintStream console = System.out;
			System.setOut(o);

			CloudSim.startSimulation();

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
		FogDevice isp = createFogDevice("ISP-Gateway", 10000, 8192, 10000, 10000, 1, 0.0, 250, 180);
		isp.setParentId(cloud.getId());
		isp.setUplinkLatency(200); // latency of connection between isp server and cloud is 200 ms
		fogDevices.add(isp);
		for (int i = 0; i < numOfFogGate; i++) {
			addFogGateways(i + "", userId, appId, isp.getId());
		}
	}

	private static void addFogGateways(String id, int userId, String appId, int parentId) {
		// TODO Auto-generated method stub
		FogDevice fogGate = createFogDevice("Fog-" + id + "-Gateway", 8000, 6144, 10000, 10000, 2, 0.0, 107, 83);
		fogDevices.add(fogGate);
		fogGate.setUplinkLatency(25);
		for (int i = 0; i < 2; i++) {
			String deviceId = id + "-" + i;
			FogDevice device = addEndDevices(deviceId, userId, appId, fogGate.getId());
			device.setUplinkLatency(5);
			fogDevices.add(device);
		}
		fogGate.setParentId(parentId);

	}

	private static FogDevice addEndDevices(String id, int userId, String appId, int parentId) {
		// TODO Auto-generated method stub
		FogDevice device = createFogDevice("Device-" + id, 4000, 2048, 100, 250, 3, 0, 87, 82);
		device.setParentId(parentId);

		Sensor sensor = new Sensor("s-" + id, "IoT_Sensor", userId, appId, new DeterministicDistribution(10.0));
		sensors.add(sensor);
		Actuator ptz = new Actuator("a-" + id, userId, appId, "Display");
		actuators.add(ptz);
		sensor.setGatewayDeviceId(device.getId());
		sensor.setLatency(2.0);
		ptz.setGatewayDeviceId(device.getId());
		ptz.setLatency(3.0);

		return device;
	}

	@SuppressWarnings({ "serial" })
	private static Application createApplication(String appId, int userId) {

		Application application = Application.createApplication(appId, userId);

		// create application modules
		application.addAppModule("module_1", 1024, 500, 250);
		application.addAppModule("module_2", 4096, 1000, 500);
		application.addAppModule("module_3", 2048, 2000, 1000);
		application.addAppModule("module_4", 1024, 1500, 300);
		application.addAppModule("module_5", 6144, 3000, 2000);
		application.addAppModule("module_6", 8192, 1500, 5000);

		// add appEdge
		application.addAppEdge("IoT_Sensor", "module_1", 3000, 500, "IoT_Sensor", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("module_1", "module_2", 6000, 500, "TT_2", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("module_2", "module_3", 6000, 500, "TT_3", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("module_3", "module_4", 6000, 500, "TT_4", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("module_4", "module_5", 6000, 500, "TT_5", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("module_5", "module_6", 30, 1500, 1000, "TT_10", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("module_6", "module_1", 30, 1500, 1000, "TT_11", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("module_1", "Display", 2000, 500, "ACTUATOR_A", Tuple.DOWN, AppEdge.ACTUATOR);
		application.addAppEdge("module_1", "Display", 2000, 500, "ACTUATOR_B", Tuple.DOWN, AppEdge.ACTUATOR);
		application.addAppEdge("module_2", "module_1", 1000, 500, "TT_9", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("module_3", "module_2", 1000, 500, "TT_8", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("module_4", "module_3", 1000, 500, "TT_7", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("module_5", "module_4", 1000, 500, "TT_6", Tuple.DOWN, AppEdge.MODULE);

		// input-output tuple generated by specified module
		// module_1
		application.addTupleMapping("module_1", "IoT_Sensor", "TT_2", new FractionalSelectivity(0.7));
		application.addTupleMapping("module_1", "TT_9", "ACTUATOR_B", new FractionalSelectivity(0.8));
		application.addTupleMapping("module_1", "TT_11", "ACTUATOR_A", new FractionalSelectivity(0.8));

		// module_2
		application.addTupleMapping("module_2", "TT_8", "TT_9", new FractionalSelectivity(0.8));
		application.addTupleMapping("module_2", "TT_2", "TT_3", new FractionalSelectivity(0.8));

		// module_3
		application.addTupleMapping("module_3", "TT_3", "TT_4", new FractionalSelectivity(0.8));
		application.addTupleMapping("module_3", "TT_7", "TT_8", new FractionalSelectivity(0.8));

		// module_4
		application.addTupleMapping("module_4", "TT_4", "TT_5", new FractionalSelectivity(0.8));
		application.addTupleMapping("module_4", "TT_6", "TT_7", new FractionalSelectivity(0.8));

		// module_5
		application.addTupleMapping("module_5", "TT_5", "TT_6", new FractionalSelectivity(0.8));

		// module_6
		application.addTupleMapping("module_6", "TT_10", "TT_11", new FractionalSelectivity(0.8));

		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
			{
				add("IoT_Sensor");
				add("module_1");
				add("module_2");
				add("module_3");
				add("module_4");
				add("module_5");
				add("module_4");
				add("module_3");
				add("module_2");
				add("module_1");
				add("Display");
			}
		});

		final AppLoop loop2 = new AppLoop(new ArrayList<String>() {
			{
				// add("IoT_Sensor");
				// add("module_1");
				// add("module_2");
				// add("module_3");
				// add("module_4");
				add("module_5");
				add("module_6");
				add("module_1");
				add("Display");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{
				add(loop1);
				add(loop2);
			}
		};

		application.setLoops(loops);

		// GeoCoverage geoCoverage = new GeoCoverage(-100, 100, -100, 100);
		return application;
	}
}