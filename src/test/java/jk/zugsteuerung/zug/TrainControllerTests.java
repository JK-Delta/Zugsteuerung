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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import jk.zugsteuerung.zug.Port;
import jk.zugsteuerung.zug.Train;
import jk.zugsteuerung.zug.TrainController;
import jk.zugsteuerung.zug.TrainService;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@RunWith(SpringRunner.class)
@SpringBootTest
//@WebMvcTest
public class TrainControllerTests {
	
	@MockBean
	private TrainService trainService;
	
	@Autowired
	private TrainController trainController;
	
	private Train testTrain = new Train();
	private Port testPort = new Port();

	@Test
	public void testTrainController() {
		TrainController trainController = new TrainController();
		assertNotNull("Cant construct TrainController", trainController);
	}

	@Test
	public void testUpdateTrain() {
		trainController.updateTrain(testTrain);
		Mockito.verify(trainService).updateTrain(testTrain);
	}

	@Test
	public void testUpdatePort() {
		trainController.updatePort(testPort);
		Mockito.verify(trainService).updatePort(testPort);
	}

	@Test
	public void testTrainList() {
		Train[] testTrainArray = new Train[0];
		Mockito.when(trainService.trainList()).thenReturn(testTrainArray);
		Train[] receivedArray = trainController.trainList();
		assertArrayEquals(testTrainArray, receivedArray);
	}

	@Test
	public void testDiscover() {
		Mockito.when(trainService.isDiscovering()).thenReturn(true);
		String receivedString = trainController.discover();
		assertEquals("1", receivedString);
		
		Mockito.when(trainService.isDiscovering()).thenReturn(false);
		receivedString = trainController.discover();
		assertEquals("0", receivedString);
	}

	@Test
	public void testConnect() {
		trainController.connect(testTrain);
		Mockito.verify(trainService).connect(testTrain);
	}

	@Test
	public void testTrainUpdateStream() {
		ArgumentCaptor<TrainService.TrainListener> trainListenerCaptor = ArgumentCaptor.forClass(TrainService.TrainListener.class);

		Flux<Train> flux = trainController.trainUpdateStream();
		assertNotNull(flux);
		
		Disposable fluxDisposable = flux.subscribe(train -> assertEquals(testTrain, train));
		Mockito.verify(trainService).registerTrainListener(trainListenerCaptor.capture());
		TrainService.TrainListener tl = trainListenerCaptor.getValue();
		tl.onTrainUpdate(testTrain);

		fluxDisposable.dispose();
		Mockito.verify(trainService).unregisterTrainListener(tl);
	}

	@Test
	public void testShutdownTrain() {
		trainController.shutdownTrain(testTrain);
		Mockito.verify(trainService).shutdownTrain(testTrain);
	}
	
	@Test
	public void testConnectAllTrains() {
		trainController.connectAllTrains();
		Mockito.verify(trainService).connectAllTrains();
	}
	
	@Test
	public void testDisconnectAllTrains() {
		trainController.disconnectAllTrains();
		Mockito.verify(trainService).disconnectAllTrains();
	}
	
	@Test
	public void testRemoveTrain() {
		trainController.removeTrain(testTrain);
		Mockito.verify(trainService).removeTrain(testTrain);
	}
	
	@Test
	public void testStopAll() {
		trainController.stopAll();
		Mockito.verify(trainService).stopAll();
	}

}
