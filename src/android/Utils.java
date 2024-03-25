package de.mopsdom.oidc.configapp;

import static android.content.Context.MODE_PRIVATE;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Utils {

  private static final Logger logger = initLogger()!=null?initLogger():LoggerFactory.getLogger(Utils.class);
  public final static String KEY_ISSUER = "ISSUER";
  public final static String KEY_CLIENT_ID = "CLIENT_ID";
  public final static String KEY_REDIRECT_URI = "REDIRECT_URI";
  public final static String KEY_LOGOUT_REDIRECT_URI = "LOGOUT_REDIRECT_URI";
  public final static String KEY_USERCLAIM = "USERCLAIM";
  public final static String KEY_ACCOUNTTYPE = "ACCOUNTTYPE";
  public final static String KEY_SCOPE = "SCOPE";
  public final static String KEY_NOTIFICATION = "NOTIFICATION";

  public final static String TAG = Utils.class.getSimpleName();

  public static void writeOIDCConfig(Context context, String json) {
    logger.trace("writeOIDCConfig");
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("oidc", json).apply();
  }

  public static String readOIDCConfig(Context context) {
    logger.trace("readOIDCConfig");
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    return authPrefs.getString("oidc", null);
  }

  public static void writeConfig(Context context, String json) {
    logger.trace("writeConfig");
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("config", json).apply();
  }

  public static String readConfig(Context context) {
    logger.trace("readConfig");
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

        logger.debug("Schreibe config...");
        writeConfig(context, jconf.toString());
      } catch (Exception e) {
        logger.error( e.getMessage(), e);
      }

      config = authPrefs.getString("config", null);
    }

    return config;
  }

  public static String getVal(Context context, String key) {
    logger.trace("getVal");
    String jsonconfig = readConfig(context);
    if (jsonconfig != null) {
      try {
        JSONObject config = new JSONObject(jsonconfig);
        String result = config.has(key.toLowerCase()) ? config.getString(key.toLowerCase()) : null;
        logger.debug("result="+result);
        return result;
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        return null;
      }
    }

    return null;
  }

  public static String readData(Context context, String key) {
    logger.trace("readData");
    AccountManager accountManager = getAccountManager(context);
    Account account = getAccount(context);
    if (account != null) {
      logger.debug("Account found");
      return accountManager.getUserData(account, key);
    }
    return null;
  }

  public static void setVal(Context context, String key, String val) {
    logger.trace("setVal");
    String jsonconfig = readConfig(context);

    JSONObject config;
    if (jsonconfig == null) {
      config = new JSONObject();
    } else {
      try {
        config = new JSONObject(jsonconfig);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        config = new JSONObject();
      }
    }

    try {
      config.put(key.toLowerCase(), val);
    } catch (JSONException e) {
      logger.error(e.getMessage(), e);
    }

    writeConfig(context, config.toString());
  }

  public static int getIdentifier(Context context, String kategorie, String name) {
    logger.trace("getIdentifier");
    int resourceId = 0;

    try {
      resourceId = context.getResources().getIdentifier(name, kategorie, context.getPackageName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return resourceId;
  }

  public static String getStringRessource(Context context, String resourceName) {
    logger.trace("getStringRessource");
    int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());

    if (resourceId != 0) {
      return context.getResources().getString(resourceId);
    } else {
      return null;
    }
  }

  public static boolean createAccount(Context context, String username, String state) {
    return createAccount(context,username,state,true);
  }
  public static boolean createAccount(Context context, String username, String state, boolean alarm) {
    logger.trace("createAccount");
    Account account = getAccount(context);

    if (account != null) {
      return false;
    }

    account = new Account(username, getAccountType(context));
    AccountManager accountManager = getAccountManager(context);
    accountManager.addAccountExplicitly(account, null, null);
    writeData(context, "state", state);
    if (alarm) {
      try {
        JSONObject jstate = new JSONObject(state);
        if (jstate.has("refresh_token_expires_in")) {
          createAlarm(context, jstate.getString("id_token"), jstate.getInt("refresh_token_expires_in"));
        }
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }

    return true;
  }

  private static PendingIntent createAlarmIntent(Context context, String id_token) {
    logger.trace("createAlarmIntent");
    Intent alarmIntent = new Intent(context, Receiver.class); // Ersetze YourReceiver durch den Namen deines Broadcast Receivers

    String issuer = (String) getClaimFromToken(id_token,"iss");
    alarmIntent.putExtra("issuer", issuer);
    alarmIntent.setAction("de.mopsdom.odic.logout");
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmIntent.hashCode(), alarmIntent, PendingIntent.FLAG_IMMUTABLE);
    return pendingIntent;
  }

  private static void createAlarm(Context context, String id_token, int refresh_token_expires_in) {
    logger.trace("createAlarm");
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    PendingIntent pendingIntent = createAlarmIntent(context, id_token);

    long iat = (Integer) getClaimFromToken(id_token, "iat");
    long alarmTime = iat + refresh_token_expires_in - 60;
    long timeInMillis = alarmTime * 1000;
    logger.info("SET ALARM TIME="+String.valueOf(timeInMillis)+" "+(new Date(timeInMillis)).toString()+" ID_TOKEN_CHECKSUM="+id_token.split("\\.")[2]);
    // Zeitpunkt festlegen, zu dem die Aktion ausgeführt werden soll
    alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
  }

  private static void cancelAlarm(Context context, String id_token) {
    logger.trace("cancelAlarm");
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    PendingIntent pendingIntent = createAlarmIntent(context, id_token);
    logger.info("CANCEL ALARM ID_TOKEN_CHECKSUM="+id_token.split("\\.")[2]);
    // Alarm abbrechen
    alarmManager.cancel(pendingIntent);
  }

  public static Logger initLogger()
  {
    File logBackFile = new File("/sdcard/Android/data/de.berlin.polizei.oidcsso/files/logs/logback.xml");

    if (logBackFile != null) {
      System.setProperty("logback.configurationFile", logBackFile.getAbsolutePath());
      return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
    return null;
  }

  public static Object getClaimFromToken(String id_token, String claim) {
    logger.trace("getClaimFromToken");
    JSONObject payload = getPayload(id_token);
    try {
      return payload.get(claim);
    } catch (Exception e) {
      logger.error( e.getMessage());
      return null;
    }
  }

  public static JSONObject getPayload(String token) {
    logger.trace("getPayload");

    String[] parts = token.split("\\.");
    String decodedString = decodeBase64(parts[1]);

    JSONObject payload = null;
    try {
      payload = new JSONObject(decodedString);
    } catch (JSONException e) {
      logger.error( e.getMessage());
      return null;
    }

    return payload;
  }

  private static String decodeBase64(String data) {
    logger.trace("decodeBase64");
    byte[] result = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      result = Base64.getDecoder().decode(data);
    } else {
      result = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
    }
    return new String(result);
  }

  public static String getAccountType(Context context) {
    logger.trace("getAccountType");
    String accounttype = getVal(context, KEY_ACCOUNTTYPE);
    if (accounttype == null) {
      accounttype = getStringRessource(context, "account_type");
      setVal(context, KEY_ACCOUNTTYPE, accounttype);
    }
    return accounttype;
  }

  public static void writeData(Context context, String key, String data) {
    logger.trace("writeData");
    AccountManager accountManager = getAccountManager(context);
    Account account = getAccount(context);
    if (account != null) {
      accountManager.setUserData(account, key, data);
    }
  }

  public static void clear(Context context) {
    logger.trace("clear");
    Account account = getAccount(context);
    if (account != null) {
      String name = account.name;
      removeAccount(context,false);
      createAccount(context, name, null,false);
    }
  }

  public static void removeAccount(Context context) {
    removeAccount(context,true);
  }
  public static void removeAccount(Context context,boolean alarm) {
    logger.trace("removeAccount");
    Account account = getAccount(context);
    if (account != null) {
      AccountManager accountManager = getAccountManager(context);

      if (alarm) {
        try {
          String state = readData(context, "state");
          JSONObject jstate = new JSONObject(state);
          if (jstate.has("refresh_token_expires_in")) {
            cancelAlarm(context, jstate.getString("id_token"));
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }

      accountManager.removeAccountExplicitly(account);
    }
  }

  private static AccountManager getAccountManager(Context context) {
    logger.trace("getAccountManager");
    return context.getSystemService(AccountManager.class);
  }

  public static Account getAccount(Context context) {
    logger.trace("getAccount");
    Account[] result = getAccountManager(context).getAccountsByType(getAccountType(context));
    if (result != null && result.length > 0) {
      return result[0];
    }
    return null;
  }

  public static String loadOpenIDConfiguration(String issuerUrl) throws Exception {
    logger.trace("loadOpenIDConfiguration");
    // Ignorieren von SSL-Fehlern
    disableSSLCertificateValidation();

    URL url = new URL(issuerUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    try {
      InputStream is = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      StringBuilder config = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        config.append(line);
      }
      return config.toString();
    } finally {
      connection.disconnect();
    }
  }

  public static void disableSSLCertificateValidation() throws Exception {
    logger.trace("disableSSLCertificateValidation");
    TrustManager[] trustAllCertificates = new TrustManager[]{
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
    };

    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAllCertificates, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    });
  }

}
