package com.xiegeo.cssr;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xiegeo.android.camera.CameraHelper;
import com.xiegeo.cssr.analyze.Analyze;
import com.xiegeo.cssr.analyze.CssImage;
import com.xiegeo.cssr.answer.Summary;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	static final String TAG = "Main";
    static final int DIALOG_LEARN_ID = 0;
    static final int DIALOG_WAIT_ID = 1;

	//static MainActivity mActivity;
    static Preview mPreview;
    static CameraCallback mCallback;
    
    private View mIconView;
    private ImageButton mSoundButton;
    Camera mCamera;
    CssImage mCssImage;
    static private Library mLibrary;
    static private Thread libCreaterThread;
    
    static AnswerTextView mAnswer;
    static Summary answerSummary;
    
    RelativeLayout mLayout;
    
    private EditText mGroupNameInput;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.v("main", "start");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mLayout = new RelativeLayout(this);
        mLayout.setKeepScreenOn(true);
        

        
        mCallback=new CameraCallback(this){
        	public boolean onTouchEvent (MotionEvent event) {
        		if(event.getAction() == MotionEvent.ACTION_DOWN) {
	        		if (mLibrary == null) {
	        			Toast.makeText(MainActivity.this, "Loading ", Toast.LENGTH_LONG)
	        			.show();
						return true;
					}
	        		CssImage newImage = CssImage.getLast();
	        		if (newImage == null|| (mCssImage != null && newImage.getCss() == mCssImage.getCss())) {
	        			//Toast.makeText(mActivity, "just in a sec", Toast.LENGTH_SHORT)
	        			//.show();
	        			return true;
					}
	        		mCssImage = newImage;
	        		mCallback.active = false;
	        		mPreview.setPaused(true);
	        		showDialog(DIALOG_LEARN_ID);
	        		return true;
        		}
        		return false;
        	}
        };
        mCallback.setText("Loading ");
        mCallback.setWidth(10000);
        mCallback.setHeight(10000);
        mCallback.setFocusableInTouchMode(true);
        
        mLayout.addView(mCallback);
        
    	libCreaterThread = new Thread() {public void run() {
    		mLibrary = Library.getLibrary(Library.getCurrentName(MainActivity.this), MainActivity.this);
    	}};
    	libCreaterThread.start();
        
    	
    	mAnswer = new AnswerTextView(this);
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.addRule(RelativeLayout.CENTER_IN_PARENT);
    	mLayout.addView(mAnswer,params);
    	
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	mSoundButton = new ImageButton(this);
    	mSoundButton.setImageResource(R.drawable.ic_action_sound);
    	mSoundButton.setBackgroundColor(Color.TRANSPARENT);
        mSoundButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Summary.enableTTS = !Summary.enableTTS;
				if (Summary.enableTTS) {
					mSoundButton.setImageResource(R.drawable.ic_action_sound);
					Toast.makeText(MainActivity.this, "Started Text To Speech", Toast.LENGTH_SHORT)
					.show();
				}else {
					mSoundButton.setImageResource(R.drawable.ic_action_no_sound);
					Toast.makeText(MainActivity.this, "Stopped Text To Speech", Toast.LENGTH_SHORT)
					.show();
				}
				
				
		        SharedPreferences llp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		        SharedPreferences.Editor editor = llp.edit();
		        editor.putBoolean("enableTTS", Summary.enableTTS);
		        editor.commit();
			}
		});
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    	mLayout.addView(mSoundButton,params);

        Button mInforButton = new Button(this);
        mInforButton.setText("Menu");
        mInforButton.setTextColor(Color.rgb(255, 255, 255));
        mInforButton.setBackgroundColor(Color.argb(100, 0, 0, 0));
        mInforButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				openOptionsMenu();
			}
		});
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM );
    	mLayout.addView(mInforButton,params);
        
        
        
        mIconView = new View(this) {
    		@Override
    		protected void onDraw(Canvas canvas) {
    			
    			if (mIconImage != null) {
    				canvas.drawBitmap(mIconImage, 0, mCssImage.iconSide, 0, 0, mCssImage.iconSide, mCssImage.iconSide, true, null);
    			}
    		}
    		@Override
    		protected void onMeasure(int widthMeasureSpec,
    				int heightMeasureSpec) {
    			if (mCssImage == null) 
    				setMeasuredDimension(50, 50);
    			else
    				setMeasuredDimension(mCssImage.iconSide, mCssImage.iconSide);
    		}
    	};
    	
    	
    }
    void resetCamera() {
    	Log.d(TAG, "resetCamera");
    	releaseCamera();
        try {
	    	Log.d(TAG, "start Camera");
	    	mCamera = CameraHelper.open(Preferences.cameraId);
	    	
	        //Toast.makeText(this, "start", Toast.LENGTH_SHORT).show();
	        Parameters p = mCamera.getParameters();
	        /*
	        List<String> fs = p.getSupportedFocusModes();
	        if(fs!=null){
		        String out="SupportedFocusModes\n";
		        for(String f :fs)
		        {
		        	out+=f+"\n";
		        }
		        Toast.makeText(this, out, Toast.LENGTH_LONG).show();
	        }else
	        	Toast.makeText(this, "no FocusModes", Toast.LENGTH_SHORT).show();
			*/
	        List<Integer> rates = p.getSupportedPreviewFrameRates();
	        
	        if(rates!=null&&rates.size()>0)
	        {
	        	int target = 10;
		        int best = rates.get(0);
		        for(int r :rates)
		        {
		        	if(Math.abs(r-target)<Math.abs(best-target))
		        		best = r;
		        }
		        //Toast.makeText(this, "FrameRate:"+best, Toast.LENGTH_LONG).show();
		        p.setPreviewFrameRate(best);
	    	}else
	    	{
	    		//Toast.makeText(this, "no FrameRates", Toast.LENGTH_SHORT).show();
	    	}
	        mCamera.setParameters(p);
	        mPreview.needBuffer = true;
	        mPreview.setCamera(mCamera);
	        mCamera.setPreviewCallbackWithBuffer(mCallback);
        } catch (Throwable e) {
			Toast.makeText(this, "Camera failed, please restart.", Toast.LENGTH_LONG)
			.show();
			Log.e(TAG, "camera failed", e);
		}
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        
        Preferences.applyPreferences(this);
        
        mSoundButton.setImageResource(Summary.enableTTS? R.drawable.ic_action_sound:R.drawable.ic_action_no_sound);
        answerSummary = new Summary(this);
        
        if (mPreview != null) {
			mLayout.removeView(mPreview);
		}
        mPreview = new Preview(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.addRule(RelativeLayout.CENTER_IN_PARENT);
        mLayout.addView(mPreview,0,params);
        setContentView(mLayout);
        
        try {
        	resetCamera();
		} catch (Throwable e) {
			Toast.makeText(this, "Camera failed, please restart.", Toast.LENGTH_LONG)
			.show();
			Log.e(TAG, "camera failed", e);
		}
        if (mLibrary == null) {
        	showDialog(DIALOG_WAIT_ID);
		}
        if (isReturnToMainActivityAfterSubActivity) {
			mPreview.setPaused(false);
			isReturnToMainActivityAfterSubActivity=false;
		}
    }
    @Override
    protected void onPause() {
    	Log.d(TAG, "onPause");
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
    	releaseCamera();
    	answerSummary.shutdown();
        super.onPause();
    }
    void releaseCamera() {
        if (mCamera != null) {
        	Log.d(TAG, "release Camera");
            mPreview.setCamera(null);
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.release();
            mCamera = null;
        }
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    }


    private int[] mIconImage;
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	final Dialog newDialog;
    	switch(id) {
		    case DIALOG_LEARN_ID:
		    	mGroupNameInput = MyDialogs.generateInputBox(
		    			this, "Name Me");
		    	newDialog = MyDialogs.generateAlertDialog(
		    			mIconView, mGroupNameInput, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String name = mGroupNameInput.getText().toString();
			                	if(mCssImage==null)
			                		Toast.makeText(MainActivity.this, "Nothing to learn yet.", Toast.LENGTH_LONG)
			                				.show();
			                	else if(name.length()<1)
			                		Toast.makeText(MainActivity.this, "Please gave a name.", Toast.LENGTH_LONG)
			                				.show();
			                	else {
			                		mLibrary.addImage(mCssImage, name);
								}
							}
						},
		    			new NameInputVerifier("Learn New Shape"){
							@Override
							boolean verify(String s, Button b) {
								if(super.verify(s, b)){
									if(mLibrary.haveGroup(s)) {
										b.setText("Reinforce");
									}
									b.setEnabled(true);
									return(true);
								}
								return false;
							}
						});
		    	newDialog.setOnDismissListener(
		    			new DialogInterface.OnDismissListener() {
							public void onDismiss(DialogInterface arg0) {
								mCallback.active = true;
								mPreview.setPaused(false);
							}
		    			});
		        break;
		    case DIALOG_WAIT_ID:
		    	newDialog = MyDialogs.generateLibraryLoadingDialog(
		    			this, libCreaterThread, null);
		    	break;
		    default:
		        newDialog = null;
	    }
	    return newDialog;
    }
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id) {
	    case DIALOG_LEARN_ID:
	    	mPreview.setPaused(true); 
	    	if (mCssImage != null) {
	    		mIconImage = mCssImage.getImage(getIconSide());
		    	mIconView.invalidate();
			}
	    	//mGroupNameInput.setAdapter(new ArrayAdapter<String>(
	    	//		mActivity, R.layout.list_item,getLibrary().getGroupNames()));
			break;
    	}
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		mPreview.setPaused(true);
		isReturnToMainActivity=true;
		return true;
	}
	
	private boolean isReturnToMainActivity=true;
	private boolean isReturnToMainActivityAfterSubActivity=false;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		isReturnToMainActivity=false;
		Intent newActivity;
		switch(item.getItemId())
		{
		case R.id.settings:
			isReturnToMainActivityAfterSubActivity=true;
			newActivity = new Intent(getBaseContext(),
                    Preferences.class);
			startActivity(newActivity);
		break;
		case R.id.help:
			isReturnToMainActivityAfterSubActivity=true;
			newActivity = new Intent(getBaseContext(),
                    HelpActivity.class);
			startActivity(newActivity);
		break;
		case R.id.library:
			isReturnToMainActivityAfterSubActivity=true;
			newActivity = new Intent(getBaseContext(),
                    GroupsActivity.class);
			startActivity(newActivity);
		break;
		default: isReturnToMainActivity = true;
		}
		
		if (isReturnToMainActivity) {
			mPreview.setPaused(false);
		}
		return true;
	}
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		if (isReturnToMainActivity) {
			mPreview.setPaused(false);
		}
		Log.v(TAG, "onOptionsMenuClosed");
		super.onOptionsMenuClosed(menu);
	}
	@Override
	public void onContextMenuClosed(Menu menu) {
		if (isReturnToMainActivity) {
			mPreview.setPaused(false);
		}
		Log.v(TAG, "onContextMenuClosed");
		super.onContextMenuClosed(menu);
	}
	
	public static void setLibrary(Library l,Context c)
	{
		mLibrary = l;
		Analyze.runs=0;
        SharedPreferences llp = c.getSharedPreferences(ListOfLibrarysView.TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = llp.edit();
        editor.putString(ListOfLibrarysView.TAG,l.getName());
        editor.commit();
	}
	public static Library getLibrary()
	{
		while(mLibrary == null)
		{
			try {
				libCreaterThread.join();
			} catch (InterruptedException e) {
				Log.e(TAG, "", e);
			}
		}
		return mLibrary;
	}
	public static int getIconSide()
	{
		return Math.min(200, Math.min(mCallback.getWidth()-20, mCallback.getHeight())/4);
	}
}