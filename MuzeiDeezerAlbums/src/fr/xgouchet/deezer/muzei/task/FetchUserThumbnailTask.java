package fr.xgouchet.deezer.muzei.task;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;

import com.deezer.sdk.model.User;

import fr.xgouchet.deezer.muzei.util.Constants;


/**
 * An async task which fetches a user thumbnail image from Deezer servers
 * 
 * @author Deezer
 * 
 */
public class FetchUserThumbnailTask extends AsyncTask<User, User, Void> {
    
    /** Buffer size */
    private static final int BUFFERED_STREAM_BUFFER_SIZE = 8196;
    
    /** Log cat tag. */
    private static final String LOG_TAG = "FetchUserThumbnailTask";
    
    /** the current app context */
    private Context mContext;
    
    private UserThumbnailTaskListener mListener;
    
    private Map<User, Bitmap> mThumbnails = new Hashtable<User, Bitmap>();
    
    /**
     * Defines the behavior of a listener for thumbnail download events
     * 
     * @author Deezer
     * 
     */
    public interface UserThumbnailTaskListener {
        
        /**
         * Called to notify the listener that the thumbnail image of a user has been downloaded
         * successfuly.
         * 
         * @param user
         *            the user whose thumbnail is now available.
         * @param bitmap
         *            the thumbnail
         */
        public void thumbnailLoaded(User user, Bitmap bitmap);
    }
    
    /**
     * @param context
     *            the app's context
     */
    public FetchUserThumbnailTask(final Context context, final UserThumbnailTaskListener listener) {
        mContext = context;
        mListener = listener;
    }
    
    
    @Override
    protected Void doInBackground(final User... params) {
        
        for (User user : params) {
            
            //android guidelines suggest to check the cancel state
            if (isCancelled()) {
                return null;
            }
            
            String pictureUrl = user.getPictureUrl() + Constants.COVER_SIZE_BIG;
            
            Log.d(LOG_TAG, "Getting " + pictureUrl);
            if ((pictureUrl == null) || (pictureUrl.length() == 0)) {
                Log.d(LOG_TAG, pictureUrl + " is null or empty. Passed");
                continue;
            }
            
            // 
            Bitmap bitmap;
            File file;
            
            //if not present, download
            try {
                file = downloadPicture(pictureUrl, user.getId());
                
                bitmap = loadBitmapFromFile(file);
            }
            catch (IOException e) {
                Log.e(LOG_TAG, "Error happened during download of " + pictureUrl, e);
                continue;
            }
            
            //if in cache now
            if (bitmap != null) {
                mThumbnails.put(user, bitmap);
                publishProgress(user);
            } else {
                Log.d(LOG_TAG, "Null Drawable");
                file.delete();
            }
        }
        
        return null;
    }
    
    @Override
    protected void onProgressUpdate(final User... values) {
        super.onProgressUpdate(values);
        for (User user : values) {
            mListener.thumbnailLoaded(user, mThumbnails.get(user));
        }
    }
    
    
    
    private File downloadPicture(final String url, final long id)
            throws MalformedURLException, IOException {
        String fileName = "thumb-" + id + ".jpg";
        File localFile = new File(mContext.getCacheDir(), fileName);
        
        if (localFile.exists()) {
            Log.w(LOG_TAG, "File already exists");
            return localFile;
        }
        
        
        Log.v(LOG_TAG, "Downloading " + url + " -> " + localFile.getAbsolutePath());
        InputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        
        try {
            //internal buffer for download.
            final byte[] buffer = new byte[5000];
            inputStream = (InputStream) new URL(url).getContent();
            if (inputStream == null) {
                throw new IOException("Unable to connect to server");
            }
            
            outputStream = new BufferedOutputStream(new FileOutputStream(localFile),
                    BUFFERED_STREAM_BUFFER_SIZE);
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            Log.d(LOG_TAG, url + " fetched");
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    Log.e(LOG_TAG, "Can't close cache input stream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                }
                catch (IOException e) {
                    Log.e(LOG_TAG, "Can't close cache output stream", e);
                }
            }
        }
        
        return localFile;
    }
    
    private Bitmap loadBitmapFromFile(final File file) {
        
        Bitmap source = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (source == null) {
            return null;
        }
        
        // Create custom bitmap
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        // Compute sizes
        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, source.getWidth(), source.getHeight());
        final RectF rectF = new RectF(rect);
        
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);
        
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);
        
        source.recycle();
        
        
        return output;
    }
}
