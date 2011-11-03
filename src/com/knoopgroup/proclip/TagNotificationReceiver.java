/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.knoopgroup.proclip;

import android.app.Activity;
import android.os.Bundle;

/**
 * Receives a tapped notification event (power-cycle bluetooth)
 */
public class TagNotificationReceiver extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// start the undock
		TagRecognized tr = new TagRecognized(getApplicationContext());
		tr.cycleBluetooth();
		finish();
		// cancel the notification
		// com.knoopgroup.proclip.TagCheck.mNotificationManager.cancel(com.knoopgroup.proclip.TagRecognized.DockNotificationId);
	}
}
