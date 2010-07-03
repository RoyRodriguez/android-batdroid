package net.batdroid;

import java.io.IOException;

import android.R.drawable;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.app.Activity;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;



public class batdroid extends Activity implements OnClickListener
{
        public static final String MSG_TAG = "BATDROID -> MainActivity";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	// Capture our button from layout
    	ImageView button_start = (ImageView)findViewById(R.id.startTetherBtn);
    	// Register the onClick listener with the implementation above
    	button_start.setOnClickListener(this);
        // Capture our button from layout
    	ImageView button_stop = (ImageView)findViewById(R.id.stopTetherBtn);
    	// Register the onClick listener with the implementation above
    	button_stop.setOnClickListener(this);
    }


    // Implement the OnClickListener callback
    public void onClick(View v) {
    
    	int id = v.getId();
    	switch(id){
    		case R.id.startTetherBtn:{
                        Log.d(MSG_TAG, "StartBtn pressed ...");
    			TextView test = (TextView)findViewById(R.id.text_start);
		    	test.setText("Processed event to start tether.");	
    		}break;
    		case R.id.stopTetherBtn:{
                        Log.d(MSG_TAG, "StopBtn pressed ...");
    			TextView test = (TextView)findViewById(R.id.text_start);
		    	test.setText("Processed event to stop tether.");	
    		}break;
    		default:{
    		//error handling here
    		}
    	}
        
    }
}
