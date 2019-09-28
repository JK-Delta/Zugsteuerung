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

import java.util.HashMap;
import java.util.Map;

public class Train {

	private final String url;
	private String name;
	private float battery, power, distance;
	private boolean online;
	private Map<Integer, Port> ports;
	private Color color;
	
	

	public Train() {
		this("", "");
	}

	public Train(String url, String name) {
		this.name = name;
		this.url = url;
		this.battery = 1.0f;
		this.power = 0.0f;
		this.online = false;
		this.color = new Color(0, 200, 0);
		this.distance = 0.0f;
		ports = new HashMap<>();
	}
	
	
	public float getBattery() {
		return battery;
	}
	
	public Color getColor() {
		return color;
	}
	
	public float getDistance() {
		return distance;
	}
	
	public String getName() {
		return name;
	}
	
	public Map<Integer, Port> getPorts() {
		return ports;
	}
	
	public float getPower() {
		return power;
	}
	
	public String getUrl() {
		return url;
	}
	
	
	public boolean isOnline() {
		return online;
	}
	
	
	public void setBattery(float battery) {
		this.battery = battery;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public void setDistance(float distance) {
		this.distance = distance;
	}
	
	public void setOnline(boolean online) {
		this.online = online;
	}
	
	public void setPower(float power) {
		this.power = power;
	}

}
