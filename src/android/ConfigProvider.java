package de.mopsdom.oidc.configapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

public class ConfigProvider extends ContentProvider {

  @Override
  public boolean onCreate() {
    Log.d(NotificationtentService.class.getSimpleName(),"onCreate");
    String connectionConfig = Utils.readConfig(getContext());
    String configString = Utils.readOIDCConfig(getContext());

    Log.i(ConfigProvider.class.getSimpleName(), connectionConfig != null ? connectionConfig : "NULL");
    Log.i(ConfigProvider.class.getSimpleName(), configString != null ? configString : "NULL");

    loadConfig(connectionConfig, configString);

    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    Log.d(NotificationtentService.class.getSimpleName(),"query");
    String result = "";
    String[] columnNames=null;
    if (uri.getPath().startsWith("/config")) {
      columnNames = new String[]{"result"};
      result = Utils.readOIDCConfig(getContext());
      if (result == null) {
        loadConfig(Utils.readConfig(getContext()), null);
      }
    } else if (uri.getPath().startsWith("/connectionconfig")) {
      columnNames = new String[]{"result"};
      result = Utils.readConfig(getContext());
    } else if (uri.getPath().startsWith("/account")) {
      result = Utils.readData(getContext(),projection[0]);
      columnNames = new String[]{projection[0]};
    }

    MatrixCursor cursor = new MatrixCursor(columnNames);
    cursor.addRow(new Object[]{result});

    return cursor;
  }

  @Override
  public String getType(Uri uri) {
    Log.d(NotificationtentService.class.getSimpleName(),"getType");

    return "application/json";
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    Log.d(NotificationtentService.class.getSimpleName(),"insert");
    if (uri.getPath().startsWith("/connectionconfig")) {
      update(uri, values, null, null);
    } else if (uri.getPath().startsWith("/config")) {
      update(uri, values, null, null);
    } else if (uri.getPath().startsWith("/account")) {
      if (values.containsKey("name")) {
        String name = values.getAsString("name");
        String state = values.getAsString("state");
        Utils.createAccount(getContext(), name, state);
      }
    }

    return uri;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    Log.d(NotificationtentService.class.getSimpleName(),"update");
    if (uri.getPath().startsWith("/connectionconfig")) {
      if (values.containsKey("config")) {
        String config = values.getAsString("config");
        Utils.writeConfig(getContext(), config);
        return 1;
      }
    } else if (uri.getPath().startsWith("/config")) {
      if (values.containsKey("config")) {
        String config = values.getAsString("config");
        Utils.writeOIDCConfig(getContext(), config);
        return 1;
      }
    } else if (uri.getPath().startsWith("/account")) {
        String key = values.getAsString("key");
        String data = values.getAsString("data");
        Utils.writeData(getContext(),key,data);
    }
    return 0;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    Log.d(NotificationtentService.class.getSimpleName(),"delete");
    if (uri.getPath().startsWith("/account")) {
      Utils.removeAccount(getContext());
      return 1;
    }
    return 0;
  }

  private void loadConfig(String connectionConfig, String configString) {
    Log.d(NotificationtentService.class.getSimpleName(),"loadConfig");
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
            String config = Utils.loadOpenIDConfiguration(issuerUrl);

            if (config != null) {
              Utils.writeOIDCConfig(getContext(), config);
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


}
