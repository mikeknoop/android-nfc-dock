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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Holds the context to launch the music app
 */
public class TagLaunch extends Activity {
	static Context context;
	static final String TAG = "TagLaunch";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		Bundle extras = getIntent().getExtras();
		String mIntentServiceString = extras.getString("service");
		if (mIntentServiceString.equals("music")) {
			launchMusic();
		} else if (mIntentServiceString.equals("home")) {
			launchHome();
		} else {
			Log.i(TAG, "No launch string match");
		}
		finish();
	}
	
    public void launchMusic() {   
		//Intent mediaIntent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
		//mediaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//context.startActivity(mediaIntent);
		//com.knoopgroup.proclip.TagCheck.context.startActivity(mediaIntent);
    	
    	//Intent mediaIntent = new Intent(Intent.ACTION_MAIN);
    	//mediaIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    	//final ComponentName cn = new ComponentName("com.google.android.music", "com.android.music.activitymanagement.TopLevelActivity");
    	//mediaIntent.setComponent(cn);
    	//mediaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	
		Intent mediaIntent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
		mediaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(mediaIntent);
    	
    }
    
    public void launchHome() {
    	Intent startMain = new Intent(Intent.ACTION_MAIN);
    	//startMain.removeCategory(Intent.CATEGORY_CAR_DOCK);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //context.startActivity(startMain);
        context.startActivity(startMain);
    }
}
