package de.mopsdom.oidc.configapp;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

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

public class ConfigProvider extends ContentProvider {

  public final static String KEY_ISSUER = "ISSUER";
  public final static String KEY_CLIENT_ID = "CLIENT_ID";
  public final static String KEY_REDIRECT_URI = "REDIRECT_URI";
  public final static String KEY_LOGOUT_REDIRECT_URI = "LOGOUT_REDIRECT_URI";
  public final static String KEY_USERCLAIM = "USERCLAIM";
  public final static String KEY_ACCOUNTTYPE = "ACCOUNTTYPE";
  public final static String KEY_SCOPE = "SCOPE";
  public final static String KEY_NOTIFICATION = "NOTIFICATION";

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

  @Override
  public boolean onCreate() {
    String connectionConfig = readConfig();
    String configString = readOIDCConfig();

    Log.i(ConfigProvider.class.getSimpleName(), connectionConfig != null ? connectionConfig : "NULL");
    Log.i(ConfigProvider.class.getSimpleName(), configString != null ? configString : "NULL");

    loadConfig(connectionConfig, configString);

    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

    String result = "";
    if (uri.getPath().startsWith("/config")) {
      result = readOIDCConfig();
      if (result == null) {
        loadConfig(readConfig(), null);
      }
    } else if (uri.getPath().startsWith("/connectionconfig")) {
      result = readConfig();
    }

    String[] columnNames = {"result"};
    MatrixCursor cursor = new MatrixCursor(columnNames);
    cursor.addRow(new Object[]{result});

    return cursor;
  }

  @Override
  public String getType(Uri uri) {
    return "application/json";
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    update(uri, values, null, null);
    return uri;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    if (uri.getPath().startsWith("/connectionconfig")) {
      if (values.containsKey("config")) {
        String config = values.getAsString("config");
        writeConfig(config);
        return 1;
      }
    } else if (uri.getPath().startsWith("/config")) {
      if (values.containsKey("config")) {
        String config = values.getAsString("config");
        writeOIDCConfig(config);
        return 1;
      }
    }
    return 0;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  private void loadConfig(String connectionConfig, String configString) {
    if ((configString == null || configString.isEmpty()) && connectionConfig != null && !connectionConfig.isEmpty()) {
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          String issuer = "";

          try {
            JSONObject json = new JSONObject(connectionConfig);
            issuer = json.getString("issuer");
          } catch (Exception e) {
            Log.e(ConfigProvider.class.getSimpleName(), e.getMessage());
            return;
          }

          String issuerUrl = issuer + "/.well-known/openid-configuration";
          try {
            String config = loadOpenIDConfiguration(issuerUrl);

            if (config != null) {
              writeOIDCConfig(config);
            }

          } catch (Exception e) {
            Log.e(ConfigProvider.class.getSimpleName(), e.getMessage());
          }
        }
      });
      t.start();

      try {
        t.join();
      } catch (InterruptedException e) {
        Log.e(ConfigProvider.class.getSimpleName(), e.getMessage());
      }
    }
  }

  public void writeOIDCConfig(String json) {
    SharedPreferences authPrefs = getContext().getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("oidc", json).apply();
  }

  public String readOIDCConfig() {
    SharedPreferences authPrefs = getContext().getSharedPreferences("auth", MODE_PRIVATE);
    return authPrefs.getString("oidc", null);
  }

  public void writeConfig(String json) {
    SharedPreferences authPrefs = getContext().getSharedPreferences("auth", MODE_PRIVATE);
    authPrefs.edit().putString("config", json).apply();
  }

  public String readConfig() {
    SharedPreferences authPrefs = getContext().getSharedPreferences("auth", MODE_PRIVATE);
    String config = authPrefs.getString("config", null);

    if (config == null) {
      try {
        JSONObject jconf = new JSONObject();

        jconf.put(KEY_CLIENT_ID.toLowerCase(), getStringRessource("default_client_id"));
        jconf.put(KEY_REDIRECT_URI.toLowerCase(), getStringRessource("default_redirect_uri"));
        jconf.put(KEY_SCOPE.toLowerCase(), getStringRessource("default_scope"));
        jconf.put(KEY_USERCLAIM.toLowerCase(), getStringRessource("default_userclaim"));
        jconf.put(KEY_ISSUER.toLowerCase(), getStringRessource("default_issuer"));
        jconf.put(KEY_LOGOUT_REDIRECT_URI.toLowerCase(), getStringRessource("default_redirect_uri_logout"));
        jconf.put(KEY_ACCOUNTTYPE.toLowerCase(), getStringRessource("account_type"));
        jconf.put(KEY_NOTIFICATION.toLowerCase(), "true");

        writeConfig(jconf.toString());
      } catch (Exception e) {
        Log.e(ConfigProvider.class.getSimpleName(), e.getMessage(), e);
      }

      config = authPrefs.getString("config", null);
    }

    return config;
  }

  public String getStringRessource(String resourceName) {
    int resourceId = getContext().getResources().getIdentifier(resourceName, "string", getContext().getPackageName());

    if (resourceId != 0) {
      return getContext().getResources().getString(resourceId);
    } else {
      return null;
    }
  }

}
