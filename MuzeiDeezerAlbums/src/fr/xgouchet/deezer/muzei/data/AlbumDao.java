package fr.xgouchet.deezer.muzei.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * 
 * @author Xavier Gouchet
 * 
 */
public class AlbumDao {
    
    
    
    private Database mDatabase;
    
    public AlbumDao(Context context) {
        mDatabase = new Database(context);
    }
    
    /**
     * @return a random album or null
     */
    public synchronized AlbumInfo getRandomAlbum() {
        
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        
        // query the DB 
        Cursor cursor = db.query(Database.Tables.ALBUMS, null, null, null, null, null, "RANDOM()",
                "1");
        if (cursor == null) {
            return null;
        }
        
        if (cursor.moveToFirst()) {
            // get the columns indices
            int idxId = cursor.getColumnIndex(Database.Albums.ID);
            int idxTitle = cursor.getColumnIndex(Database.Albums.TITLE);
            int idxArtist = cursor.getColumnIndex(Database.Albums.ARTIST);
            
            // get the album info
            AlbumInfo album = new AlbumInfo();
            album.id = cursor.getLong(idxId);
            album.title = cursor.getString(idxTitle);
            album.artist = cursor.getString(idxArtist);
            return album;
        } else {
            return null;
        }
        
    }
    
    public synchronized List<AlbumInfo> getSelectedAlbums() {
        
        List<AlbumInfo> albums = new ArrayList<AlbumInfo>();
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        
        
        // Query the database
        Cursor cursor = db.query(Database.Tables.ALBUMS, null, null, null, null, null,
                null);
        if (cursor == null) {
            return albums;
        }
        
        // get the columns indices
        int idxId = cursor.getColumnIndex(Database.Albums.ID);
        int idxTitle = cursor.getColumnIndex(Database.Albums.TITLE);
        int idxArtist = cursor.getColumnIndex(Database.Albums.ARTIST);
        int idxCover = cursor.getColumnIndex(Database.Albums.COVER);
        
        // fill the list
        while (cursor.moveToNext()) {
            AlbumInfo album = new AlbumInfo();
            album.id = cursor.getLong(idxId);
            album.title = cursor.getString(idxTitle);
            album.artist = cursor.getString(idxArtist);
            album.cover = cursor.getString(idxCover);
            albums.add(album);
        }
        
        cursor.close();
        return albums;
    }
    
    
    public synchronized void addAlbum(AlbumInfo album) {
        
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(Database.Albums.ID, album.id);
            values.put(Database.Albums.TITLE, album.title);
            values.put(Database.Albums.ARTIST, album.artist);
            values.put(Database.Albums.COVER, album.cover);
            db.insertOrThrow(Database.Tables.ALBUMS, null, values);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            db.close();
        }
    }
    
    
    public synchronized void removeAlbum(AlbumInfo album) {
        
        String where = Database.Albums.ID + "=?";
        String[] args = new String[] {
                Long.toString(album.id)
        };
        
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        db.beginTransaction();
        try {
            
            db.delete(Database.Tables.ALBUMS, where, args);
            
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            db.close();
        }
    }
}
