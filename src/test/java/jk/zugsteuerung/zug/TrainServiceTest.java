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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.nio.file.StandardOpenOption.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.ValueListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jk.zugsteuerung.zug.Color;
import jk.zugsteuerung.zug.Message;
import jk.zugsteuerung.zug.Port;
import jk.zugsteuerung.zug.Train;
import jk.zugsteuerung.zug.TrainConnection;
import jk.zugsteuerung.zug.TrainService;
import jk.zugsteuerung.zug.TrainService.MessageHandler;
import jk.zugsteuerung.zug.TrainService.TrainListener;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TrainServiceTest {
	
	@SpyBean
	private TrainService trainService;
	
	@Rule
	public TemporaryFolder configFolder = new TemporaryFolder();

	@Mock
	private ThreadPoolTaskScheduler mockScheduler;
	@Mock
	private BluetoothManager mockBtManager;
	@Mock
	private CharacteristicGovernor mockCharacteristicGovernor;
	@Mock
	private Map<Integer, MessageHandler> mockHandlerMap;
	
	private Map<String, TrainConnection> internalTrainMap;
	
	private Train testTrain, offlineTrain;
	private Port testPort, offlinePort;
	private TrainConnection testTrainCon, offlineTrainCon;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		ReflectionTestUtils.setField(trainService, "taskScheduler", mockScheduler);
		ReflectionTestUtils.setField(trainService, "bluetoothManager", mockBtManager);
		testTrain = new Train("90:84:2B:00:00:00", "TestTrain");
		testTrain.setOnline(true);
		testTrain.setColor(new Color(50, -20, 300));
		testPort = new Port(testTrain.getUrl(), 0, 1);
		testPort.setPower(120);
		testTrain.getPorts().put(testPort.getId(), testPort);
		
		offlineTrain = new Train("90:84:2B:FF:FF:FF", "OfflineTrain");
		offlineTrain.setOnline(false);
		offlinePort = new Port(offlineTrain.getUrl(), 0, 1);
		offlinePort.setPower(-50);
		offlineTrain.getPorts().put(offlinePort.getId(), offlinePort);
		internalTrainMap = (Map<String, TrainConnection>) ReflectionTestUtils.getField(trainService, "trainMap");
		internalTrainMap.clear();
		internalTrainMap.put(testTrain.getUrl(), testTrainCon = new TrainConnection(testTrain));
		internalTrainMap.put(offlineTrain.getUrl(), offlineTrainCon = new TrainConnection(offlineTrain));
	}
	
	@Test
	public void testTrainService() {
		assertNotNull(ReflectionTestUtils.getField(trainService, "taskScheduler"));
	}
	
	@Test
	public void testLoadConfig() throws IOException {
		File configFile = configFolder.newFile();
		String configContent = "{\r\n" + 
				"  \"trains\" : [ {\r\n" + 
				"    \"url\" : \"1\",\r\n" + 
				"    \"name\" : \"TestTrain\",\r\n" + 
				"    \"battery\" : 0.0,\r\n" + 
				"    \"power\" : 0.0,\r\n" + 
				"    \"distance\" : 0.0,\r\n" + 
				"    \"online\" : false,\r\n" + 
				"    \"ports\" : { },\r\n" + 
				"    \"color\" : {\r\n" + 
				"      \"r\" : 0,\r\n" + 
				"      \"g\" : 200,\r\n" + 
				"      \"b\" : 0\r\n" + 
				"    }\r\n" + 
				"  } ]\r\n" + 
				"}";
		Files.write(configFile.toPath(), Collections.singletonList(configContent), StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING, WRITE);
		TrainService trainService = new TrainService();
		ReflectionTestUtils.setField(trainService, "configPath", configFile.toString());
		
		trainService.loadConfig();
		@SuppressWarnings("unchecked")
		Map<String, TrainConnection> trainMap = (Map<String, TrainConnection>) ReflectionTestUtils.getField(trainService, "trainMap");
		assertEquals(1, trainMap.size());
		assertNotNull(trainMap.get("1"));
		assertEquals("TestTrain", trainMap.get("1").getTrain().getName());
	}
	
	@Test
	public void testSaveConfig() throws IOException {
		File configFile = configFolder.newFile();
		TrainService trainService = new TrainService();
		ReflectionTestUtils.setField(trainService, "configPath", configFile.toString());
		@SuppressWarnings("unchecked")
		Map<String, TrainConnection> trainMap = (Map<String, TrainConnection>) ReflectionTestUtils.getField(trainService, "trainMap");
		Train testTrain = new Train("12:34", "TestTrain");
		testTrain.setOnline(true);
		trainMap.put("1", new TrainConnection(testTrain));
		
		trainService.saveConfig();
		List<String> configLines = Files.readAllLines(configFile.toPath());
		StringBuilder configBuilder = new StringBuilder();
		configLines.forEach(line -> configBuilder.append(line));
		String savedConfig = configBuilder.toString();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode mainConfigNode = mapper.readTree(savedConfig);
		
		assertNotNull(mainConfigNode.get("trains"));
		assertEquals(1, mainConfigNode.get("trains").size());
		JsonNode trainNode = mainConfigNode.get("trains").get(0); 
		Train t = mapper.treeToValue(trainNode, Train.class);
		assertEquals("12:34", t.getUrl());
		assertEquals("TestTrain", t.getName());
		assertEquals(false, t.isOnline());
	}
	
	@Test
	public void testRegisterAndUnregisterTrainListener() {
		TrainListener listener = new TrainListener() {
			@Override
			public void onTrainUpdate(Train t) {}
		};
		trainService.registerTrainListener(listener);
		@SuppressWarnings("unchecked")
		List<TrainListener> trainListeners = (List<TrainListener>) ReflectionTestUtils.getField(trainService, "trainListeners");
		assertTrue(trainListeners.contains(listener));
		
		trainService.unregisterTrainListener(listener);
		assertFalse(trainListeners.contains(listener));
	}
	
	@Test
	public void testPushTrainUpdate() {
		final Train testTrain = new Train();
		final class Counter {int i = 0;};
		final Counter counter = new Counter();
		TrainListener listener = new TrainListener() {
			@Override
			public void onTrainUpdate(Train t) {
				assertEquals(testTrain, t);
				counter.i++;
			}
		};
		trainService.registerTrainListener(listener);	
		trainService.pushTrainUpdate(testTrain);
		assertEquals(1, counter.i);

		trainService.unregisterTrainListener(listener);
	}
	
	@Test
	public void testEnqueueMotorUpdate() {
		trainService.enqueueMotorUpdate(testPort);
		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(mockScheduler).schedule(Mockito.notNull(), (Instant) Mockito.notNull());
		verify(mockScheduler).schedule(taskCaptor.capture(), (Instant) Mockito.notNull());

		testTrainCon.setCharacteristicGovernor(mockCharacteristicGovernor);
		Mockito.when(mockCharacteristicGovernor.isReady()).thenReturn(true);
		Mockito.when(mockCharacteristicGovernor.isWritable()).thenReturn(true);
		taskCaptor.getValue().run();
		verify(mockCharacteristicGovernor).write(Mockito.notNull());
	}
	
	@Test
	public void testEnqueueLedColorUpdate() {
		trainService.enqueueLedColorUpdate(testTrain, 0);
		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(mockScheduler).schedule(Mockito.notNull(), (Instant) Mockito.notNull());
		verify(mockScheduler).schedule(taskCaptor.capture(), (Instant) Mockito.notNull());

		testTrainCon.setCharacteristicGovernor(mockCharacteristicGovernor);
		Mockito.when(mockCharacteristicGovernor.isReady()).thenReturn(true);
		Mockito.when(mockCharacteristicGovernor.isWritable()).thenReturn(true);
		taskCaptor.getValue().run();
		verify(mockCharacteristicGovernor).write(Mockito.notNull());
	}
	
	@Test
	public void testUpdateTrain() {
		trainService.updateTrain(testTrain);
		assertEquals(50, testTrain.getColor().getR());
		assertEquals(0, testTrain.getColor().getG());
		assertEquals(255, testTrain.getColor().getB());
		verify(mockScheduler).schedule(Mockito.notNull(), (Instant) Mockito.notNull());
	}
	
	@Test
	public void testUpdatePort() {
		trainService.updatePort(testPort);
		assertEquals(100, testPort.getPower());
		verify(mockScheduler).schedule(Mockito.notNull(), (Instant) Mockito.notNull());
		
		trainService.updatePort(testPort); // Power is now the same, so no call to enqueueMotorUpdate should happen
		verify(mockScheduler).schedule(Mockito.notNull(), (Instant) Mockito.notNull()); // Still only once
		
		trainService.updatePort(offlinePort); // Now train is offline, so no call again
		verify(mockScheduler).schedule(Mockito.notNull(), (Instant) Mockito.notNull()); // Still only once
	}
	
	@Test
	public void testTrainList() {
		Train[] trainArray = trainService.trainList();
		List<Train> trainList = Arrays.asList(trainArray);
		assertTrue(trainList.contains(testTrain));
		assertTrue(trainList.contains(offlineTrain));
	}
	
	@Test
	public void testToggleDiscovery() {
		assertFalse(trainService.isDiscovering());
		
		trainService.toggleDiscovery();
		assertTrue(trainService.isDiscovering());
		Mockito.verify(mockBtManager).getDiscoveredDevices();
		Mockito.verify(mockBtManager).addDeviceDiscoveryListener(Mockito.notNull());

		trainService.toggleDiscovery();
		assertFalse(trainService.isDiscovering());
		Mockito.verify(mockBtManager).removeDeviceDiscoveryListener(Mockito.notNull());
	}
	
	@Test
	public void testOnDiscoveredDevice() {
		DiscoveredDevice mockDevice = Mockito.mock(DiscoveredDevice.class);
		Mockito.when(mockDevice.getDisplayName()).thenReturn("DiscoverTest");
		URL failUrl = new URL("/FF:FF:FF:FF:FF:FF/12:34:56:EE:EE:EE");
		URL passUrl1 = new URL("/FF:FF:FF:FF:FF:FF/90:84:2B:EE:EE:EE");
		URL passUrl2 = new URL("/FF:FF:FF:FF:FF:FF/00:16:53:EE:EE:EE");

		Mockito.when(mockDevice.getURL()).thenReturn(failUrl);
		trainService.onDiscoveredDevice(mockDevice);
		verify(trainService, times(0)).pushTrainUpdate(Mockito.any());
		
		Mockito.when(mockDevice.getURL()).thenReturn(passUrl1);
		ArgumentCaptor<Train> trainCaptor = ArgumentCaptor.forClass(Train.class);
		trainService.onDiscoveredDevice(mockDevice);
		verify(trainService, times(1)).pushTrainUpdate(Mockito.any());
		verify(trainService).pushTrainUpdate(trainCaptor.capture());
		Train receivedTrain = trainCaptor.getValue();
		assertEquals(passUrl1.getDeviceAddress(), receivedTrain.getUrl());
		
		Mockito.when(mockDevice.getURL()).thenReturn(passUrl2);
		trainCaptor = ArgumentCaptor.forClass(Train.class);
		trainService.onDiscoveredDevice(mockDevice);
		verify(trainService, times(2)).pushTrainUpdate(Mockito.any());
		verify(trainService, times(2)).pushTrainUpdate(trainCaptor.capture());
		receivedTrain = trainCaptor.getValue();
		assertEquals(passUrl2.getDeviceAddress(), receivedTrain.getUrl());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testConnect() {
		trainService.connect(testTrain);
		assertNull(internalTrainMap.get(testTrain.getUrl()).getDeviceGovernor());

		URL adapterUrl = new URL("/FF:FF:FF:FF:FF:FF");
		ReflectionTestUtils.setField(trainService, "adapterUrl", adapterUrl);
		DeviceGovernor mockDeviceGovernor = Mockito.mock(DeviceGovernor.class);
		CharacteristicGovernor mockCharacteristicGovernor = Mockito.mock(CharacteristicGovernor.class);
		Mockito.when(mockBtManager.getDeviceGovernor(Mockito.any())).thenReturn(mockDeviceGovernor);
		Mockito.when(mockBtManager.getCharacteristicGovernor(Mockito.any(), Mockito.eq(true))).thenReturn(mockCharacteristicGovernor);
		Mockito.when(mockDeviceGovernor.getURL()).thenReturn(adapterUrl.copyWithDevice(offlineTrain.getUrl()));
		
		trainService.connect(offlineTrain);
		verify(mockBtManager).getDeviceGovernor(Mockito.notNull());
		verify(mockBtManager).getCharacteristicGovernor(Mockito.notNull(), Mockito.eq(true));
		verify(mockCharacteristicGovernor).addValueListener(Mockito.notNull());
		verify(mockCharacteristicGovernor).whenReady(Mockito.notNull());
		assertNotNull(offlineTrainCon.getDeviceGovernor());
		assertNotNull(offlineTrainCon.getCharacteristicGovernor());
		
		// Test the valueListener lambda
		ArgumentCaptor<ValueListener> valueArgumentCaptor = ArgumentCaptor.forClass(ValueListener.class);
		verify(mockCharacteristicGovernor).addValueListener(valueArgumentCaptor.capture());
		ReflectionTestUtils.setField(trainService, "handlerMap", mockHandlerMap);
		when(mockHandlerMap.containsKey(99)).thenReturn(true);
		MessageHandler mockMessageHandler = mock(MessageHandler.class);
		when(mockHandlerMap.get(99)).thenReturn(mockMessageHandler);
		valueArgumentCaptor.getValue().changed(new byte[] {0, 0, 99});
		verify(mockHandlerMap).containsKey(99);
		verify(mockHandlerMap).get(99);
		verify(mockMessageHandler).handleMessage(Mockito.eq(offlineTrainCon), Mockito.notNull());
		
		// Test the whenReady() lambda
		ArgumentCaptor<Function<BluetoothGovernor, Integer>> whenReadyCaptor
				= ArgumentCaptor.forClass(Function.class);
		verify(mockCharacteristicGovernor).whenReady(whenReadyCaptor.capture());
		when(mockScheduler.scheduleWithFixedDelay(Mockito.any(), Mockito.anyLong())).thenReturn(mock(ScheduledFuture.class));
		whenReadyCaptor.getValue().apply(null);
		assertNotNull(offlineTrainCon.getBatteryTask());
		assertNotNull(offlineTrainCon.getDistanceTask());
		assertTrue(offlineTrain.isOnline());
		verify(trainService).pushTrainUpdate(offlineTrain);
	}

	@Test
	public void testHubPropertiesHandler() {
		TrainConnection trainCon = new TrainConnection(testTrain);
		Message batteryMessage = new Message(new byte[]{6, 0, 
				(byte) TrainService.MessageType.HUB_PROPERTIES, 6, 6, 49});
		Message distanceMessage = new Message(new byte[]{6, 0, 
				(byte) TrainService.MessageType.HUB_PROPERTIES, 5, 6, -49});

		trainService.hubPropertiesHandler(trainCon, batteryMessage);
		assertEquals(49, testTrain.getBattery(), 0.001);
		
		trainService.hubPropertiesHandler(trainCon, distanceMessage);
		assertEquals(49f/127, testTrain.getDistance(), 0.001);
	}
	
	@Test
	public void testHubAttachedIoHandler() {
		TrainConnection trainCon = new TrainConnection(testTrain);
		Message attachedMessage = new Message(new byte[]{6, 0, 
				(byte) TrainService.MessageType.HUB_ATTACHED_IO, 99, 1, 49});
		Message detachedMessage = new Message(new byte[]{5, 0, 
				(byte) TrainService.MessageType.HUB_ATTACHED_IO, 99, 0});
		
		trainService.hubAttachedIoHandler(trainCon, attachedMessage);
		assertTrue(testTrain.getPorts().containsKey(99));
		assertEquals(49, testTrain.getPorts().get(99).getDeviceType());
		
		trainService.hubAttachedIoHandler(trainCon, detachedMessage);
		assertFalse(testTrain.getPorts().containsKey(99));
	}
	
	@Test
	public void testPortModeInformationHandler() {
		// Not implemented to do something in TrainService
	}
	
	@Test
	public void testShutdownTrain() {
		CharacteristicGovernor mockCharacteristicGovernor = Mockito.mock(CharacteristicGovernor.class);
		DeviceGovernor mockDeviceGovernor = Mockito.mock(DeviceGovernor.class);
		ScheduledFuture<?> mockBatteryUpdateTask = Mockito.mock(ScheduledFuture.class);
		ScheduledFuture<?> mockDistanceUpdateTask = Mockito.mock(ScheduledFuture.class);
		internalTrainMap.get(offlineTrain.getUrl()).setCharacteristicGovernor(mockCharacteristicGovernor);
		TrainConnection testTrainCon = internalTrainMap.get(testTrain.getUrl());
		testTrainCon.setCharacteristicGovernor(mockCharacteristicGovernor);
		testTrainCon.setDeviceGovernor(mockDeviceGovernor);
		testTrainCon.setBatteryTask(mockBatteryUpdateTask);
		testTrainCon.setDistanceTask(mockDistanceUpdateTask);
		// Dont shutdown offline train
		trainService.shutdownTrain(offlineTrain);
		verify(mockCharacteristicGovernor, times(0)).isReady();
		
		// Online train
		Mockito.when(mockCharacteristicGovernor.isReady()).thenReturn(true);
		trainService.shutdownTrain(testTrain);
		verify(mockCharacteristicGovernor).isReady();
		verify(mockCharacteristicGovernor).write(Mockito.notNull());
		verify(mockCharacteristicGovernor).removeValueListener(null);
		verify(mockDeviceGovernor).setConnectionControl(false);
		verify(mockBatteryUpdateTask).cancel(false);
		verify(mockDistanceUpdateTask).cancel(false);
		assertFalse(testTrain.isOnline());
	}

	@Test
	public void testConnectAllTrains() {
		// Put offline train online so connect() will return early
		offlineTrain.setOnline(true);
		trainService.connectAllTrains();
		verify(trainService, times(internalTrainMap.size())).connect(Mockito.notNull());
		assertNotEquals(0, internalTrainMap.size());
	}
	
	@Test
	public void testDisconnectAllTrains() {
		testTrain.setOnline(false);
		trainService.disconnectAllTrains();
		verify(trainService, times(internalTrainMap.size())).shutdownTrain(Mockito.notNull());
		assertNotEquals(0, internalTrainMap.size());
	}
	
	@Test
	public void testRemoveTrain() {
		trainService.removeTrain(offlineTrain);
		assertFalse(internalTrainMap.containsKey(offlineTrain.getUrl()));
	}
	
	@Test
	public void testStopAll() {
		trainService.stopAll();
		assertEquals(0, testPort.getPower());
		assertEquals(-50, offlinePort.getPower());
		verify(trainService).enqueueMotorUpdate(testPort);
		verify(trainService).pushTrainUpdate(testTrain);
	}
	
}
