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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class TrainController {
	
	@Autowired
	private TrainService trainService;
	
	
	@RequestMapping("api/train")
	public Train updateTrain(@RequestBody Train train) {
		return trainService.updateTrain(train);
	}
	
	@RequestMapping("api/port")
	public void updatePort(@RequestBody Port port) {
		trainService.updatePort(port);
	}
	
	@RequestMapping("api/trainList")
	public Train[] trainList() {
		return trainService.trainList();
	}
	
	@RequestMapping("api/discover")
	public String discover() {
		trainService.toggleDiscovery();
		
		if(trainService.isDiscovering()) {
			return "1";
		} else {
			return "0";
		}
	}
	
	@RequestMapping("api/connect")
	public void connect(@RequestBody Train receivedTrain) {
		trainService.connect(receivedTrain);
	}
	
	@RequestMapping("api/trainUpdateStream")
	public Flux<Train> trainUpdateStream() {
		return Flux.<Train>create(emitter -> {
			TrainService.TrainListener tl = train -> emitter.next(train);
			trainService.registerTrainListener(tl);
			emitter.onDispose(() -> {
				trainService.unregisterTrainListener(tl);
			});
		});
	}
	
	@RequestMapping("api/disconnect")
	public void shutdownTrain(@RequestBody Train train) {
		trainService.shutdownTrain(train);
	}
	
	@RequestMapping("api/connectAll")
	public void connectAllTrains() {
		trainService.connectAllTrains();
	}
	
	@RequestMapping("api/disconnectAll")
	public void disconnectAllTrains() {
		trainService.disconnectAllTrains();
	}
	
	@RequestMapping("api/remove")
	public void removeTrain(@RequestBody Train train) {
		trainService.removeTrain(train);
	}
	
	@RequestMapping("api/stopAll")
	public void stopAll() {
		trainService.stopAll();
	}
	
}
