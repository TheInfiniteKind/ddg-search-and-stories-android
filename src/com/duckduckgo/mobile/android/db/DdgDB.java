package com.duckduckgo.mobile.android.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.duckduckgo.mobile.android.DDGApplication;
import com.duckduckgo.mobile.android.objects.history.HistoryObject;
import com.duckduckgo.mobile.android.util.AppShortInfo;
import com.duckduckgo.mobile.android.util.DDGUtils;
import com.duckduckgo.mobile.android.util.PreferencesManager;

public class DdgDB {

	private SQLiteDatabase db;

	private SQLiteStatement insertStmtApp;
	
	private static final String APP_INSERT = "insert or replace into " +
			DdgDBContracts.APP_TABLE.TABLE_NAME + " (" +
			DdgDBContracts.APP_TABLE.COLUMN_TITLE + "," + DdgDBContracts.APP_TABLE.COLUMN_PACKAGE +
			") values (?,?)";
	
	// if type = recent search, data = query.  if type = web page / feed item, data = title, url is target
	// extraType is for feed source
//	private static final String HISTORY_INSERT = "insert or replace into " + DdgDBContracts.HISTORY_TABLE.TABLE_NAME + " (type, data, url, extraType) values (?,?,?,?)";

	
	public DdgDB(Context context) {
	      OpenHelper openHelper = new OpenHelper(context);
	      this.db = openHelper.getWritableDatabase();
	      this.insertStmtApp = this.db.compileStatement(APP_INSERT);
	}

	public long insertSavedSearch(String query) {
		return insertSavedSearch(null, query);
	}
	
	public long insertSavedSearch(String title, String query) {
		if(query == null)
			return -1L;
		
		ContentValues contentValues = new ContentValues();				 
		contentValues.put(DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY, query);
		contentValues.put(DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_TITLE, (title==null)?query:title);
		// delete old record if exists
		this.db.delete(DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME, DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY + "=?", new String[]{query});
		return this.db.insert(DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME, null, contentValues);
	}
	
	public long insertApp(AppShortInfo appInfo) {
	      this.insertStmtApp.bindString(1, appInfo.name);
	      this.insertStmtApp.bindString(2, appInfo.packageName);
	      long result = this.insertStmtApp.executeInsert();
	      return result;
	}
	
	public long insertRecentSearch(String query) {
		if(PreferencesManager.getRecordHistory()) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE, "R");
			contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_DATA, query);
			contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_URL, "");
			contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_EXTRA_TYPE, "");
			contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_FEED_ID, "");
			// delete old record if exists
			this.db.delete(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='R' AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_DATA + "=?", new String[]{query});
			return this.db.insert(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, contentValues);
		}
		return -1l;
	}
	
	public long insertHistoryObject(HistoryObject object) {
		if(object.getType().equals("F")) {
			return insertFeedItemToHistory(object.getData(), object.getUrl(), object.getExtraType(), object.getFeedId());
		}
		else if(object.getType().equals("W")) {
			return insertWebPage(object.getData(), object.getUrl());
		}
		else if(object.getType().equals("R")) {
			return insertRecentSearch(object.getData());
		}
		return -1L;
	}
	
	public long insertWebPage(String title, String url) {
		if(PreferencesManager.getRecordHistory()) {
			ContentValues contentValues = new ContentValues();
			contentValues.put("type", "W");
			contentValues.put("data", title);
			contentValues.put("url", url);
			contentValues.put("extraType", "");
			contentValues.put("feedId", "");
			// delete old record if exists
			this.db.delete(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, "type='W' AND data=? AND url=?", new String[]{title, url});
			return this.db.insert(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, contentValues);
		}
		return -1l;
	}
	
	public long insertFeedItemToHistory(String title, String url, String extraType, String feedId) {		
		if(PreferencesManager.getRecordHistory()) {
	        ContentValues contentValues = new ContentValues();
	        contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE, "F");
	        contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_DATA, title);
	        contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_URL, url);
	        contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_EXTRA_TYPE, extraType);
	        contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_FEED_ID, feedId);
	        // delete old record if exists
			this.db.delete(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='F' AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_FEED_ID + "=?", new String[]{feedId});
			long res = this.db.insertOrThrow(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, contentValues);
	        return res;
		}
		return -1l;
	}
	
	public void deleteApps() {
	      this.db.delete(DdgDBContracts.APP_TABLE.TABLE_NAME, null, null);
	}
	
	public void deleteHistory() {
	      this.db.delete(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, null);
	}
	
	public int deleteSavedSearch(String query) {
		return this.db.delete(DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME, DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY + "=?", new String[]{query});
	}
	
	public int deleteHistoryByDataUrl(String data, String url) {
		return this.db.delete(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, DdgDBContracts.HISTORY_TABLE.COLUMN_DATA + "=? AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_URL + "=?", new String[]{data, url});
	}
	
	public int deleteHistoryByFeedId(String feedId) {
		return this.db.delete(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, DdgDBContracts.HISTORY_TABLE.COLUMN_FEED_ID + "=?", new String[]{feedId});
	}
	
	private AppShortInfo getAppShortInfo(Cursor c) {
		return new AppShortInfo(c.getString(0), c.getString(1));
	}
	
	private HistoryObject getHistoryObject(Cursor c) {
		return new HistoryObject(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
	}
	
	public ArrayList<AppShortInfo> selectApps(String title){
		ArrayList<AppShortInfo> apps = null;
		Cursor c = null;
		try {
			c = this.db.query(DdgDBContracts.APP_TABLE.TABLE_NAME, null, DdgDBContracts.APP_TABLE.COLUMN_TITLE + " MATCH ?", new String[]{title + "*"}, null, null, null);
			if(c.moveToFirst()) {
				apps = new ArrayList<AppShortInfo>(20);
				do {
					apps.add(getAppShortInfo(c));
				} while(c.moveToNext());
			}
		} finally {
			if(c!=null) {
				c.close();
			}
		}
		return apps;
	}
	
	/**
	 * for checking saved results
	 */
	public boolean isSavedSearch(String query) {
		if(query == null) {
            return false;
        }
		boolean out = false;
        Cursor c = null;
        try {
			c = this.db.query(DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME, null, DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY + "=?", new String[]{query}, null, null, null);
			out = c.moveToFirst();
        } finally {
            if(c!=null) {
                c.close();
            }
        }
        return out;
	}
	
	/**
	 * for checking ordinary web pages
	 * @param pageTitle
	 * @param pageUrl
	 * @return
	 */
	public boolean isSaved(String pageTitle, String pageUrl) {
		if(pageUrl == null) {
            return false;
        }
		
		if(pageTitle == null) {
            pageTitle = "";
        }

        boolean out = false;
        Cursor c = null;
        try {
            //Cursor c = this.db.query(DdgDBContracts.FEED_TABLE.TABLE_NAME, null, "title=? AND url=? AND hidden='F'", new String[]{pageTitle, pageUrl} , null, null, null);
			c = this.db.query(DdgDBContracts.FEED_TABLE.TABLE_NAME, null, DdgDBContracts.FEED_TABLE.COLUMN_TITLE + "=? AND " + DdgDBContracts.FEED_TABLE.COLUMN_URL + "=? AND " + DdgDBContracts.FEED_TABLE.COLUMN_FAVORITE + "!='F'", new String[]{pageTitle, pageUrl}, null, null, null);
			out = c.moveToFirst();
        } finally {
            if(c!=null) {
                c.close();
            }
        }
		return out;
	}
	
	/**
	 * for checking ordinary web pages
	 */
	public boolean isSavedInHistory(String data, String url) {
		if(url == null) {
            return false;
        }
		
		if(data == null) {
            data = "";
        }

        boolean out = false;
        Cursor c = null;
        try {
			c = this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, DdgDBContracts.HISTORY_TABLE.COLUMN_DATA + "=? AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_URL + "=?", new String[]{data, url}, null, null, null);
			out = c.moveToFirst();
        } finally {
            if(c!=null) {
                c.close();
            }
        }
		return out;
	}
	
	public boolean isQueryInHistory(String query) {
		if(query == null) {
            return false;
        }

        boolean out = false;
        Cursor c = null;
        try {
			c = this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, DdgDBContracts.HISTORY_TABLE.COLUMN_DATA + "=? AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_URL + "='' AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='R'", new String[]{query}, null, null, null);
			out = c.moveToFirst();
        } finally {
            if(c!=null) {
                c.close();
            }
        }
        return out;
	}
	
	public ArrayList<HistoryObject> selectHistory(){
		Cursor c = this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, null, null , null, null, null);
        ArrayList<HistoryObject> historyItems = null;
		if(c.moveToFirst()) {
			historyItems = new ArrayList<HistoryObject>(30);
			do {
				historyItems.add(getHistoryObject(c));
			} while(c.moveToNext());
		}
        c.close();
		
		return historyItems;
	}
	
	public Cursor getCursorSearchHistory() {
		return this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='R'", null, null, null, DdgDBContracts.HISTORY_TABLE._ID + " DESC");
	}

    public Cursor getCursorSearchHistory(int limit) {
		return this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='R'", null, null, null, DdgDBContracts.HISTORY_TABLE._ID + " DESC", "" + (limit >= 0 ? limit : 1));
	}
	
	public Cursor getCursorStoryHistory() {
		return this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='F'", null, null, null, DdgDBContracts.HISTORY_TABLE._ID + " DESC");
	}

    public Cursor getCursorSearchHistory(String input) {
		return this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + "='R' AND " + DdgDBContracts.HISTORY_TABLE.COLUMN_DATA + " LIKE '" + input + "%'", null, null, null, DdgDBContracts.HISTORY_TABLE._ID + " DESC");
	}
	
	public Cursor getCursorHistory() {
		return this.db.query(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, null, null, null, null, DdgDBContracts.HISTORY_TABLE._ID + " DESC");
	}
	
	public Cursor getCursorSavedSearch() {
		return this.db.query(DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME, null, null, null, null, null, DdgDBContracts.SAVED_SEARCH_TABLE._ID + " DESC");
	}
	
	
	private static class OpenHelper extends SQLiteOpenHelper {

	      OpenHelper(Context context) {
	         super(context, DdgDBContracts.DATABASE_NAME, null, DdgDBContracts.DATABASE_VERSION);
	      }
	      
	      	private void dropTables(SQLiteDatabase db) {
	      		db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.FEED_TABLE.TABLE_NAME);
		  		db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.APP_TABLE.TABLE_NAME);
		  		db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.HISTORY_TABLE.TABLE_NAME);
		  		db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME);
	      	}
	      	
	      	private void createFeedTable(SQLiteDatabase db) {
				db.execSQL("CREATE TABLE " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "("
						+ DdgDBContracts.FEED_TABLE._ID + " VARCHAR(300) UNIQUE, "
						+ DdgDBContracts.FEED_TABLE.COLUMN_TITLE + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_DESCRIPTION + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_FEED + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_URL + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_IMAGE_URL + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_FAVICON + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_TIMESTAMP + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_CATEGORY + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_TYPE + " VARCHAR(300), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_ARTICLE_URL + " VARCHAR(300), "
						//+"hidden CHAR(1)"
						+ DdgDBContracts.FEED_TABLE.COLUMN_HIDDEN + " CHAR(1), "
						+ DdgDBContracts.FEED_TABLE.COLUMN_FAVORITE + " VARCHAR(300)"
						+ ")"
				);

				db.execSQL("CREATE INDEX idx_id ON " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " (" + DdgDBContracts.FEED_TABLE._ID + ") ");
				db.execSQL("CREATE INDEX idx_idtype ON " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " (" + DdgDBContracts.FEED_TABLE._ID + ", " + DdgDBContracts.FEED_TABLE.COLUMN_TYPE + ") ");
			}
	      	
	      	private void createAppTable(SQLiteDatabase db) {
				db.execSQL("CREATE VIRTUAL TABLE " + DdgDBContracts.APP_TABLE.TABLE_NAME + " USING FTS3 ("
						+ DdgDBContracts.APP_TABLE.COLUMN_TITLE + " VARCHAR(300), "
						+ DdgDBContracts.APP_TABLE.COLUMN_PACKAGE + " VARCHAR(300) "
						+ ")"
				);
			}
	      	
	      	private void createHistoryTable(SQLiteDatabase db) {
				db.execSQL("CREATE TABLE " + DdgDBContracts.HISTORY_TABLE.TABLE_NAME + "("
						+ DdgDBContracts.HISTORY_TABLE._ID + " INTEGER PRIMARY KEY, "
						+ DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE + " VARCHAR(300), "
						+ DdgDBContracts.HISTORY_TABLE.COLUMN_DATA + " VARCHAR(300), "
						+ DdgDBContracts.HISTORY_TABLE.COLUMN_URL + " VARCHAR(300), "
						+ DdgDBContracts.HISTORY_TABLE.COLUMN_EXTRA_TYPE + " VARCHAR(300), "
						+ DdgDBContracts.HISTORY_TABLE.COLUMN_FEED_ID + " VARCHAR(300)"
						+ ")"
				);
			}
	      	
	      	private void createSavedSearchTable(SQLiteDatabase db) {
				db.execSQL("CREATE TABLE " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME + "(" +
						DdgDBContracts.SAVED_SEARCH_TABLE._ID + " INTEGER PRIMARY KEY, " +
						DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_TITLE + " VARCHAR(300), " +
						DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY + " VARCHAR(300) UNIQUE)");
			}

		    @Override
		  	public void onCreate(SQLiteDatabase db) {		  			  
		  			createFeedTable(db);	
		  			createAppTable(db);
		  			createHistoryTable(db);		  			
		  			createSavedSearchTable(db); 
		  	}
	
		  	@Override
		  	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		  		if(oldVersion == 4 && newVersion >= 12) {		  			
	  				ContentValues contentValues = new ContentValues();	  	
		  			
		  			// shape old FEED_TABLE like the new, and rename it as FEED_TABLE_old
		  			db.execSQL("DROP INDEX IF EXISTS idx_id");
		      		db.execSQL("DROP INDEX IF EXISTS idx_idtype");
		  			db.execSQL("ALTER TABLE " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " RENAME TO " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
		  			
		  			dropTables(db);
		  			onCreate(db);
		  			
		  			SharedPreferences sharedPreferences = DDGApplication.getSharedPreferences();
		  					  			
		  			// ***** recent queries *******
		  			List<String> recentQueries = DDGUtils.loadList(sharedPreferences, "recentsearch");
		  			Collections.reverse(recentQueries);
		  			for(String query : recentQueries) {
		  				// insertRecentSearch
		  				contentValues.clear();
						contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_TYPE, "R");
						contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_DATA, query);
						contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_URL, "");
						contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_EXTRA_TYPE, "");
						contentValues.put(DdgDBContracts.HISTORY_TABLE.COLUMN_FEED_ID, "");
		  				db.insert(DdgDBContracts.HISTORY_TABLE.TABLE_NAME, null, contentValues);
		  			}
		  			// ****************************
		  			
		  			// ****** saved search ********
					Cursor c = db.query(DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old", new String[]{"url"}, DdgDBContracts.FEED_TABLE.COLUMN_FEED + "=''", null, null, null, null);
		  			while(c.moveToNext()) {
		  				final String url = c.getString(0);
		  				final String query = DDGUtils.getQueryIfSerp(url);
		  				if(query == null)
		  					continue;
		  				contentValues.clear();
		  				contentValues.put(DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY, query);
		  				db.insert(DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME, null, contentValues);
		  			}
		  			// *****************************
		  					  					  					  			
		  			// ***** saved feed items *****
		  			db.execSQL("DELETE FROM " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old WHERE "+ DdgDBContracts.FEED_TABLE.COLUMN_FEED+"='' ");
		  			db.execSQL("INSERT INTO " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " SELECT *,'','F' FROM " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
		  			db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
		  			// ****************************
		  					  					  		
		  		}
		  		else if(oldVersion == 12 && newVersion >= 14) {		  			
		  			// shape old FEED_TABLE like the new, and rename it as FEED_TABLE_old
		  			db.execSQL("DROP INDEX IF EXISTS idx_id");
		      		db.execSQL("DROP INDEX IF EXISTS idx_idtype");
		  			db.execSQL("ALTER TABLE " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " RENAME TO " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
		  			
		  			db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.FEED_TABLE.TABLE_NAME);
		  			createFeedTable(db);
		  			
		  			// ***** saved feed items *****
					db.execSQL("DELETE FROM " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old WHERE " + DdgDBContracts.FEED_TABLE.COLUMN_FEED + "='' ");
					db.execSQL("INSERT INTO " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " SELECT " +
							DdgDBContracts.FEED_TABLE._ID + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_TITLE + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_DESCRIPTION + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_FEED + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_URL + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_IMAGE_URL + "," +
							DdgDBContracts.FEED_TABLE.COLUMN_FAVICON + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_TIMESTAMP + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_CATEGORY + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_TYPE + ", " +
							"'' AS " + DdgDBContracts.FEED_TABLE.COLUMN_ARTICLE_URL + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_HIDDEN + " FROM " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
		  			db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
		  			// ****************************
		  		}
                else if(oldVersion == 14 && newVersion >=15) {
                    // shape old FEED_TABLE like the new, and rename it as FEED_TABLE_old
                    db.execSQL("DROP INDEX IF EXISTS idx_id");
                    db.execSQL("DROP INDEX IF EXISTS idx_idtype");
                    db.execSQL("ALTER TABLE " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " RENAME TO " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");

                    db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.FEED_TABLE.TABLE_NAME);
                    createFeedTable(db);

                    // ***** saved feed items *****
					db.execSQL("DELETE FROM " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old WHERE " + DdgDBContracts.FEED_TABLE.COLUMN_FEED + "='' ");
					db.execSQL("INSERT INTO " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " SELECT " +
							DdgDBContracts.FEED_TABLE._ID + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_TITLE + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_DESCRIPTION + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_FEED + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_URL + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_IMAGE_URL + "," +
							DdgDBContracts.FEED_TABLE.COLUMN_FAVICON + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_TIMESTAMP + ", " + "" +
							DdgDBContracts.FEED_TABLE.COLUMN_CATEGORY + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_TYPE + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_ARTICLE_URL + ", " +
							DdgDBContracts.FEED_TABLE.COLUMN_HIDDEN + ", " +
							"'F' FROM " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
					db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.FEED_TABLE.TABLE_NAME + "_old");
                    //***** set new favlue for favorite *****
                    String newFavoriteValue = String.valueOf(System.currentTimeMillis());
					db.execSQL("UPDATE " + DdgDBContracts.FEED_TABLE.TABLE_NAME + " SET " + DdgDBContracts.FEED_TABLE.COLUMN_FAVORITE + "=" + newFavoriteValue + " WHERE " + DdgDBContracts.FEED_TABLE.COLUMN_HIDDEN + "='F'");
					// ****************************
                }
				else if(oldVersion == 15 && newVersion >= 16) {
					db.execSQL("ALTER TABLE " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME+ " RENAME TO " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME + "_old");
					db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME);
					createSavedSearchTable(db);
					db.execSQL("INSERT INTO " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME + " SELECT " +
							DdgDBContracts.SAVED_SEARCH_TABLE._ID + ", " +
							DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY + ", " +
							DdgDBContracts.SAVED_SEARCH_TABLE.COLUMN_QUERY + " FROM " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME + "_old");
					db.execSQL("DROP TABLE IF EXISTS " + DdgDBContracts.SAVED_SEARCH_TABLE.TABLE_NAME+ "_old");
				}
		  		else {
		  			dropTables(db);
			  		onCreate(db);
		  		}
		  	}
	}
	
	public void close(){
		db.close();
	}
	
	public SQLiteDatabase getSQLiteDB() {
		return db;
	}

}
