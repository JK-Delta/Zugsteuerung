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

import java.util.concurrent.ScheduledFuture;

import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.ValueListener;

public class TrainConnection {
	
	private Train train;
	
	private DeviceGovernor deviceGovernor;
	private CharacteristicGovernor characteristicGovernor;
	
	private ScheduledFuture<?> batteryTask;
	private ScheduledFuture<?> distanceTask;
	private ValueListener valueListener;
	
	public TrainConnection(Train t) {
		train = t;
	}
	
	public Train getTrain() {
		return train;
	}
	public void setTrain(Train train) {
		this.train = train;
	}
	public DeviceGovernor getDeviceGovernor() {
		return deviceGovernor;
	}
	public void setDeviceGovernor(DeviceGovernor deviceGovernor) {
		this.deviceGovernor = deviceGovernor;
	}
	public CharacteristicGovernor getCharacteristicGovernor() {
		return characteristicGovernor;
	}
	public void setCharacteristicGovernor(CharacteristicGovernor characteristicGovernor) {
		this.characteristicGovernor = characteristicGovernor;
	}
	public ScheduledFuture<?> getBatteryTask() {
		return batteryTask;
	}
	public void setBatteryTask(ScheduledFuture<?> batteryTask) {
		this.batteryTask = batteryTask;
	}
	public ScheduledFuture<?> getDistanceTask() {
		return distanceTask;
	}
	public void setDistanceTask(ScheduledFuture<?> distanceTask) {
		this.distanceTask = distanceTask;
	}
	public ValueListener getValueListener() {
		return valueListener;
	}
	public void setValueListener(ValueListener valueListener) {
		this.valueListener = valueListener;
	}
	
}
