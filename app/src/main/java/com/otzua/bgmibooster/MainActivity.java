package com.otzua.bgmibooster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String TAG = "BGMIBooster";
    private TextView tvUserCount;
    private static final String PREFS_NAME = "BGMIPrefs";
    private static final String KEY_USER_COUNTED = "user_counted_v2";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserCount = findViewById(R.id.tvUserCount);
        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        Button btnOpenJoyose = findViewById(R.id.btnOpenJoyose);
        Button btnLaunchBGMI = findViewById(R.id.btnLaunchBGMI);
        
        Button btnGameTurbo = findViewById(R.id.btnGameTurbo);
        Button btnCleaner = findViewById(R.id.btnCleaner);
        
        // Secret Settings
        Button btnPerformanceMode = findViewById(R.id.btnPerformanceMode);
        Button btnAutostart = findViewById(R.id.btnAutostart);
        
        Button btnShare = findViewById(R.id.btnShare);
        TextView tvDeveloper = findViewById(R.id.tvDeveloper);

        // User counter setup
        startUserCounter();

        btnRefresh.setOnClickListener(v -> {
            tvUserCount.setText(R.string.user_count_loading);
            updateUserCount();
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        });

        btnOpenJoyose.setOnClickListener(v -> {
            if (!openAppSettings("com.xiaomi.joyose")) {
                if (!openAppSettings("com.miui.joyose")) {
                    Toast.makeText(this, getString(R.string.joyose_not_found), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLaunchBGMI.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.pubg.imobile");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, getString(R.string.bgmi_not_found), Toast.LENGTH_LONG).show();
            }
        });

        btnGameTurbo.setOnClickListener(v -> {
            showGameTurboDialog();
            if (!launchIntent("com.miui.securitycenter", "com.miui.gamebooster.ui.GameBoosterMainActivity")) {
                launchIntent("com.miui.securitycenter", "com.miui.gamebooster.videobox.settings.VideoBoxSettings");
            }
        });

        btnCleaner.setOnClickListener(v -> {
            safeStartIntent(new Intent("miui.intent.action.GARBAGE_CLEANUP"));
        });

        // HIDDEN PERFORMANCE MODE (Reddit Method)
        btnPerformanceMode.setOnClickListener(v -> {
            if (!launchIntent("com.android.settings", "com.android.settings.fuelgauge.PowerModeSettings")) {
                if (!launchIntent("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")) {
                    Toast.makeText(this, "Performance mode not found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAutostart.setOnClickListener(v -> {
            if (!launchIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")) {
                Toast.makeText(this, "Autostart settings not found", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text, getString(R.string.github_url)));
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        tvDeveloper.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.instagram_url))));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startUserCounter() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUserCount();
                timerHandler.postDelayed(this, 60000); 
            }
        };
        timerHandler.post(updateRunnable);
    }

    private void updateUserCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean alreadyCounted = prefs.getBoolean(KEY_USER_COUNTED, false);

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String namespace = "bgmi_booster_otzua";
                String key = "active_users";
                String urlString = "https://api.counterapi.dev/v1/" + namespace + "/" + key;
                
                if (!alreadyCounted) {
                    urlString += "/up";
                }

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    final String count = json.optString("count", "0");

                    handler.post(() -> {
                        if (!isFinishing()) {
                            tvUserCount.setText(count);
                            if (!alreadyCounted) {
                                prefs.edit().putBoolean(KEY_USER_COUNTED, true).apply();
                            }
                        }
                    });
                } else {
                    handler.post(() -> {
                         if (!isFinishing()) tvUserCount.setText(getString(R.string.user_count_fallback));
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    if (!isFinishing()) tvUserCount.setText(getString(R.string.user_count_fallback));
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private boolean launchIntent(String packageName, String className) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void safeStartIntent(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Feature not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGameTurboDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.game_turbo_guide_title)
                .setMessage(R.string.game_turbo_guide_message)
                .setPositiveButton(R.string.btn_got_it, null)
                .show();
    }

    private boolean openAppSettings(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(updateRunnable);
        executor.shutdownNow();
    }
}
