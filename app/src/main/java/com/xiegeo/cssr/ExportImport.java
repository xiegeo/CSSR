package com.xiegeo.cssr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.os.Environment;
import android.util.Log;

import com.xiegeo.cssr.analyze.Analyze;
import com.xiegeo.cssr.analyze.Css;
import com.xiegeo.cssr.analyze.CssImage;

public class ExportImport {
	static final String TAG = "ExportImport";
	static boolean recalcuated = false;
	
	
	
	public static int getAnalyzeVersion(){
		return Analyze.CALCULATION_VERSION * 10000 + Analyze.csize;
	}

	public static boolean isExternalWriteable() {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED);
	}
	public static boolean isExternalReadable() {
		String state = Environment.getExternalStorageState();
		return state.equals(Environment.MEDIA_MOUNTED)||state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
	}
	
	public static boolean exportFile(TreeMap<String,ArrayList<CssImage>> saveMap, File f) {
		boolean result = false;
		FileOutputStream fout = null;
		Log.i("LibrarySave", "saving as:"+f);
		try {
			f.delete();
			f.createNewFile();
			fout = new FileOutputStream(f);
			result = exportNSSC(saveMap, fout);
			Log.i("LibrarySave", "saved:"+result+"|"+f.exists()+":"+f.length()/1024.f+"Kbytes");
		} catch (FileNotFoundException e) {
			Log.e("LibrarySave", "FileNotFoundException", e);
		} catch (IOException e) {
			Log.e("LibrarySave", "IOException", e);
		} finally {
			try {
				if (fout != null ) {
					fout.close();
				}
			} catch (IOException e) {
			}
		}
		return result;
	}
	
	/*
	 * type version
	 * length
	 * name short[] short[] 
	 * ...
	 * Css[]
	 * 
	 */
	public static boolean exportNSSC(TreeMap<String,ArrayList<CssImage>> map, FileOutputStream fout) {
		final String typeString = "NSSC";
		boolean result = false;
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(fout,1048576));
			out.writeUTF(typeString);
			out.writeInt(getAnalyzeVersion());
			
	    	int count = 0;
	    	for (ArrayList<CssImage> list : map.values()) {
				count += list.size();
			}
			out.writeInt(count);
			Css[] csses = new Css[count];
			int i = 0;
			for ( Entry<String, ArrayList<CssImage>> e  : map.entrySet()) {
				String name = e.getKey();
				for (CssImage image : e.getValue()) {
					out.writeObject(name);
					out.writeObject(image.getX());
					out.writeObject(image.getY());
					csses[i] = image.getCss();
					i++;
				}
			}
			out.writeObject(csses);
			out.close();
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null ) {
					out.close();
				}else {
					fout.close();
				}
			} catch (IOException e) {
			}
		}
		return result;
	}
	
	
	public static File[] listFiles(File dir, final String suffix){
		return dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith(suffix)) {
					return true;
				}
				return false;
			}
		});
	}
	static int expectedLength = 0;
	static int processing = -1;
	static int looper = 0;
	static String loopIcons ="▉▊▋▌▍▎▏▎▍▌▋▊"; //"|/-\\";
	static String title = null;
	static String state = "load";
	public static String getState() {
		if (processing == -1) {
			if (looper >= loopIcons.length()) {
				looper = 0;
			}
			return state+loopIcons.charAt(looper++);
		}
		String line = state +":"+processing+"/"+expectedLength;
		if (title!=null) {
			return title +"\n"+ line;
		}
		return line;
	}
	public static TreeMap<String,ArrayList<CssImage>> importLibrary(FileInputStream fin) {
		state = "Loading...";
		ObjectInputStream in = null;
		TreeMap<String,ArrayList<CssImage>> store = new TreeMap<String,ArrayList<CssImage>>();
		try {
			in = new ObjectInputStream(new BufferedInputStream(fin, 1048576));
			String typeString = in.readUTF();
			if (typeString.equals("v1.0")) {
				@SuppressWarnings("unchecked")
				TreeMap<String, ArrayList<CssImage>> readObject = (TreeMap<String, ArrayList<CssImage>>) in.readObject();
				store = readObject;
			}
			else {
				int version = in.readInt();
				int length = in.readInt();
				expectedLength = length;
				if (typeString.equals("NSSC")) {
					state = "Loading Shapes";
					boolean recalculate = version != getAnalyzeVersion();
					String[] names = new String[length];
					short[][] xs = new short[length][];
					short[][] ys = new short[length][];
					for (int i = 0; i < length; i++) {
						processing = i+1;
						expectedLength = length;
						names[i] = (String) in.readObject();
						state = "Loading Shapes ("+names[i]+")";
						xs[i] = (short[]) in.readObject();
						ys[i] = (short[]) in.readObject();
					}
					processing = -1;
					
					
					recalcuated = false;
					Css[] csses = null;
					if (recalculate) {
						csses = recalculateCsses(xs, ys);
					}else {
						try{
							state = "Loading Index";
							csses = (Css[]) in.readObject();
						}catch (Exception e) {
							Log.e(TAG, "File corrupted, some recalculation is required.",e);
							csses = recalculateCsses(xs, ys);
						}
					}
					
					for (int i = 0; i < length; i++) {
						CssImage image = new CssImage(xs[i],ys[i],csses[i]);
						Library.addImage(store, image, names[i]);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				if (in != null) {
					in.close();
				}else if (fin != null) {
					fin.close();
				}
			} catch (IOException e) {
			}
		}
		return store;
		
	}
	private static Css[] recalculateCsses(short[][] x, short[][] y) {
		state = "Calculating Index";
		int length = y.length;
		Css[] out = new Css[length];
		for (int i = 0; i < length; i++) {
			processing = i+1;
			expectedLength = length;
			out[i] = Analyze.toCss(x[i], y[i]);
		}
		processing = -1;
		recalcuated = true;
		return out;
	}
}
