package com.duckduckgo.mobile.android.util;

import com.duckduckgo.mobile.android.container.DuckDuckGoContainer;

import java.util.HashSet;
import java.util.Set;

/**
 * This class contains public objects and primitives accessed and modified all throughout the app.
 */
public class DDGControlVar {
	
	public static SCREEN START_SCREEN = SCREEN.SCR_SEARCH_HOME_PAGE;//SCREEN.SCR_STORIES;	// stories

	public static String regionString = "wt-wt";	// world traveler (none) as default
	
	public static Set<String> defaultSources = null;
	public static Set<String> userAllowedSources = null;
	public static Set<String> userDisallowedSources = null;
		
	public static Set<String> readArticles = new HashSet<String>();
	
	public static boolean homeScreenShowing = true;
	
	public static boolean includeAppsInSearch = false;
    public static int useExternalBrowser = DDGConstants.ALWAYS_INTERNAL;
	public static boolean isAutocompleteActive = true;
	public static boolean automaticFeedUpdate = true;

	public static DuckDuckGoContainer mDuckDuckGoContainer;

	public static boolean mCleanSearchBar = false;
		
	public static boolean hasAppsIndexed = false;
	
	public static Set<String> getRequestSources() throws InterruptedException {
		Set<String> requestSources = new HashSet<String>(DDGControlVar.defaultSources);
		requestSources.removeAll(DDGControlVar.userDisallowedSources);
		requestSources.addAll(DDGControlVar.userAllowedSources);
		return requestSources;
	}
	
	public static final Object DECODE_LOCK = new Object();
}
