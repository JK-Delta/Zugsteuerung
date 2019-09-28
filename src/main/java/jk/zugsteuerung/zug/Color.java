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

public class Color {

	private int r, g, b;
	
	public Color() {}
	public Color(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public int getB() {
		return b;
	}
	public int getG() {
		return g;
	}
	public int getR() {
		return r;
	}
	public byte toByteB() {
		return (byte) b;
	}
	public byte toByteG() {
		return (byte) g;
	}
	public byte toByteR() {
		return (byte) r;
	}
	
	public void setB(int b) {
		this.b = b;
	}
	public void setG(int g) {
		this.g = g;
	}
	public void setR(int r) {
		this.r = r;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Color) {
			Color c = (Color) obj;
			if(c.r == r && c.g == g && c.b == b) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return r + (g << 8) + (b << 16);
	}
	
}
