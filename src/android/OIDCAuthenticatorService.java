package de.mopsdom.oidc.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OIDCAuthenticatorService extends Service {
    OIDCAuthenticator authenticator;

    @Override
    public IBinder onBind(Intent intent) {
        authenticator = new OIDCAuthenticator(this);
        return authenticator.getIBinder();
    }
}
