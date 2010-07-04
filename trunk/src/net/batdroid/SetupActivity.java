/**
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *  You should have received a copy of the GNU General Public License along with
 *  this program; if not, see <http://www.gnu.org/licenses/>.
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Seth Lemons.
 */

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
//import android.tether.system.Configuration;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

public class SetupActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
       
        private BatDroidApplication application = null;
       
        private ProgressDialog progressDialog;
       
        public static final String MSG_TAG = "BATDROID -> SetupActivity";

    private String currentSSID;
    private String currentChannel;
    private String currentIpAddress;
    private String currentLAN;
    private String currentNetmask;
    private String currentTransmitPower;
   
    private EditTextPreference prefPassphrase;
    private EditTextPreference prefSSID;
    private EditTextPreference prefIP;
    private EditTextPreference prefNetmask;
    private static int ID_DIALOG_RESTARTING = 2;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        // Init Application
        this.application = (BatDroidApplication)this.getApplication();
       
        // Init CurrentSettings
        this.currentSSID = "ssidpref";//this.application.settings.getString("ssidpref", "AndroidTether");
	this.currentIpAddress = "10.0.0.0/8";
	this.currentNetmask = "255.255.255.0";
        this.currentChannel = "channelpref";// this.application.settings.getString("channelpref", "6");
        this.currentLAN = "lannetworkpref";//this.application.settings.getString("lannetworkpref", this.application.DEFAULT_LANNETWORK);
        this.currentTransmitPower = "txpowerpref";//this.application.settings.getString("txpowerpref", "disabled");
       
        addPreferencesFromResource(R.layout.setupview);

        // Disable "Transmit power" if not supported
        if (!this.application.isTransmitPowerSupported()) {
                PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
                ListPreference txpowerPreference = (ListPreference)findPreference("txpowerpref");
                wifiGroup.removePreference(txpowerPreference);
        }
       
       
        // SSID-Validation
        this.prefSSID = (EditTextPreference)findPreference("ssidpref");
        this.prefSSID.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
          public boolean onPreferenceChange(Preference preference,
          Object newValue) {
            String message = validateSSID(newValue.toString());
            if(!message.equals("")) {
              SetupActivity.this.application.displayToastMessage(message);
              return false;
            }
            return true;
        }});
	//Ip Validation
        this.prefIP = (EditTextPreference)findPreference("ippref");
        this.prefIP.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
          public boolean onPreferenceChange(Preference preference,
          Object newValue) {
            String message = validateIP(newValue.toString());
            if(!message.equals("")) {
              SetupActivity.this.application.displayToastMessage(message);
              return false;
            }
            return true;
        }});
	//Ip Validation
        this.prefNetmask = (EditTextPreference)findPreference("netmaskpref");
        this.prefNetmask.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
          public boolean onPreferenceChange(Preference preference,
          Object newValue) {
            String message = validateNetmask(newValue.toString());
            if(!message.equals("")) {
              SetupActivity.this.application.displayToastMessage(message);
              return false;
            }
            return true;
        }});

   
           //     Boolean bluetoothOn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("bluetoothon", false);
                Message msg = Message.obtain();
             //   msg.what = bluetoothOn ? 0 : 1;
             //   SetupActivity.this.setWifiPrefsEnableHandler.sendMessage(msg);
    }
       
    @Override
    protected void onResume() {
        Log.d(MSG_TAG, "Calling onResume()");
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
   
    @Override
    protected void onPause() {
        Log.d(MSG_TAG, "Calling onPause()");
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);  
    }
   
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_RESTARTING) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Restart B.A.T.M.A.N.");
                progressDialog.setMessage("Please wait while restarting...");
                progressDialog.setIndeterminate(false);
                progressDialog.setCancelable(true);
                return progressDialog;
        }
        return null;
    }
   
   
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateConfiguration(sharedPreferences, key);
    }
   
    Handler restartingDialogHandler = new Handler(){
        public void handleMessage(Message msg) {
                if (msg.what == 0)
                        SetupActivity.this.showDialog(SetupActivity.ID_DIALOG_RESTARTING);
                else
                        SetupActivity.this.dismissDialog(SetupActivity.ID_DIALOG_RESTARTING);
                super.handleMessage(msg);
                System.gc();
        }
    };
   
   Handler displayToastMessageHandler = new Handler() {
        public void handleMessage(Message msg) {
                if (msg.obj != null) {
                        SetupActivity.this.application.displayToastMessage((String)msg.obj);
                }
                super.handleMessage(msg);
                System.gc();
        }
    };
   
   
    private void updateConfiguration(final SharedPreferences sharedPreferences, final String key) {
        new Thread(new Runnable(){
                        public void run(){
                                String message = null;
                        if (key.equals("ssidpref")) {
                                String newSSID = sharedPreferences.getString("ssidpref", "AndroidTether");
                                if (SetupActivity.this.currentSSID.equals(newSSID) == false) {
                                        SetupActivity.this.currentSSID = newSSID;
                                        message = "SSID changed to '"+newSSID+"'.";
                                        try{
                                                if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        // Show RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                                        // Restart Tethering
                                                        //SetupActivity.this.application.restartTether();
                                                        // Dismiss RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                }
                                        }
                                        catch (Exception ex) {
                                                message = "Unable to restart B.A.T.M.A.N!";
                                        }
                                        // Send Message
                                        Message msg = new Message();
                                        msg.obj = message;
                                        SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                                }
                        }
                        else if (key.equals("ippref")) {
                                String newIP = sharedPreferences.getString("ippref", "10.0.0.0/8");
                                if (SetupActivity.this.currentIpAddress.equals(newIP) == false) {
                                        SetupActivity.this.currentIpAddress = newIP;
                                        message = "IP changed to '"+newIP+"'.";
                                        try{
                                                if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        // Show RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                                        // Restart Tethering
                                                        //SetupActivity.this.application.restartTether();
                                                        // Dismiss RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                }
                                        }
                                        catch (Exception ex) {
                                                message = "Unable to restart B.A.T.M.A.N!";
                                        }
                                        // Send Message
                                        Message msg = new Message();
                                        msg.obj = message;
                                        SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                                }
                        }
                        else if (key.equals("netmaskpref")) {
                                String newNetmask = sharedPreferences.getString("netmaskpref", "255.255.255.0");
                                if (SetupActivity.this.currentNetmask.equals(newNetmask) == false) {
                                        SetupActivity.this.currentNetmask = newNetmask;
                                        message = "Netmask changed to '"+newNetmask+"'.";
                                        try{
                                                if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        // Show RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                                        // Restart Tethering
                                                        //SetupActivity.this.application.restartTether();
                                                        // Dismiss RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                }
                                        }
                                        catch (Exception ex) {
                                                message = "Unable to restart B.A.T.M.A.N.!";
                                        }
                                        // Send Message
                                        Message msg = new Message();
                                        msg.obj = message;
                                        SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                                }
                        }
                        else if (key.equals("channelpref")) {
                                String newChannel = sharedPreferences.getString("channelpref", "6");
                                if (SetupActivity.this.currentChannel.equals(newChannel) == false) {
                                        SetupActivity.this.currentChannel = newChannel;
                                        message = "Channel changed to '"+newChannel+"'.";
                                        try{
                                                if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        // Show RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                                        // Restart Tethering
                                                        //SetupActivity.this.application.restartTether();
                                                        // Dismiss RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                }
                                        }
                                        catch (Exception ex) {
                                                message = "Unable to restart B.A.T.M.A.N.!";
                                        }
                                        // Send Message
                                        Message msg = new Message();
                                        msg.obj = message;
                                        SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                                }
                        }
                        else if (key.equals("wakelockpref")) {
                                        try {
                                                boolean disableWakeLock = sharedPreferences.getBoolean("wakelockpref", true);
                                                if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        if (disableWakeLock){
                                                                //SetupActivity.this.application.releaseWakeLock();
                                                                message = "Wake-Lock is now disabled.";
                                                        }
                                                        else{
                                                                //SetupActivity.this.application.acquireWakeLock();
                                                                message = "Wake-Lock is now enabled.";
                                                        }
                                                }
                                        }
                                        catch (Exception ex) {
                                                message = "Unable to save Auto-Sync settings!";
                                        }
                                       
                                        // Send Message
                                Message msg = new Message();
                                msg.obj = message;
                                SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                        }

                        else if (key.equals("txpowerpref")) {
                                String transmitPower = sharedPreferences.getString("txpowerpref", "disabled");
                                if (transmitPower.equals(SetupActivity.this.currentTransmitPower) == false) {
                                        // Restarting
                                                try{
                                                        if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        // Show RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                                        // Restart Tethering
                                                                //SetupActivity.this.application.restartTether();
                                                        // Dismiss RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                        }
                                                }
                                                catch (Exception ex) {
                                                        Log.e(MSG_TAG, "Exception happend while restarting service - Here is what I know: "+ex);
                                                }
                                       
                                                message = "Transmit power changed to '"+transmitPower+"'.";
                                                SetupActivity.this.currentTransmitPower = transmitPower;
                                               
                                        // Send Message
                                        Message msg = new Message();
                                        msg.obj = message;
                                        SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                                }
                        }                      
                        else if (key.equals("lannetworkpref")) {
                                String lannetwork = sharedPreferences.getString("lannetworkpref", SetupActivity.this.application.DEFAULT_LANNETWORK);
                                if (lannetwork.equals(SetupActivity.this.currentLAN) == false) {
                                        // Restarting
                                                try{
                                                        if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
                                                        // Show RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                                        // Restart Tethering
                                                                //SetupActivity.this.application.restartTether();
                                                        // Dismiss RestartDialog
                                                        SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                        }
                                                }
                                                catch (Exception ex) {
                                                        Log.e(MSG_TAG, "Exception happend while restarting service - Here is what I know: "+ex);
                                                }

                                                message = "LAN-network changed to '"+lannetwork+"'.";
                                                SetupActivity.this.currentLAN = lannetwork;
                                               
                                        // Send Message
                                        Message msg = new Message();
                                        msg.obj = message;
                                        SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
                                }
                        }                      
                        else if (key.equals("bluetoothon")) {
                                Boolean bluetoothOn = sharedPreferences.getBoolean("bluetoothon", false);
                                Message msg = Message.obtain();
                                msg.what = bluetoothOn ? 0 : 1;
                                SetupActivity.this.setWifiPrefsEnableHandler.sendMessage(msg);
                                        try{
                                                if (application.coretask.isNatEnabled() && (application.coretask.isProcessRunning("bin/dnsmasq") || application.coretask.isProcessRunning("bin/pand"))) {
                                                // Show RestartDialog
                                                SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
                                               
                                                // Restart Tethering
                                                //SetupActivity.this.application.restartTether();

                                                // Dismiss RestartDialog
                                                SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
                                                }
                                        }
                                        catch (Exception ex) {
                                        }
                        }
                        else if (key.equals("bluetoothkeepwifi")) {
                                Boolean bluetoothWifi = sharedPreferences.getBoolean("bluetoothkeepwifi", false);
                                if (bluetoothWifi) {
                                        //SetupActivity.this.application.enableWifi();
                                }
                        }
                        }
                }).start();
    }
   
    Handler  setWifiPrefsEnableHandler = new Handler() {
        public void handleMessage(Message msg) {
                        PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
                        wifiGroup.setEnabled(msg.what == 1);
                super.handleMessage(msg);
        }
    };
 
    public String validateSSID(String newSSID){
      String message = "";
      String validChars = "ABCDEFGHIJKLMONPQRSTUVWXYZ" +
      "abcdefghijklmnopqrstuvwxyz" +
      "0123456789_.";
      for (int i = 0 ; i < newSSID.length() ; i++) {
        if (!validChars.contains(newSSID.substring(i, i+1))) {
          message = "SSID contains invalid characters";
        }
      }
      if (newSSID.equals("")) {
        message = "New SSID cannot be empty";
        }
        if (message.length() > 0)
          message += ", not saved.";
        return message;
    }
    public String validateIP(String newIP){
      String message = "";
      String validChars = "." +
      "/" +
      "0123456789_.";
      for (int i = 0 ; i < newIP.length() ; i++) {
        if (!validChars.contains(newIP.substring(i, i+1))) {
          message = "IP contains invalid characters";
        }
      }
      if (newIP.equals("")) {
        message = "New IP address cannot be empty";
        }
        if (message.length() > 0)
          message += ", not saved.";
        return message;
    }
       public String validateNetmask(String newNetmask){
      String message = "";
      String validChars = "." +
      "/" +
      "0123456789_.";
      for (int i = 0 ; i < newNetmask.length() ; i++) {
        if (!validChars.contains(newNetmask.substring(i, i+1))) {
          message = "Netmask contains invalid characters";
        }
      }
      if (newNetmask.equals("")) {
        message = "New netmask address cannot be empty";
        }
        if (message.length() > 0)
          message += ", not saved.";
        return message;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean supRetVal = super.onCreateOptionsMenu(menu);
        SubMenu installBinaries = menu.addSubMenu(0, 0, 0, getString(R.string.setup_activity_reinstall));
        installBinaries.setIcon(drawable.ic_menu_set_as);
        return supRetVal;
    }    
   
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean supRetVal = super.onOptionsItemSelected(menuItem);
        Log.d(MSG_TAG, "Menuitem:getId  -  "+menuItem.getItemId()+" -- "+menuItem.getTitle());
        if (menuItem.getItemId() == 0) {
                this.application.installFiles();
        }
        return supRetVal;
    }
}

