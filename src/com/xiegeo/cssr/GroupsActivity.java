package com.xiegeo.cssr;

import java.io.File;
import java.util.TreeSet;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class GroupsActivity extends Activity {
	static final String TAG = "GroupsActivity";
	static final int DIALOG_DELETE_LIB_ID = 2000;
	static final int DIALOG_COPY_LIB_ID = 2001;
	static final int DIALOG_MERGE_LIB_ID = 2002;
	static final int DIALOG_EXPORT_LIB_ID = 2003;
	GroupsView mGroupsView;
	Library mLibrary;
	TreeSet<String> libNames;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mLibrary = MainActivity.getLibrary();
		super.onCreate(savedInstanceState);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mGroupsView = new GroupsView(this);
		setContentView(mGroupsView);

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mLibrary = MainActivity.getLibrary();
		setTitle(mLibrary.getName());
		libNames = Library.getLibNames(this);
		mGroupsView.update();
	}
	@Override
	protected void onPause() {
		super.onPause();
		mLibrary = null;
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.groups_menu, menu);
        return true;
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{		
		case R.id.delete_lib:
			showDialog(DIALOG_DELETE_LIB_ID);
			return true;
		case R.id.copy_lib:
			showDialog(DIALOG_COPY_LIB_ID);
			return true;
		case R.id.change_lib:
			Intent newActivity = new Intent(getBaseContext(),
                    ListOfLibrarysActivity.class);
			startActivity(newActivity);
			return true;
		case R.id.export_lib:
			if (!ExportImport.isExternalWriteable()) {
				Toast.makeText(this, "External Media is not writeable.", Toast.LENGTH_LONG)
				.show();
			}else {
				showDialog(DIALOG_EXPORT_LIB_ID);
			}
			return true;
		}
		return false;
	}
	String newLibName = "";
    @SuppressWarnings("deprecation")
	protected Dialog onCreateDialog(int id) {
    	final Dialog dialog;
    	final EditText input;
    	switch(id) {
		    case DIALOG_DELETE_LIB_ID:
		    	dialog = MyDialogs.generateConformationDialog(
		    			this, "Delete Library", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								mLibrary.clearData();
								Intent newActivity = new Intent(getBaseContext(),
					                    ListOfLibrarysActivity.class);
								startActivity(newActivity);
							}
		    			});
		    	return dialog;
		    case DIALOG_COPY_LIB_ID:
		    	input = MyDialogs.generateInputBox(this, "Library Name");
		    	dialog = MyDialogs.generateAlertDialog(
		    			"Copy/Merge Library",
		    			input,
		    			new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								newLibName = input.getText().toString();
								if(libNames.contains(newLibName)){
									showDialog(DIALOG_MERGE_LIB_ID);
								}else{
									Library newLibrary = Library.copyLibrary(mLibrary, newLibName,GroupsActivity.this);
									if(newLibrary.equals(mLibrary)){
										Toast.makeText(input.getContext(), "bad naming, not copied", Toast.LENGTH_LONG)
										.show();
									}else{
										Toast.makeText(input.getContext(), "Copied", Toast.LENGTH_SHORT)
										.show();
									}
								}
							}
		    			},
		    			new NameInputVerifier("need Name"){
							@Override
							boolean verify(String s, Button b) {
								//Log.w(TAG, s+b+libNames);
								if(super.verify(s, b)){
									if(libNames.contains(s)){
										if(s.equals(mLibrary.getName())){
											b.setText("No Change");
											return false;
										}
										b.setText("Merge");
									}else{
										b.setText("Copy");
									}
									return true;
								}
								return false;
							}
						});
		    	return dialog;	
		    case DIALOG_MERGE_LIB_ID:
		    	dialog = MyDialogs.generateConformationDialog(
		    			this, newLibName+" will be expanded", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								Library receiver = Library.getLibrary(newLibName,GroupsActivity.this);
								Library.mergeLibrarys(mLibrary, receiver);
							}
		    			});
		    	return dialog;
		    case DIALOG_EXPORT_LIB_ID:
		    	input = MyDialogs.generateInputBox(this, "File Name");
		    	input.setText(mLibrary.getName());
		    	dialog = MyDialogs.generateAlertDialog(
		    			"Export Library",
		    			input,
		    			new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								if (!ExportImport.isExternalWriteable()) {
									Toast.makeText(input.getContext(), "External Media is not writeable.", Toast.LENGTH_LONG)
									.show();
									return;
								}
								
								String fileName = input.getText().toString() + ".cssl";
								File to = Environment.getExternalStorageDirectory();
								to = new File(to, fileName);
								if(ExportImport.exportFile(mLibrary.getMapForExport(),to)) {
									Toast.makeText(input.getContext(), "Exported", Toast.LENGTH_LONG)
									.show();
								}else {
									Toast.makeText(input.getContext(), "Failed, try a different name.", Toast.LENGTH_LONG)
									.show();
								}
							}
		    			},
		    			new NameInputVerifier("Export"){
							File dir = Environment.getExternalStorageDirectory();
							@Override
							boolean verify(String s, Button b) {
								if(super.verify(s, b)){
									if (s.contains(".")) {
										b.setText("Can't contain \".\"");
										return false;
									}
									if (new File(dir, s+".cssl").exists()) {
										b.setText("Export (Over Write)");
									}
									return true;
								}
								return false;
							}
						});
		    	return dialog;	
		    default:
		        ;
	    }
    	return super.onCreateDialog(id);
    }
}
