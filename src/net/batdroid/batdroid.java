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
import android.widget.TableRow;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;


public class batdroid extends Activity implements OnClickListener
{
  public static final String MSG_TAG = "BATDROID -> MainActivity";
	public boolean tether_on = false;
	private	ImageView button_start = null;
	private	ImageView button_stop = null;
        private TableRow startTblRow = null;
        private TableRow stopTblRow = null;

  private BatDroidApplication application = null;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      // Init Application
      this.application = (BatDroidApplication)this.getApplication();

      // Capture our button from layout
    	button_start = (ImageView)findViewById(R.id.startTetherBtn);
    	// Register the onClick listener with the implementation above
    	button_start.setOnClickListener(this);
      // Capture our button from layout
    	button_stop = (ImageView)findViewById(R.id.stopTetherBtn);
    	// Register the onClick listener with the implementation above
    	button_stop.setOnClickListener(this);
      
      // do startup checks
      if (this.application.startupCheckPerformed == false) {
        this.application.startupCheckPerformed = true;
                
        // TODO - Check root-permission, files
        //if (!this.application.coretask.hasRootPermission())
        //  this.openNotRootDialog();
        
        // Check if binaries need to be updated
        if (this.application.binariesExists() == false /*|| this.application.coretask.filesetOutdated()*/) {
          //if (this.application.coretask.hasRootPermission()) {
            this.application.installFiles();
            //}
        }
      }
    }


    // Implement the OnClickListener callback
    public void onClick(View v) {
    
    	int id = v.getId();
    	switch(id){
    		case R.id.startTetherBtn:{
                        Log.d(MSG_TAG, "StartBtn pressed ...");
    			TextView test = (TextView)findViewById(R.id.text_start);
		    	test.setText("Processed event to start tether.");
			tether_on = true;
			toggleStartStop();	
    		}break;
    		case R.id.stopTetherBtn:{
                        Log.d(MSG_TAG, "StopBtn pressed ...");
    			TextView test = (TextView)findViewById(R.id.text_start);
		    	test.setText("Processed event to stop tether.");
			tether_on = false;
			toggleStartStop();	
    		}break;
    		default:{
    		//error handling here
    		}
    	}
        
    }

   private void toggleStartStop() {

		if(tether_on==false)
		{//the tether is off
			this.startTblRow.setVisibility(View.VISIBLE);
			this.stopTblRow.setVisibility(View.GONE);
		}else if(tether_on==true)
		{//the tether is on
			this.startTblRow.setVisibility(View.GONE);
			this.stopTblRow.setVisibility(View.VISIBLE);		
		}

	}
}
