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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BatDroidApplication extends Application {

	public static final String MSG_TAG = "BATDROID -> BatDroidApplication";
	
	public final String DEFAULT_PASSPHRASE = "abcdefghijklm";
	public final String DEFAULT_LANNETWORK = "10.0.0.0/8";
	public final String DEFAULT_ENCSETUP   = "wpa_supplicant";
	
	// Devices-Information
	public String deviceType = "unknown"; 
	public String interfaceDriver = "wext"; 
	
	// StartUp-Check perfomed
	public boolean startupCheckPerformed = false;
	
	// Client-Connect-Thread
	private Thread clientConnectThread = null;
	private static final int CLIENT_CONNECT_ACDISABLED = 0;
	private static final int CLIENT_CONNECT_AUTHORIZED = 1;
	private static final int CLIENT_CONNECT_NOTAUTHORIZED = 2;
	
	// Data counters
	private Thread trafficCounterThread = null;

	// WifiManager
	private WifiManager wifiManager;
	//public String adhocNetworkDevice = null;
	
	// PowerManagement
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

	// DNS-Server-Update Thread
	private Thread dnsUpdateThread = null;	
	
	// Preferences
	public SharedPreferences settings = null;
	public SharedPreferences.Editor preferenceEditor = null;
	
    // Notification
	public NotificationManager notificationManager;
	private Notification notification;
	private int clientNotificationCount = 0;
	
	// Intents
	private PendingIntent mainIntent;
	private PendingIntent accessControlIntent;
    
	// Original States
	private static boolean origWifiState = false;
	
	// Supplicant
	public CoreTask.WpaSupplicant wpasupplicant = null;
	// TiWlan.conf
	public CoreTask.TiWlanConf tiwlan = null;
	// adhoc.conf
	public CoreTask.AdHocConfig adhoccfg = null;
	
	// CoreTask
	public CoreTask coretask = null;
	
	// WebserviceTask
	// TODO - needed ? public WebserviceTask webserviceTask = null;
	
	// Update Url
	private static final String APPLICATION_PROPERTIES_URL = "http://android-batdroid.googlecode.com/svn/download/update/all/unstable/application.properties";
	private static final String APPLICATION_DOWNLOAD_URL = "http://android-batdroid.googlecode.com/files/";
	
	
	@Override
	public void onCreate() {
		Log.d(MSG_TAG, "Calling onCreate()");
		
		//create CoreTask
		this.coretask = new CoreTask();
		this.coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
		Log.d(MSG_TAG, "Current directory is "+this.getApplicationContext().getFilesDir().getParent());

		// TODO - updates create WebserviceTask
		//this.webserviceTask = new WebserviceTask();
		
        // Check Homedir, or create it
        this.checkDirs(); 
        
        // Set device-information
        this.deviceType = Configuration.getDeviceType();
        this.interfaceDriver = Configuration.getWifiInterfaceDriver(this.deviceType);
        
        // Preferences
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // preferenceEditor
        this.preferenceEditor = settings.edit();
		
        // init wifiManager
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        
        // Supplicant config
        this.wpasupplicant = this.coretask.new WpaSupplicant();
        
        // tiwlan.conf
        this.tiwlan = this.coretask.new TiWlanConf();
        
        // adhoc.cfg
        this.adhoccfg = this.coretask.new AdHocConfig();
        this.adhoccfg.read();

        // Powermanagement
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TETHER_WAKE_LOCK");

        // init notificationManager
        this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    	this.notification = new Notification(R.drawable.start_notification, "BatDroid", System.currentTimeMillis());
    	this.mainIntent = PendingIntent.getActivity(this, 0, new Intent(this, batdroid.class), 0);

	}

	@Override
	public void onTerminate() {
		Log.d(MSG_TAG, "Calling onTerminate()");
    	// Stopping BatDroid
		this.stopBatDroid();
		// Remove all notifications
		this.notificationManager.cancelAll();
	}
	
	
	public void updateConfiguration() {
		
		long startStamp = System.currentTimeMillis();
		
		boolean encEnabled = this.settings.getBoolean("encpref", false);
		boolean acEnabled = this.settings.getBoolean("acpref", false);
		String ssid = this.settings.getString("ssidpref", "batman");
    String txpower = this.settings.getString("txpowerpref", "disabled");
    String lannetwork = this.settings.getString("lannetworkpref", DEFAULT_LANNETWORK);
    String wepkey = this.settings.getString("passphrasepref", DEFAULT_PASSPHRASE);
    String wepsetupMethod = this.settings.getString("encsetuppref", DEFAULT_ENCSETUP);
        
		// adhoc.conf
        String subnet = lannetwork.substring(0, lannetwork.lastIndexOf("."));
        this.adhoccfg.read();
		this.adhoccfg.put("device.type", deviceType);
        this.adhoccfg.put("wifi.essid", ssid);
		this.adhoccfg.put("ip.network", lannetwork.split("/")[0]);
		this.adhoccfg.put("ip.gateway", subnet + ".254");    
		
		// Checking if rp_filter is active
		if (this.coretask.isRPFilterEnabled()) {
			this.adhoccfg.put("system.rp_filter", "1");
		}	else {
			this.adhoccfg.put("system.rp_filter", "0");
		}
		
		/**
		 * TODO: Quick and ugly workaround for nexus
		 */
		if (Configuration.getWifiInterfaceDriver(this.deviceType).equals(Configuration.DRIVER_SOFTAP_GOG)) {
			this.adhoccfg.put("wifi.interface", "wl0.1");
		} else {
			this.adhoccfg.put("wifi.interface", this.coretask.getProp("wifi.interface"));
		}

		this.adhoccfg.put("wifi.txpower", txpower);

    // Make sure to remove wpa_supplicant.conf
    if (this.wpasupplicant.exists()) {
      this.wpasupplicant.remove();
    }			

    // install wpa_supplicant
    this.installWpaSupplicantConfig();
    this.adhoccfg.put("wifi.encryption", "open");
    this.adhoccfg.put("wifi.encryption.key", "none");
		
		// determine driver wpa_supplicant
		this.adhoccfg.put("wifi.driver", Configuration.getWifiInterfaceDriver(deviceType));
		
		// writing config-file
		if (this.adhoccfg.write() == false) {
			Log.e(MSG_TAG, "Unable to update adhoc.conf!");
		}
		
		/*
		 * TODO
		 * Need to find a better method to identify if the used device is a
		 * HTC Dream aka T-Mobile G1
		 */
		if (deviceType.equals(Configuration.DEVICE_DREAM)) {
			Hashtable<String,String> values = new Hashtable<String,String>();
			values.put("dot11DesiredSSID", this.settings.getString("ssidpref", "batman"));
			values.put("dot11DesiredChannel", this.settings.getString("channelpref", "5"));
			this.tiwlan.write(values);
		}
		
		Log.d(MSG_TAG, "Creation of configuration-files took ==> "+(System.currentTimeMillis()-startStamp)+" milliseconds.");
	}
	
	// Start/Stop BatDroid
    public boolean startBatDroid() {

      // Updating all configs
      this.updateConfiguration();
     
      
	this.disableWifi();
      
      // Update resolv.conf-file
      String dns[] = this.coretask.updateResolvConf();     
      
    	// Starting service


    	if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/adhoc start 1")) {
        
        //this.clientConnectEnable(true);
    		this.trafficCounterEnable(true);
    		this.dnsUpdateEnable(dns, true);
        
        // Acquire Wakelock
        this.acquireWakeLock();
        
    		return true;
    	}
    	return false;
    }
    
  public boolean stopBatDroid() {
		// Diaabling polling-threads
    this.trafficCounterEnable(false);
    this.dnsUpdateEnable(false);
		//this.clientConnectEnable(false);    
    this.releaseWakeLock();
    boolean stopped = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/adhoc stop 1");
		this.notificationManager.cancelAll();
    this.enableWifi();
		return stopped;
  }
	
  public boolean restartBatDroid() {
    boolean status = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/adhoc stop 1");
		this.notificationManager.cancelAll();
    this.trafficCounterEnable(false);

    // Updating all configs
    this.updateConfiguration();       
    
    this.disableWifi();
    
    // Starting service
    if (status == true)
      status = this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH+"/bin/adhoc start 1");
        
    this.showStartNotification();
    this.trafficCounterEnable(true);
    
    return status;
  }
    
    public String getAdhocNetworkDevice() {
			/**
			 * TODO: Quick and ugly workaround for nexus
			 */
			if (Configuration.getWifiInterfaceDriver(this.deviceType).equals(Configuration.DRIVER_SOFTAP_GOG)) {
				return "wl0.1";
			}
			else {
				return this.coretask.getProp("wifi.interface");
			}

    }
    
    // gets user preference on whether wakelock should be disabled while batmand is running
    public boolean isWakeLockDisabled(){
		return this.settings.getBoolean("wakelockpref", true);
	} 
	
    // gets user preference on whether sync should be disabled while batmand is running
    public boolean isSyncDisabled(){
		return this.settings.getBoolean("syncpref", false);
	}
    
    // gets user preference on whether sync should be disabled while batmand is running
    public boolean isUpdatecDisabled(){
		return this.settings.getBoolean("updatepref", false);
	}
    
    // get preferences on whether donate-dialog should be displayed
    public boolean showDonationDialog() {
    	return this.settings.getBoolean("donatepref", true);
    }

    // Wifi
    public void disableWifi() {
    	if (this.wifiManager.isWifiEnabled()) {
    		origWifiState = true;
    		this.wifiManager.setWifiEnabled(false);
    		Log.d(MSG_TAG, "Wifi disabled!");
        	// Waiting for interface-shutdown
    		try {
    			Thread.sleep(5000);
    		} catch (InterruptedException e) {
    			// nothing
    		}
    	}
    }
    
    public void enableWifi() {
    	if (origWifiState) {
        	// Waiting for interface-restart
    		this.wifiManager.setWifiEnabled(true);
    		try {
    			Thread.sleep(5000);
    		} catch (InterruptedException e) {
    			// nothing
    		}
    		Log.d(MSG_TAG, "Wifi started!");
    	}
    }
    
    // WakeLock
	public void releaseWakeLock() {
		try {
			if(this.wakeLock != null && this.wakeLock.isHeld()) {
				Log.d(MSG_TAG, "Trying to release WakeLock NOW!");
				this.wakeLock.release();
			}
		} catch (Exception ex) {
			Log.d(MSG_TAG, "Ups ... an exception happend while trying to release WakeLock - Here is what I know: "+ex.getMessage());
		}
	}
    
	public void acquireWakeLock() {
		try {
			if (this.isWakeLockDisabled() == false) {
				Log.d(MSG_TAG, "Trying to acquire WakeLock NOW!");
				this.wakeLock.acquire();
			}
		} catch (Exception ex) {
			Log.d(MSG_TAG, "Ups ... an exception happend while trying to acquire WakeLock - Here is what I know: "+ex.getMessage());
		}
	}
    
    public int getNotificationType() {
		return Integer.parseInt(this.settings.getString("notificationpref", "2"));
    }
    
    // Notification
    public void showStartNotification() {
		notification.flags = Notification.FLAG_ONGOING_EVENT;
    	notification.setLatestEventInfo(this, "BatDroid", "B.A.T.M.A.N. is currently running ...", this.mainIntent);
    	this.notificationManager.notify(-1, this.notification);
    }
    
    
    public boolean binariesExists() {
    	File file = new File(this.coretask.DATA_FILE_PATH+"/bin/adhoc");
    	return file.exists();
    }
    
    public void installWpaSupplicantConfig() {
    	this.copyFile(this.coretask.DATA_FILE_PATH+"/conf/wpa_supplicant.conf", "0644", R.raw.wpa_supplicant_conf);
    }
    
    Handler displayMessageHandler = new Handler(){
        public void handleMessage(Message msg) {
       		if (msg.obj != null) {
       			BatDroidApplication.this.displayToastMessage((String)msg.obj);
       		}
        	super.handleMessage(msg);
        }
    };

    public void installFiles() {
    	new Thread(new Runnable(){
			public void run(){
				String message = null;
			
			// adhoc
		    	if (message == null) {
			    	message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/adhoc", "0755", R.raw.adhoc);
		    	}
		    	// iptables
		    	if (message == null) {
			    	message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/iptables", "0755", R.raw.iptables);
		    	}

			//Batmand binarie,  "batmand-rv1543_armv6l" renamed to "batmand"
			 if (message == null) {
            			message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/batmand", "0755", 	R.raw.batmand);
          }
		    	// ifconfig
		    	if (message == null) {
			    	message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/ifconfig", "0755", R.raw.ifconfig);
		    	}	
		    	// iwconfig
		    	if (message == null) {
			    	message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/iwconfig", "0755", R.raw.iwconfig);
		    	}
		    	// ultra_bcm_config
		    	if (message == null) {
			    	message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/ultra_bcm_config", "0755", R.raw.ultra_bcm_config);
		    	}
				
				/**
				 * Installing fix-scripts if needed
				 */
				if (BatDroidApplication.this.deviceType.equals(Configuration.DEVICE_DROID) 
						|| BatDroidApplication.this.deviceType.equals(Configuration.DEVICE_LEGEND)) {
					// fixpersist.sh
					if (message == null) {
						message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/fixpersist.sh", "0755", R.raw.fixpersist_sh);
					}				
				}
				if (BatDroidApplication.this.deviceType.equals(Configuration.DEVICE_DREAM) 
						|| BatDroidApplication.this.deviceType.equals(Configuration.DEVICE_LEGEND)) {				
					// fixroute.sh
					if (message == null) {
						message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/fixroute.sh", "0755", R.raw.fixroute_sh);
					}
				}
				
        // tiwlan.ini
				if (message == null) {
					BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/conf/tiwlan.ini", "0644", R.raw.tiwlan_ini);
				}
				// edify script
				if (message == null) {
					BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/conf/adhoc.edify", "0644", R.raw.adhoc_edify);
				}
				// adhoc.cfg
				if (message == null) {
					BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/conf/adhoc.conf", "0644", R.raw.adhoc_conf);
				}
				
				// wpa_supplicant drops privileges, we need to make files readable.
				BatDroidApplication.this.coretask.chmod(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/conf/", "0755");

        if (message == null) {
          message = "Binaries and config-files installed!";
        }
                                
        // Sending message
        Message msg = new Message();
        msg.obj = message;
        BatDroidApplication.this.displayMessageHandler.sendMessage(msg);
      }
        }).start();
    }


    /*
     * Update checking. We go to a predefined URL and fetch a properties style file containing
     * information on the update. These properties are:
     * 
     * versionCode: An integer, version of the new update, as defined in the manifest. Nothing will
     *              happen unless the update properties version is higher than currently installed.
     * fileName: A string, URL of new update apk. If not supplied then download buttons
     *           will not be shown, but instead just a message and an OK button.
     * message: A string. A yellow-highlighted message to show to the user. Eg for important
     *          info on the update. Optional.
     * title: A string, title of the update dialog. Defaults to "Update available".
     * 
     * Only "versionCode" is mandatory.
     */
    public void checkForUpdate() {
      /* TODO - updates 
    	if (this.isUpdatecDisabled()) {
    		Log.d(MSG_TAG, "Update-checks are disabled!");	
    		return;
    	}
    	new Thread(new Runnable(){
			public void run(){
				Looper.prepare();
				// Getting Properties
				Properties updateProperties = BatDroidApplication.this.webserviceTask.queryForProperty(APPLICATION_PROPERTIES_URL);
				if (updateProperties != null && updateProperties.containsKey("versionCode")) {
				  
					int availableVersion = Integer.parseInt(updateProperties.getProperty("versionCode"));
					int installedVersion = BatDroidApplication.this.getVersionNumber();
					String fileName = updateProperties.getProperty("fileName", "");
					String updateMessage = updateProperties.getProperty("message", "");
					String updateTitle = updateProperties.getProperty("title", "Update available");
					if (availableVersion != installedVersion) {
						Log.d(MSG_TAG, "Installed version '"+installedVersion+"' and available version '"+availableVersion+"' do not match!");
						MainActivity.currentInstance.openUpdateDialog(APPLICATION_DOWNLOAD_URL+fileName,
						    fileName, updateMessage, updateTitle);
					}
				}
				Looper.loop();
			}
    	}).start();
      */
    }
   
    public void downloadUpdate(final String downloadFileUrl, final String fileName) {
      /* TODO - updates
    	new Thread(new Runnable(){
			public void run(){
				Message msg = Message.obtain();
            	msg.what = MainActivity.MESSAGE_DOWNLOAD_STARTING;
            	msg.obj = "Downloading update...";
            	MainActivity.currentInstance.viewUpdateHandler.sendMessage(msg);
				BatDroidApplication.this.webserviceTask.downloadUpdateFile(downloadFileUrl, fileName);
				Intent intent = new Intent(Intent.ACTION_VIEW); 
			    intent.setDataAndType(android.net.Uri.fromFile(new File(WebserviceTask.DOWNLOAD_FILEPATH+"/"+fileName)),"application/vnd.android.package-archive"); 
			    MainActivity.currentInstance.startActivity(intent);
			}
    	}).start();*/
    }
    
    private String copyFile(String filename, String permission, int ressource) {
    	String result = this.copyFile(filename, ressource);
    	if (result != null) {
    		return result;
    	}
    	if (this.coretask.chmod(filename, permission) != true) {
    		result = "Can't change file-permission for '"+filename+"'!";
    	}
    	return result;
    }
    
    private String copyFile(String filename, int ressource) {
    	File outFile = new File(filename);
    	Log.d(MSG_TAG, "Copying file '"+filename+"' ...");
    	InputStream is = this.getResources().openRawResource(ressource);
    	byte buf[] = new byte[1024];
        int len;
        try {
        	OutputStream out = new FileOutputStream(outFile);
        	while((len = is.read(buf))>0) {
				out.write(buf,0,len);
			}
        	out.close();
        	is.close();
		} catch (IOException e) {
			return "Couldn't install file - "+filename+"!";
		}
		return null;
    }
    
    private void checkDirs() {
    	File dir = new File(this.coretask.DATA_FILE_PATH);
    	if (dir.exists() == false) {
    			this.displayToastMessage("Application data-dir does not exist!");
    	}
    	else {
    		//String[] dirs = { "/bin", "/var", "/conf", "/library" };
    		String[] dirs = { "/bin", "/var", "/conf" };
    		for (String dirname : dirs) {
    			dir = new File(this.coretask.DATA_FILE_PATH + dirname);
    	    	if (dir.exists() == false) {
    	    		if (!dir.mkdir()) {
    	    			this.displayToastMessage("Couldn't create " + dirname + " directory!");
    	    		}
    	    	}
    	    	else {
    	    		Log.d(MSG_TAG, "Directory '"+dir.getAbsolutePath()+"' already exists!");
    	    	}
    		}
    	}
    }
    
    
    // Display Toast-Message
	public void displayToastMessage(String message) {
		LayoutInflater li = LayoutInflater.from(this);
		View layout = li.inflate(R.layout.toastview, null);
		TextView text = (TextView) layout.findViewById(R.id.toastText);
		text.setText(message);
		Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}
    
    public int getVersionNumber() {
    	int version = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionCode;
        } catch (Exception e) {
            Log.e(MSG_TAG, "Package name not found", e);
        }
        return version;
    }
    
    public String getVersionName() {
    	String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (Exception e) {
            Log.e(MSG_TAG, "Package name not found", e);
        }
        return version;
    }

    /*
     * This method checks if changing the transmit-power is supported
     */
    public boolean isTransmitPowerSupported() {
    	// Only supported for the nexusone 
    	if (this.deviceType.equals(Configuration.DEVICE_NEXUSONE) 
    			&& this.interfaceDriver.startsWith("softap") == false) {
    		return true;
    	}
    	return false;
    }    
    
    

 
    public void dnsUpdateEnable(boolean enable) {
    	this.dnsUpdateEnable(null, enable);
    }
    
   	public void dnsUpdateEnable(String[] dns, boolean enable) {
   		if (enable == true) {
			if (this.dnsUpdateThread == null || this.dnsUpdateThread.isAlive() == false) {
				this.dnsUpdateThread = new Thread(new DnsUpdate(dns));
				this.dnsUpdateThread.start();
			}
   		} else {
	    	if (this.dnsUpdateThread != null)
	    		this.dnsUpdateThread.interrupt();
   		}
   	}
       
    class DnsUpdate implements Runnable {

    	String[] dns;
    	
    	public DnsUpdate(String[] dns) {
    		this.dns = dns;
    	}
    	
		public void run() {
            while (!Thread.currentThread().isInterrupted()) {
            	String[] currentDns = BatDroidApplication.this.coretask.getCurrentDns();
            	if (this.dns == null || this.dns[0].equals(currentDns[0]) == false || this.dns[1].equals(currentDns[1]) == false) {
            		this.dns = BatDroidApplication.this.coretask.updateResolvConf();
            	}
                // Taking a nap
       			try {
    				Thread.sleep(10000);
    			} catch (InterruptedException e) {
    				Thread.currentThread().interrupt();
    			}
            }
		}
    }    
    
   	public void trafficCounterEnable(boolean enable) {
      /* TODO - traffic counter
   		if (enable == true) {
			if (this.trafficCounterThread == null || this.trafficCounterThread.isAlive() == false) {
				this.trafficCounterThread = new Thread(new TrafficCounter());
				this.trafficCounterThread.start();
			}
   		} else {
	    	if (this.trafficCounterThread != null)
	    		this.trafficCounterThread.interrupt();
   		}
      */
   	}
   	
  /* TODO - TrafficCounter
   	class TrafficCounter implements Runnable {
   		private static final int INTERVAL = 2;  // Sample rate in seconds.
   		long previousDownload;
   		long previousUpload;
   		long lastTimeChecked;
   		public void run() {
   			this.previousDownload = this.previousUpload = 0;
   			this.lastTimeChecked = new Date().getTime();

   			String adhocNetworkDevice = BatDroidApplication.this.getAdHocNetworkDevice();
   			
   			while (!Thread.currentThread().isInterrupted()) {
		        // Check data count
		        long [] trafficCount = BatDroidApplication.this.coretask.getDataTraffic(adhocNetworkDevice);
		        long currentTime = new Date().getTime();
		        float elapsedTime = (float) ((currentTime - this.lastTimeChecked) / 1000);
		        this.lastTimeChecked = currentTime;
		        DataCount datacount = new DataCount();
		        datacount.totalUpload = trafficCount[0];
		        datacount.totalDownload = trafficCount[1];
		        datacount.uploadRate = (long) ((datacount.totalUpload - this.previousUpload)*8/elapsedTime);
		        datacount.downloadRate = (long) ((datacount.totalDownload - this.previousDownload)*8/elapsedTime);
				Message message = Message.obtain();
				message.what = batdroid.MESSAGE_TRAFFIC_COUNT;
				message.obj = datacount;
				batdroid.currentInstance.viewUpdateHandler.sendMessage(message); 
				this.previousUpload = datacount.totalUpload;
				this.previousDownload = datacount.totalDownload;
                try {
                    Thread.sleep(INTERVAL * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
   			}
			Message message = Message.obtain();
			message.what = batdroid.MESSAGE_TRAFFIC_END;
			batdroid.currentInstance.viewUpdateHandler.sendMessage(message); 
   		}
   	}
  */
   	
   	public class DataCount {
   		// Total data uploaded
   		public long totalUpload;
   		// Total data downloaded
   		public long totalDownload;
   		// Current upload rate
   		public long uploadRate;
   		// Current download rate
   		public long downloadRate;
   	}
}
