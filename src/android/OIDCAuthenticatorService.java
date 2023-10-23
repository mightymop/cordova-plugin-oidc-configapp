package de.mopsdom.oidc.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class OIDCAuthenticatorService extends Service {
    OIDCAuthenticator authenticator;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(OIDCAuthenticator.class.getSimpleName(),"onBind");
        authenticator = new OIDCAuthenticator(this);
        return authenticator.getIBinder();
    }
}
