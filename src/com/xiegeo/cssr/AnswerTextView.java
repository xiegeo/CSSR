package com.xiegeo.cssr;


import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

public class AnswerTextView extends TextView {
	static final String TAG = "AnswerTextView";
	private final int totalTextWidth;
	
	public AnswerTextView(Context context) {
		super(context);
		totalTextWidth = context.getResources().getDisplayMetrics().widthPixels *5/6;
	}
	
	@Override
	public void setText(CharSequence text, BufferType type) {
		float length = Math.max(10, text.length());
		float cw = (totalTextWidth/length);
		setTextSize(TypedValue.COMPLEX_UNIT_PX,cw);
		super.setText(text, type);
	}


}