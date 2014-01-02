package com.xiegeo.cssr.analyze;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author george
 */
public class Css implements Comparable<Css>, Serializable{

	private static final long serialVersionUID = 2;
	
	public ArrayList<Peak> peaks = new ArrayList<Peak>();
    double sigmaSqrtTotal = 0;

    
    public Css(ArrayList<ArrayList<Integer>> inflectionMap, float[] x, float[] y) {
    	final int length = x.length;
    	double acL = 0; // the accumulative length
    	final double[] acLs = new double[length];
    	
    	for (int i = 1; i < length; i++) {
			acL += Math.hypot(x[i-1]-x[i], y[i-1]-y[i]);
			acLs[i] = acL;
		}
    	acL += Math.hypot(x[length-1]-x[0], y[length-1]-y[0]);
    	final int[] indexs = new int[length];
    	
    	acL = Peak.loopLength/acL;
    	for (int i = 1; i < length; i++) {
    		indexs[i] = (int) (acLs[i]*acL);
		}
    	

        final int topSigma = inflectionMap.size()-1;
        final int miniSigma = 1;
        final int maxPicLength = 30;
        
        if(topSigma<0) return;
       
        
        
        for (int i = topSigma; i >= miniSigma && peaks.size()<maxPicLength; i--) {
            for (Iterator<Integer> level = inflectionMap.get(i).iterator(); level.hasNext();) {
                Peak p = new Peak(i-miniSigma, indexs[level.next()]);
                addPeak(p);
            }
        	
        }
        
        
        
/*
 * final int shift = Peak.loopLength/100;
         
        for (Iterator<Integer> top = inflectionMap.get(topSigma).iterator(); top.hasNext();) {
            Peak p = new Peak(topSigma-miniSigma, indexs[top.next()]);
            if (peaks.isEmpty()) {
                addPeak(p);
            }else{
                Peak last = peaks.get(peaks.size()-1);
                if( p.dis - last.dis > shift*3 ){
                    addPeak(p);
                }
            }
        }
        

        for (int i = topSigma-1; i >= miniSigma && peaks.size()<maxPicLength; i--) {
            //System.out.print(i + ", ");
            Iterator<Integer> a = inflectionMap.get(i+1).iterator();
            Iterator<Integer> b = inflectionMap.get(i).iterator();
            int an = a.next();
            int bn = b.next();
            boolean haveMore = true;
            while(haveMore && peaks.size()<maxPicLength ) {
                boolean nextA = false;
                boolean nextB = false;
                if ((bn == length-1 && inflectionMap.get(i+1).get(0)==0)
                    ||(bn >= length-3&& inflectionMap.get(i+1).get(0)+length-bn > 3)){
                    haveMore=false;
                }
                else if(an - bn > shift) {
                    if (peaks.isEmpty()) {
                        addPeak(new Peak(i-miniSigma, indexs[bn]));
                    }else{
                        Peak last = peaks.get(peaks.size()-1);
                        //assert last.sigma!=i||bn>last.dis;
                        if(last.sigma != i || bn - last.dis > shift*3 ){
                            addPeak(new Peak(i-miniSigma, indexs[bn]));
                        }else{
                            //System.out.println("skip:"+new Peak(i, bn));
                        }
                        //System.out.println(bn - last.dis);
                    }
                    nextB = true;
                }else if(an - bn < shift*-1){
                    nextA = true;
                }else{
                    nextA = true;
                    nextB = true;
                    //System.out.println("skip2:"+new Peak(i, bn));
                }

                if (nextA&&a.hasNext()) {
                    an = a.next();
                }else{
                    if (!a.hasNext()) {
                        an = Integer.MAX_VALUE;
                    }
                }
                if (nextB&&b.hasNext()) {
                    bn = b.next();
                }else{
                    haveMore &= !nextB;
                }
            }
        }*/
        for (Peak peak : peaks) {
            peak.adjustSignificance(sigmaSqrtTotal);
        }
    }


    private void addPeak(Peak p)
    {
    	sigmaSqrtTotal += Math.sqrt((p.sigma +1));
        peaks.add(p);
    }

    public Css[] getMajorRotations() {
        int numberOfshifts = Math.min(2, peaks.size());
        Css[] rotatedCsses = new Css[numberOfshifts];
        for (int i = 0; i < numberOfshifts; i++) {
            Peak s = peaks.get(i);
            int shift = -s.dis;
            rotatedCsses[i] = new Css(this, shift);
        }
        return rotatedCsses;
    }

    private Css(Css from, int shift)
    {
        sigmaSqrtTotal = from.sigmaSqrtTotal;
        peaks = new ArrayList<Peak>(from.peaks.size());
        for (Iterator<Peak> pi = from.peaks.iterator(); pi.hasNext();) {
            peaks.add(new Peak(pi.next(), shift));
        }
    }
    public Css(Css reverse)
    {
        sigmaSqrtTotal = reverse.sigmaSqrtTotal;
        peaks = new ArrayList<Peak>(reverse.peaks.size());
        for (Iterator<Peak> pi = reverse.peaks.iterator(); pi.hasNext();) {
        	Peak p = pi.next();
            peaks.add(new Peak(p.sigma, -p.dis, p.significance));
        }
    }
    
    public double orderMatcher() {
		return peaks.get(0).significance;
	}
    
    
    transient private Css zeroed;
    transient private Css reversed;
    public double match(Css other, double tolerence, final boolean mirror)
    {
        
        
        double ratio = sigmaSqrtTotal/(double)other.sigmaSqrtTotal;
        if (ratio > 1.5 || ratio < 0.66) return 0;
        
        double sr = peaks.get(0).significance/other.peaks.get(0).significance;
        if (sr > 1.5 || sr < 0.66) return 0;
	    
        
        int numberOfshifts = Math.min(2, peaks.size());
        double lowerBound = 0;
        
        for (int i = 0; i < numberOfshifts; i++) {
            int shift = -peaks.get(i).dis;
            //System.out.println("shift:"+shift);
            zeroed = zeroed==null?new Css(this, shift):zeroed;
            ArrayList<Peak> tryPeaks = zeroed.peaks;
            //System.out.println("matching:"+shifted +"\nwith"+other);
            double tryMatch = match(tryPeaks, other.peaks, tolerence-lowerBound);
            if (tryMatch > 0) {
                lowerBound += tryMatch;
            }
            
            if (mirror) {
                tryPeaks = reversed==null?new Css(zeroed).peaks:reversed.peaks;
                tryMatch = match(tryPeaks, other.peaks, tolerence-lowerBound);
                if (tryMatch > 0) {
                    lowerBound += tryMatch;
                }
			}
        }

        return lowerBound;
    }
    private static double match(ArrayList<Peak> a, ArrayList<Peak> b, double tol)
    {
        Iterator<Peak> ai = a.iterator();
        Iterator<Peak> bi = a.iterator();
        while(tol>0&&(ai.hasNext()||bi.hasNext()))
        {
            if (ai.hasNext()) {
                Peak an = ai.next();
                tol -= smallestError(an, b)*an.significance;
            }
            if (bi.hasNext()) {
                Peak bn = bi.next();
                tol -= smallestError(bn, a)*bn.significance;
            }
        }
        //System.out.println("match peaks:"+tol);
        return tol;
    }
    private static double smallestError(Peak p, List<Peak> l)
    {
        double smallestError = 1;
        int size = l.size();
        int index = Collections.binarySearch(l, p);
        int indexDec = index<0?-index-2:index-1;
        int indexInc = indexDec+1;
        boolean up=0<=indexDec, down=indexInc<size;
        Peak current;
        while (up||down) {
			
			if (up) {
				current = l.get(indexDec);
				
				double se= p.sigmaError(current);
				if (se >= smallestError) {
					up = false;
				}else {
					double error = se + p.disError(current);
					smallestError = Math.min(smallestError, error);
					
					indexDec--;
					up &= 0<=indexDec;
				}
			}
			if(down) {
				//Log.d("Css","indexDown:"+indexInc +"<"+size);
				current = l.get(indexInc);
				
				double se= p.sigmaError(current);
				if (se >= smallestError) {
					down = false;
				}else {
					double error = se + p.disError(current);
					smallestError = Math.min(smallestError, error);
					
					indexInc++;
					down &= indexInc<size;
				}
			}
		}
        //System.out.println("smallestError:"+index+p+smallestError+l);
        return smallestError;
    }
    @Override
    public String toString() {
    	StringBuffer buffer = new StringBuffer(150);
    	buffer.append("sum:").append(sigmaSqrtTotal).append(" size:").append(peaks.size());
    	int end = Math.min(5, peaks.size());
    	for (int i = 0; i < end; i++) {
    		buffer.append(peaks.get(i));
		}
        return buffer.toString();
    }

    public int compareTo(Css o)
    {
        return (int)Math.signum(orderMatcher() - o.orderMatcher());
    }

}