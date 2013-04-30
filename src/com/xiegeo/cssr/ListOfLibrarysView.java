package com.xiegeo.cssr;

import java.util.ArrayList;
import java.util.TreeSet;

import android.content.Context;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListOfLibrarysView extends ListView {
	public static final String TAG = "ListOfLibrarysView";
	public static final String newLib = "Create new Library";
	ArrayAdapter<String> mAdapter;

	static TreeSet<String> names;
	
	public ListOfLibrarysView(Context c) {
		super(c);
		
		setBackgroundColor(Color.argb(200, 0, 0, 0));
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}
	
	protected void update() {
		final Context context = getContext();
		names = Library.getLibNames(context);
		
		mAdapter = new ArrayAdapter<String>(context, R.layout.list_item, listGenerater());
		setAdapter(mAdapter);
	}
	

	private ArrayList<String> listGenerater()
	{
		final Context context = getContext();
		ArrayList<String> out = new ArrayList<String>(names.size()+2);
		out.add(newLib);
		String currentLibName = Library.getCurrentName(context);
		out.add(currentLibName + " (Current)");
		for (String name : Library.getLibNames(context)) {
			if (!name.equals(currentLibName) ) {
				out.add(name);
			}
		}
		//Log.v(TAG,out.toString());
		return out;
	}
	static boolean libraryExits(String name)
	{
		return names.contains(name);
	}
}
