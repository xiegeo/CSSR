package com.xiegeo.cssr.answer;

public class SearchAnswer{
	public static final SearchAnswer NO_MATCH = new SearchAnswer("",0);
	
	public final String name;
	public final double percent;// 0 to 1
	public final long birth = System.nanoTime();
	public SearchAnswer(String name, double percent) {
		super();
		this.name = name;
		this.percent = percent;
	}
	public double miniDifference(SearchAnswer[] recentAnswers) {
		double miniDiff = 1.0;
		for (SearchAnswer a : recentAnswers) {
			miniDiff = Math.min(miniDiff, difference(a));
		}
		return miniDiff;
	}
	public double difference(SearchAnswer c){
		if (c == NO_MATCH) {
			return percent;
		}
		else if (name == c.name) {
			return Math.abs(percent-c.percent);
		}
		return 1.0;
	}
	@Override
	public String toString() {
		return name + ":" + (int)(percent*100) +"%";
	}
}