package com.xiegeo.cssr.analyze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import android.graphics.Color;
import android.util.Log;

public class Analyze {
	public static int turtleJump = 1;
	public static boolean showCSS = false; //set by settings
	public static final int csize = 400; //calculation pref
	static final int minCsize = 200;
	static final int maxSigma = 800;
	public static final int CALCULATION_VERSION = 12;
	
	static final String TAG = Analyze.class.getName();
	public static int runs =0;
	public static Analyze last;
	
	static int colorCode = Color.RED;
	
	StringBuilder msg = new StringBuilder(100);
	final byte[] NV21;
	public final int width;
	public final int height;
	public static int[] colorMap;
	static int colorMapWidth;
	
	final AbstractAreaSelector myColor;
	
	public Css css = null;
	IntArray mRegionEdge;
	public Analyze(byte[] NV21data, int imgWidth, int imgHight)
	{
		final int csize = Analyze.csize;
		msg.append(++runs);
		NV21 = NV21data;
		width = imgWidth;
		height = imgHight;
		
		
		myColor = new ColorGroupedAreaSelector(NV21, width, height);
		
		if (colorMap == null || colorMap.length!=width * height) {
			colorMap = new int[width * height];//ScanLine.graph;
			colorMapWidth = width;
		}else {
			Arrays.fill(colorMap, 0);
		}
		
		mRegionEdge = getRegionEdge();
		//Log.v(TAG, "regionEdge returned: "+regionEdge.size());
		
		//mapdots(regionEdge, Color.rgb(255, 0, 0));
		
		
        if (mRegionEdge.size()<10) {
            ;//to small, does nothing
        }else
        {
        	//sample csize # of points
            float[] x = new float[csize];
            float[] y = new float[csize];
            final int have = mRegionEdge.size();
            int index = 0;
            int next = mRegionEdge.get(index);
            for (int i=0; i<csize;i++) {
                while(index*csize < i*have && index<have-1){
                    index++;
                    next = mRegionEdge.get(index);
                }
                x[i] = toX(next);
                y[i] = toY(next);
            }
            
            css = toCss(x,y,showCSS);
            
            Log.i("css", css.toString());
            if(css.peaks.size() == 0)
            {
            	css = null;
            	//last = null;
            }else
            	last = this;
        }
        
	}
	private static float[] resample(float[] a, int newLength) {
		if (a.length == newLength) {
			return a;
		}
		float[] r = new float[newLength];
		final int have = a.length;
        int index = 0;
        float next = a[index];
        for (int i=0; i<newLength;i++) {
            while(index*csize < i*have && index<have-1){
                index++;
                next = a[index];
            }
            r[i] = next;
        }
		return r;
	} 
	public static Css toCss(short[] x, short[] y) {
		return toCss(resample(toFloats(x), csize),resample(toFloats(y), csize),false);
		
	}
	private static float[] toFloats(short[] a) {
		float[] out = new float[a.length];
		for (int i = a.length-1; i >= 0; i--) {
			out[i] = a[i];
		}
		return out;
	}
	public static Css toCss(float[] x, float[] y, final boolean draw) {
		float scale = 1;
        ArrayList<ArrayList<Integer>> inflectionMap = new ArrayList<ArrayList<Integer>>();
        boolean done = false;
        int sigma = 0;
        
        for (; sigma < 3; sigma++) {
        	scale = smoothBoundarysInPlaceWithScale(x,y,scale);
		}
        
        ArrayList<Integer> inflectionPoints = inflectionPoints(x, y);
        inflectionMap.add(inflectionPoints);
        
        for (; !done&&x.length>2&&sigma<maxSigma; sigma++) {
        	
        	if (x.length >= minCsize*2 && sigma % 6 == 4) {
				shorten(inflectionMap);
				x = shorten(x);
				y = shorten(y);
			}else {
				scale = smoothBoundarysInPlaceWithScale(x,y,scale);
			}
            inflectionPoints = inflectionPoints(x, y,inflectionPoints);
            if (!inflectionPoints.isEmpty()) {
                inflectionMap.add(inflectionPoints);
            }else{
                done = true;
            }
            
            if (draw) {
                if (sigma % 3 == 0) {
                	mapdots(scale,x,y, Color.argb(255, 0, (255-sigma%255)/3,(sigma%255)/3));
				}
                mapdots(scale,x,y, inflectionPoints, Color.argb(255, 255, (sigma*2)%256,255));
			}

        }
        return new Css(inflectionMap, x,y);
	}
	
	@SuppressWarnings("unused")
	private static void mapdots(LinkedList<Integer> points, int color) {
		for (int p : points) {
			colorMap[toY(p)*colorMapWidth+toX(p)] = color;
		}
	}
	//@SuppressWarnings("unused")
	private static void mapdots(float scale, float[] x, float[] y, int color) {
		for (int i = 0; i < y.length; i++) {
			int mapIndex = (int)(x[i]/scale)+(int)(y[i]/scale)*colorMapWidth;
			if (colorMap[mapIndex] == 0) {
				colorMap[mapIndex] = color;
			}
		}
	}
	//@SuppressWarnings("unused")
	private static void mapdots(float scale, float[] x, float[] y, Collection<Integer> noted, int notedColour) {
        for (Iterator<Integer> it = noted.iterator(); it.hasNext();) {
            int n = it.next();
            colorMap[(int)(x[n]/scale)+(int)(y[n]/scale)*colorMapWidth] = notedColour;
        }
	}
	private static int sizeGuess = 2000; 
	private IntArray getRegionEdge() {
		final int stepSize = Analyze.turtleJump;
		final int start = toPoint(width/2, height/2);
		//LinkedList<Integer> links = new LinkedList<Integer>();
        //LinkedList<Integer> sparce = new LinkedList<Integer>();
		IntArray regionEdgeArray = new IntArray(sizeGuess*2);
		int last = -1; //do not record the same point again
        int first = -1;
        int on = start;
        int next = start + stepSize;
        int d = 0;
        /* 1
          2 0
           3 */
        int rotation = 0;//make sure it went around starting point, d without mod

        int count = 0;//in case of bug
        
        final int stride = 0xFFFF;
        final int dx = stepSize;
        final int xMin = dx;
        final int xMax = width-xMin;
        final int dy = stepSize*stride;
        final int yMin = dy;
        final int yMax = height*stride-yMin;
        
        while((rotation < 4 || first!=next) && count<500000)
        {
        	
        	count++;
        	int x = toX(next);
        	int y = toY(next);
            if(myColor.isOut(x,y) ||
            		next<yMin || next>=yMax|| x < xMin || x >= xMax)
            {
                if (first == -1) {
                    first = next;
                }
                
                if (next != last) {
                	regionEdgeArray.add(next);
                	last = next;
				}
                
                d = (d+1)&3;
                rotation ++;
                
                //colorMap[toY(next)*width+x] = colorCode;
            }
            else
            {
                if (first!=-1){
                    if (next > first && next < start + width/2) {
                        d = 0;
                        rotation =0;
                        first = -1;
                        regionEdgeArray.clear();
                    }else{
                        d = (d+3)&3;
                        rotation --;
                    }
                }
                on = next;

            }

            switch(d)
            {
                case 0: next = on + dx; break;
                case 1: next = on - dy;break;
                case 2: next = on - dx; break;
                case 3: next = on + dy; break;
            }
            

        }
        if (count > 5000) {
			Log.w(TAG, "count:"+count);
		}
        for (int i = regionEdgeArray.size()-1; i >= 0; i-- ) {
			int e = regionEdgeArray.get(i);
			colorMap[toY(e)*width+toX(e)] = Color.RED;
		}
        sizeGuess = Math.max((int)(sizeGuess * 0.8),regionEdgeArray.size());
        return regionEdgeArray;
	}
	
	/*private static double[] arrayRam = new double[0];
    private static double[] smoothBoundary(final double[] b)
    {
        assert b.length>2;
        
        double[]out;
        if (arrayRam.length == b.length) {
			out = arrayRam;
		}else {
			out = new double[b.length];
		}
        arrayRam = b;
        
        out[0] = ((b[b.length-1] + b[0] + b[1])/3);
        for (int i = 1; i < b.length-1; i++) {
            out[i]=((b[i-1] + b[i]*1 + b[i+1])/3);
        }
        out[b.length-1] = ((b[b.length-2] + b[b.length-1] + b[0])/3);
        return out;
    }*/
    private static void smoothBoundaryInPlaceScaleByThree(final float[] b)
    { 
    	float b1 = b[b.length-1];
    	float b2 = b[0];
    	float bf = b2;
        for (int i = 0; i < b.length-1; i++) {
        	float b3 = b[i+1];
            b[i]=(b1+b2+b3);//3; Scale by 3
            b1=b2;b2=b3;
        }
        b[b.length-1]=(b1+b2+bf);//3;
    }
    private static void scaleInPlace(final float[] b, float scale)
    { 
        for (int i = 0; i < b.length; i++) {
            b[i]=b[i]/scale;
        }
    }
    private static float smoothBoundarysInPlaceWithScale(final float[] a,final float[] b, float scale)
    { 
    	smoothBoundaryInPlaceScaleByThree(a);
    	smoothBoundaryInPlaceScaleByThree(b);
    	scale *= 3;
    	if (scale > Math.pow(2, 32) || scale < Math.pow(2, -32)) {
    		scaleInPlace(a,scale);
    		scaleInPlace(b,scale);
    		scale = 1;
		}
    	return scale;
    }
    
    private static void shorten(ArrayList<ArrayList<Integer>> inflectionMap)
    {
    	for (ArrayList<Integer> inflections : inflectionMap) {
			for (int i = 0; i < inflections.size(); i++) {
				inflections.set(i, inflections.get(i)/2);
			}
		}
    }
    private static float[] shorten(float[] a)
    {
    	float[] out = new float[a.length/2];
    	for (int i = 0; i < out.length; i++) {
			out[i] = (a[i*2] + a[i*2+1]) /2;
		}
    	return out;
    }
    
    /*private static double[] curvature(double[] x, double[] y)
    {
        int last = x.length-1;
        double[] out = new double[x.length];
        double x1 = x[last];
        double y1 = y[last];
        int next = 0;
        double x2 = x[next];
        double y2 = y[next++];
        double x3, y3;
        while(next <= last)
        {
            x3 = x[next];
            y3 = y[next];
            out[next-1] = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
            x1=x2;x2=x3;
            y1=y2;y2=y3;
            next++;
        }
        x3 = x[0];
        y3 = y[0];
        out[last] = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
        return out;
    }*/
    private static ArrayList<Integer> inflectionPoints(float[] x, float[] y)
    {
        ArrayList<Integer> out = new ArrayList<Integer> ();
        
        int last = x.length-1;
        float x1 = x[last];
        float y1 = y[last];
        int next = 0;
        float x2 = x[next];
        float y2 = y[next++];
        float x3 = x[next];
        float y3 = y[next++];
        float k1 = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
        x1=x2;x2=x3;
        y1=y2;y2=y3;
        float k2;
        while(next <= last)
        {
            x3 = x[next];
            y3 = y[next];
            k2 = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
            if ((k1 > 0 && k2 <= 0) ||(k1 < 0 && k2 >= 0)) {
                out.add(next-1);
            }
            x1=x2;x2=x3;
            y1=y2;y2=y3;
            k1=k2;
            next++;
        }
        x3 = x[0];
        y3 = y[0];
        k2 = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
        if ((k1 > 0 && k2 <= 0) ||(k1 < 0 && k2 >= 0)) {
            out.add(next-1);
        }
        /*double[] k = curvature(x, y);
        double k1 = k[k.length-1];
        double k2;
        for (int i = 0; i < k.length; i++) {
            k2 = k[i];
            if ((k1 > 0 && k2 <= 0) ||((k1 < 0 && k2 >= 0))) {
                out.add(i);
            }
            k1=k2;
        }*/
        return out;
    }
    /*
     * generate the inflection points of the new level, remove none tips from the old
     */
    private static ArrayList<Integer> inflectionPoints(final float[] x, final float[] y, final ArrayList<Integer> lowerLevel)
    {
    	final int length = x.length;
    	ArrayList<Integer> higherLevel = new ArrayList<Integer> (length);
    	ArrayList<Integer> tips = new ArrayList<Integer> ();
    	
    	int lastadded = -1;
    	for (final int n : lowerLevel) {
			int next = (n - 3 + length) % length;
			final int end = (n + 3) % length;
			float x1 = x[next];
	        float y1 = y[next++];
	        if (next == length) next = 0;
	        float x2 = x[next];
	        float y2 = y[next++];
	        if (next == length) next = 0;
	        float x3 = x[next];
	        float y3 = y[next++];
	        if (next == length) next = 0;
	        float k1 = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
	        x1=x2;x2=x3;
	        y1=y2;y2=y3;
	        float k2;
	        boolean haveInflection = false;
	        while (!haveInflection && next != end ) {
	            x3 = x[next];
	            y3 = y[next];
	            k2 = x1*(y2-y3)+x2*(y3-y1)+x3*(y1-y2);
	            if ((k1 > 0 && k2 <= 0) ||(k1 < 0 && k2 >= 0)) {
	            	haveInflection = true;
	            	final int found = next>0 ? next-1 : next + length-1;
	                if (found == lastadded) {
						// same as last one, nothing to add.
					}else{
						 higherLevel.add(found);
						 lastadded = found;
					}
	            }
	            x1=x2;x2=x3;
	            y1=y2;y2=y3;
	            k1=k2;
	            next++;
	            if (next == length) next = 0;
			} 
	        if (!haveInflection) {
	        	if (tips.isEmpty()) {
	        		tips.add(n);
				}else if (Math.abs(tips.get(tips.size()-1) - n) > 3) {
					tips.add(n);
				}
			}
		}
    	//if(higherLevel.isEmpty()) return higherLevel;
    	
    	if ( higherLevel.size() > 1 && lastadded == higherLevel.get(0)) {
			higherLevel.remove(higherLevel.size()-1);
		}
    	
    	lowerLevel.clear();
    	lowerLevel.addAll(tips);
    	
    	return higherLevel;
    }
    
    static short toX(int point)
    {
    	return (short)point;
    }
    static short toY(int point)
    {
    	return (short)(point>>16);
    }
    static int toPoint(short x, short y)
    {
    	return ((int)y<<16|x);
    }
    static int toPoint(int x, int y)
    {
    	return (y<<16|x);
    }
}

