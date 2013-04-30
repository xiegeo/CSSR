package com.xiegeo.cssr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.TreeMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xiegeo.cssr.analyze.CssImage;
@SuppressWarnings("deprecation")
public class ListOfLibrarysActivity extends Activity {
	static final String TAG = ListOfLibrarysActivity.class.getSimpleName();
	
    static final int DIALOG_NEW_LIBRARY_ID = 3000;
    static final int DIALOG_IMPORT_LIBRARY_ID = 3001;
    static final int DIALOG_WAIT_ID = 3002;
    static Runnable finishedWait;
	ListOfLibrarysView mLibraryListView;
	boolean openFile = false;
	static Thread importThread;
	TreeMap<String,ArrayList<CssImage>> importMap = null;
	String importFileName = "";
	Library imLibrary = null;
	static Intent oldIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mLibraryListView = new ListOfLibrarysView(this);
		mLibraryListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, final View view, int position,
					long id) {
				if (position == 0) {
					showDialog(DIALOG_NEW_LIBRARY_ID);
				}else if (position == 1) {
					gotoMain();
				}else {
					final String name = ((TextView)view).getText().toString();
					importThread = new Thread("Switch Lib") {
						public void run() {MainActivity.setLibrary(Library.getLibrary(name,view.getContext()),view.getContext());};
					};
					importThread.setDaemon(true);
					importThread.start();
					finishedWait = new Runnable() {
						public void run() {
							mLibraryListView.update();
							gotoMain();
						}
					};
					showDialog(DIALOG_WAIT_ID);
				}
				
			}
		});
		setContentView(mLibraryListView);	
	}
	
	

	private void setImport() {
		imLibrary.setImageMap(importMap);
		MainActivity.setLibrary(imLibrary,this);
		mLibraryListView.update();
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		mLibraryListView.update();
		
		Intent intent = getIntent();
		Log.i(ListOfLibrarysActivity.class.getName(), intent.toString());
		if (oldIntent != intent && intent.getData() != null) {
			Log.i(TAG, "open file");
			oldIntent = intent;
			openFile = true;
			if (recentDialog != null) {
				Toast.makeText(this, "Already working on something, please wait and try to open file again.", Toast.LENGTH_LONG)
				.show();
			}else {
				File open = new File(intent.getData().getPath()); 
				importFileName = open.getName();
				int nameEnds = importFileName.lastIndexOf(".");
				importFileName = importFileName.substring(0, nameEnds);
				final FileInputStream fileInputStream;
				try {
					fileInputStream = new FileInputStream(open);
					showDialog(DIALOG_IMPORT_LIBRARY_ID);
					importThread = new Thread("import file") {
						public void run() {
							importMap = ExportImport.importLibrary(fileInputStream);
						}
					};
					importThread.setDaemon(true);
					importThread.start();
				} catch (FileNotFoundException e) {
					Toast.makeText(this, "file not found", Toast.LENGTH_LONG)
					.show();
				}
			}
		}else {
			if (importThread!=null && importThread.isAlive()) {
				showDialog(DIALOG_WAIT_ID);
			}
		}
	}
	
	Dialog recentDialog = null;
	DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
		public void onDismiss(DialogInterface d) {
			if (recentDialog == d) {
				recentDialog = null;
			}
		}
	};
    @Override
    protected Dialog onCreateDialog(int id) {
    	final Dialog newDialog;
    	final EditText input;
    	switch(id) {
		    case DIALOG_NEW_LIBRARY_ID:
		    	input = MyDialogs.generateInputBox(
		    			this,"Name a library");
		    	newDialog = MyDialogs.generateAlertDialog(
		    			"Create New Library", input, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String name = input.getText().toString();
								Library test = null;
			                	Toast swichingToast = Toast.makeText(input.getContext(), "Switching Library", Toast.LENGTH_LONG);
								swichingToast.show();
			                	test = Library.getLibrary(name, ListOfLibrarysActivity.this);
			                	if (!test.save(true)) {
			                		swichingToast.cancel();
									Toast.makeText(input.getContext(), "Try naming it something else", Toast.LENGTH_LONG)
            							.show();
									return;
								}
								MainActivity.setLibrary(test,input.getContext());
								mLibraryListView.update();
								swichingToast.cancel();
							}
						},
						new LibraryNameInputVerifier("Creat"));
		    	break;
		    case DIALOG_IMPORT_LIBRARY_ID:

		    	input = MyDialogs.generateInputBox(
		    			this,"Name import library");
		    	input.setText(importFileName);
		    	
		    	newDialog = MyDialogs.generateAlertDialog(
		    			"Import", input, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								importFileName = input.getText().toString();
								imLibrary = Library.getLibrary(importFileName, ListOfLibrarysActivity.this);
								if (importMap==null) {
									finishedWait = new Runnable() {
										public void run() {
											setImport();
										}
									};
									showDialog(DIALOG_WAIT_ID);
								}else {
									setImport();
								}
							}
						},
						new LibraryNameInputVerifier("Import"));
		    	
		    	break;
		    case DIALOG_WAIT_ID:
		    	newDialog = MyDialogs.generateLibraryLoadingDialog(
		    			this, importThread, finishedWait);
		    	break;
		    default:
		        newDialog = null;
	    }
    	if (newDialog != null) {
    		newDialog.setOnDismissListener(dismissListener);
		}
	    return newDialog;
    }
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
    	super.onPrepareDialog(id, dialog, args);
    	recentDialog = dialog;
    	switch (id) {
		case DIALOG_WAIT_ID:
			
			break;

		default:
			break;
		}
    }
    
    class LibraryNameInputVerifier extends NameInputVerifier{
    	LibraryNameInputVerifier(String defaltPositiveText) {
    		super(defaltPositiveText);
    	}
    	boolean verify(String s, android.widget.Button b) {
    		if(super.verify(s, b)){
    			if(ListOfLibrarysView.libraryExits(s)){
    				b.setText("Already Exists");
    				return false;
    			}
    			return true;
    		}
    		return false;
    	}
    }
    public void gotoMain() {
		Intent mainActivity = new Intent(getBaseContext(),MainActivity.class);
    	startActivity(mainActivity);
    	if (openFile) {
    		//finish();
		}
    }
}


