package com.xiegeo.cssr.analyze;

import android.graphics.Canvas;

public abstract class AbstractAreaSelector {
	static final String TAG="AreaSelector";
	final byte[] NV21;
	final int w, h;
	final int colorStart;
	public AbstractAreaSelector(byte[] NV21data, int w, int h) {
		this.NV21 = NV21data;
		this.w = w;
		this.h = h;
		colorStart = w *h;
	}
	
	int colorCode;
	public abstract boolean isOut(int xp,int yp);
	

	public void draw(Canvas canvas) {}
	
	public int yIndexToCindex(int p)
	{
		return colorStart + ((p%w)&-2) + (p/w/2*w);
	}
	public int colorIndex(int x, int y)
	{
		return colorStart + (x&-2) + (y/2*w);
	}
	public int yToTurtle(int p)
	{
		return ((p/w)<<16|(p%w));
	}
}
