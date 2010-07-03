package net.batdroid;

import android.app.Application;

public class BatDroidApplication extends Application {
  // StartUp-Check perfomed
  public boolean startupCheckPerformed = false;

  public boolean binariesExists() {
    // TODO
    //File file = new File(this.coretask.DATA_FILE_PATH+"/bin/tether");
    //return file.exists();
    return true;
  }

  public void installFiles() {
  }

}


