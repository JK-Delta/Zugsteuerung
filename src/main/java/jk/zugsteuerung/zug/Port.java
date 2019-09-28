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

public class Port {

	private String url;
	private int id, deviceType, power;
	
	public Port() {
		url = "";
	}
	
	public Port(String url, int id, int deviceType) {
		this.url = url;
		this.id = id;
		this.deviceType = deviceType;
	}
	
	public int getDeviceType() {
		return deviceType;
	}
	
	public int getId() {
		return id;
	}
	
	public int getPower() {
		return power;
	}
	
	public String getUrl() {
		return url;
	}
	
	/**Sets the type of the connected LPF2 Device according to the
	 * <a href="https://lego.github.io/lego-ble-wireless-protocol-docs/index.html#io-type-id">io-type-id list</a>.
	 * 
	 * @param deviceType
	 */
	public void setDeviceType(int deviceType) {
		this.deviceType = deviceType;
	}

	public void setPower(int power) {
		this.power = power;
	}
	
	
}
