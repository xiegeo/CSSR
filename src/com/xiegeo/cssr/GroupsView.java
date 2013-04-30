package com.xiegeo.cssr;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GroupsView extends ListView {
	public static final String TAG = "GroupsView";
	
	ArrayAdapter<String> mAdapter;
	public GroupsView(Context c) {
		super(c);
		//setBackgroundColor(Color.argb(200, 0, 0, 0));
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		update();
		
		final GroupsView me = this;
		setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				
				String name = mAdapter.getItem(position);
				ImageGridActivity.cssName = name;
				Intent newActivity = new Intent(me.getContext(),
	                    ImageGridActivity.class);
				me.getContext().startActivity(newActivity);
			}
		});
	}
	
	public void update() {
		if (MainActivity.getLibrary().isEmpty()) {
			mAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_item, new String[]{"Empty"});
			setEnabled(false);
		}else {
			mAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_item, MainActivity.getLibrary().getGroupNames());
			setEnabled(true);
		}setAdapter(mAdapter);
		//Log.v(TAG, "updated:"+getAdapter().getCount());
	}
}


