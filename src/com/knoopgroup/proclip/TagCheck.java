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
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;

/**
 * An {@link Activity} which handles a broadcast of a new tag that the device
 * just discovered.
 */
public class TagCheck extends Activity {

    static final String TAG = "TagCheck";
    static Intent tagIntent;
    static Ndef ndefTag;
    static NotificationManager mNotificationManager;
    static AudioManager mAudioManager;
    static PowerManager mPowerManager;
    static KeyguardManager mKeyguardManager;
    static KeyguardLock mKeyguardLock;
    static TagRecognized tr;
    static Context context;
    
    /**
     * This activity will finish itself in this amount of time if the user
     * doesn't do anything.
     */
    static final int ACTIVITY_TIMEOUT_MS = 1 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mKeyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		mKeyguardLock = mKeyguardManager.newKeyguardLock(TAG);
		context = this;
        finish();
        resolveIntent(getIntent());
    }

    void resolveIntent(Intent intent) {
        // Parse the intent
    	tagIntent = intent;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
        	
		   Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		   ndefTag = Ndef.get(tag);
		   
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                 NdefMessage msg = (NdefMessage) rawMsgs[0];
                 NdefRecord[] records = msg.getRecords();
                 String check = byteToStr(records[0].getPayload());
                 if (check.contains("com.knoopgroup.proclip")) {
                	 // recognized, take action
                	 tr = new TagRecognized(getApplicationContext());
                	 tr.go();
                 } else {
                	 // Tag scanned is not a proclip indicator
                 }
            } else {
                // Unknown tag type
            }
        } else {
            Log.e(TAG, "Unknown intent " + intent);
            finish();
            return;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

     /**
     * Converts a byte to a String.
     * @param input
     * @return byte
     */
    public String byteToStr(byte[] input) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < input.length; i++)
            if (input[i] != 0) {
                buffer.append( new Character((char)input[i]).toString());
            }
        return buffer.toString();
    }

}
