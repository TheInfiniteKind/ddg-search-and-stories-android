package com.duckduckgo.mobile.android.util;

public class ReadArticlesManager {
	/*
	public static Boolean addReadArticle(FeedObject feedObject){
		String feedId = feedObject.getId();
		if(feedId != null){
			DDGControlVar.readArticles.add(feedId);
			return true;
		}
		return false;
	}*/
	
	public static String getCombinedStringForReadArticles() {
		String combinedStringForReadArticles = "";
		for(String id : DDGControlVar.readArticles){
			combinedStringForReadArticles += id + "-";
		}
		return combinedStringForReadArticles;
	}
}
