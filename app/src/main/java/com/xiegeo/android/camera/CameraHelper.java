package com.xiegeo.android.camera;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
/*
 * to support:
 * GINGERBREAD (2.3) (9)
 * open multiple cameras
 * front facing cameras
 *
 */
public class CameraHelper {

	private static final Base VERSIONED;
	private static final int numberOfCameras;
	static {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD ) {
			VERSIONED = new Base();
		}else {
			VERSIONED = new Gingerbread();
		}
		numberOfCameras = VERSIONED.getNumberOfCameras();
	}
	
	public static int getNumberOfCameras() {
		return numberOfCameras;
	}
	
	/*
	 * generate ids for use by ListPreference
	 */
	public static String[] getAllCameraIds() {
		String[] ids = new String[numberOfCameras];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = ""+i;
		}
		return ids;
	}
	/*
	 * generate human readable name for use by ListPreference
	 */
	public static String[] getAllCameraNames() {
		String[] ids = new String[numberOfCameras];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = VERSIONED.name(i);
		}
		return ids;
	}
	
	public static Camera open(int cameraId) {
		return VERSIONED.open(cameraId);
	}
	
	public static boolean isFrontFacing(int cameraId) {
		return VERSIONED.isFrontFacing(cameraId);
	}
	
	private static class Base{
		public int getNumberOfCameras() {
			return 1;
		}
		public Camera open(int cameraId) {
			return Camera.open();
		}
		public Boolean isFrontFacing(int cameraId) {
			return false;
		}
		
		public String name(int cameraId) {
			if (isFrontFacing(cameraId)) {
				return "#" + cameraId +" front";
			}else {
				return "#" + cameraId +" back";
			}
		}
	}
	
	@TargetApi(9)
	private static class Gingerbread extends Base{
		private Camera.CameraInfo info = new Camera.CameraInfo();
		public int getNumberOfCameras() {
			return Camera.getNumberOfCameras();
		}
		public Camera open(int cameraId) {
			return Camera.open(cameraId);
		}
		public Boolean isFrontFacing(int cameraId) {
			Camera.getCameraInfo(cameraId, info);
			return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
		}

	}
	
}
