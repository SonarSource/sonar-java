package checks.security;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;

public class ReceivingIntentsCheckSample {

  public void register(Context context, BroadcastReceiver receiver,
                       IntentFilter filter,
                       String broadcastPermission,
                       Handler scheduler,
                       int flags) {
    context.registerReceiver(receiver, filter); // Noncompliant {{Make sure that intents are received safely here.}}
    context.registerReceiver(receiver, filter, flags); // Noncompliant

    // Broadcasting intent with "null" for broadcastPermission
    context.registerReceiver(receiver, filter, null, scheduler); // Noncompliant
    context.registerReceiver(receiver, filter, null, scheduler, flags); // Noncompliant


    context.registerReceiver(receiver, filter, broadcastPermission, scheduler); // OK
    context.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags); // OK
  }

  void callThroughActivity(MyActivity myActivity,BroadcastReceiver receiver, IntentFilter filter) {
    myActivity.registerReceiver(receiver, filter); // Noncompliant
    getActivity().registerReceiver(receiver, filter); // Noncompliant
  }

  public Activity getActivity() {
    return new MyActivity();
  }

  class MyActivity extends Activity {
    public void bad(BroadcastReceiver br, IntentFilter filter) {
      getActivity().registerReceiver(br, filter); // Noncompliant
    }
    public Activity getActivity() {
      return this;
    }
  }
}


