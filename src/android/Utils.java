package de.mopsdom.oidc.configapp;

import static android.content.Context.MODE_PRIVATE;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Utils {

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
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("oidc", json).apply();
  }

  public static String readOIDCConfig(Context context) {
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    return authPrefs.getString("oidc", null);
  }

  public static void writeConfig(Context context, String json) {
    SharedPreferences authPrefs = context.getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("config", json).apply();
  }

  public static String readConfig(Context context) {
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
        Log.e(TAG, e.getMessage(), e);
      }

      config = authPrefs.getString("config", null);
    }

    return config;
  }

  public static String getVal(Context context, String key) {
    String jsonconfig = readConfig(context);
    if (jsonconfig != null) {
      try {
        JSONObject config = new JSONObject(jsonconfig);
        return config.has(key.toLowerCase()) ? config.getString(key.toLowerCase()) : null;
      } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
        return null;
      }
    }

    return null;
  }

  public static String readData(Context context, String key) {
    AccountManager accountManager = getAccountManager(context);
    Account account = getAccount(context);
    if (account != null) {
      return accountManager.getUserData(account, key);
    }
    return null;
  }

  public static void setVal(Context context, String key, String val) {
    String jsonconfig = readConfig(context);

    JSONObject config;
    if (jsonconfig == null) {
      config = new JSONObject();
    } else {
      try {
        config = new JSONObject(jsonconfig);
      } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
        config = new JSONObject();
      }
    }

    try {
      config.put(key.toLowerCase(), val);
    } catch (JSONException e) {
      Log.e(TAG, e.getMessage(), e);
    }

    writeConfig(context, config.toString());
  }

  public static int getIdentifier(Context context, String kategorie, String name) {
    int resourceId = 0;

    try {
      resourceId = context.getResources().getIdentifier(name, kategorie, context.getPackageName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return resourceId;
  }

  public static String getStringRessource(Context context, String resourceName) {
    int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());

    if (resourceId != 0) {
      return context.getResources().getString(resourceId);
    } else {
      return null;
    }
  }

  public static boolean createAccount(Context context, String username, String state) {
    Account account = getAccount(context);

    if (account != null) {
      return false;
    }

    account = new Account(username, getAccountType(context));
    AccountManager accountManager = getAccountManager(context);
    accountManager.addAccountExplicitly(account, null, null);
    writeData(context, "state", state);

    return true;
  }

  public static String getAccountType(Context context) {
    String accounttype = getVal(context, KEY_ACCOUNTTYPE);
    if (accounttype == null) {
      accounttype = getStringRessource(context, "account_type");
      setVal(context, KEY_ACCOUNTTYPE, accounttype);
    }
    return accounttype;
  }

  public static void writeData(Context context, String key, String data) {
    AccountManager accountManager = getAccountManager(context);
    Account account = getAccount(context);
    if (account != null) {
      accountManager.setUserData(account, key, data);
    }
  }

  public static void clear(Context context) {
    Account account = getAccount(context);
    if (account != null) {
      String name = account.name;
      removeAccount(context);
      createAccount(context, name, null);
    }
  }

  public static void removeAccount(Context context) {
    Account account = getAccount(context);
    if (account != null) {
      AccountManager accountManager = getAccountManager(context);
      accountManager.removeAccountExplicitly(account);
    }
  }

  private static AccountManager getAccountManager(Context context) {
    return context.getSystemService(AccountManager.class);
  }

  public static Account getAccount(Context context) {
    Account[] result = getAccountManager(context).getAccountsByType(getAccountType(context));
    if (result != null && result.length > 0) {
      return result[0];
    }
    return null;
  }

  public static String loadOpenIDConfiguration(String issuerUrl) throws Exception {
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
