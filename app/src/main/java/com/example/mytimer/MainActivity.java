package com.example.mytimer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {
    private static final String PREFS = "timer_state";
    private SharedPreferences prefs;
    private TextView timeView, statusView, elapsedView;
    private EditText minutesInput, secondsInput;
    private LinearLayout presets;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            renderTime();
            handler.postDelayed(this, 250);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12, 15, 23));
        getWindow().setNavigationBarColor(Color.rgb(12, 15, 23));
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 40);
        }
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(12, 15, 23));

        TextView title = new TextView(this);
        title.setText("MY TIMER");
        title.setTextColor(Color.rgb(139, 156, 255));
        title.setTextSize(14);
        title.setLetterSpacing(0.18f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        statusView = new TextView(this);
        statusView.setText("COUNTDOWN");
        statusView.setTextColor(Color.rgb(150, 157, 174));
        statusView.setTextSize(12);
        statusView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(36);
        root.addView(statusView, sp);

        timeView = new TextView(this);
        timeView.setText("00:05:00");
        timeView.setTextColor(Color.WHITE);
        timeView.setTextSize(48);
        timeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        timeView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(10);
        root.addView(timeView, tp);

        elapsedView = new TextView(this);
        elapsedView.setText("Elapsed: 00:00:00");
        elapsedView.setTextColor(Color.rgb(139, 156, 255));
        elapsedView.setTextSize(14);
        elapsedView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elapsedP = new LinearLayout.LayoutParams(-1, -2);
        elapsedP.topMargin = dp(8);
        root.addView(elapsedView, elapsedP);

        TextView hint = new TextView(this);
        hint.setText("Set duration");
        hint.setTextColor(Color.rgb(190, 196, 211));
        hint.setTextSize(14);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(32);
        root.addView(hint, hp);

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.HORIZONTAL);
        fields.setGravity(Gravity.CENTER);
        minutesInput = new EditText(this);
        secondsInput = new EditText(this);
        styleInput(minutesInput, "Minutes");
        styleInput(secondsInput, "Seconds");
        fields.addView(minutesInput, new LinearLayout.LayoutParams(0, dp(56), 1));
        View gap = new View(this);
        fields.addView(gap, new LinearLayout.LayoutParams(dp(12), 1));
        fields.addView(secondsInput, new LinearLayout.LayoutParams(0, dp(56), 1));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(12);
        root.addView(fields, fp);

        presets = new LinearLayout(this);
        presets.setGravity(Gravity.CENTER);
        renderPresets();
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = dp(14);
        root.addView(presets, pp);
        Button editPresets = button("Customize presets", false);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(40));
        ep.topMargin = dp(6);
        root.addView(editPresets, ep);
        editPresets.setOnClickListener(v -> customizePresets());

        TextView volumeTitle = new TextView(this);
        volumeTitle.setText("ALARM VOLUME  " + prefs.getInt("alarm_volume", 70) + "%");
        volumeTitle.setTextColor(Color.rgb(190, 196, 211));
        volumeTitle.setTextSize(13);
        LinearLayout.LayoutParams vtp = new LinearLayout.LayoutParams(-1, -2);
        vtp.topMargin = dp(24);
        root.addView(volumeTitle, vtp);
        SeekBar volumeBar = new SeekBar(this);
        volumeBar.setMax(100);
        volumeBar.setProgress(prefs.getInt("alarm_volume", 70));
        volumeBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(139, 156, 255)));
        volumeBar.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.rgb(139, 156, 255)));
        root.addView(volumeBar, new LinearLayout.LayoutParams(-1, dp(42)));
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeTitle.setText("ALARM VOLUME  " + progress + "%");
                if (fromUser) prefs.edit().putInt("alarm_volume", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button start = button("START TIMER", true);
        LinearLayout.LayoutParams startP = new LinearLayout.LayoutParams(-1, dp(54));
        startP.topMargin = dp(28);
        root.addView(start, startP);
        start.setOnClickListener(v -> startTimer());

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        Button pause = button("PAUSE / RESUME", false);
        Button reset = button("RESET", false);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, dp(48), 1);
        ap.setMargins(0, dp(12), dp(5), 0);
        actions.addView(pause, ap);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(48), 1);
        rp.setMargins(dp(5), dp(12), 0, 0);
        actions.addView(reset, rp);
        root.addView(actions);
        pause.setOnClickListener(v -> sendAction(TimerService.ACTION_PAUSE));
        reset.setOnClickListener(v -> {
            sendAction(TimerService.ACTION_RESET);
            minutesInput.setText("5");
            secondsInput.setText("0");
        });

        TextView foot = new TextView(this);
        foot.setText("Runs in the background • Alarm when finished");
        foot.setTextColor(Color.rgb(112, 121, 143));
        foot.setTextSize(12);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fop = new LinearLayout.LayoutParams(-1, -2);
        fop.topMargin = dp(28);
        root.addView(foot, fop);

        setContentView(root);
        if (minutesInput.getText().length() == 0) minutesInput.setText("5");
        if (secondsInput.getText().length() == 0) secondsInput.setText("0");
        renderTime();
    }

    private void renderPresets() {
        presets.removeAllViews();
        String saved = prefs.getString("presets", "1,5,10,30");
        String[] items = saved.split(",");
        for (String item : items) {
            try {
                final int mins = Integer.parseInt(item.trim());
                if (mins < 1 || mins > 1440) continue;
                Button b = button(mins + " min", false);
                b.setTextSize(12);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(42), 1);
                bp.setMargins(dp(3), 0, dp(3), 0);
                presets.addView(b, bp);
                b.setOnClickListener(v -> {
                    minutesInput.setText(String.valueOf(mins));
                    secondsInput.setText("0");
                });
            } catch (NumberFormatException ignored) {}
        }
    }

    private void customizePresets() {
        EditText entry = new EditText(this);
        entry.setSingleLine(true);
        entry.setInputType(2);
        entry.setText(prefs.getString("presets", "1,5,10,30"));
        entry.setHint("Example: 2, 7, 15, 45");
        new AlertDialog.Builder(this).setTitle("Customize quick presets")
            .setMessage("Enter up to four durations in minutes, separated by commas (1–1440).")
            .setView(entry)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dialog, which) -> {
                String[] parts = entry.getText().toString().split(",");
                StringBuilder valid = new StringBuilder();
                int count = 0;
                for (String part : parts) {
                    try {
                        int n = Integer.parseInt(part.trim());
                        if (n >= 1 && n <= 1440 && count < 4) {
                            if (count++ > 0) valid.append(",");
                            valid.append(n);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (count > 0) {
                    prefs.edit().putString("presets", valid.toString()).apply();
                    renderPresets();
                }
            }).show();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
            (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            prefs.getBoolean("alarm_playing", false)) {
            Intent stop = new Intent(this, TimerService.class);
            stop.setAction(TimerService.ACTION_DISMISS_ALARM);
            startService(stop);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void styleInput(EditText e, String hint) {
        e.setSingleLine(true);
        e.setInputType(2);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(112, 121, 143));
        e.setTextSize(16);
        e.setGravity(Gravity.CENTER);
        e.setPadding(dp(8), 0, dp(8), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(26, 31, 45));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(49, 57, 78));
        e.setBackground(bg);
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(15, 18, 29) : Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? Color.rgb(139, 156, 255) : Color.rgb(31, 37, 53));
        bg.setCornerRadius(dp(14));
        if (!primary) bg.setStroke(dp(1), Color.rgb(49, 57, 78));
        b.setBackground(bg);
        return b;
    }

    private void startTimer() {
        long m = parse(minutesInput, 0);
        long s = parse(secondsInput, 0);
        long total = m * 60 + s;
        if (total < 1) {
            statusView.setText("ENTER A DURATION");
            return;
        }
        Intent i = new Intent(this, TimerService.class);
        i.setAction(TimerService.ACTION_START);
        i.putExtra(TimerService.EXTRA_DURATION, total * 1000L);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        statusView.setText("TIMER RUNNING");
    }

    private long parse(EditText e, long fallback) {
        try { return Long.parseLong(e.getText().toString().trim()); }
        catch (Exception ex) { return fallback; }
    }

    private void sendAction(String action) {
        Intent i = new Intent(this, TimerService.class);
        i.setAction(action);
        if (TimerService.ACTION_START.equals(action) && Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private void renderTime() {
        long remaining;
        boolean running;
        synchronized (prefs) {
            running = prefs.getBoolean("running", false);
            remaining = prefs.getLong("remaining", 300000L);
            if (running) remaining = Math.max(0, prefs.getLong("end_at", 0L) - System.currentTimeMillis());
        }
        long totalSec = (remaining + 999) / 1000;
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        timeView.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s));
        long totalDuration = prefs.getLong("total_duration", remaining);
        long elapsed = Math.max(0, totalDuration - remaining);
        long elapsedSec = elapsed / 1000;
        long eh = elapsedSec / 3600, em = (elapsedSec % 3600) / 60, es = elapsedSec % 60;
        elapsedView.setText("Elapsed: " + String.format(java.util.Locale.US, "%02d:%02d:%02d", eh, em, es));
        if (running) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            statusView.setText("TIMER RUNNING");
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (prefs.getBoolean("finished", false)) statusView.setText("TIME IS UP");
            else statusView.setText("COUNTDOWN");
        }

    }

    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onResume() { super.onResume(); handler.post(ticker); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(ticker); }
}
