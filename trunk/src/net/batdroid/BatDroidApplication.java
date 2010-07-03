package net.batdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Application;
import android.util.Log;
import android.os.Message;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageInfo;


public class BatDroidApplication extends Application {

  public static final String MSG_TAG = "BATDROID -> BatDroidApplication";

  // StartUp-Check perfomed
  public boolean startupCheckPerformed = false;

  // Devices-Information
  public String deviceType = "unknown"; 
  public String interfaceDriver = "wext";
  
  // Supplicant
  public CoreTask.WpaSupplicant wpasupplicant = null;
  // TiWlan.conf
  public CoreTask.TiWlanConf tiwlan = null;
  // tether.conf
  public CoreTask.TetherConfig tethercfg = null;
        
  // CoreTask
  public CoreTask coretask = null;

  @Override public void onCreate() {
    Log.d(MSG_TAG, "Calling onCreate()");
    
    //create CoreTask
    this.coretask = new CoreTask();
    this.coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
    Log.d(MSG_TAG, "Current directory is "+this.getApplicationContext().getFilesDir().getParent());
    
    // Check Homedir, or create it
    this.checkDirs(); 
        
    // Set device-information
    this.deviceType = Configuration.getDeviceType();
    this.interfaceDriver = Configuration.getWifiInterfaceDriver(this.deviceType);
        
    // Preferences
    // TODO this.settings = PreferenceManager.getDefaultSharedPreferences(this);
                
    // preferenceEditor
    // TODO this.preferenceEditor = settings.edit();
                
    // init wifiManager
    // TODO wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        
        
    // Supplicant config
    this.wpasupplicant = this.coretask.new WpaSupplicant();
        
    // tiwlan.conf
    this.tiwlan = this.coretask.new TiWlanConf();
        
    // tether.cfg
    this.tethercfg = this.coretask.new TetherConfig();
    this.tethercfg.read();

    // Powermanagement
    // TODO powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    // TODO wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TETHER_WAKE_LOCK");

    // init notificationManager
    //this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    //this.notification = new Notification(R.drawable.start_notification, "Wireless Tether", System.currentTimeMillis());
    //this.mainIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
    //this.accessControlIntent = PendingIntent.getActivity(this, 1, new Intent(this, AccessControlActivity.class), 0);
  }


  @Override public void onTerminate() {
    Log.d(MSG_TAG, "Calling onTerminate()");
    // Stopping Tether
    // TODO this.stopTether();
    // Remove all notifications
    // TODO this.notificationManager.cancelAll();
  }

  public boolean binariesExists() {
    // TODO
    File file = new File(this.coretask.DATA_FILE_PATH+"/bin/tether");
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
          // tether
          if (message == null) {
            message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/tether", "0755", R.raw.tether);
          }
          // iptables
          if (message == null) {
            message = BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/bin/iptables", "0755", R.raw.iptables);
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
            BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.edify", "0644", R.raw.tether_edify);
          }
          // tether.cfg
          if (message == null) {
            BatDroidApplication.this.copyFile(BatDroidApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.conf", "0644", R.raw.tether_conf);
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

}


