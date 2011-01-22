/*
 * Copyright (C) 2011 DroidGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.droidgiro;

public class Registration {

	private String channel;

	private String message;

	public String getChannel() {
		return channel;
	}

	public String getMessage() {
		return message;
	}

	public boolean isSuccessful() {
		return channel != null;
	}

	@Override
	public String toString() {
		return "Registration [channel: " + channel + "]";
	}
}
