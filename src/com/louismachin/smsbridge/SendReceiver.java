package com.louismachin.smsbridge;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   adb shell am broadcast -f 32 -n com.louismachin.smsbridge/.SendReceiver \
 *       --es to +447700900123 --es body 'hello'
 *
 * Result code 200 = handed to the radio, 400 = bad arguments, 500 = exception.
 * (0 is avoided on purpose: it is also what `am broadcast` reports when nothing ran.)
 * `am broadcast` prints it, so the server can check "result=200".
 * Whether the network then accepted it is logged by SentReceiver a moment later.
 */
public class SendReceiver extends BroadcastReceiver {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String to = intent.getStringExtra("to");
        String body = intent.getStringExtra("body");
        if (to == null || to.isEmpty() || body == null || body.isEmpty()) {
            Log.w(Config.TAG, "send: missing 'to' or 'body'");
            setResultCode(400);
            return;
        }
        try {
            SmsManager sms = Build.VERSION.SDK_INT >= 31
                    ? ctx.getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(body);

            // One callback per part, so SentReceiver can log what the radio actually did.
            ArrayList<PendingIntent> sent = new ArrayList<>();
            for (int n = 0; n < parts.size(); n++) {
                Intent cb = new Intent(ctx, SentReceiver.class)
                        .putExtra("to", to)
                        .putExtra("part", n + 1)
                        .putExtra("parts", parts.size());
                // Unique request code, or Android reuses one PendingIntent for every part.
                sent.add(PendingIntent.getBroadcast(ctx, NEXT_ID.getAndIncrement(), cb,
                        PendingIntent.FLAG_UPDATE_CURRENT));
            }

            sms.sendMultipartTextMessage(to, null, parts, sent, null);
            Log.i(Config.TAG, "send: queued " + parts.size() + " part(s) to " + to);
            setResultCode(200);
        } catch (Exception e) {
            Log.e(Config.TAG, "send failed", e);
            setResultCode(500);
            setResultData(String.valueOf(e));
        }
    }
}