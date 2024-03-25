package de.mopsdom.oidc.configapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONObject;

import de.mopsdom.oidc.cordova.Utils;
import de.mopsdom.oidc.cordova.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ForceDeleteAccountActivity extends AppCompatActivity {

  private static final Logger logger = LoggerFactory.getLogger(ForceDeleteAccountActivity.class);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String callbackurl = getIntent().getStringExtra("callbackurl");
    de.mopsdom.oidc.cordova.Utils.removeAccount(this);
    refreshNotification();
    startActivity(getCallbackIntent(callbackurl));
    finish();
  }

  private Intent getCallbackIntent(String action) {
    Intent resultIntent = new Intent(action);
    resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    setResult(RESULT_OK);
    return resultIntent;
  }

  private void refreshNotification() {

    logger.debug(oidc.class.getSimpleName(), "refreshNotification");
    try {
      String config = de.mopsdom.oidc.cordova.Utils.getConfData(this, "connectionconfig");
      JSONObject json = new JSONObject(config);

      ComponentName componentName = new ComponentName("de.berlin.polizei.oidcsso", "de.mopsdom.oidc.configapp.NotificationtentService");
      Intent intent = new Intent();
      intent.setComponent(componentName);
      intent.setAction("de.mopsdom.oidc.NOTIFY_MESSAGE");
      intent.putExtra("notify", json.has("notification") && json.getBoolean("notification"));

      String state = de.mopsdom.oidc.cordova.Utils.readData(this, "state");
      intent.putExtra("isauth", state != null);
      if (state != null) {
        JSONObject jstate = new JSONObject(state);
        String id_token = jstate.getString("id_token");
        String picture = (String) de.mopsdom.oidc.cordova.Utils.getClaimFromToken(id_token, "picture");
        String persnr = (String) de.mopsdom.oidc.cordova.Utils.getClaimFromToken(id_token, "persnr");
        String upn = (String) Utils.getClaimFromToken(id_token, "upn");
        if (picture != null && !picture.isEmpty()) {
          intent.putExtra("picture", picture);
        }
        if (persnr != null && !persnr.isEmpty()) {
          intent.putExtra("persnr", persnr);
        }
        if (upn != null && !upn.isEmpty()) {
          intent.putExtra("upn", upn);
        }
      }

      startService(intent);

    } catch (Exception e) {
      logger.error(oidc.class.getSimpleName(), e.getMessage(),e);
    }
  }
}
