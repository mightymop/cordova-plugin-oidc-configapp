package de.mopsdom.oidc.configapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class Receiver extends BroadcastReceiver {

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

    Log.e(Receiver.class.getSimpleName(), "onReceive**********************************************");

    if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)||action.equalsIgnoreCase("de.mopsdom.odic.logout")) {

      Log.i("RECEIVER", action);
      if (Utils.getAccount(context) != null) {
        Log.i("RECEIVER", "Remove account, change Notification");
        Utils.removeAccount(context);
        String notiCfg = Utils.getVal(context, Utils.KEY_NOTIFICATION);
        if (notiCfg.equalsIgnoreCase("true") || notiCfg.equalsIgnoreCase("1")) {
          NotificationtentService.showForegroundNotification(context, false, null, null, null);
        } else {
          NotificationtentService.cancelForegroundNotification(context);
        }
      }
    }

    if (action.equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED) ||
      action.equalsIgnoreCase(Intent.ACTION_PACKAGE_DATA_CLEARED) ||
      action.equalsIgnoreCase(Intent.ACTION_PACKAGE_REPLACED)) {
      Log.i("RECEIVER", "ACTION_PACKAGE_ADDED||ACTION_PACKAGE_DATA_CLEARED||ACTION_PACKAGE_REPLACED");
      Uri dataUri = intent.getData();

      if (dataUri != null) {
        Log.i("RECEIVER", "Found datauri");
        String packageName = dataUri.getEncodedSchemeSpecificPart();

        Log.i("RECEIVER", packageName);
        if (packageName.equalsIgnoreCase(context.getPackageName())) {
          Log.i("RECEIVER", packageName + "=own package name");
          if (Utils.getAccount(context) != null) {
            Log.i("RECEIVER", "Remove account, change Notification");
            Utils.removeAccount(context);
            String notiCfg = Utils.getVal(context, Utils.KEY_NOTIFICATION);
            if (notiCfg.equalsIgnoreCase("true") || notiCfg.equalsIgnoreCase("1")) {
              NotificationtentService.showForegroundNotification(context, false, null, null, null);
            } else {
              NotificationtentService.cancelForegroundNotification(context);
            }
          }
        }
      }
    }
  }


}
