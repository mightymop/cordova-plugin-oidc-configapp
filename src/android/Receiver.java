package de.mopsdom.oidc.configapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Receiver extends BroadcastReceiver {

  private static final Logger logger = Utils.initLogger()!=null?Utils.initLogger():LoggerFactory.getLogger(Receiver.class);

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

    logger.trace("onReceive**********************************************");
    logger.debug("ACTION="+action);

    if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)||action.equalsIgnoreCase("de.mopsdom.odic.logout")) {

      if (Utils.getAccount(context) != null) {
        logger.info("RECEIVER", "Remove account, change Notification");
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
      logger.debug("RECEIVER", "ACTION_PACKAGE_ADDED||ACTION_PACKAGE_DATA_CLEARED||ACTION_PACKAGE_REPLACED");
      Uri dataUri = intent.getData();

      if (dataUri != null) {
        logger.info("RECEIVER", "Found datauri");
        String packageName = dataUri.getEncodedSchemeSpecificPart();

        logger.info("RECEIVER", packageName);
        if (packageName.equalsIgnoreCase(context.getPackageName())) {
          logger.info("RECEIVER", packageName + "=own package name");
          if (Utils.getAccount(context) != null) {
            logger.info("RECEIVER", "Remove account, change Notification");
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
      else
      {
        logger.info("no datauri found!");
      }
    }
  }


}
