package com.xiegeo.cssr;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileListActivity extends Activity {
	
	File selected = null;
	File[] list = null;
	ListView mListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mListView = new ListView(this);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selected = list[position];
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		selected = null;
		update();
	}
	
	void update(){
		list = ExportImport.listFiles(Environment.getExternalStorageDirectory(), ".cssl");
		ArrayAdapter<File> a = new ArrayAdapter<File>(this, R.layout.list_item,list);
		mListView.setAdapter(a);
	}
}
