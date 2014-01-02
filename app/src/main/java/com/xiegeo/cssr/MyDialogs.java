package com.xiegeo.cssr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyDialogs{

	static Dialog generateLibraryLoadingDialog(Context c, final Thread importThread, final Runnable after) {
		final TextView state = new TextView(c);
		final Dialog newDialog = new Dialog(c) {
			Dialog thisDialog = this;
			@Override
			public void onAttachedToWindow() {
				new Thread() {
					Runnable updateStatics = new Runnable() {
						public void run() {
							state.setText(ExportImport.getState());
						}
					};
					
					public void run() {
						do{
							state.post(updateStatics);
							try {
								importThread.join(200);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}while (importThread.isAlive());
						if (after!=null) {
							state.post(after);
						}
						
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						thisDialog.dismiss();
					};
				}.start();
				super.onAttachedToWindow();
			}
		};
		newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		newDialog.setCancelable(false);
		
		state.setText(ExportImport.getState());
		newDialog.setContentView(state);

		return newDialog;
	}
	static AlertDialog generateConformationDialog(
			final Context context,
			final String title,
			final DialogInterface.OnClickListener positiveAction) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
    	builder.setPositiveButton("Yes", positiveAction);
    	builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
    	final AlertDialog alertDialog = builder.create();
    	return alertDialog;
	}
	static EditText generateInputBox(Context context, String hint) {
		final EditText input = new EditText(context);
		input.setHint(hint);
		input.setSingleLine();
		return input;
	}
	static AlertDialog generateAlertDialog(
			final EditText input,
			final DialogInterface.OnClickListener positiveAction,
			final NameInputVerifier verifier) {
		return generateAlertDialog((View)null, input, positiveAction, verifier);
	}
	static AlertDialog generateAlertDialog(
			final String header,
			final EditText input,
			final DialogInterface.OnClickListener positiveAction,
			final NameInputVerifier verifier) {
		TextView headTextView = new TextView(input.getContext());
		headTextView.setText(header);
		return generateAlertDialog(headTextView, input, positiveAction, verifier);
	}
	static AlertDialog generateAlertDialog(
			final View header,
			final EditText input,
			final DialogInterface.OnClickListener positiveAction,
			final NameInputVerifier verifier) {
		
		final Context context = input.getContext();
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	final LinearLayout layout = new LinearLayout(context);
    	
    	input.setId(1);
    	
    	layout.setOrientation(LinearLayout.VERTICAL);
    	layout.setGravity(Gravity.CENTER);
    	if (header != null) {
    		layout.addView(header);
		}
    	layout.addView(input);
    	
    	builder.setView(layout);
    	
    	DialogInterface.OnClickListener verifiedAction = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				input.setText(input.getText().toString().trim());
				positiveAction.onClick(dialog, which);
			}
		};
    	builder.setPositiveButton(verifier==null?"OK":verifier.defaltPositiveText, verifiedAction);
    	builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
    	final AlertDialog alertDialog = builder.create();
    	
    	if(verifier != null){
	    	alertDialog.setOnShowListener(new OnShowListener() {
	    		Button positiveButten;
	    		boolean started = false;
				public void onShow(DialogInterface arg0) {
					if(!started){
						started = true;
						positiveButten = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
				    	input.addTextChangedListener(new TextWatcher() {
							public void onTextChanged(CharSequence s, int start, int before, int count) {
								positiveButten.setEnabled(verifier.verify(s.toString().trim(),positiveButten));
							}
							public void beforeTextChanged(CharSequence s, int start, int count,
									int after) {}
							public void afterTextChanged(Editable s) {}
						});
					}
					positiveButten.setEnabled(verifier.verify(input.getText().toString(),positiveButten));
				}
			});

    	}
    	
		return alertDialog;
	}
}
class NameInputVerifier{
	static final NameInputVerifier HAVE_CONTENT_VERIFIER = new NameInputVerifier("OK");
	final String defaltPositiveText;
	NameInputVerifier(String defaltPositiveText){
		this.defaltPositiveText = defaltPositiveText;
	}
	boolean verify(String s, Button b){
		boolean haveContent = s.length()>0;
		b.setText(defaltPositiveText);
		return haveContent;
	}
}
