package com.otzua.bgmibooster;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenJoyose = findViewById(R.id.btnOpenJoyose);
        Button btnLaunchBGMI = findViewById(R.id.btnLaunchBGMI);
        TextView tvDeveloper = findViewById(R.id.tvDeveloper);

        btnOpenJoyose.setOnClickListener(v -> {
            if (!openAppSettings("com.xiaomi.joyose")) {
                if (!openAppSettings("com.miui.joyose")) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                        startActivity(intent);
                        Toast.makeText(this, getString(R.string.joyose_not_found), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnLaunchBGMI.setOnClickListener(v -> {
            String bgmiPackage = "com.pubg.imobile";
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(bgmiPackage);
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, getString(R.string.bgmi_not_found), Toast.LENGTH_LONG).show();
            }
        });

        tvDeveloper.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.instagram_url)));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean openAppSettings(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
