package de.mopsdom.oidc.configapp;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;


import de.berlin.polizei.oidcsso.BuildConfig;

public class oidcconfigapp extends CordovaPlugin {

  private static final String TAG = "oidcconfigapp";
  private static final Logger logger = LoggerFactory.getLogger(oidcconfigapp.class);

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    try {

      logger.info("Log4j-Konfiguration wurde geladen.");
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
