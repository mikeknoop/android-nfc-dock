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
import android.app.Notification;
import android.app.PendingIntent;
import android.util.Log;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;


/**
 * Receives a recognized proclip tag -- take action on it
 */
public class TagRecognized extends Activity {
	
	static final String TAG = "TagRecognized";
	public Handler mDockHandler;
	static Thread tUndockWatchdog;
	public Context context;
	static final int DockNotificationId = 1;
	public int btCheckTries;
	static boolean docked = false;
	static boolean killAllUndockWatchdog = false;
	static int prevMediaVolume;
		
	public TagRecognized(Context c) {
		context = c;
	}
	
	public void go() {
		mDockHandler = new Handler();
		dock();
		if (killAllUndockWatchdog) {
			if (tUndockWatchdog.isAlive()) {
				tUndockWatchdog.interrupt();
			}
		}
		tUndockWatchdog = new Thread() {
		    @Override
		    public void run() {
		        try {
		        	while (true) {
		        		// only care about checking undock if we are docked
		        		if (docked == true) {
				        	if(!com.knoopgroup.proclip.TagCheck.ndefTag.isConnected()) {
				        		// try reconnecting just to make sure there wasn't a temporary connection issue
				        		com.knoopgroup.proclip.TagCheck.ndefTag.connect();
							   if (com.knoopgroup.proclip.TagCheck.ndefTag.isConnected()) {
								   // still connected, it was a temporary disconnect
								   com.knoopgroup.proclip.TagCheck.ndefTag.close();	
							   } else {
								   // officially unconnected
								   com.knoopgroup.proclip.TagCheck.ndefTag.close();
								   undock();
							   }
				        	}
		        		}
			        	sleep(2*1000);
		        	}
		        } catch (Throwable t) {
		            // caught
		        }
		    }
		};
		tUndockWatchdog.start();
	}
	
	public void dock() {
		if (docked == true) {
			// we are already docked
		} else {
			enableNotification("Bluetooth starting...", "Bluetooth starting...");
			
			// docking sound
			playSound();
			
			// Bluetooth
			dockBluetoothHandler();
	
			// Set the volume in a delay otherwise the dock sound gets played at full vol
			final Runnable setVolume = new Runnable() {
				   public void run() {
						// Save the current value of mediavol
						AudioManager am = com.knoopgroup.proclip.TagCheck.mAudioManager;
						prevMediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
						int maxMediaVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
						int newMediaVolume = (int) Math.floor((double) maxMediaVolume * 0.95);
						am.setStreamVolume(AudioManager.STREAM_MUSIC, newMediaVolume, AudioManager.FLAG_SHOW_UI);
				   }
			};
			mDockHandler.removeCallbacks(setVolume);
			mDockHandler.postDelayed(setVolume, 250);
			
			// trigger whatever action we wanted to run when docked
			triggerDockIntentAction();
		}
	}
	
	public void undock() {
		undock(false);
	}
	public void undock(Boolean fromNotification) {
		writeLog("undock");
		if (docked == false) {
			// Undock method called before dock finished. Retry undock again in a few seconds
			final Runnable undockHandler = new Runnable() {
				   public void run() {
					   if (docked == false) {
						   // still not ready...
						   mDockHandler.removeCallbacks(this);
						   mDockHandler.postDelayed(this, 2*1000);
					   } else {
						   // now it is okay to continue undocking
						   undock();
					   }
				   }
			};
			mDockHandler.removeCallbacks(undockHandler);
			mDockHandler.postDelayed(undockHandler, 3*1000);
		} else {
			enableNotification("Bluetooth stopping...", "Bluetooth stopping...");

			// set volume back to before
			AudioManager am = com.knoopgroup.proclip.TagCheck.mAudioManager;
			am.setStreamVolume(AudioManager.STREAM_MUSIC, prevMediaVolume, AudioManager.FLAG_SHOW_UI);
			
			// Play the remove sound in a delay else it is played at full vol
			final Runnable setVolume = new Runnable() {
				   public void run() {
					   playSound();
				   }
			};
			mDockHandler.removeCallbacks(setVolume);
			mDockHandler.postDelayed(setVolume, 250);

			// stop bluetooth
			undockBluetoothHandler();
			
			// trigger whatever custom action we want to take when we undock
			triggerUndockIntentAction();
			
			// finally, kill the undock watchdog
			if (!fromNotification) {
				tUndockWatchdog.interrupt();
			}
			// always
			killAllUndockWatchdog = true;
		}
	}
	
	public void dockBluetoothHandler() {
		bluetoothStart();
		// Check to see if bluetooth was properly enabled
		btCheckTries = 0;
		final Runnable checkBluetooth = new Runnable() {
			   public void run() {
				   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				   if(mBluetoothAdapter.isEnabled()) {
					   // adapter is enabled, good to go
					   enableNotification("Bluetooth started", "Tap to undock manually");
					   docked = true;
				   } else {
					   // adapter is disabled, bluetooth hasn't started yet
					   mBluetoothAdapter.enable();
					   if (btCheckTries > 5) {
						   enableNotification("Bluetooth did not start", "Tap to undock manually");
						   docked = true;
					   }
					   
					   btCheckTries = btCheckTries + 1;
					   mDockHandler.removeCallbacks(this);
					   mDockHandler.postDelayed(this, 2*1000);
				   }
			   }
		};
		mDockHandler.removeCallbacks(checkBluetooth);
		mDockHandler.postDelayed(checkBluetooth, 2*1000);
	}
	
	public void undockBluetoothHandler() {
		bluetoothStop();
		final Runnable checkBluetoothStop = new Runnable() {
			   public void run() {
				   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				   if(mBluetoothAdapter.isEnabled()) {
					   // adapter is still enabled, try disabling again
					   bluetoothStop();
					   mDockHandler.postDelayed(this, 2500);
				   } else {
					   // adapter is disabled! done with bluetooth
					   disableNotification();
					   docked = false;
				   }
			   }
		};
		mDockHandler.removeCallbacks(checkBluetoothStop);
		mDockHandler.postDelayed(checkBluetoothStop, 2*1000);
	}
	
	public void bluetoothStart() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			enableNotification("Bluetooth not supported", "Tap to undock manually");
		} else {
			if (isBluetoothEnabled()) {
				// enabled
			} else {
				// disabled
				mBluetoothAdapter.enable();
			}
		}
	}
	
	public void bluetoothStop() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothAdapter.disable();
	}
	
	public Boolean isBluetoothEnabled() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.isEnabled()) {
			return true;
		} else {
			return false;
		}
	}
	
	public void writeLog(String msg) {
		Log.i(TAG, msg);
	}
	
	public void playSound() {
		 MediaPlayer mp = MediaPlayer.create(context, R.raw.discovered_tag_notification);
	     mp.start();
	}
	
	public void enableNotification(CharSequence tickerText, CharSequence contentText) {
		int icon = R.drawable.iconsmall;
		//CharSequence tickerText = "ProClip docked";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		
		CharSequence contentTitle = "ProClip docked";
		Intent notificationIntent = new Intent(context, TagNotificationReceiver.class);
		
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		com.knoopgroup.proclip.TagCheck.mNotificationManager.notify(DockNotificationId, notification);
	}
	
	public void disableNotification() {
		com.knoopgroup.proclip.TagCheck.mNotificationManager.cancel(DockNotificationId);
	}

	public void triggerDockIntentAction() {	
		/*
		Intent dockIntent = new Intent(Intent.ACTION_MAIN);
		dockIntent.addCategory(Intent.CATEGORY_CAR_DOCK);
		dockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		com.knoopgroup.proclip.TagCheck.context.startActivity(dockIntent);
		*/
		launchService("music");
	}

    public void triggerUndockIntentAction() {
    	launchService("home");
    }

    public void launchService(String service) {   
		Intent launchIntent = new Intent(context, TagLaunch.class);
		// service gets interpretted in TagLaunch
		launchIntent.putExtra("service",service);
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		com.knoopgroup.proclip.TagCheck.context.startActivity(launchIntent);
    }
    
}
