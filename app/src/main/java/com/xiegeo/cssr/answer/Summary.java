package com.xiegeo.cssr.answer;

import java.util.Arrays;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

public class Summary {
	private static final String TAG = "Summary";
	private static final int ANSWERS = 8;
	private static final long AnserMaxAge = (long) 3e9; // e9 = sec
	private static final long ClearedFlagTimeShown = (long) 0.5e9; 
	private SearchAnswer[] recentAnswers = new SearchAnswer[ANSWERS];
	private double[] recentDiff = new double[ANSWERS];
	private int answerIndex = 0; //index of ring
	long clearedTimer = System.nanoTime() + ClearedFlagTimeShown;
	
	public static boolean enableTTS = false;
	private final TextToSpeech tts;
	private boolean ttsIsReady = false;
	private String lastSpokenName = null;
	private static final long lastSpokenMaxAge = (long) 5e9; 
	private static final long lastSpokenMinAge = (long) 1e9; 
	private long lastSpokenTime = System.nanoTime() - lastSpokenMaxAge;
	
	public Summary(final Context context) {
		Arrays.fill(recentAnswers, SearchAnswer.NO_MATCH);
		
		tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
			public void onInit(int status) {
				Log.i(TAG, "onInit tts "+status);
				if (status == TextToSpeech.SUCCESS) {
					ttsIsReady = true;
				}else {
					String errorMsg = "Text To Speech failed: "+status;
					Log.e(TAG,errorMsg);
					Toast.makeText(context, errorMsg, Toast.LENGTH_LONG)
					.show();
				}
			}
		});
	}
	
	public String update(SearchAnswer searchAnswer) {
		final long now = System.nanoTime();
		double newDiff = searchAnswer.miniDifference(recentAnswers);
		if (!expected(newDiff)) {
			Arrays.fill(recentAnswers, SearchAnswer.NO_MATCH);
			clearedTimer = now + ClearedFlagTimeShown;
		}
		recentDiff[answerIndex] = newDiff;
		
		recentAnswers[answerIndex] = searchAnswer;
		answerIndex = (answerIndex+1)%ANSWERS;
		
		SearchAnswer best = SearchAnswer.NO_MATCH;
		long longAgo = now - AnserMaxAge;
		for (int i = 0;i<ANSWERS;i++) {
			if(recentAnswers[i].birth < longAgo) {
				recentAnswers[i] = SearchAnswer.NO_MATCH;
				recentDiff[i] = 0;
				//Log.i("old", "now:"+now+" birth:"+recentAnswers[i].birth);
			}
			else if(recentAnswers[i].percent>best.percent) {
				best = recentAnswers[i];
			}
		}
		double score = best.percent;
		for (SearchAnswer a : recentAnswers) {
			if (a != SearchAnswer.NO_MATCH) {
				double age = now - a.birth;
				double rel = age/AnserMaxAge;
				if (rel > 0) {
					if(!a.name.equals(best.name)) {
						score -= (a.percent/(ANSWERS/4.0)) * rel;
					}else {
						score += (a.percent/(ANSWERS*2.0)) * rel;
					}
				}
			}
		}
		
		StringBuffer out = new StringBuffer(best.name);
		if (score>.9) {
			out.append("!");
		}else if (score>.8) {
			;
		}else if (score>.7) {
			out.append("?");
		}else if (score>.6) {
			out.append("??");
		}else if (score>.5) {
			out.append("???");
		}else {
			out = new StringBuffer();
		}
		
		if (clearedTimer > now ) {
			out.append("△");
		}
		
		
		if (enableTTS && ttsIsReady && score>.8 &&  lastSpokenTime < now - lastSpokenMinAge  &&
				( (lastSpokenName == null || !lastSpokenName.equals(best.name)) || lastSpokenTime < now - lastSpokenMaxAge ) ) {
			lastSpokenName = best.name;
			lastSpokenTime = now;
			tts.speak(best.name, TextToSpeech.QUEUE_FLUSH, null);
		}
		
		
		
		return out.toString();
	}
	boolean expected(double diff) {
		if(diff < .1) return true;
		for (double d : recentDiff) {
			if (diff < 1.5*d) {
				return true;
			}
		}
		return false;
	}
	
	public void shutdown() {
		tts.shutdown();
	}
}
