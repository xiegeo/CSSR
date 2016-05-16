package com.xiegeo.cssr.analyze;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Sampler {
	static final String TAG = "Sampler";
	
	// in turtle format
	static private final IntArray endPoints = new IntArray(16);
	
	static final int[][] scanPrim =
	{{2,0},{-2,0},{0,2},{0,-2},
	 {2,2},{-2,2},{2,-2},{-2,-2},
	 {2,1},{-2,1},{1,2},{1,-2},
	 {2,-1},{-2,-1},{-1,2},{-1,-2}};
	public static short[][][] lineScans(final AbstractAreaSelector area)
	{
		short[][][] scans = new short[scanPrim.length][][];
		for (int i = 0; i < scanPrim.length; i++) {
			scans[i] = lineScan(area, scanPrim[i][0], scanPrim[i][1]);
		}
		return scans;
	}
	/**
	 * start from centre, stop at a significant edge
	 * @param area	The area selector holding the sample
	 * @param dx	
	 * @param dy	
	 * @return 3 short (0-255) arrays for Y V U
	 */
	public static short[][] lineScan(final AbstractAreaSelector area, final int dx, final int dy){
		final byte[] NV21 = area.NV21;
		final int w = area.w;
		final int h = area.h;
		final int step = dx+dy*w;
		final int a1 = -dy-dx + (dx-dy)*w;
		final int a2 = dy-dx + (-dx-dy)*w;
		final int b1 = -dy+2*dx + (dx+2*dy)*w;
		final int b2 = dy+2*dx + (-dx+2*dy)*w;
		
		final int startPx = w/2 + h/2*w;
		
		final int maxLength;
		if(dy!=0 && ( (dx==0) || Math.abs((h)/dy) <= Math.abs((w)/dx)))
		{	maxLength = Math.abs(h/2/dy)-4;
		}else
		{	maxLength = Math.abs(w/2/dx)-4;
		}
		
		final int cutoff = 5;
		final double roundFacter = 0.8;
		final double roundMultiplier = roundFacter/(1-roundFacter);//4

		int[] data = new int[maxLength];
		final int length;
		edge:
		{
			int i = maxLength-1;
			double brightAvg = (int)(NV21[startPx+step*i]&0xFF) *roundMultiplier;
			data[i] = (int)brightAvg;
			for(int k = startPx+step*(--i); i>=cutoff; k-=step)
			{
				//Log.d(TAG,"k: "+k + " x:"+k%w + " y:"+k/w);
				int bright = NV21[k]&0xFF;
				brightAvg = (bright + brightAvg)*roundFacter;
				data[i] = (int)brightAvg;
				i--;
			}
			/*
			i=0;
			brightAvg = (int)(NV21[startPx+step*i]&0xFF) *roundMaltiplier;
			for(int k = startPx+step*(++i); i<cutoff; k+=step)
			{
				int bright = NV21[k]&0xFF;
				brightAvg = (bright + brightAvg)*roundFacter;
				i++;
			}
			*/
			//i = cutoff;
			i++;
			int scanMax = 0;
			for(int k = startPx+step*i; i<maxLength; k+=step)
			{
				//Log.d(TAG,"k: "+k);
				int diff = (int)((brightAvg - data[i]) /roundMultiplier);
				int bright = NV21[k]&0xFF;
				brightAvg = (bright + brightAvg)*roundFacter;
				
				int diff2 = diff*diff;
				scanMax = Math.max(scanMax, diff2);
				data[i] = diff2;
				i++;
			}
			//Log.d(TAG,"scanMax: "+scanMax);
			i = cutoff;
			int scanColorMax = 0;
			for(int k = startPx+step*i; i<maxLength; k+=step)
			{
				if(scanMax < data[i]*8)
				{
					int a1i = area.yIndexToCindex(k+a1);
					int a2i = area.yIndexToCindex(k+a2);
					int b1i = area.yIndexToCindex(k+b1);
					int b2i = area.yIndexToCindex(k+b2);
					int Vd = ((NV21[a1i]&0xFF) + (NV21[a2i]&0xFF) - (NV21[b1i]&0xFF) - (NV21[b2i]&0xFF));
					int Ud = ((NV21[a1i+1]&0xFF) + (NV21[a2i+1]&0xFF) - (NV21[b1i+1]&0xFF) - (NV21[b2i+1]&0xFF));
					int cd = (Vd*Vd + Ud*Ud)/4;
					int td = data[i]+cd;
					data[i] = td;
					scanColorMax = Math.max(scanColorMax, td);
				}
				i++;
			}
			for(i=cutoff;i<maxLength;i++)
			{
				if(scanColorMax < data[i]*3)
				{
					length=i+1-5;
					break edge;
				}
			}
			
			//Log.w(TAG,"use defalt length");
			length = maxLength/2;
			//throw new Error("edge not found:"+w+"x"+h);
		}
		endPoints.add(area.yToTurtle(startPx+step*length));
		short[] Ydata = new short[length];
		
		int k = startPx;
		for (int i = 0; i<length;i++ ) {
			Ydata[i] = (short) (NV21[k] & 0xFF);
			k+=step;
		}
		
		
		short[] Vdata = new short[length];
		short[] Udata = new short[length];
		
		k = area.yIndexToCindex(startPx);
		final int step1 = area.yIndexToCindex(startPx+step) - area.yIndexToCindex(startPx);
		final int step2 = area.yIndexToCindex(startPx+step*2) - area.yIndexToCindex(startPx+step);
		
		for (int i = 0; i<length;i++ ) {
			//Log.d(TAG,"k1: "+k);
			Vdata[i] = (short) (NV21[k] & 0xFF);
			Udata[i] = (short) (NV21[k+1] & 0xFF);
			k+=step1;
			if (++i<length) {
				//Log.d(TAG,"k2: "+k);
				Vdata[i] = (short) (NV21[k] & 0xFF);
				Udata[i] = (short) (NV21[k+1] & 0xFF);
				k+=step2;
			}
		}
		
		return new short[][] {Ydata,Vdata,Udata};
	}
	
	static Paint myPaint;
	static {
		myPaint = new Paint();
		myPaint.setARGB(255, 0, 0, 255);
	}
	public static void clearEndPoints()
	{
		endPoints.clear();
	}
	public static void drawEndPoints(Canvas canvas, int w, int h, int dx, int dy)
	{
		for (int i = 0; i < endPoints.size(); i++) {
			int p = endPoints.get(i);
			canvas.drawLine(w/2, h/2, Analyze.toX(p)+dx, Analyze.toY(p)+dy, myPaint);
		}
	}
	
	public static short max(short s1, short s2) {
		if (s1 < s2) {
			return s2;
		}
		return s1;
	}
	public static short max(short s1, short s2, short s3) {
		if (s1 < s2) {
			s1 = s2;
		}
		if (s1 < s3) {
			return s3;
		}
		return s1;
	}
	public static short min(short s1, short s2) {
		if (s1 > s2) {
			return s2;
		}
		return s1;
	}
	public static short min(short s1, short s2, short s3) {
		if (s1 > s2) {
			s1 = s2;
		}
		if (s1 > s3) {
			return s3;
		}
		return s1;
	}
	
	public static short max(short[] s) {
		short max = s[0];
		for (int i = 1; i < s.length; i++) {
			if (max < s[i]) {
				max = s[i];
			}
		}
		return max;
	}
	public static short min(short[] s) {
		short min = s[0];
		for (int i = 1; i < s.length; i++) {
			if (min > s[i]) {
				min = s[i];
			}
		}
		return min;
	}
}
