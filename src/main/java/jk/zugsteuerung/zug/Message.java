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

public class Message {
	
	private byte[] array;
	private int messageLength;
	
	public Message(byte[] messageArray) {
		array = messageArray;
		messageLength = array[0];
		if(messageLength > array.length) {
			messageLength = array.length;
			array[0] = (byte) messageLength;
		}
	}
	
	public byte[] getByteArray() {
		return array;
	}
	
	public byte byteAt(int index) {
		return array[index];
	}
	
	public String getString(int start, int length) {
		return new String(array, start, length);
	}
	
	
	public static Message motorPowerChange(int portId, int power) {
		return new Message(new byte[] {8, 0, (byte) 0x81, (byte) portId, 0x11, 0x51, 0, (byte) power});
	}

	public static Message ledChange(int colorIndex) {
		return new Message(new byte[] {8, 0, (byte) 0x81, 0x32, 0x11, 0x51, 0x00, (byte) colorIndex});
	}
	
	public static Message ledChangeRgb(Color c) {
		// Command Port Output(0x81), LED Port(0x32), Execute without buffer(0x11)
		// Sub Command WriteDirectModeData(0x51), RGB Mode(0x01)
		return new Message(new byte[] {10, 0, (byte) 0x81, 0x32, 0x11, 0x51, 0x01, c.toByteR(), c.toByteG(), c.toByteB()});
	}
	
	public static Message shutdown() {
		return new Message(new byte[] {4, 0, (byte) 0x2, 0x1});
	}

}
