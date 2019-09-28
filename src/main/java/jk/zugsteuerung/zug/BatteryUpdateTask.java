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

import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;

public class BatteryUpdateTask implements Runnable {
	
	private TrainConnection trainCon;
	
	public BatteryUpdateTask(TrainConnection trainCon) {
		this.trainCon = trainCon;
	}

	@Override
	public void run() {
		byte[] batteryRequest = new byte[] {5, 0, (byte) 0x1, 0x6, 0x5};
		
		Train t = trainCon.getTrain();
		CharacteristicGovernor cg = trainCon.getCharacteristicGovernor();
		
		System.out.println("Update Battery for Train " + trainCon.getTrain().getUrl());
		if(t.isOnline() && cg.isReady()) {
			cg.write(batteryRequest);
			System.out.println("Update Battery for Train " + trainCon.getTrain().getUrl() + " successful");
		} else {
			System.out.println("Update Battery for Train " + trainCon.getTrain().getUrl() + " failed");
		}
	}
}
