package com.louismachin.smsbridge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Called by the system once per part after SendReceiver hands a text to the radio,
 * with the network's verdict. Watch with: adb logcat -s smsbridge
 */
public class SentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int code = getResultCode();
        String what = intent.getStringExtra("to")
                + " (part " + intent.getIntExtra("part", 0) + "/" + intent.getIntExtra("parts", 0) + ")";
        if (code == Activity.RESULT_OK) {
            Log.i(Config.TAG, "sent OK: " + what);
        } else {
            // errorCode is the radio's own code, mostly useful with GENERIC_FAILURE
            Log.e(Config.TAG, "send FAILED: " + what + ": " + describe(code)
                    + " errorCode=" + intent.getIntExtra("errorCode", -1));
        }
    }

    private static String describe(int code) {
        switch (code) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: return "GENERIC_FAILURE";
            case SmsManager.RESULT_ERROR_RADIO_OFF:       return "RADIO_OFF";
            case SmsManager.RESULT_ERROR_NULL_PDU:        return "NULL_PDU";
            case SmsManager.RESULT_ERROR_NO_SERVICE:      return "NO_SERVICE";
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:  return "LIMIT_EXCEEDED";
            default: return "result code " + code;
        }
    }
}