package com.xiegeo.cssr.analyze;

import java.util.Arrays;


public class ColorGroupedAreaSelector extends AbstractAreaSelector {

	private final ColorGroupedStatics statics; 
	public ColorGroupedAreaSelector(byte[] NV21data, int w, int h) {
		super(NV21data, w, h);
		statics = calculateAndMerge(Sampler.lineScans(this));
	}

	@Override
	public boolean isOut(int xp, int yp) {
		int dis = colorIndex(xp, yp);
		int v = (NV21[dis] & 0xFF)/ColorGroupedStatics.INTERVAL;
		int u = (NV21[dis+1] & 0xFF)/ColorGroupedStatics.INTERVAL;
		/*if (statics.isUnfilled(v, u)) {
			colorCode = Color.GREEN;
			return true;
		}
		colorCode = Color.RED;*/
		
		int y = NV21[yp*w+xp] & 0xFF;
		
		
		return y>statics.high[v][u] || y<statics.low[v][u];
	}
	
	private static ColorGroupedStatics calculateAndMerge(short[][][] samples)
	{
		ColorGroupedStatics[] list = new ColorGroupedStatics[samples.length];
		for (int i = 0; i < list.length; i++) {
			list[i] = new ColorGroupedStatics(samples[i][0], samples[i][1], samples[i][2]);
		}
		return new ColorGroupedStatics(list);
	}
}
class ColorGroupedStatics{
	//GROUPS * INTERVAL = 256
	static final int GROUPS = 32;//32;
	static final int INTERVAL = 256/GROUPS;
	final short[][] high = new short[GROUPS][GROUPS];
	final short[][] low = new short[GROUPS][GROUPS];
	static void fill(short[][] buf, short val) {
		for (short[] fs : buf) {
			Arrays.fill(fs,val);
		}
	}
	ColorGroupedStatics(final ColorGroupedStatics[] list){
		final short[] hs = new short[list.length];
		final short[] ls = new short[list.length];
		int kickOut = list.length/4;
		for (int i = 0; i < GROUPS; i++) {
			for (int j = 0; j < GROUPS; j++) {
				int size = 0;
				for (int k = 0; k < list.length; k++) {
					if (!list[k].isUnfilled(i, j)) {
						hs[size] = list[k].high[i][j];
						ls[size] = list[k].low[i][j];
						size++;
					}
				}
				Arrays.sort(hs, 0, size);
				Arrays.sort(ls, 0, size);
				
				final short h,l;
				if (size < kickOut) {
					h = 0;
					l = 255;
				}else {
					h = hs[size-kickOut];
					l = ls[kickOut];
				}
				high[i][j] = h;
				low[i][j] = l;
			}
		}
		expand();
	}
	
	ColorGroupedStatics(short[] Ydata,short[] Vdata,short[] Udata){
		fill(low,(short)255);
		int length = Udata.length;
		for (int i = 0; i < length; i++) {
			add(Vdata[i]/INTERVAL, Udata[i]/INTERVAL, Ydata[i]);
		}
	}
	boolean isUnfilled(int vi, int ui){
		return high[vi][ui] == 0;
	}
	void add(int vi, int ui, short Y){
		short Yh = (short) (Y + 15);
		short Yl = (short) (Y - 15);
		if (isUnfilled(vi, ui)) {
			high[vi][ui] = Yh;
			low[vi][ui] = Yl;
		}else {
			high[vi][ui] = Sampler.max(high[vi][ui], Yh);
			low[vi][ui] = Sampler.min(low[vi][ui], Yl);
		}
	}
	void expand() {
		for (int i = 0; i < GROUPS; i++) {
			short h1 = high[i][0];
			short l1 = low[i][0];
			short h2 = high[i][1];
			short l2 = low[i][1];
			for (int j = 2; j < GROUPS; j++) {
				short h3=high[i][j];
				short l3=low[i][j];
				high[i][j-1] = Sampler.max(h1,h2,h3);
				low[i][j-1] = Sampler.min(l1,l2,l3);
				h1=h2;h2=h3;l1=l2;l2=l3;
			}
		}
		for (int i = 0; i < GROUPS; i++) {
			short h1 = high[0][i];
			short l1 = low[0][i];
			short h2 = high[1][i];
			short l2 = low[1][i];
			for (int j = 2; j < GROUPS; j++) {
				short h3=high[j][i];
				short l3=low[j][i];
				high[j-1][i] = Sampler.max(h1,h2,h3);
				low[j-1][i] = Sampler.min(l1,l2,l3);
				h1=h2;h2=h3;l1=l2;l2=l3;
			}
		}
	}
}
