package com.xiegeo.cssr.analyze;

import java.io.Serializable;
import java.util.ArrayList;

import android.graphics.Canvas;

public class Peak implements Comparable<Peak>, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static final int disImp = 1;
    static final int loopLength = 1024;

    final int sigma;
    final int dis;
    double significance;

    public Peak(int sigma, int dis) {
        this.sigma = sigma;
        this.dis = dis;
    }
    public Peak(int sigma, int dis, double sig) {
        this.sigma = sigma;
        this.dis = dis;
        significance = sig;
    }
    public Peak(Peak p, int shift){
        sigma = p.sigma;
        dis = p.dis+shift;
        significance = p.significance;
    }
    public void adjustSignificance(double sigmaSqrtTotal){
        significance = Math.sqrt(sigma+1)/sigmaSqrtTotal;
    }

    public double disError(Peak p)
    {
        int disp = Math.abs(p.dis-dis)%loopLength;
        if (disp > loopLength/2) {
            disp = loopLength - disp;
        }
        double dd = (double)disp/(loopLength/4.0);
        return dd*dd;
    }
    public double sigmaError(Peak p)
    {
        double sd = (sigma-p.sigma)/(sigma+p.sigma+1.0)*8.0;
        return sd*sd;
    }
    @Override
    public String toString() {
        return "["+sigma+", "+dis+", "+(float)significance+"]";
    }

    /*
     * Peaks are ordered for large to small.
     */
	public int compareTo(Peak another) {
		return  another.sigma - sigma;
	}
	
	public static void drawPicks(Canvas canvas, ArrayList<Peak> peaks) {
		final int cw = canvas.getWidth();
		final int ch = canvas.getHeight();
		double rMax = Math.min(cw, ch)/2 -3;
		double  s = rMax / Math.max(200, Analyze.last.css.peaks.get(0).sigma);
		for (Peak p : Analyze.last.css.peaks) {
			double r = s * p.sigma;
			double d = p.dis*(Math.PI*2/Peak.loopLength);
			int x = (int)(r * Math.cos(d));
			int y = (int)(r * Math.sin(d));
			canvas.drawLine(cw/2, ch/2, cw/2+x, ch/2-y, Sampler.myPaint);
		}
	}

}