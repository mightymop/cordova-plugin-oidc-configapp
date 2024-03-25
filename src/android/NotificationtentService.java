package de.mopsdom.oidc.configapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationtentService extends Service {

  private static final Logger logger = Utils.initLogger()!=null?Utils.initLogger():LoggerFactory.getLogger(NotificationtentService.class);
  private static final int NOTIFICATION_ID = 1;

  public static int getIdByName(Context context, String idName) {
    logger.trace("getIdByName");
    int resourceId = 0;

    try {
      return context.getResources().getIdentifier(idName, "id", context.getPackageName());
    } catch (Exception e) {
      return -1;
    }
  }

  public static void showForegroundNotification(Context context, boolean isauth, Bitmap bmp, String persnr, String upn) {

    logger.trace("showForegroundNotification");
    RemoteViews customViewBig = new RemoteViews(context.getPackageName(), getLayoutResourceId(context, "notification_layout_large"));
    RemoteViews customViewNormal = new RemoteViews(context.getPackageName(), getLayoutResourceId(context, "notification_layout"));

    Intent notificationIntent = null;

    String className = context.getPackageName() + ".MainActivity"; // Beispiel-Klassenname als String
    Class<?> targetClass = null;
    try {
      targetClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
      return;
    }

    if (targetClass != null) {
      notificationIntent = new Intent(context, targetClass);
      // Führen Sie hier den Intent aus
    } else {
      return;
    }

    customViewBig.setTextViewText(getIdByName(context, "notification_button"), "An-/Abmeldung");
    if (!isauth) {
      customViewBig.setTextViewText(getIdByName(context, "notification_text"), "Kein Benutzer angemeldet");
      customViewNormal.setTextViewText(getIdByName(context, "notification_text"), "Status: Abgemeldet");
    } else {
      customViewBig.setTextViewText(getIdByName(context, "notification_text"), persnr != null ? persnr : upn);
      customViewNormal.setTextViewText(getIdByName(context, "notification_text"), "Status: Angemeldet");
    }

    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    customViewBig.setOnClickPendingIntent(getIdByName(context, "notification_button"), pendingIntent);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
      Utils.getStringRessource(context, "app_name") + "_CHANNEL_ID")
      .setSmallIcon(isauth ? getDrawableResourceId(context, "baseline_lock_open_24") : getDrawableResourceId(context, "baseline_lock_24"))
      .setContentTitle("An-/Abmeldung")
      .setCustomBigContentView(customViewBig)
      .setCustomContentView(customViewNormal)
      .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
      .setOngoing(true); // Setze die Benachrichtigung auf dauerhaft

    if (bmp != null) {
      customViewBig.setImageViewBitmap(getIdByName(context, "notification_image"), bmp);
    } else {
      customViewBig.setImageViewResource(getIdByName(context, "notification_image"), getDrawableResourceId(context, "baseline_account_circle_24"));
    }

    // Erstelle das Notification-Objekt
    Notification notification = builder.build();

    // Zeige die Benachrichtigung an
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    // Erstelle den Notification-Kanal (erforderlich ab Android 8.0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String appname = Utils.getStringRessource(context,"app_name");
      NotificationChannel channel = new NotificationChannel(appname + "_CHANNEL_ID",
        appname, NotificationManager.IMPORTANCE_HIGH);
      notificationManager.createNotificationChannel(channel);
    }

    // Zeige die Benachrichtigung im Vordergrund an
    notificationManager.notify(NOTIFICATION_ID, notification);
  }

  public static int getLayoutResourceId(Context context, String layoutName) {

    logger.trace("getLayoutResourceId");
    return Utils.getIdentifier(context, "layout", layoutName);
  }

  public static int getDrawableResourceId(Context context, String layoutName) {

    logger.trace("getDrawableResourceId");
    return Utils.getIdentifier(context, "drawable", layoutName);
  }

  public static void cancelForegroundNotification(Context context) {

    logger.trace("cancelForegroundNotification");
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(NOTIFICATION_ID);
  }

  public static Bitmap drawableToBitmap(Drawable drawable) {

    logger.trace("drawableToBitmap");
    if (drawable instanceof BitmapDrawable) {
      return ((BitmapDrawable) drawable).getBitmap();
    }

    int width = drawable.getIntrinsicWidth();
    int height = drawable.getIntrinsicHeight();

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
  }

  @Override
  public void onCreate() {

    logger.trace("onCreate");
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    logger.trace("onStartCommand");
    if (intent != null && intent.hasExtra("notify")) {
      if (intent.getBooleanExtra("notify", false)) {
        Bitmap bitmap = null;
        byte[] decodedBytes = null;

        if (intent.hasExtra("picture")) {
          String picture = intent.getStringExtra("picture");
          if (!picture.isEmpty()) {
            logger.debug("PICTURE: "+picture);
            decodedBytes = Base64.decode((String) picture, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
          }
        }

        showForegroundNotification(this,
          intent.hasExtra("isauth") && intent.getBooleanExtra("isauth", false),
          bitmap,
          intent.hasExtra("persnr") ? intent.getStringExtra("persnr") : null,
          intent.hasExtra("upn") ? intent.getStringExtra("upn") : null);
      } else {
        cancelForegroundNotification(this);
      }
    } else {
      cancelForegroundNotification(this);
    }

    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    logger.trace("onBind");
    return null;
  }
}
