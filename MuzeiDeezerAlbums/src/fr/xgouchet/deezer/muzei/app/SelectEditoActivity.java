package fr.xgouchet.deezer.muzei.app;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.OAuthException;
import com.deezer.sdk.network.request.event.RequestListener;

import fr.xgouchet.deezer.muzei.R;
import fr.xgouchet.deezer.muzei.data.EditoDao;
import fr.xgouchet.deezer.muzei.data.EditoInfo;
import fr.xgouchet.deezer.muzei.util.Constants;


public class SelectEditoActivity extends ListActivity {
    
    private DeezerConnect mConnect;
    
    private EditoDao mEditoDao;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_PROGRESS);
        
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        getListView().setOnItemClickListener(mItemSelectedListener);
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        mEditoDao = new EditoDao(this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, this);
        
        setResult(RESULT_CANCELED);
        
        listEditos();
    }
    
    
    /**
     * Fetch the list of editos from server
     */
    private void listEditos() {
        
        setProgressBarVisibility(true);
        setProgressBarIndeterminate(true);
        
        
        DeezerRequest editoRequest = new DeezerRequest("editorial");
        mConnect.requestAsync(editoRequest, mEditoRequestListener);
    }
    
    private void hideProgress() {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                setProgressBarVisibility(false);
            }
        });
    }
    
    private void updateSelection() {
        // Get the list of Editos 
        List<EditoInfo> selectedEditos = mEditoDao.getSelectedEditos();
        
        // check all the rows in the list
        int count = getListAdapter().getCount();
        for (int i = 0; i < count; ++i) {
            EditoInfo edito = (EditoInfo) getListAdapter().getItem(i);
            getListView().setItemChecked(i, (selectedEditos.contains(edito)));
        }
    }
    
    private void showToast(final int resource) {
        runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                Toast.makeText(SelectEditoActivity.this, resource, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // List View Item Selection listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private OnItemClickListener mItemSelectedListener = new OnItemClickListener() {
        
        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                final long id) {
            EditoInfo edito = (EditoInfo) getListAdapter().getItem(position);
            
            if (getListView().isItemChecked(position)) {
                mEditoDao.addEdito(edito);
            } else {
                mEditoDao.removeEdito(edito);
            }
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Editorial Request Listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private RequestListener mEditoRequestListener = new RequestListener() {
        
        @Override
        public void onComplete(final String response, final Object requestId) {
            
            hideProgress();
            
            try {
                final List<EditoInfo> editos = parseResponse(response);
                
                runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        setListAdapter(new ArrayAdapter<EditoInfo>(SelectEditoActivity.this,
                                android.R.layout.simple_list_item_multiple_choice, editos));
                        
                        updateSelection();
                    }
                });
                
            }
            catch (Exception e) {
                showToast(R.string.error_editos_list);
//                Log.e("Select", "onComplete", e);
            }
            
        }
        
        @Override
        public void onOAuthException(final OAuthException e, final Object requestId) {
            hideProgress();
            Log.e("Select", "onOAuthException", e);
        }
        
        @Override
        public void onMalformedURLException(final MalformedURLException e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_editos_list);
//            Log.e("Select", "onMalformedURLException", e);
        }
        
        @Override
        public void onIOException(final IOException e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_editos_list);
//            Log.e("Select", "onIOException", e);
        }
        
        @Override
        public void onDeezerError(final DeezerError e, final Object requestId) {
            hideProgress();
            showToast(R.string.error_editos_list);
//            Log.e("Select", "onDeezerError", e);
        }
        
        private List<EditoInfo> parseResponse(final String response)
                throws JSONException {
            List<EditoInfo> editos = new LinkedList<EditoInfo>();
            
            JSONObject jsonResponse = new JSONObject(response);
            
            // get edito array
            JSONArray jsonData = jsonResponse.optJSONArray("data");
            if (jsonData == null) {
                return editos;
            }
            
            int count = jsonData.length();
            for (int i = 0; i < count; ++i) {
                JSONObject jsonEdito = jsonData.getJSONObject(i);
                
                if ("editorial".equals(jsonEdito.optString("type"))) {
                    EditoInfo edito = new EditoInfo();
                    edito.id = jsonEdito.optLong("id");
                    edito.name = jsonEdito.optString("name");
                    editos.add(edito);
                }
                
            }
            
            return editos;
        }
        
        
    };
    
}
