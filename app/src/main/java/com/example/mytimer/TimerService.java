package com.example.mytimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class TimerService extends Service {
    public static final String ACTION_START = "com.example.mytimer.START";
    public static final String ACTION_PAUSE = "com.example.mytimer.PAUSE";
    public static final String ACTION_RESET = "com.example.mytimer.RESET";
    public static final String ACTION_DISMISS_ALARM = "com.example.mytimer.DISMISS_ALARM";
    public static final String EXTRA_DURATION = "duration";
    private static final String CHANNEL = "my_timer_channel";
    private static final int NOTIFICATION_ID = 71;
    private android.content.SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private MediaPlayer alarmPlayer;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            boolean running = prefs.getBoolean("running", false);
            if (running) {
                long remaining = prefs.getLong("end_at", 0L) - System.currentTimeMillis();
                if (remaining <= 0) {
                    long finishedAt = System.currentTimeMillis();
                    prefs.edit().putBoolean("running", false).putBoolean("finished", true)
                        .putLong("remaining", 0).putLong("finished_at", finishedAt)
                        .putLong("alarm_stopped_at", 0L).putBoolean("alarm_playing", true).apply();
                    playAlarm();
                    showNotification("Time is up!", "Press a volume button or tap Stop alarm.");
                    releaseWakeLock();
                    return;
                }
                prefs.edit().putLong("remaining", remaining).apply();
                showNotification("My Timer is running", format(remaining) + " remaining");
                handler.postDelayed(this, 1000);
            } else {
                showNotification("My Timer paused", format(prefs.getLong("remaining", 0)) + " remaining");
                stopForeground(false);
                stopSelf();
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("timer_state", MODE_PRIVATE);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_START.equals(action)) {
            long duration = intent.getLongExtra(EXTRA_DURATION, 300000L);
            prefs.edit().putLong("remaining", duration).putLong("total_duration", duration)
                .putLong("end_at", System.currentTimeMillis() + duration)
                .putLong("finished_at", 0L).putLong("alarm_stopped_at", 0L)
                .putBoolean("alarm_playing", false)
                .putBoolean("running", true).putBoolean("finished", false).apply();
            acquireWakeLock();
            startForeground(NOTIFICATION_ID, notification("My Timer is running", format(duration) + " remaining"));
            handler.removeCallbacks(tick);
            handler.post(tick);
        } else if (ACTION_PAUSE.equals(action)) {
            if (prefs.getBoolean("running", false)) {
                long rem = Math.max(0, prefs.getLong("end_at", 0) - System.currentTimeMillis());
                prefs.edit().putLong("remaining", rem).putBoolean("running", false).apply();
            } else {
                long rem = prefs.getLong("remaining", 0);
                if (rem > 0) {
                    prefs.edit().putLong("end_at", System.currentTimeMillis() + rem)
                        .putBoolean("running", true).putBoolean("finished", false).apply();
                    acquireWakeLock();
                    startForeground(NOTIFICATION_ID, notification("My Timer is running", format(rem) + " remaining"));
                    handler.removeCallbacks(tick);
                    handler.post(tick);
                }
            }
        } else if (ACTION_DISMISS_ALARM.equals(action)) {
            stopAlarm();
            prefs.edit().putBoolean("alarm_playing", false)
                .putLong("alarm_stopped_at", System.currentTimeMillis()).apply();
            stopForeground(true);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
            stopSelf();
        } else if (ACTION_RESET.equals(action)) {
            handler.removeCallbacks(tick);
            stopAlarm();
            prefs.edit().putBoolean("alarm_playing", false).apply();
            prefs.edit().putBoolean("running", false).putBoolean("finished", false)
                .putBoolean("alarm_playing", false).putLong("remaining", 300000L)
                .putLong("total_duration", 300000L).putLong("finished_at", 0L)
                .putLong("alarm_stopped_at", 0L).apply();
            releaseWakeLock();
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "My Timer", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Countdown status and completion alert");
            ch.enableVibration(true);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification notification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification.Builder builder = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(text).setContentIntent(pi)
            .setOngoing(prefs.getBoolean("running", false) || prefs.getBoolean("alarm_playing", false))
            .setCategory(Notification.CATEGORY_ALARM);
        if (prefs.getBoolean("alarm_playing", false)) {
            Intent stop = new Intent(this, TimerService.class);
            stop.setAction(ACTION_DISMISS_ALARM);
            PendingIntent stopPi = PendingIntent.getService(this, 2, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            builder.addAction(android.R.drawable.ic_media_pause, "Stop alarm", stopPi);
        }
        return builder.build();
    }

    private void showNotification(String title, String text) {
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification(title, text));
    }

    private String format(long ms) {
        long sec = Math.max(0, (ms + 999) / 1000);
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60);
    }

    private void playAlarm() {
        try {
            stopAlarm();
            String selected = prefs.getString("ringtone_uri", "");
            if ("silent".equals(selected)) return;
            if (selected.isEmpty()) {
                alarmPlayer = MediaPlayer.create(getApplicationContext(), R.raw.timer_alarm);
            } else {
                try {
                    alarmPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(selected));
                } catch (Exception ignored) {
                    alarmPlayer = MediaPlayer.create(getApplicationContext(), R.raw.timer_alarm);
                }
                if (alarmPlayer == null) {
                    alarmPlayer = MediaPlayer.create(getApplicationContext(), R.raw.timer_alarm);
                }
            }
            if (alarmPlayer != null) {
                if (Build.VERSION.SDK_INT >= 21) {
                    alarmPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                } else {
                    alarmPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
                }
                float volume = prefs.getInt("alarm_volume", 70) / 100f;
                alarmPlayer.setVolume(volume, volume);
                alarmPlayer.setLooping(true);
                alarmPlayer.setOnCompletionListener(mp -> stopAlarm());
                alarmPlayer.start();
            }
        } catch (Exception ignored) {}
    }

    private void stopAlarm() {
        try {
            if (alarmPlayer != null) {
                if (alarmPlayer.isPlaying()) alarmPlayer.stop();
                alarmPlayer.release();
                alarmPlayer = null;
            }
        } catch (Exception ignored) { alarmPlayer = null; }
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            if (wakeLock == null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyTimer:Countdown");
            if (!wakeLock.isHeld()) wakeLock.acquire(4 * 60 * 60 * 1000L);
        } catch (Exception ignored) {}
    }
    private void releaseWakeLock() {
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        releaseWakeLock();
        stopAlarm();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
