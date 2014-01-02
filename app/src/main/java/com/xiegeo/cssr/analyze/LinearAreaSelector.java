package com.xiegeo.cssr.analyze;

import java.util.Arrays;

import android.graphics.Color;

public class LinearAreaSelector extends AbstractAreaSelector {

	private final LinearStatics statics; 
	
	public LinearAreaSelector(byte[] NV21data, int w, int h) {
		super(NV21data, w, h);
		statics = calculateAndMerge(Sampler.lineScans(this));
		
	}

	@Override
	public boolean isOut(int xp, int yp) {
		if(Math.abs(xp*2-w)<20 && Math.abs(yp*2-h)<20)
		{
			return false;
		}
		
		int y = NV21[yp*w+xp] & 0xFF;
		/*int y2 = NV21[pos+1] & 0xFF;
		int y3 = NV21[pos+width] & 0xFF;
		int dxs = (y-y2)*(y-y2);
		int dys = (y-y3)*(y-y3);*/
		//if(dxs+dys>20) return true;
		
		if (y<statics.Ylow || y>statics.Yhigh)
		{
			colorCode = Color.RED;
			return true;
		}
			
		
		int dis = colorIndex(xp, yp);
		
		
		/*int u = NV21[dis] & 0xFF;
		int v = NV21[dis+1] & 0xFF;
		double e = Math.abs(u-Uave)/Ue + Math.abs(v-Vave)/Ve;
		if (e>2)
			return true;
		e -= Math.abs(y-Yave)/Ye;
		return e>1;*/
		
		
		colorCode = Color.GREEN;
		int v = NV21[dis] & 0xFF;
		if(LinearStatics.errorS2(y,v, statics.VYc, statics.VYr) > statics.VYes)
			return true;
		int u = NV21[dis+1] & 0xFF;
		return (LinearStatics.errorS2(y,u, statics.UYc, statics.UYr) > statics.UYes);
	}
	
	private static LinearStatics calculateAndMerge(short[][][] samples)
	{
		LinearStatics[] l = new LinearStatics[samples.length];
		for (int i = 0; i < l.length; i++) {
			l[i] = new LinearStatics(samples[i][0], samples[i][1], samples[i][2]);
		}
		return new LinearStatics(l);
	}
}
class LinearStatics{
	final int Ylow,Yhigh;
	final float UYr,VYr;
	final int UYc,VYc;
	final int VYes,UYes;
	LinearStatics(LinearStatics[] toMerge){
		
		int length = toMerge.length;
		float[] UYrArray = new float[length];
		float[] VYrArray = new float[length];
		int[] UYcArray = new int[length];
		int[] VYcArray = new int[length];
		int[] VYesArray = new int[length];
		int[] UYesArray = new int[length];
		int[] YlowArray = new int[length];
		int[] YhighArray = new int[length];
		for (int i = 0; i < length; i++) {
			UYrArray[i] = toMerge[i].UYr;
			VYrArray[i] = toMerge[i].VYr;
			UYcArray[i] = toMerge[i].UYc;
			VYcArray[i] = toMerge[i].VYc;
			VYesArray[i] = toMerge[i].VYes;
			UYesArray[i] = toMerge[i].UYes;
			YlowArray[i] = toMerge[i].Ylow;
			YhighArray[i] = toMerge[i].Yhigh;
		}
		Arrays.sort(UYrArray);
		UYr =UYrArray[length/2];
		Arrays.sort(VYrArray);
		VYr = VYrArray[length/2];
		Arrays.sort(UYcArray);
		UYc = UYcArray[length/2];
		Arrays.sort(VYcArray);
		VYc = VYcArray[length/2];
		Arrays.sort(VYesArray);
		VYes = VYesArray[length/2];
		Arrays.sort(UYesArray);
		UYes = UYesArray[length/2];
		Arrays.sort(YlowArray);
		Ylow = YlowArray[length*1/4] - 10;
		Arrays.sort(YhighArray);
		Yhigh = YhighArray[length*3/4] + 10;
	}
	LinearStatics(short[] Ydatas,short[] Vdatas,short[] Udatas){
		float[] Ydata = toFloat(Ydatas);
		float[] Vdata = toFloat(Vdatas);
		float[] Udata = toFloat(Udatas);
		
		int length = Ydata.length;
		float sumY = sum(Ydata);
		float sumV = sum(Vdata);
		float sumU = sum(Udata);
		float sumYY = sumSqard(Ydata);
		
		float sumYs = sumY*sumY;
		float sumUY = sumMalt(Udata, Ydata);
		float sumVY = sumMalt(Vdata, Ydata);

		UYr = (length*sumUY-sumU*sumY)/(length*sumYY - sumYs);
		UYc = (int)((sumU - UYr*sumY)/length);
		
		VYr = (length*sumVY-sumV*sumY)/(length*sumYY - sumYs);
		VYc = (int)((sumV - VYr*sumY)/length);
		
		int Yave = (int)(sumY/length);
		int Ye = (int)error(sumY, sumYY, length);
		
		Ylow = Yave - Ye;
		Yhigh = Yave + Ye;
		
		VYes = (int) errorS(Ydata,Vdata, VYc, VYr);
		UYes = (int) errorS(Ydata,Udata, UYc, UYr);
	}
	static float[] toFloat(short[] a) {
		float[] out = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i];
		}
		return out;
	}
	private static float sum(float[] a)
	{
		float sum = 0;
		for (float f : a) {
			sum += f;
		}
		return sum;
	}
	private static float sumSqard(float[] a)
	{
		float sum = 0;
		for (float f : a) {
			sum += f*f;
		}
		return sum;
	}
	private static float sumMalt(float[] a,float[] b)
	{
		float sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i]*b[i];
		}
		return sum;
	}
	private static float error(float Sa,float Saa, int n)
	{
		return (float)Math.sqrt(Saa/n - Sa/n*Sa/n);
	}
	
	/*
	 * u|v = r*y +c
	 */
	private static float errorS(float y,float w, float c, float r)
	{
		float err = w - c + r*y;
		return err*err;
	}
	static float errorS2(int y,int w, int c, float r)
	{
		int err = w - c + (int)(r*y);
		return err*err;
	}
	private static float errorS(float[] y,float[] w, float c, float r)
	{
		float sum = 0;
		for (int i = 0; i < w.length; i++) {
			sum += errorS(y[i], w[i], c, r);
		}
		return sum/w.length;
	}
}
