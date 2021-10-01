package checks.security;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;

public class AndroidBroadcastingCheck {
  public void broadcast(Intent intent, Context context, UserHandle user,
                        BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
                        String initialData, Bundle initialExtras,
                        String broadcastPermission) {
    context.sendBroadcast(intent); // Noncompliant {{Make sure that broadcasting intents is safe here.}}
    context.sendBroadcastAsUser(intent, user); // Noncompliant

    // Broadcasting intent with "null" for receiverPermission
    context.sendBroadcast(intent, null); // Noncompliant
    context.sendBroadcastAsUser(intent, user, null); // Noncompliant
    context.sendOrderedBroadcast(intent, null); // Noncompliant
    context.sendOrderedBroadcastAsUser(intent, user, null, resultReceiver, scheduler, initialCode, initialData, initialExtras); // Noncompliant

    context.sendStickyBroadcast(intent); // Noncompliant
    context.sendStickyBroadcastAsUser(intent, user); // Noncompliant
    context.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras); // Noncompliant
    context.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode, initialData, initialExtras); // Noncompliant

    context.sendBroadcast(intent, broadcastPermission); // Ok
    context.sendBroadcastAsUser(intent, user, broadcastPermission); // Ok
    context.sendOrderedBroadcast(intent, broadcastPermission); // Ok
    context.sendOrderedBroadcastAsUser(intent, user,broadcastPermission, resultReceiver,
      scheduler, initialCode, initialData, initialExtras); // Ok
  }

  void callThroughActivity(MyActivity myActivity, Intent intent) {
    myActivity.sendBroadcast(intent); // Noncompliant
    getActivity().sendBroadcast(intent); // Noncompliant
  }

  Activity getActivity() {
    return new MyActivity();
  }

  class MyActivity extends Activity {
    public void doSomething(Intent intent) {
      this.sendStickyBroadcast(intent); // Noncompliant
    }
  }
}
