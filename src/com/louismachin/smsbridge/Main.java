package com.louismachin.smsbridge;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Invisible config activity.
 * Launching it also takes the app out of the "stopped" state Android puts freshly installed apps in,
 * so the SMS receiver starts working.
 *
 *   adb shell am start -n com.louismachin.smsbridge/.Main \
 *       --es secret s3cret --es webhook http://127.0.0.1:4535/incoming
 */
public class Main extends Activity {
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String secret = getIntent().getStringExtra("secret");
        String webhook = getIntent().getStringExtra("webhook");
        if (secret != null) Config.set(this, "secret", secret);
        if (webhook != null) Config.set(this, "webhook", webhook);
        Log.i(Config.TAG, "configured: webhook=" + Config.webhook(this)
                + " secret=" + (Config.secret(this).isEmpty() ? "(unset)" : "(set)"));
        finish(); // Theme.NoDisplay activities must finish before onResume
    }
}
