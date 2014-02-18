package fr.xgouchet.deezer.muzei.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public final class EditoDao {
    
    
    private Database mDatabase;
    
    public EditoDao(Context context) {
        mDatabase = new Database(context);
    }
    
    /**
     * @return a random edito or null
     */
    public synchronized EditoInfo getRandomEdito() {
        
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        
        // query the DB 
        Cursor cursor = db.query(Database.Tables.EDITOS, null, null, null, null, null, "RANDOM()",
                "1");
        if (cursor == null) {
            return null;
        }
        
        if (cursor.moveToFirst()) {
            // get the columns indices
            int idxId = cursor.getColumnIndex(Database.Editos.ID);
            int idxName = cursor.getColumnIndex(Database.Editos.NAME);
            
            // get the edito info
            EditoInfo edito = new EditoInfo();
            edito.id = cursor.getLong(idxId);
            edito.name = cursor.getString(idxName);
            return edito;
        } else {
            return null;
        }
        
    }
    
    
    
    public synchronized List<EditoInfo> getSelectedEditos() {
        
        List<EditoInfo> editos = new ArrayList<EditoInfo>();
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        
        
        // Query the database
        Cursor cursor = db.query(Database.Tables.EDITOS, null, null, null, null, null,
                null);
        if (cursor == null) {
            return editos;
        }
        
        // get the columns indices
        int idxId = cursor.getColumnIndex(Database.Editos.ID);
        int idxName = cursor.getColumnIndex(Database.Editos.NAME);
        
        // fill the list
        while (cursor.moveToNext()) {
            EditoInfo edito = new EditoInfo();
            edito.id = cursor.getLong(idxId);
            edito.name = cursor.getString(idxName);
            editos.add(edito);
        }
        
        cursor.close();
        return editos;
    }
    
    
    public synchronized void addEdito(EditoInfo edito) {
        
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(Database.Editos.ID, edito.id);
            values.put(Database.Editos.NAME, edito.name);
            db.insertOrThrow(Database.Tables.EDITOS, null, values);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            db.close();
        }
    }
    
    
    public synchronized void removeEdito(EditoInfo edito) {
        
        String where = Database.Editos.ID + "=?";
        String[] args = new String[] {
                Long.toString(edito.id)
        };
        
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        db.beginTransaction();
        try {
            
            db.delete(Database.Tables.EDITOS, where, args);
            
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
            db.close();
        }
    }
}
