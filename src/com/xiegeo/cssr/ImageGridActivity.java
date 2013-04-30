package com.xiegeo.cssr;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.xiegeo.cssr.analyze.CssImage;

public class ImageGridActivity extends Activity {
	public static final String TAG = "ImageGridActivity";
	static final int DIALOG_IMAGE_OPTIONS_ID = 1000;
	static final int DIALOG_NEW_NAME_ID = 1001;
	static final int DIALOG_NEW_GROUP_ID = 1002;
	static final int DIALOG_DELETE_GROUP_ID = 1003;
	static final int DIALOG_DELETE_IMAGE_ID = 1004;
	public static String cssName;
	
	Library mLibrary;
	static CssImage currentImage;
	CssImageView newNameImageView;
	ImageGridView mGridView;
	ImageOptionsView mOptions;
	//Dialog imageOptionsDialog;
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mLibrary = MainActivity.getLibrary();
		super.onCreate(savedInstanceState);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		mGridView = new ImageGridView(this);
		mGridView.setNumColumns(GridView.AUTO_FIT);
		mGridView.setColumnWidth(MainActivity.getIconSide());
		mGridView.setGravity(Gravity.CENTER); 
		mGridView.setMinimumHeight(10000);
		mGridView.setMinimumWidth(10000);
		setContentView(mGridView);
		
		
		mOptions = new ImageOptionsView(this);
		mOptions.addAction("Move", new OnClickListener() {
			
			public void onClick(View arg0) {
				dismissDialog(DIALOG_IMAGE_OPTIONS_ID);
				showDialog(DIALOG_NEW_NAME_ID);
			}
		});
		mOptions.addAction("Delete", new OnClickListener() {
			public void onClick(View arg0) {
				dismissDialog(DIALOG_IMAGE_OPTIONS_ID);
				showDialog(DIALOG_DELETE_IMAGE_ID);
			}
		});
		mOptions.addAction("Cancel", new OnClickListener() {
			public void onClick(View arg0) {
				dismissDialog(DIALOG_IMAGE_OPTIONS_ID);
			}
		});

		mGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Log.v(TAG, "onItemClick:"+position);
				if (view.getClass() == CssImageView.class) {
					CssImageView v = (CssImageView) view;
					currentImage = v.getImage();
					mOptions.mImageView.setImage(currentImage);
					showDialog(DIALOG_IMAGE_OPTIONS_ID);
				}
			}
		});


	}
	@Override
	protected void onResume() {
		super.onResume();
		mLibrary = MainActivity.getLibrary();
		updateGrid();
	}
	void updateGrid() {
		CssImage[] openedImages =mLibrary.getGroup(cssName);
		if (openedImages.length==0) {
			finish();
		}else {
			setTitle(cssName + " of " + mLibrary.getName() + " ("+openedImages.length+")");
			mGridView.setImages(openedImages);
		}
	}
	@Override
	protected void onPause() {
		super.onPause();
		mLibrary=null;
	}
	@Override
	protected void onStop() {
		super.onStop();
		CssImageView.clearCache();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			break;

		default:
			break;
		}

	    return super.onKeyDown(keyCode, event);
	}
	@SuppressWarnings("deprecation")
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
		    case DIALOG_IMAGE_OPTIONS_ID:
		    	Dialog imageOptionsDialog = new Dialog(this);
		    	imageOptionsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		    	//imageOptionsDialog.setContentView(R.layout.image_options);
		    	imageOptionsDialog.setContentView(mOptions);
		    	return imageOptionsDialog;
		    case DIALOG_NEW_NAME_ID:
		    	newNameImageView = new CssImageView(null, MainActivity.getIconSide(), this);
		    	final EditText input = MyDialogs.generateInputBox(
		    			this, "Name Me");
		    	Dialog newNameDialog = MyDialogs.generateAlertDialog(
		    			newNameImageView, input, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String newName = input.getText().toString();
								CssImage image = currentImage;
								if (mLibrary.deleteImage(image, cssName)) {
									Toast.makeText(input.getContext(), "Moved", Toast.LENGTH_SHORT)
									.show();
									mLibrary.addImage(image, newName);
									updateGrid();
								}
							}
						},
		    			new NameInputVerifier("Move"){
							@Override
							boolean verify(String s, Button b) {
								if(super.verify(s, b)){
									if(mLibrary.haveGroup(s)){
										if(cssName.equals(s)){
											b.setText("No Change");
											return false;
										}
									}else{
										b.setText("Move to New Shap");
									}
									return true;
								}
								return false;
							}
						});

		    	return newNameDialog;
		    case DIALOG_NEW_GROUP_ID:
		    	final EditText ginput = MyDialogs.generateInputBox(
		    			this, "Rename group");
		    	Dialog gnameDialog = MyDialogs.generateAlertDialog(
		    			ginput, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String newName = ginput.getText().toString();
								if (mLibrary.renameGroup(cssName, newName)) {
									Toast.makeText(ginput.getContext(), "Moved", Toast.LENGTH_SHORT)
									.show();
									updateGrid();
								}
							}
						},
		    			new NameInputVerifier("Rename"){
							@Override
							boolean verify(String s, Button b) {
								if(super.verify(s, b)){
									if(mLibrary.haveGroup(s)){
										if(s.equals(cssName)){
											b.setText("No Change");
											return false;
										}
										b.setText("Merge");
									}
									return true;
								}
								return false;
							}
						});

		    	return gnameDialog;
		    case DIALOG_DELETE_GROUP_ID:
		    	Dialog deleteDialog = MyDialogs.generateConformationDialog(
		    			this,"Delete group", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								if(mLibrary.deleteGroup(cssName)) {
									Toast.makeText(mGridView.getContext(), "Deleted", Toast.LENGTH_SHORT)
									.show();
									updateGrid();
								}
							}
		    			});
		    	return deleteDialog;
		    case DIALOG_DELETE_IMAGE_ID:
		    	Dialog deleteIDialog = MyDialogs.generateConformationDialog(
		    			this,"Delete", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								if(mLibrary.deleteImage(currentImage, cssName)) {
									Toast.makeText(mGridView.getContext(), "Deleted", Toast.LENGTH_SHORT)
									.show();
									updateGrid();
								}
							}
		    			});
		    	return deleteIDialog;
		    default:
		        ;
	    }
    	return super.onCreateDialog(id);
    }
	@SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	
    	switch(id) {
		    case DIALOG_IMAGE_OPTIONS_ID:
		    	mOptions.mImageView.setImage(currentImage);
		    	break;
		    case DIALOG_NEW_NAME_ID:
		    	newNameImageView.setImage(currentImage);
		    	break;
		    default:
		    	super.onPrepareDialog(id, dialog);
	    }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_grid_menu, menu);
        return true;
    }
    @SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{		
		case R.id.move:
			showDialog(DIALOG_NEW_GROUP_ID);
			return true;
		case R.id.delete:
			showDialog(DIALOG_DELETE_GROUP_ID);
			return true;
		}
		return false;
	}
}
