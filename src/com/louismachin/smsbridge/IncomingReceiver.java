package com.louismachin.smsbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** POSTs every incoming SMS to the configured webhook as JSON. */
public class IncomingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs == null || msgs.length == 0) return;

        // A long text arrives as several parts in one broadcast; stitch them back together.
        StringBuilder body = new StringBuilder();
        for (SmsMessage m : msgs) body.append(m.getMessageBody());
        final String from = msgs[0].getOriginatingAddress();
        final long ts = msgs[0].getTimestampMillis();
        final String text = body.toString();
        final String webhook = Config.webhook(ctx);
        final String secret = Config.secret(ctx);
        if (webhook.isEmpty()) {
            Log.w(Config.TAG, "incoming from " + from + " but no webhook configured; run install.sh");
            return;
        }

        // No network on the main thread; goAsync keeps the receiver alive (~10s).
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("from", from);
                json.put("body", text);
                json.put("timestamp", ts);
                int code = post(webhook, secret, json.toString());
                Log.i(Config.TAG, "incoming from " + from + " -> HTTP " + code);
            } catch (Exception e) {
                Log.e(Config.TAG, "webhook failed (text is still in the phone's inbox)", e);
            } finally {
                pending.finish();
            }
        }).start();
    }

    private static int post(String url, String secret, String payload) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("X-Sms-Secret", secret);
            try (OutputStream out = c.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            return c.getResponseCode();
        } finally {
            c.disconnect();
        }
    }
}