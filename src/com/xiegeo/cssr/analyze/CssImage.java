package com.xiegeo.cssr.analyze;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;

import android.graphics.Color;



public class CssImage implements Serializable, Comparable<CssImage>{

	private static final long serialVersionUID = 1L;
	private static final boolean CACHES = false;
	
	static final int MAX_EDGE_LENGTH = 400;
	private Css css;
	
	final Date creationTime = new Date();
	
	//only used on first learn
	transient private IntArray mRegionEdge;
	//then saved as following
	private short[] x;
	private short[] y;
	
	transient private WeakReference<int[]> weakIcon = null;
	public transient int iconSide=0;
	
	public static CssImage getLast(){
		Analyze last = Analyze.last;
		if (last == null) {
			return null;
		}
		return new CssImage(last.mRegionEdge, last.css);
	}
	
	private CssImage(IntArray regionEdge, Css css) {
		mRegionEdge = regionEdge;
		this.css = css;
	}
	
	public CssImage(short[] xs, short[] ys, Css css) {
		x = xs;
		y = ys;
		this.css = css;
	}
	
	
	public synchronized int[] getImage(int side)
	{
		side = Math.max(36, side);
		int[] icon = weakIcon==null?null:weakIcon.get();
		if (icon == null || side != iconSide) {
			iconSide = side;
			icon = fillIcon(iconSide, Color.rgb(0, 255, 0));
			if (CACHES) {
				addIconCache(icon);
			}
		}
		weakIcon = new WeakReference<int[]>(icon);
		return icon;
	}
	

	public Css getCss() {
		return css;
	}

	private void fillXY()
	{
		if (x != null) {
			return;
		}
		int edgeLenth = Math.min(mRegionEdge.size(), MAX_EDGE_LENGTH);
		x = new short[edgeLenth];
		y = new short[edgeLenth];
        int have = mRegionEdge.size();
        int index = 0;
        int next = mRegionEdge.get(index);
        for (int i=0; i<edgeLenth;i++) {
            while(index*edgeLenth < i*have && index<have){
                index++;
                next = mRegionEdge.get(index);
            }
			x[i] = Analyze.toX(next);
			y[i] = Analyze.toY(next);
        }
	}
	
	public short[] getX() {
		fillXY();
		return x;
	}
	public short[] getY() {
		fillXY();
		return y;
	}

	private int[] fillIcon(int side, int color)
	{
		final int[] colorMap = new int[side*side];
		
		short left = Short.MAX_VALUE;
		short right = Short.MIN_VALUE;
		short top = Short.MAX_VALUE;
		short bottem = Short.MIN_VALUE;
		fillXY();
		
		for (short d : getX()) {
			left = (left < d) ? left : d;
			right = (right > d) ? right : d;
		}
		
		for (short d : getY()) {
			top = (top < d) ? top : d;
			bottem = (bottem > d) ? bottem : d;
		}
		
		right += side/2;
		left -= side/2;
		bottem += side/2;
		top -= side/2;
		double w = right - left +1;
		double h = bottem - top +1;
		double r ;
		int dis ;
		if ( w > h) {
			r = side/(double)Math.max(1, w);
			dis = (int)((w - h)/2*r) * side;
		}else {
			r = side/(double)Math.max(1, h);
			dis = (int)((h - w)/2*r);
		}
		
		for (int j = 0; j < getX().length; j++) {
			colorMap[dis + (int)((getX()[j]-left)*r) + (int)((getY()[j]-top)*r)*side] = color;
		}
		return colorMap;
	}
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		fillXY();
		out.defaultWriteObject();
	}
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
	}

	public int compareTo(CssImage another) {
		return -creationTime.compareTo(another.creationTime);
	}
	
	private static int[][] iconCache = new int[50][];
	private static int cachePosition = 0;
	private static void addIconCache(int[] icon) {
		iconCache[cachePosition]=icon;
		cachePosition++;
		if (cachePosition>=iconCache.length) {
			cachePosition=0;
		}
	}




}
