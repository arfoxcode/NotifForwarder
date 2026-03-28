package com.arif.notifforwarder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class SecretCodeReceiver extends BroadcastReceiver {

    private static final String SECRET = "8642";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) return;

        String number = getResultData();
        if (number == null) number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (number == null) return;

        if (!number.replaceAll("[^0-9*#]", "").contains("*#*#" + SECRET)) return;

        setResultData(null);

        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, "com.arif.notifforwarder.MainLauncher"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(launch);
    }
}
