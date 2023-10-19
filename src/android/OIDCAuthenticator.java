package de.mopsdom.oidc.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import de.mopsdom.oidc.configapp.Utils;


public class OIDCAuthenticator extends AbstractAccountAuthenticator {
  public static final String TOKEN_TYPE_ID = "id_token";
  public static final String TOKEN_TYPE_ACCESS = "access_token";
  public static final String TOKEN_TYPE_REFRESH = "refresh_token";

  private static final String TAG = OIDCAuthenticator.class.getSimpleName();
  private final Context mContext;

  public OIDCAuthenticator(Context context) {
    super(context);
    mContext = context;
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    return null;
  }

  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {

    final Bundle bundle = new Bundle();
    bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
    bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "Diese Methode zur Anmeldung wird nicht unterstützt");
    return bundle;
  }

  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override
  public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
    String state = Utils.readData(mContext, "state");

    if (state == null) {
      response.onError(1, "Nutzer nicht angemeldet!");
      return null;
    }

    try {
      JSONObject json = new JSONObject(state);
      String token = json.getString(authTokenType);
      if (token == null) {
        response.onError(1, "Nutzer nicht angemeldet!");
      }

      return createTokenBundle(token, authTokenType, account.name);
    } catch (Exception e) {
      response.onError(2, e.getMessage());
      return null;
    }
  }

  public Bundle createTokenBundle(String token, String tokentype, String accountname) {
    final Bundle result = new Bundle();
    result.putString(AccountManager.KEY_ACCOUNT_NAME, accountname);
    result.putString(AccountManager.KEY_ACCOUNT_TYPE, tokentype);
    result.putString(AccountManager.KEY_AUTHTOKEN, token);
    return result;
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    switch (authTokenType) {
      case TOKEN_TYPE_ID:
        return TOKEN_TYPE_ID;

      case TOKEN_TYPE_ACCESS:
        return TOKEN_TYPE_ACCESS;

      case TOKEN_TYPE_REFRESH:
        return TOKEN_TYPE_REFRESH;

      default:
        return null;
    }
  }

  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {

    Log.i(TAG, "ACCOUNT=" + account.name + " (" + account.type + ")");
    for (String itm : features) {
      Log.i(TAG, "hasFeatures: feature=" + itm);
    }

    return null;
  }


}
