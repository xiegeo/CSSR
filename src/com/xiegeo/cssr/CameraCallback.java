package com.xiegeo.cssr;



import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import com.xiegeo.cssr.analyze.Analyze;
import com.xiegeo.cssr.analyze.Peak;
import com.xiegeo.cssr.analyze.Sampler;
import com.xiegeo.cssr.answer.SearchAnswer;

public class CameraCallback extends TextView implements PreviewCallback {
	static final String TAG = "CameraCallback";
	static Paint bluePaint;
	static {
		bluePaint = new Paint();
		bluePaint.setARGB(255, 0, 0, 255);
		bluePaint.setStrokeWidth(2.5f);
	}
	
	public CameraCallback(Context c) {
		super(c);
		setBackgroundColor(Color.TRANSPARENT);
		setTextSize(TypedValue.COMPLEX_UNIT_FRACTION, 30);
	}
    private boolean displayedNewFrame = true; //one onDraw for every frame
    boolean active = true;//to synchronize Analyze.last
	byte[] newData;
	Size previewSize;
	int[] colorMap;
	int colorMapStride;
	Camera mCamera;
	
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (!displayedNewFrame || !active) {
			camera.addCallbackBuffer(data);
			//Log.d(TAG,"skipped: displayedNewFrame:"+displayedNewFrame+" active:"+active+" Main.mLibrary:"+Main.getLibrary());
			return;
		}
		mCamera = camera;
		displayedNewFrame = false;
		newData = data;
		previewSize = camera.getParameters().getPreviewSize();
		new CalculateTask().execute(this);		
	}
	SearchAnswer calculate() {
		try {
			Sampler.clearEndPoints();
			Log.v(TAG, "start analyze");
			Analyze a = new Analyze(newData, previewSize.width, previewSize.height);
			colorMap = Analyze.colorMap;
			colorMapStride = a.width;
			return MainActivity.getLibrary().find(a.css);
		}catch (Throwable e) {
			Log.w(TAG, e);
		}
		return SearchAnswer.NO_MATCH;
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (colorMap!=null) {
			final int cw = canvas.getWidth();
			final int ch = canvas.getHeight();
			final int pw = previewSize.width;
			final int ph = previewSize.height;
			
			if (Preferences.flipedImage) {
				canvas.scale(-1, 1, cw/2, ch/2);
			}
			
			if ( (cw == pw || ch == ph)) {
				int dx = (cw-pw)/2;
				int dy = (ch-ph)/2;
				canvas.drawBitmap(colorMap,0,colorMapStride, dx, dy, pw, ph, true, null);
			}else {
				Log.i(TAG, "resizing");
				Bitmap bitmap = Bitmap.createBitmap(colorMap,0,colorMapStride, pw, ph, Bitmap.Config.ARGB_8888);
				Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
				
				double ratioSrc = ph/(double)pw;
				double ratioDst = ch/(double)cw;
				double s;
				if (ratioSrc>ratioDst) {
					s = ch/(double)ph;
				}else {
					s = cw/(double)pw;
				}
				int dx = (cw-(int)(pw*s))/2;
				int dy = (ch-(int)(ph*s))/2;
				Rect dst = new Rect(dx, dy, cw-dx, ch-dy);
				canvas.drawBitmap(bitmap, src, dst, null);
			}
			
			
			//Sampler.drawEndPoints(canvas, cw, ch, dx, dy);
			
			if(Analyze.showCSS && Analyze.last != null) {
				Peak.drawPicks(canvas, Analyze.last.css.peaks);
			}else {
				//draw cross
				int cross = ch/20;
				canvas.drawLine(cw/2, (ch-cross)/2, cw/2, (ch+cross)/2, bluePaint);
				canvas.drawLine((cw-cross)/2, ch/2, (cw+cross)/2, ch/2, bluePaint);
			}
		}
		displayedNewFrame = true;
	}
	
	static int getBufferSize(Camera c) {
		Camera.Size size = c.getParameters().getPreviewSize();
		PixelFormat p = new PixelFormat();
        PixelFormat.getPixelFormatInfo(c.getParameters().getPreviewFormat(),p);
		
		return (size.height * size.width * p.bitsPerPixel)/8;
	}
}


