package com.duckduckgo.mobile.android.util;

public enum SCREEN {
	/*SCR_SEARCH_HOME_PAGE(0), */SCR_WEBVIEW(1), SCR_ABOUT(2), SCR_HELP(3), SCR_SETTINGS(4)/*, SCR_SEARCH(5)*/;
	
	private int code;
	
	private SCREEN(int c) {
		   code = c;
		 }
		 
	public int getCode() {
		   return code;
	}
	
	public static SCREEN getByCode(int code){
		switch(code){/*
			case 0:
                return SCR_SEARCH_HOME_PAGE;*/
			case 1:
                return SCR_WEBVIEW;
            case 2:
				return SCR_ABOUT;
            case 3:
                return SCR_HELP;
            case 4:
                return SCR_SETTINGS;/*
            case 5:
                return SCR_SEARCH;*/
			default:
				return SCR_WEBVIEW;//SCR_SEARCH_HOME_PAGE;
		}	
			
	}
}