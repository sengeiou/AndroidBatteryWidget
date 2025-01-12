package com.github.xckevin927.android.battery.widget.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.xckevin927.android.battery.widget.receiver.BatteryChangeReceiver;
import com.github.xckevin927.android.battery.widget.receiver.BatteryWidget;

public class WidgetUpdateService extends Service {

    private static final String TAG = "WidgetUpdateService";

    private static final int ALARM_DURATION  = 5 * 60 * 1000; // service 自启间隔
    private static final int UPDATE_DURATION = 10 * 1000;     // Widget 更新间隔
    private static final int UPDATE_MESSAGE  = 1000;

    private static boolean isServiceAlive = false;

    private UpdateHandler updateHandler;

    private BatteryChangeReceiver batteryChangeReceiver;

    public static void start(Context context) {
        if (isServiceAlive) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.bindService(new Intent(context, WidgetUpdateService.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {

                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            }, BIND_AUTO_CREATE);
        } else {
            ContextCompat.startForegroundService(context, new Intent(context, WidgetUpdateService.class));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(0, new Notification());

        updateHandler = new UpdateHandler();
        postMessage();

        batteryChangeReceiver = new BatteryChangeReceiver();
        registerReceiver(batteryChangeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        isServiceAlive = true;
        Log.e(TAG, "onCreate: ");
    }

    private void postMessage() {
        if (updateHandler == null) {
            return;
        }
        Message message = updateHandler.obtainMessage();
        message.what = UPDATE_MESSAGE;
        updateHandler.sendMessageDelayed(message, UPDATE_DURATION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 每个 ALARM_DURATION 自启一次
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(getBaseContext(), WidgetUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(), 0,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ALARM_DURATION, pendingIntent);

        return START_STICKY;
    }

    private void updateWidget() {
        Log.e(TAG, "updateWidget: ");
        // 更新 Widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
//        appWidgetManager.updateAppWidget(new ComponentName(getApplicationContext(), MyWidgetProvider.class), remoteViews);

        Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), BatteryWidget.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);

        // 发送下次更新的消息
        postMessage();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryChangeReceiver);
        updateHandler.removeCallbacks(null);
        updateHandler = null;
        isServiceAlive = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    private class MyBinder extends Binder {

    }

    private final class UpdateHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_MESSAGE:
                    updateWidget();
                    break;
                default:
                    break;
            }
        }
    }

}
