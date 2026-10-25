package com.louismachin.smsbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

/**
 *   adb shell am broadcast -f 32 -n com.louismachin.smsbridge/.SendReceiver \
 *       --es to +447700900123 --es body 'hello'
 *
 * Result code 0 = handed to the radio, 1 = bad arguments, 2 = exception.
 * `am broadcast` prints it, so the server can check "result=0".
 */
public class SendReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String to = intent.getStringExtra("to");
        String body = intent.getStringExtra("body");
        if (to == null || to.isEmpty() || body == null || body.isEmpty()) {
            Log.w(Config.TAG, "send: missing 'to' or 'body'");
            setResultCode(1);
            return;
        }
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= 31
                    ? ctx.getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(body);
            sms.sendMultipartTextMessage(to, null, parts, null, null);
            Log.i(Config.TAG, "send: " + parts.size() + " part(s) to " + to);
            setResultCode(0);
        } catch (Exception e) {
            Log.e(Config.TAG, "send failed", e);
            setResultCode(2);
            setResultData(String.valueOf(e));
        }
    }
}
