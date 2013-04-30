package com.xiegeo.cssr;

import android.os.AsyncTask;

import com.xiegeo.cssr.analyze.Analyze;
import com.xiegeo.cssr.answer.SearchAnswer;

class CalculateTask extends AsyncTask<CameraCallback, Void, SearchAnswer> {
   
	private String summaryString;
	
	protected SearchAnswer doInBackground(CameraCallback... tasks) {

    	SearchAnswer ans = tasks[0].calculate();
    	summaryString = MainActivity.answerSummary.update(ans);
    	tasks[0].mCamera.addCallbackBuffer(tasks[0].newData);
        return ans;
    }
    protected void onPostExecute(SearchAnswer result) {
		MainActivity.mCallback.setText(MainActivity.getLibrary().getName() + ":" + Analyze.runs + "\n"+ result);
		MainActivity.mAnswer.setText(summaryString);
    }
}