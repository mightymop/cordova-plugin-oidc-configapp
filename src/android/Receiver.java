package de.mopsdom.oidc.configapp;

import static android.content.Context.MODE_PRIVATE;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import de.mopsdom.oidc.cordova.Utils;

public class Receiver extends BroadcastReceiver {

  public final static String KEY_NOTIFICATION = "NOTIFICATION";
  public final static String KEY_ISSUER = "ISSUER";
  public final static String KEY_CLIENT_ID = "CLIENT_ID";
  public final static String KEY_REDIRECT_URI = "REDIRECT_URI";
  public final static String KEY_LOGOUT_REDIRECT_URI = "LOGOUT_REDIRECT_URI";
  public final static String KEY_CONFIG = "CONFIG";
  public final static String KEY_USERCLAIM = "USERCLAIM";
  public final static String KEY_ACCOUNTTYPE = "ACCOUNTTYPE";
  public final static String KEY_SCOPE = "SCOPE";


  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

    Log.e("RECEIVER", "onReceive**********************************************");

    if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {

      Log.i("RECEIVER", "ACTION_BOOT_COMPLETED");
      if (getAccount(context) != null) {
        Log.i("RECEIVER", "Remove account, change Notification");
        removeAccount(context);
        String notiCfg = getVal(context, KEY_NOTIFICATION);
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
          if (getAccount(context) != null) {
            Log.i("RECEIVER", "Remove account, change Notification");
            removeAccount(context);
            String notiCfg = getVal(context, KEY_NOTIFICATION);
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

  public void removeAccount(Context context) {
    Account account = getAccount(context);
    if (account != null) {
      AccountManager accountManager = getAccountManager(context);
      accountManager.removeAccountExplicitly(account);
    }
  }

  private AccountManager getAccountManager(Context context) {
    return context.getSystemService(AccountManager.class);
  }

  public String getAccountType(Context context) {
    String accounttype = Utils.getVal(context, Utils.KEY_ACCOUNTTYPE);
    if (accounttype == null) {
      accounttype = getStringRessource(context, "account_type");
      Utils.setVal(context, Utils.KEY_ACCOUNTTYPE, accounttype);
    }
    return accounttype;
  }

  public Account getAccount(Context context) {
    Account[] result = getAccountManager(context).getAccountsByType(getAccountType(context));
    if (result != null && result.length > 0) {
      return result[0];
    }
    return null;
  }

  public String getVal(Context context, String key) {
    String jsonconfig = readConfig(context);
    if (jsonconfig != null) {
      try {
        JSONObject config = new JSONObject(jsonconfig);
        return config.has(key.toLowerCase()) ? config.getString(key.toLowerCase()) : null;
      } catch (Exception e) {
        Log.e(Receiver.class.getSimpleName(), e.getMessage(), e);
        return null;
      }
    }

    return null;
  }

  public String readConfig(Context context) {
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    String config = authPrefs.getString("config", null);

    if (config == null) {
      try {
        JSONObject jconf = new JSONObject();

        jconf.put(KEY_CLIENT_ID.toLowerCase(), getStringRessource(context, "default_client_id"));
        jconf.put(KEY_REDIRECT_URI.toLowerCase(), getStringRessource(context, "default_redirect_uri"));
        jconf.put(KEY_SCOPE.toLowerCase(), getStringRessource(context, "default_scope"));
        jconf.put(KEY_USERCLAIM.toLowerCase(), getStringRessource(context, "default_userclaim"));
        jconf.put(KEY_ISSUER.toLowerCase(), getStringRessource(context, "default_issuer"));
        jconf.put(KEY_LOGOUT_REDIRECT_URI.toLowerCase(), getStringRessource(context, "default_redirect_uri_logout"));
        jconf.put(KEY_ACCOUNTTYPE.toLowerCase(), getStringRessource(context, "account_type"));
        jconf.put(KEY_NOTIFICATION.toLowerCase(), "true");

        writeConfig(context, jconf.toString());
      } catch (Exception e) {
        Log.e(Receiver.class.getSimpleName(), e.getMessage(), e);
      }

      config = authPrefs.getString("config", null);
    }

    return config;
  }

  public void writeConfig(Context context, String json) {
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("config", json).apply();
  }

  public String getStringRessource(Context context, String resourceName) {
    int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());

    if (resourceId != 0) {
      return context.getResources().getString(resourceId);
    } else {
      return null;
    }
  }
}
