/*
 * Copyright 2019 Jan Kowollik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jk.zugsteuerung.zug;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceDiscoveryListener;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class TrainService {
	
	public class MessageType {
		final static int HUB_PROPERTIES = 0x01;
		final static int HUB_ACTIONS = 0x02;
		final static int HUB_ALERTS = 0x03;
		final static int HUB_ATTACHED_IO = 0x04;
		final static int ERROR = 0x05;
		
		final static int PORT_MODE_INFORMATION = 0x44;
	}
	
	private ThreadPoolTaskScheduler taskScheduler;
	private Map<String, TrainConnection> trainMap = new HashMap<String, TrainConnection>();
	
	private BluetoothManager bluetoothManager = null;
	private boolean discovery = false;
	private DeviceDiscoveryListener deviceDiscoveryListener;
	private URL adapterUrl;
	
	private List<TrainListener> trainListeners = new CopyOnWriteArrayList<TrainService.TrainListener>();
	private Map<Integer, MessageHandler> handlerMap = new HashMap<Integer, TrainService.MessageHandler>();
	
	@Value("${configpath}")
	private String configPath;
	
	
	public interface TrainListener {
		void onTrainUpdate(Train t);
	}
	
	public interface MessageHandler {
		void handleMessage(TrainConnection trainCon, Message m);
	}
	
	public TrainService() {
		taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(1); // We use it as a queue so only one Thread should exist
		taskScheduler.initialize();

		handlerMap.put(MessageType.HUB_ATTACHED_IO, this::hubAttachedIoHandler);
		handlerMap.put(MessageType.HUB_PROPERTIES, this::hubPropertiesHandler);
		handlerMap.put(MessageType.PORT_MODE_INFORMATION, this::portModeInformationHandler);
	}
	
	@PostConstruct
	public void loadConfig() {
		File configFile = new File(configPath);
		if(configFile.isFile()) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode mainConfigNode = mapper.readTree(configFile);
				mainConfigNode.get("trains").forEach(trainNode -> {
					try {
						Train t = mapper.treeToValue(trainNode, Train.class);
						trainMap.put(t.getUrl(), new TrainConnection(t));
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				});
			} catch (IOException e) {
				System.err.println("Error while reading configuration file.");
			}
		}
	}
	
	@PreDestroy
	public void saveConfig() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode mainConfigNode = mapper.createObjectNode();
		ArrayNode trainListNode = mainConfigNode.putArray("trains");
		trainMap.values().forEach(trainCon -> {
			trainCon.getTrain().setBattery(0);
			trainCon.getTrain().setOnline(false);
			trainListNode.addPOJO(trainCon.getTrain());
		});
		mapper.writerWithDefaultPrettyPrinter().writeValue(new File(configPath), mainConfigNode);
		
	}
	
	public void registerTrainListener(TrainListener tl) {
		trainListeners.add(tl);
	}
	
	public void unregisterTrainListener(TrainListener tl) {
		trainListeners.remove(tl);
	}
	
	public void pushTrainUpdate(Train t) {
		trainListeners.forEach(listener -> listener.onTrainUpdate(t));
	}

	
	public void enqueueMotorUpdate(Port port) {
		taskScheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				Message powerChangeMessage = Message.motorPowerChange(port.getId(), port.getPower());
				CharacteristicGovernor cg = trainMap.get(port.getUrl()).getCharacteristicGovernor();
				if(cg.isReady() && cg.isWritable()) {
					cg.write(powerChangeMessage.getByteArray());
				}
			}
			
		}, Instant.now());
	}
	
	public void enqueueLedColorUpdate(Train t, int delay) {
		taskScheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				Color c = t.getColor();
				
				// Workaround since RGB Mode does not yet work
				int colorIndex = 0;
				if(c.getR() ==   0 && c.getG() == 200 && c.getB() ==   0) { colorIndex = 6;	}
				if(c.getR() == 240 && c.getG() == 220 && c.getB() ==   0) { colorIndex = 7;	}
				if(c.getR() == 180 && c.getG() ==  20 && c.getB() ==   0) { colorIndex = 9;	}
				if(c.getR() == 120 && c.getG() ==   0 && c.getB() == 160) { colorIndex = 2;	}
				if(c.getR() ==   0 && c.getG() == 100 && c.getB() == 180) { colorIndex = 3;	}
				if(c.getR() ==   0 && c.getG() == 160 && c.getB() == 220) { colorIndex = 4;	}
				if(c.getR() == 180 && c.getG() == 180 && c.getB() == 180) { colorIndex = 5;	}
				if(c.getR() == 240 && c.getG() == 240 && c.getB() == 240) { colorIndex = 10;}

				Message changeLed = Message.ledChange(colorIndex);
				CharacteristicGovernor cg = trainMap.get(t.getUrl()).getCharacteristicGovernor();
				if(cg.isReady() && cg.isWritable()) {
					cg.write(changeLed.getByteArray());
				}
			}
			
		}, Instant.now().plusMillis(delay));
	}
	
	public void enqueuePortInfo(Train t, int portId, int mode, int informationType, int delay) {
		taskScheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				byte[] command = new byte[] {6, 0, 0x22, (byte) portId, (byte) mode, (byte) informationType};
				CharacteristicGovernor cg = trainMap.get(t.getUrl()).getCharacteristicGovernor();
				if(cg.isReady() && cg.isWritable()) {
					cg.write(command);
				}
			}
			
		}, Instant.now().plusMillis(delay));
	}
	
	public void enqueuePortInfo2(Train t, int portId, int informationType, int delay) {
		taskScheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				byte[] command = new byte[] {6, 0, 0x21, (byte) portId, (byte) informationType};
				CharacteristicGovernor cg = trainMap.get(t.getUrl()).getCharacteristicGovernor();
				if(cg.isReady() && cg.isWritable()) {
					cg.write(command);
				}
			}
			
		}, Instant.now().plusMillis(delay));
	}
	
	public Train updateTrain(Train train) {
		Train t = trainMap.get(train.getUrl()).getTrain();
		
		Color newColor = new Color(
				clamp(train.getColor().getR(), 0, 255),
				clamp(train.getColor().getG(), 0, 255),
				clamp(train.getColor().getB(), 0, 255));
		if(!t.getColor().equals(newColor)) {
			t.setColor(newColor);
			if(t.isOnline()) {
				enqueueLedColorUpdate(t, 100);
			}
		}
		
		return t;
	}
	
	public void updatePort(Port port) {
		Train t = trainMap.get(port.getUrl()).getTrain();
		Port p = t.getPorts().get(port.getId());
		
		if(p == null || !t.isOnline()) { // Maybe Port detached shortly after the user tried to use it
			// TODO Send the Client the current train?
			return;
		}
		
		int newPowerValue = clamp(port.getPower(), -100, 100);
		if(p.getPower() != newPowerValue) {
			p.setPower(newPowerValue);
			
			enqueueMotorUpdate(p);
		}
		
		return;
	}
	
	public Train[] trainList() {
		List<Train> trainList = new ArrayList<>();
		trainMap.values().forEach(trainCon -> trainList.add(trainCon.getTrain()));
		return trainList.toArray(new Train[0]);
	}
	
	public void toggleDiscovery() {
		if(discovery) {
			discovery = false;
			bluetoothManager.removeDeviceDiscoveryListener(deviceDiscoveryListener);
			//bluetoothManager.getAdapterGovernor(adapterUrl).setDiscoveringControl(false);
		} else {
			discovery = true;
			ensureBluetoothAvailable();

			//bluetoothManager.getAdapterGovernor(adapterUrl).setDiscoveringControl(true);
			// Process devices discovered before discovery was activated since they wont be received by the discovery listener again
			bluetoothManager.getDiscoveredDevices().forEach(discoveredDevice -> onDiscoveredDevice(discoveredDevice));
			// Process newly detected devices
			bluetoothManager.addDeviceDiscoveryListener(deviceDiscoveryListener = new DeviceDiscoveryListener() {
				
				@Override
				public void discovered(DiscoveredDevice discoveredDevice) {
					onDiscoveredDevice(discoveredDevice);
				}
			});
		}
		
		/*byte[] data;
		for (DeviceGovernor b : bleDeviceMap.values()) {
			// LEGO Manufacturer ID is 0x397 / 919
			Map<Short, byte[]> manufacturerData = b.getManufacturerData();
			byte[] releventData = manufacturerData.get((short) 0x397);
			if(releventData == null) {
				continue;
			}
			// relevantData has everything after the manufacturer id
			System.out.println("Button State (must be 0 or 1): " + releventData[0]);
			byte systemTypeAndDeviceNumber = releventData[1];
			System.out.println("System Type and Device Number (0x0 - 0xff): " + systemTypeAndDeviceNumber);
			System.out.println("Device Capabilities (0x01 - 0xff): " + releventData[2]);
			System.out.println("Last network (0 - 255): " + releventData[3]);
			System.out.println("Status (0 - 255): " + releventData[4]);
			System.out.println("Option (0 - 255): " + releventData[5]);
			
			String[] systemTypes = {"LEGO Wedo 2.0", "LEGO Duplo", "LEGO System", "LEGO System"};
			int systemType = (systemTypeAndDeviceNumber & 0xd0) >> 5;
			System.out.println("System type: " + systemTypes[systemType]);
			
			if(systemType == 2) {
				String[] deviceNumbers = {"", "2 Port Hub", "2 Port Handset", ""};
				int deviceNumber = systemTypeAndDeviceNumber & 0x1f;
				System.out.println("Device Number: " + deviceNumbers[deviceNumber]);
			}
		}*/
	}
	
	public void onDiscoveredDevice(DiscoveredDevice discoveredDevice) {
		System.out.println("Discovered " + discoveredDevice.getDisplayName() + "/" + discoveredDevice.getURL().getDeviceAddress());
		
		// Only connect to Lego Devices based on MAC address
		if(!discoveredDevice.getURL().getDeviceAddress().startsWith("90:84:2B")             // LEGO System A/S
				&& !discoveredDevice.getURL().getDeviceAddress().startsWith("00:16:53")) {  // LEGO System A/S IE Electronics Division
			return;
		}
		
		if(trainMap.containsKey(discoveredDevice.getURL().getDeviceAddress())) {
			return;
		}
		
		Train t = new Train(
				discoveredDevice.getURL().getDeviceAddress(), 
				discoveredDevice.getDisplayName());
		
		trainMap.put(t.getUrl(), new TrainConnection(t));
		
		pushTrainUpdate(t);
	}
	
	public boolean isDiscovering() {
		return discovery;
	}
	
	public void connect(Train receivedTrain) {
		TrainConnection trainCon = trainMap.get(receivedTrain.getUrl());
		Train train = trainCon.getTrain();
		if(train.isOnline()) {
			return;
		}
		
		ensureBluetoothAvailable();
		DeviceGovernor deviceGov = trainCon.getDeviceGovernor();
		if(deviceGov == null){
			
			deviceGov = bluetoothManager.getDeviceGovernor(adapterUrl.copyWithDevice(train.getUrl()));
			trainCon.setDeviceGovernor(deviceGov);
			
			CharacteristicGovernor charaGov = trainCon.getCharacteristicGovernor();
			if(charaGov == null) {
				charaGov = bluetoothManager.getCharacteristicGovernor(deviceGov.getURL().copyWith("00001623-1212-EFDE-1623-785FEABCD123", "00001624-1212-EFDE-1623-785FEABCD123"), true);
				trainCon.setCharacteristicGovernor(charaGov);
			}
			
			if(trainCon.getValueListener() == null) {
				ValueListener vl = new ValueListener() {
					
					@Override
					public void changed(byte[] value) {
						System.out.println(Arrays.toString(value));
						int messageType = value[2];
						if(handlerMap.containsKey(messageType)) {
							handlerMap.get(messageType).handleMessage(trainCon, new Message(value));
						}
					}
				};
				charaGov.addValueListener(vl);
				trainCon.setValueListener(vl);
			}
			
			charaGov.whenReady(new Function<BluetoothGovernor, Integer>() {

				@Override
				public Integer apply(BluetoothGovernor bg) {
					System.out.println("Train connected!");

					ScheduledFuture<?> batteryFuture = taskScheduler.scheduleWithFixedDelay(new BatteryUpdateTask(trainCon), 20_000);
					trainCon.setBatteryTask(batteryFuture);
					
					ScheduledFuture<?> distanceFuture = taskScheduler.scheduleWithFixedDelay(new DistanceUpdateTask(trainCon), 15_000);
					trainCon.setDistanceTask(distanceFuture);
					
					train.setOnline(true);
					
					pushTrainUpdate(train);
					
					return 0;
				}
				
			});
			
		}
	}
	
	public void hubPropertiesHandler(TrainConnection trainCon, Message m) {
		int property = m.byteAt(3);
		//int operation = value[4]; // can only be 6 here for Upstream Update
		if(property == 5) { // RSSI
			int rssi = m.byteAt(5); // Between -127 and 0
			trainCon.getTrain().setDistance((-rssi) / 127f);
			pushTrainUpdate(trainCon.getTrain());
			//System.out.println("RSSI: " + rssi);
		}
		if(property == 6) { // Battery Voltage
			int battery = m.byteAt(5); // Between 0 and 100
			trainCon.getTrain().setBattery(battery);
			pushTrainUpdate(trainCon.getTrain());
		}
	}
	public void hubAttachedIoHandler(TrainConnection trainCon, Message m) {
		int portId = m.byteAt(3);
		if(m.byteAt(4) == 1) { // Type: Attached
			Port p = new Port(trainCon.getTrain().getUrl(), portId, m.byteAt(5));
			trainCon.getTrain().getPorts().put(p.getId(), p);
			System.out.println("Attached Device at Port " + portId);
			
			// TODO After Reconnect, Ports are rediscovered -> set Power if it was non-zero before?
			
			// Check for LED Port(50/0x32), if there is one we change its color away from the default white
			if(portId == 0x32) {
				enqueueLedColorUpdate(trainCon.getTrain(), 300);
			}

			if(portId == 59 || portId == 60 || portId == 50) {
				//enqueuePortInfo(train, portId, 1, 0, 800 + portId * 30);
				//enqueuePortInfo(train, portId, 1, 1, 3800 + portId * 30);
				//enqueuePortInfo(train, portId, 1, 2, 8000 + portId * 30);
				//enqueuePortInfo(train, portId, 1, 3, 12000 + portId * 30);
				//enqueuePortInfo(train, portId, 1, 4, 16000 + portId * 30);
			}
			
			//enqueuePortInfo2(train, portId, 0, 21000 + portId * 30);
			//enqueuePortInfo2(train, portId, 1, 25000 + portId * 30);
		}
		if(m.byteAt(4) == 0) { // Type: Detached
			trainCon.getTrain().getPorts().remove(portId);
			System.out.println("Detached Device at Port " + portId);
		}
		// Push Port info to clients
		pushTrainUpdate(trainCon.getTrain());
	}
	public void portModeInformationHandler(TrainConnection trainCon, Message m) {
		int portId =  m.byteAt(3);
		//int mode =  m.byteAt(4);
		int informationType =  m.byteAt(5);
		if(informationType == 0) {
			String name = m.getString(6, 11); // Length should be derived from message length but message length is 18 independent of name length
			// So we do what the Hub currently delivers instead of what the LEGO documentation says.
			System.out.println("Port " + portId + " has Name " + name);
		}
		if(informationType == 4) {
			String name = m.getString(6, 5);
			System.out.println("Port " + portId + " has Unit " + name); 
		}
	}
	
	public void shutdownTrain(Train train) {
		TrainConnection trainCon = trainMap.get(train.getUrl());
		Train t = trainCon.getTrain();
		if(!t.isOnline()) {
			return;
		}
		
		Message shutdownMessage = Message.shutdown();
		CharacteristicGovernor cg = trainCon.getCharacteristicGovernor();
		
		if(cg.isReady()) {
			cg.write(shutdownMessage.getByteArray());
			t.setOnline(false);
			trainCon.setCharacteristicGovernor(null);
			trainCon.getDeviceGovernor().setConnectionControl(false);
			trainCon.setDeviceGovernor(null);
			
			trainCon.getBatteryTask().cancel(false);
			trainCon.setBatteryTask(null);
			trainCon.getDistanceTask().cancel(false);
			trainCon.setDistanceTask(null);
			cg.removeValueListener(trainCon.getValueListener());
			trainCon.setValueListener(null);
			
			trainCon.getTrain().getPorts().values().forEach(port -> port.setPower(0));
			pushTrainUpdate(trainCon.getTrain());
		}
		
	}
	
	public void connectAllTrains() {
		trainMap.values().forEach(trainCon -> connect(trainCon.getTrain()));
	}
	
	public void disconnectAllTrains() {
		trainMap.values().forEach(trainCon -> shutdownTrain(trainCon.getTrain()));
	}
	
	public void removeTrain(Train train) {
		Train t = trainMap.get(train.getUrl()).getTrain();
		if(t.isOnline()) {
			shutdownTrain(train);
		}
		trainMap.remove(train.getUrl());
	}
	
	public void stopAll() {
		trainMap.values().forEach(trainCon -> {
			if(trainCon.getTrain().isOnline()) {
				trainCon.getTrain().getPorts().values().forEach(port -> {
					if(port.getDeviceType() == 1 || port.getDeviceType() == 2) {
						port.setPower(0);
						enqueueMotorUpdate(port);
					}
				});
				pushTrainUpdate(trainCon.getTrain());
			}
		});
	}
	
	private void ensureBluetoothAvailable() {
		if(bluetoothManager == null) {
			bluetoothManager = new BluetoothManagerBuilder()
				.withTinyBTransport(true)
				.withDiscovering(true)
				.withStarted(true)
				.build();
			bluetoothManager.getDiscoveredAdapters().forEach(discoveredAdapter -> adapterUrl = discoveredAdapter.getURL());
			System.out.println("Adapter: " + adapterUrl.toString());
		}
	}
	
	private int clamp(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}
	
}
