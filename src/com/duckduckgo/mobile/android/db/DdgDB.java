package com.duckduckgo.mobile.android.db;

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
import com.duckduckgo.mobile.android.util.DDGUtils;

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
