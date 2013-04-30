package com.xiegeo.cssr;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

public class ImageOptionsView extends LinearLayout {

	final CssImageView mImageView;
	
	
	public ImageOptionsView(Context context) {
		super(context);
		setOrientation(VERTICAL);
		setGravity(Gravity.CENTER);
		mImageView = new CssImageView(null, MainActivity.getIconSide(), context);
		
		addView(mImageView,LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
	}

	public void addAction(String text, OnClickListener action)
	{
		Button button = new Button(getContext());
		button.setText(text);
		button.setOnClickListener(action);
		//button.setGravity(Gravity.CENTER);
		//button.setGravity(Gravity.FILL);
		//button.setWidth(300);
		addView(button,LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
		requestLayout();
	}
}
