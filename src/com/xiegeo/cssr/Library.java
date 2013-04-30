package com.xiegeo.cssr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.xiegeo.cssr.analyze.Css;
import com.xiegeo.cssr.analyze.CssImage;
import com.xiegeo.cssr.answer.SearchAnswer;

public class Library {
	static final String TAG = "Library";
	static final String backupSuffix = "_Back";
	static final String fileSuffix = "_Library";
	static final String newfileSuffix = "_New";

	static boolean mirror = true;

	static String getCurrentName(Context context) {
		 final SharedPreferences llp = context.getSharedPreferences(ListOfLibrarysView.TAG, Context.MODE_PRIVATE);
	     return llp.getString(ListOfLibrarysView.TAG, "First Library");
	}
	
	static synchronized void mergeLibrarys(Library sender, Library receiver) {
		if(sender == receiver) return;
		sender.readLock.lock();
		receiver.writeLock.lock();
		
		TreeMap<String,ArrayList<CssImage>> sendMap = sender.storeMap;
		TreeMap<String,ArrayList<CssImage>> receMap = receiver.storeMap;
		
		for (String name : sendMap.keySet()) {
			ArrayList<CssImage> s = sendMap.get(name);
			ArrayList<CssImage> r = receMap.get(name);
			if (r==null) {
				receMap.put(name, new ArrayList<CssImage>(s));
			}else {
				r.addAll(s);
			}
		}
		receiver.dirty=true;
		sender.readLock.unlock();
		receiver.writeLock.unlock();
		receiver.asyncSave();
	}
	static Library copyLibrary(Library oldLibrary, String newName, Context context) {
		if (oldLibrary.mName.equals(newName)) {
			return oldLibrary;
		}
		Library newLibrary = new Library(newName, context);
		if (!newLibrary.isEmpty()) {
			return oldLibrary;
		}
		oldLibrary.readLock.lock();
		newLibrary.setImageMap(oldLibrary.storeMap);
		oldLibrary.readLock.unlock();
		if (!newLibrary.save(true)) {
			return oldLibrary;
		}
		return newLibrary;
	}
	public static TreeSet<String> getLibNames(final Context context)
	{
		//final Context context = Main.mActivity;
		String[] filelist = context.fileList();
		TreeSet<String> out = new TreeSet<String>();
		for (String fileName : filelist) {
			if (fileName.endsWith(Library.fileSuffix))
			{
				String name = fileName.substring(0,fileName.length()-Library.fileSuffix.length());
				out.add(name);
			}else if (fileName.endsWith(Library.fileSuffix+Library.backupSuffix)) {
				String name = fileName.substring(0,fileName.length()-Library.fileSuffix.length()-Library.backupSuffix.length());
				out.add(name);
			}
		}
		Log.v(TAG,out.toString());
		return out;
	}
	
	private static TreeMap<String, WeakReference<Library>> libMap = new TreeMap<String, WeakReference<Library>>();
	public synchronized static Library getLibrary(String libName, Context context){;
		WeakReference<Library> libRef = libMap.get(libName);
		Library lib;
		if(libRef==null || (lib = libRef.get())==null){
			lib = new Library(libName, context);
			libMap.put(libName, new WeakReference<Library>(lib));
		}else{
			Log.i(TAG, "Reused: "+lib);
		}
		return lib;
	}
	final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);;
	final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
	final ReentrantReadWriteLock.WriteLock writeLock = lock .writeLock();
	
	private final String mName;
	String stat;
	private TreeMap<Css,String> searchMap;
	private TreeMap<String,ArrayList<CssImage>> storeMap;
    
    private boolean dirty = false;
    private Context context;
	private Library(String name, Context context) {
		this.context = context;
		mName = name;
		Log.v("Library", name);
		FileInputStream fin = null;
		//ZipInputStream zin = null;
		//ObjectInputStream in = null;
		try {
			Log.d(TAG, "dir:"+new File(getFilePath()).getAbsolutePath());
			File open;
			try {
				fin = context.openFileInput(getFilePath()+backupSuffix);
				open = context.getFileStreamPath(getFilePath()+backupSuffix);
				Log.i("Library", "open backup:"+open.exists()+":"+ open.length()+"bytes");
			}catch (FileNotFoundException e) {
				fin = context.openFileInput(getFilePath());
				open = context.getFileStreamPath(getFilePath());
				Log.i("Library", "open :"+open.exists()+":"+ open.length()+"bytes");
			}
			
			/*Log.i("Library", "1");
			zin = new ZipInputStream(new BufferedInputStream(fin,8192));
			Log.i("Library", "2");
			ZipEntry ze = zin.getNextEntry();
			Log.i("Library", "3:"+ze.getName());
			in = new ObjectInputStream(new BufferedInputStream(zin, 65536));*/
			/*in = new ObjectInputStream(new BufferedInputStream(fin, 1048576));
			String versionString = in.readUTF();
			Log.i("Library", "versionString:"+versionString);
			if (!versionString.equals("v1.0")) {
				throw new Exception("version is " + versionString);
			}*/
			setImageMap(ExportImport.importLibrary(fin));
			Log.i("Library", "read done");
			stat = "done";
		} catch (Exception e) {
			setImageMap(new TreeMap<String,ArrayList<CssImage>>());
			if (e.getClass() == FileNotFoundException.class) {
				//first run
			}else {
				Log.e("Library", "Library lost!", e);
				//Toast.makeText(Main.mActivity, "Library lost!" + mName, Toast.LENGTH_LONG)
				//.show();
				stat = "Library lost";
			}
		}
		try {
			if (fin != null) {
				fin.close();
			}
		} catch (IOException e) {
			Log.e("Library", "error on close of read", e);
		}
		dirty = ExportImport.recalcuated;
	}

	private ThreadGroup savingThreadGroup = new ThreadGroup("saving");
	private AtomicInteger queueCounter = new AtomicInteger(0);
	
	public void clearData() {
		setImageMap(new TreeMap<String,ArrayList<CssImage>>());
		asyncSave();
	}
	private void deleteSaveFile() {
		File saveFile = context.getFileStreamPath(getFilePath());
		File backupFile = context.getFileStreamPath(getFilePath()+backupSuffix);
		saveFile.delete();
		backupFile.delete();
	}
	private void asyncSave() {
		if(queueCounter.get()<3){
			queueCounter.incrementAndGet();
			Thread saveThread = new Thread(savingThreadGroup,"") {
				public void run() {
					Log.i("LibrarySave", "Queued");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
					save(false);
					queueCounter.decrementAndGet();
				};
			};
			saveThread.start();
		}
		//return saveThread;
	}
    TreeMap<String,ArrayList<CssImage>> getMapForExport(){
    	readLock.lock();
    	TreeMap<String,ArrayList<CssImage>> saveMap = new TreeMap<String,ArrayList<CssImage>>(storeMap);
    	readLock.unlock();
    	return saveMap;
    }
	public synchronized boolean save(boolean alwaysSave) {
		
		readLock.lock();
		boolean runAgain = false;
		if (isEmpty()) {
			if(alwaysSave) {
				runAgain = true;
				asyncSave(); //try with delete possibility later
			}
			else {
				deleteSaveFile();
				readLock.unlock();
				return true;
			}
		}
		if(!dirty && !alwaysSave) {
			Log.i("LibrarySave", "skiped");
			readLock.unlock();
			return true;
		}
		Log.i("LibrarySave", "copying");
		dirty = runAgain;
		TreeMap<String,ArrayList<CssImage>> saveMap = new TreeMap<String,ArrayList<CssImage>>(storeMap);
		readLock.unlock();
		
		boolean saved = false;
		Log.i("LibrarySave", "saving:"+getFilePath());

		File saveFile = context.getFileStreamPath(getFilePath());
		File backupFile = context.getFileStreamPath(getFilePath()+backupSuffix);
		
		if (backupFile.exists()) {
			Log.i("LibrarySave", "last save is incomplete");
			saveFile.delete();
		}else if (saveFile.exists()) {
			Log.i("LibrarySave", "use old file as backup");
			saveFile.renameTo(backupFile);
		}else {
			Log.i("LibrarySave", "first time saving");
		}
		saved = ExportImport.exportFile(saveMap, saveFile);
		if(saved) {
			backupFile.delete();
		}

		return saved;
	}
	
	String getFilePath()
	{
		return mName+fileSuffix;
	}

	void setImageMap(TreeMap<String,ArrayList<CssImage>> newImageMap) {
		writeLock.lock();
    	storeMap = new TreeMap<String,ArrayList<CssImage>>(newImageMap);
    	searchMap = new TreeMap<Css,String>();
    	for (Entry<String,ArrayList<CssImage>> entry : storeMap.entrySet()) {
    		String name = entry.getKey();
			for (CssImage cssImage : entry.getValue()) {
				learnShapeAs(cssImage.getCss(), name);
				//Log.v("Library", "have "+name +":"+ cssImage);
			}
			Log.v("Library", "have "+name +"*"+ entry.getValue().size());
		}
    	dirty = true;
    	asyncSave();
    	writeLock.unlock();
	}
	void addImage(CssImage image, String name)
    {
		writeLock.lock();
    	ArrayList<CssImage> namedSet = copyNoNull(storeMap.get(name));
		storeMap.put(name, namedSet);
    	namedSet.add(image);
    	learnShapeAs(image.getCss(), name);
    	dirty = true;
    	writeLock.unlock();
    	asyncSave();
    }
	static void addImage(TreeMap<String,ArrayList<CssImage>> store, CssImage image, String name)
	{
		ArrayList<CssImage> namedSet = store.get(name);
		if (namedSet == null) {
			namedSet = new ArrayList<CssImage>();
		}
		namedSet.add(image);
		store.put(name, namedSet);
	}
    
	boolean deleteImage(CssImage image, String name)
    {
		writeLock.lock();
    	ArrayList<CssImage> namedSet = copyNoNull(storeMap.get(name));
    	boolean deleted = namedSet.remove(image);
    	if (deleted) {
	    	if (namedSet.isEmpty()) {
				storeMap.remove(name);
			}else {
				storeMap.put(name, namedSet);
			}
	    	setImageMap(storeMap);
	    	asyncSave();
		}
    	writeLock.unlock();
    	return deleted;
    }
	boolean deleteGroup(String name)
    {
		writeLock.lock();
    	ArrayList<CssImage> group = storeMap.remove(name);
    	boolean deleted = false;
    	if (group != null) {
	    	deleted = true;
	    	setImageMap(storeMap);
	    	asyncSave();
		}
    	writeLock.unlock();
    	return deleted;
    }
	boolean renameGroup(String oldName, String newName)
	{
		writeLock.lock();
		ArrayList<CssImage> oldSet = storeMap.remove(oldName);
		boolean moved = true;
		if(oldSet == null || oldName.equals(newName)){
			moved = false;
		}else{
			ArrayList<CssImage> newSet = storeMap.get(newName);
			if(newSet == null){
				newSet = oldSet;
			}else{
				newSet.addAll(oldSet);
			}
			storeMap.put(newName, newSet);
			setImageMap(storeMap);
		}
		writeLock.unlock();
		asyncSave();
    	return moved;
	}
	boolean haveGroup(String name){
		readLock.lock();
		boolean exist = storeMap.get(name) != null;
		readLock.unlock();
		return exist;
	}
    
    
    private void learnShapeAs(Css shap, String name)
    {
    	for (Css css : shap.getMajorRotations()) {
    		searchMap.put(css, name);
		}
        //System.out.println("learned:"+cssMap.size());
    }
    public SearchAnswer find(Css shape)
    {
    	Log.i(TAG,"find:" + shape);
    	if (shape == null) {
			return SearchAnswer.NO_MATCH;
		}
    	readLock.lock();
    	
    	/* need api 9
        Iterator<Entry<Css, String>> des = cssMap.headMap(shape, false).descendingMap().entrySet().iterator();
        Iterator<Entry<Css, String>> ans = cssMap.tailMap(shape, true).entrySet().iterator();
        */
    	Iterator<Entry<Css, String>> des = searchMap.headMap(shape).entrySet().iterator();
    	Iterator<Entry<Css, String>> ans = searchMap.tailMap(shape).entrySet().iterator();
    	final double worstMatch = 0; // 0 to 1
    	double match = worstMatch; 
        String name = "";
        int i = 0;
        
        while (des.hasNext() || ans.hasNext()) {
            Entry<Css, String> next = null;
            i++;
            if ((des.hasNext()&&i%2==0) || !ans.hasNext()) {
                next = des.next();
            }else
                next = ans.next();
            
            double newMatch = shape.match(next.getKey(), 1-match, mirror);
            if (newMatch > 0) {
                name = next.getValue();
                match += newMatch;
                Log.v(TAG,name + ":good:" + match+next);
            }
        }
        readLock.unlock();
        if (match == worstMatch) {
        	return SearchAnswer.NO_MATCH;
		}
        return new SearchAnswer(name, match);
        //return name + ":" + (int)((match-worstMatch)/((1-worstMatch)/100)) +"%";
    }
    String[] getGroupNames() {
    	readLock.lock();
    	Set<String> keySet = storeMap.keySet();
    	String[] names = new String[0];
    	if(keySet != null){
    		names = keySet.toArray(names);
    	}
    	readLock.unlock();
    	return  names;
    }
    CssImage[] getGroup(String cssName) {
    	readLock.lock();
    	ArrayList<CssImage> get = storeMap.get(cssName);
    	CssImage[] names = new CssImage[0];
    	if(get != null){
    		if (get.isEmpty()) {
    			storeMap.remove(cssName);
			}
    		names = get.toArray(names);
    	}
    	readLock.unlock();
    	return  names;
    }
    boolean isEmpty() {
    	readLock.lock();
    	boolean test = storeMap.size() == 0;
    	readLock.unlock();
    	return test;
    }
    String getName(){
    	return mName;
    }
    int cssCount() {
    	int count = 0;
    	for (ArrayList<CssImage> list : storeMap.values()) {
			count += list.size();
		}
    	return count;
    }

    static ArrayList<CssImage> copyNoNull(ArrayList<CssImage> obj) {
    	if (obj==null) {
			return new ArrayList<CssImage>();
		}
    	return new ArrayList<CssImage>(obj);
    }
    @Override
    public String toString() {
    	return super.toString() +" "+mName;
    }
}
