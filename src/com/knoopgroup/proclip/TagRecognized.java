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
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;


/**
 * Receives a recognized proclip tag -- take action on it
 */
public class TagRecognized extends Activity {
	
	static final String TAG = "TagRecognized";
	public Handler mNefHandler, mVolumeHandler, mBluetoothHandler, mBluetoothHandlerOff, mBluetoothHandlerOn, mDockHandler, mUndockHandler;
	public Context context;
	static final int DockNotificationId = 1;
	static int docked = 0;
	static int prevMediaVolume;
	static int btTries;
	static Boolean btPowerCycling = false;
		
	public TagRecognized(Context c) {
		context = c;
	}
	
	public void go() {
		dock();
		mNefHandler = new Handler();
		final Runnable checkUndock = new Runnable() {
			   public void run() {
				   if(com.knoopgroup.proclip.TagCheck.ndefTag.isConnected()) {
					   mNefHandler.postDelayed(this, 5000);
				   } else {
					   try {
						   com.knoopgroup.proclip.TagCheck.ndefTag.connect();
						   if (com.knoopgroup.proclip.TagCheck.ndefTag.isConnected()) {
							   // still connected, loop again
							   com.knoopgroup.proclip.TagCheck.ndefTag.close();	
							   mNefHandler.postDelayed(this, 3*1000);
						   } else {
							   // nope there was an error. Attempt to undock now
							   com.knoopgroup.proclip.TagCheck.ndefTag.close();	
							   undock();
						   }		   
					   } catch (Throwable t) {
						   // Tag can't be reached! Undock now
						   undock();
					   }
				   }
			   }
		};

		// setup the polling loop to check for device undock
		mNefHandler.removeCallbacks(checkUndock);
		mNefHandler.postDelayed(checkUndock, 3*1000);
	}
	
	public void dock() {
		if (docked > 0 || btPowerCycling) {
			// Undock method called before dock finished. Retry again in a few seconds
			mDockHandler = new Handler();
			final Runnable undockHandler = new Runnable() {
				   public void run() {
					   if (docked > 0 || btPowerCycling) {
						   // still not ready...
						   mDockHandler.removeCallbacks(this);
						   mDockHandler.postDelayed(this, 2*1000);
					   } else {
						   // now it is okay to continue docking
						   dock();
					   }
				   }
			};
			mDockHandler.removeCallbacks(undockHandler);
			mDockHandler.postDelayed(undockHandler, 3*1000);
		} else {
			unlockScreen();
			docked=docked+1;
			playSound();
			
			// Bluetooth
			enableNotification("Bluetooth starting...", "Bluetooth starting...");
			bluetoothStart();
	
			// Set the volume in a delay otherwise the dock sound gets played at full vol
			mVolumeHandler = new Handler();
			final Runnable setVolume = new Runnable() {
				   public void run() {
						// Save the current value of mediavol
						AudioManager am = com.knoopgroup.proclip.TagCheck.mAudioManager;
						prevMediaVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
						int maxMediaVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
						int newMediaVolume = (int) Math.floor((double) maxMediaVolume * 0.95);
						am.setStreamVolume(AudioManager.STREAM_MUSIC, newMediaVolume, AudioManager.FLAG_SHOW_UI);
						docked=docked+1;
				   }
			};
			
			mVolumeHandler.removeCallbacks(setVolume);
			mVolumeHandler.postDelayed(setVolume, 250);
			
			triggerDockIntent();
			docked=docked+1;
			
			writeLog("ProClip docked.");
		}
	}
	
	private void unlockScreen() {
		//com.knoopgroup.proclip.TagCheck.mKeyguardManager.exitKeyguardSecurely(null);
		//com.knoopgroup.proclip.TagCheck.mKeyguardLock.disableKeyguard();
		//com.knoopgroup.proclip.TagCheck.mKeyguardLock.reenableKeyguard();
	}

	public void undock() {
		if (docked < 4 || btPowerCycling) {
			// Undock method called before dock finished. Retry again in a few seconds
			mUndockHandler = new Handler();
			final Runnable undockHandler = new Runnable() {
				   public void run() {
					   if (docked < 4 || btPowerCycling) {
						   // still not ready...
							mUndockHandler.removeCallbacks(this);
							mUndockHandler.postDelayed(this, 2*1000);
					   } else {
						   // now it is okay to continue undocking
						   undock();
					   }
				   }
			};
			mUndockHandler.removeCallbacks(undockHandler);
			mUndockHandler.postDelayed(undockHandler, 3*1000);
		} else {
			enableNotification("Bluetooth stopping...", "Bluetooth stopping...");
			docked=docked-1;
			
			AudioManager am = com.knoopgroup.proclip.TagCheck.mAudioManager;
			am.setStreamVolume(AudioManager.STREAM_MUSIC, prevMediaVolume, AudioManager.FLAG_SHOW_UI);
			docked=docked-1;
	
			//playSound();
			bluetoothStop();
			
			triggerUndockIntent();
			docked=docked-1;
			// bluetooth might not disable first try, so we set a timer
			mBluetoothHandler = new Handler();
			final Runnable checkBluetoothStop = new Runnable() {
				   public void run() {
					   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
					   if(mBluetoothAdapter.isEnabled()) {
						   // adapter is still enabled, try disabling again
						   bluetoothStop();
						   mBluetoothHandler.postDelayed(this, 2500);
					   } else {
						   // adapter is disabled! done with bluetooth
							disableNotification();
							writeLog("ProClip Undocked.");
							docked=docked-1;
					   }
				   }
			};
	
			// setup the polling loop to check for device undock
			mBluetoothHandler.removeCallbacks(checkBluetoothStop);
			mBluetoothHandler.postDelayed(checkBluetoothStop, 2500);
		}
	}
	
	public void bluetoothStart() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
		} else {
			if (isBluetoothEnabled()) {
				// enabled
				enableNotification("ProClip docked", "ProClip docked");
			} else {
				// disabled, just enable it
				mBluetoothAdapter.enable();
				// Check to see if bluetooth was properly enabled
				mBluetoothHandler = new Handler();
				btTries = 0;
				final Runnable checkBluetooth = new Runnable() {
					   public void run() {
						   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
						   if(mBluetoothAdapter.isEnabled()) {
							   // adapter is enabled, good to go
							   enableNotification("ProClip docked", "Tap to power-cycle Bluetooth");
							   docked=docked+1;
						   } else {
							   // adapter is disabled, bluetooth hasn't started yet
							   mBluetoothAdapter.enable();
							   if (btTries > 5) {
								   enableNotification("Bluetooth did not start.", "Tap to power-cycle Bluetooth");
								   docked=docked+1;
								   // mPowerManager.reboot("Bluetooth failed");
							   }
							   
							   btTries = btTries + 1;
							   mBluetoothHandler.removeCallbacks(this);
							   mBluetoothHandler.postDelayed(this, 2*1000);
						   }
					   }
				};
				mBluetoothHandler.removeCallbacks(checkBluetooth);
				mBluetoothHandler.postDelayed(checkBluetooth, 2*1000);
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
		//CharSequence contentText = "Tap to power-cycle Bluetooth";
		Intent notificationIntent = new Intent(context, TagNotificationReceiver.class);
		
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		com.knoopgroup.proclip.TagCheck.mNotificationManager.notify(DockNotificationId, notification);
	}
	
	public void disableNotification() {
		com.knoopgroup.proclip.TagCheck.mNotificationManager.cancel(DockNotificationId);
	}

	public void triggerDockIntent() {	
		/*
		Intent dockIntent = new Intent(Intent.ACTION_MAIN);
		dockIntent.addCategory(Intent.CATEGORY_CAR_DOCK);
		dockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		com.knoopgroup.proclip.TagCheck.context.startActivity(dockIntent);
		*/
		launchService("music");
	}

    public void triggerUndockIntent() {

    	launchService("home");
    }

    public void launchService(String service) {   

		Intent launchIntent = new Intent(context, TagLaunch.class);
		launchIntent.putExtra("service",service);
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		com.knoopgroup.proclip.TagCheck.context.startActivity(launchIntent);
    	
    }

    // always leaves power on after finished
	public void cycleBluetooth() {
		if (docked < 4) {
			// do nothing, not fully docked yet
		} else {
			btPowerCycling = true;
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			enableNotification("Bluetooth is power-cycling...", "Bluetooth is power-cycling...");
			if (isBluetoothEnabled()) {
				// bluetooth enabled (expected), so disable it then re-enable
				mBluetoothHandlerOff = new Handler();
				mBluetoothAdapter.disable();
				final Runnable checkBluetoothOff = new Runnable() {
					   public void run() {
						   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
						   if(mBluetoothAdapter.isEnabled()) {
							   mBluetoothHandlerOff.removeCallbacks(this);
							   mBluetoothHandlerOff.postDelayed(this, 2*1000);
						   } else {
							   // now bt is diabled, re-enable it
							    mBluetoothHandlerOff.removeCallbacks(this);
								mBluetoothHandlerOn = new Handler();
								btTries = 0;
								mBluetoothAdapter.disable();
								final Runnable checkBluetoothOn = new Runnable() {
									   public void run() {
										   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
										   if(mBluetoothAdapter.isEnabled()) {
											   // bluetooth is enabled. Done.
											   enableNotification("Bluetooth power-cycle complete", "Tap to power-cycle Bluetooth");
											   btPowerCycling = false;
											   mBluetoothHandlerOn.removeCallbacks(this);
										   } else {
											   // now bt is diabled, re-enable it
											   mBluetoothAdapter.enable();
											   if (btTries > 5) {
												   enableNotification("Bluetooth did not start", "Tap to power-cycle Bluetooth");
												   btPowerCycling = false;
												   // mPowerManager.reboot("Bluetooth failed");
											   }
											   
											   btTries = btTries + 1;
											   mBluetoothHandlerOn.removeCallbacks(this);
											   mBluetoothHandlerOn.postDelayed(this, 2*1000);
										   }
									   }
								};
								mBluetoothHandlerOn.removeCallbacks(checkBluetoothOn);
								mBluetoothHandlerOn.postDelayed(checkBluetoothOn, 2*1000);
						   }
					   }
				};
				mBluetoothHandlerOff.removeCallbacks(checkBluetoothOff);
				mBluetoothHandlerOff.postDelayed(checkBluetoothOff, 2*1000);
			} else {
				// bluetooth is not enabled, just enable it
			}
		}
	}
    
}
