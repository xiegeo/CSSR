package com.xiegeo.cssr;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class HelpActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_layout);

		// text2 has links specified by putting <a> tags in the string
		// resource. By default these links will appear but not
		// respond to user input. To make them active, you need to
		// call setMovementMethod() on the TextView object.

		TextView linksView = (TextView) findViewById(R.id.links);
		linksView.setMovementMethod(LinkMovementMethod.getInstance());
		TextView thanksView = (TextView) findViewById(R.id.thanks);
		thanksView.setMovementMethod(LinkMovementMethod.getInstance());

	}
}
