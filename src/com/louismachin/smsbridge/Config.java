package com.louismachin.smsbridge;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings stored on the phone. Both are written by install.sh via the Main activity. */
final class Config {
    static final String TAG = "smsbridge";

    private Config() {}

    static String webhook(Context c) { return prefs(c).getString("webhook", ""); }
    static String secret(Context c)  { return prefs(c).getString("secret", ""); }

    static void set(Context c, String key, String value) {
        prefs(c).edit().putString(key, value).commit();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("bridge", Context.MODE_PRIVATE);
    }
}