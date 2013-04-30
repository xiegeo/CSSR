package com.xiegeo.cssr;

import com.xiegeo.android.camera.CameraHelper;
import com.xiegeo.cssr.analyze.Analyze;
import com.xiegeo.cssr.answer.Summary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
	
	public static int cameraId = 0;
	
	public static boolean flipedImage = false;
	
	static void applyPreferences(Context c) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        Analyze.turtleJump = Integer.valueOf(sp.getString("turtleJump", ""+Analyze.turtleJump));
        Library.mirror = sp.getBoolean("mirror", Library.mirror);
        Preview.pausable = sp.getBoolean("pausable", Preview.pausable);
        Analyze.showCSS = sp.getBoolean("showCSS", Analyze.showCSS);
        Summary.enableTTS = sp.getBoolean("enableTTS", Summary.enableTTS);
        
        cameraId = Integer.valueOf(sp.getString("cameraId", ""+cameraId));
        flipedImage = CameraHelper.isFrontFacing(cameraId);
	}
	
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        ListPreference cameraPreference = (ListPreference) findPreference("cameraId");
        cameraPreference.setEntries(CameraHelper.getAllCameraNames());
        cameraPreference.setEntryValues(CameraHelper.getAllCameraIds());
        
        if (CameraHelper.getNumberOfCameras() < 2) {
			cameraPreference.setEnabled(false);
			cameraPreference.setSummary("Only one camera detected.");
		}
	}
}
