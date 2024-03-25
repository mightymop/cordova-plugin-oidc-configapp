package de.mopsdom.oidc.configapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import de.berlin.polizei.oidcsso.BuildConfig;
import de.berlin.polizei.oidcsso.R;

public class oidcconfigapp extends CordovaPlugin {

  private static final String TAG = "oidcconfigapp";
  public static final String LOGBACK_CONFIG_FILE = "logback.xml";
  private static  Logger logger ;

  public static File copyFileFromResourcesToExternalStorage(Context context, String destinationDir, String destinationFileName) {

    try {
      InputStream inputStream = context.getAssets().open(LOGBACK_CONFIG_FILE);
      File dir = new File(context.getExternalFilesDir(null), destinationDir);
      if (!dir.exists()) {
        if (!dir.mkdirs()) {
          Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
          return null;
        }
      }
      File destFile = new File(dir, destinationFileName);
      FileOutputStream outputStream = new FileOutputStream(destFile);
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, length);
      }
      outputStream.close();
      inputStream.close();
      return destFile;
    } catch (IOException e) {
      Log.e("LogbackConfigUtils", "Failed to copy logback.xml to internal storage", e);
      return null;
    }
  }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    File logBackFile = new File("/sdcard/Android/data/de.berlin.polizei.oidcsso/files/logs/logback.xml");
    if (!logBackFile.exists())
    {
      logBackFile = copyFileFromResourcesToExternalStorage(cordova.getActivity(), "logs","logback.xml");
    }

    try {
      if (logBackFile != null) {
        logger = Utils.initLogger()!=null?Utils.initLogger():LoggerFactory.getLogger(oidcconfigapp.class);
        logger.info("Logback-Konfiguration wurde geladen.");

        LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        StatusManager statusManager = loggerContext.getStatusManager();
        if (statusManager != null) {
          for (Status status : statusManager.getCopyOfStatusList()) {
            logger.info(status.toString());
          }
        }

      } else {
        logger.error("Logback Konfiguration nicht geladen!");
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, e.getMessage(),e);
    }
  }

  @Override
  public boolean execute(@NonNull final String action, final JSONArray data, final CallbackContext callbackContext) {

    switch (action) {

      case "version":
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, BuildConfig.VERSION_NAME));
        break;

      default:
        return false;
    }

    return true;
  }
}
