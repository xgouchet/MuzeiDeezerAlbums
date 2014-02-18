package fr.xgouchet.deezer.muzei.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


/**
 * 
 * @author Xavier Gouchet
 * 
 */
public class Database extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "gallery_source.db";
    private static final int DATABASE_VERSION = 1;
    
    interface Tables {
        
        String EDITOS = "editos";
        String ALBUMS = "albums";
    }
    
    interface Editos extends BaseColumns {
        
        String ID = "edito_id";
        String NAME = "name";
    }
    
    interface Albums extends BaseColumns {
        
        String ID = "album_id";
        String TITLE = "album_title";
        String ARTIST = "album_artist";
        String COVER = "album_cover";
    }
    
    
    /**
     * @param context
     */
    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.EDITOS + " ("
                + Editos._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Editos.ID + " INTEGER NOT NULL,"
                + Editos.NAME + " TEXT NOT NULL,"
                + "UNIQUE (" + Editos.ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.ALBUMS + " ("
                + Albums._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Albums.ID + " INTEGER NOT NULL,"
                + Albums.TITLE + " TEXT,"
                + Albums.ARTIST + " TEXT,"
                + Albums.COVER + " TEXT,"
                + "UNIQUE (" + Albums.ID + ") ON CONFLICT REPLACE)");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: proper migrations
        db.execSQL("DROP TABLE IF EXISTS " + Tables.EDITOS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.ALBUMS);
        onCreate(db);
    }
}
