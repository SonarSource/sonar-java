package checks.sustainability;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Handler;
import java.util.concurrent.Executor;

public class AndroidExactAlarmCheckSample {

  public void setExact(AlarmManager alarmManager, PendingIntent operation) {
    alarmManager.setExact(0, 1000, operation); // Noncompliant [[sc=18;ec=26]] {{Use "set" instead of "setExact".}}
    alarmManager.set(0, 1000, operation); // Compliant

    var noAlarmManager = new NoAlarmManager();
    noAlarmManager.setExact(0, 1000, operation); // Compliant
  }

  public void setExact(AlarmManager alarmManager, AlarmManager.OnAlarmListener listener, Handler targetHandler) {
    alarmManager.setExact(0, 1000, "", listener, targetHandler); // Noncompliant
    alarmManager.set(0, 1000, "", listener, targetHandler); // Compliant

    var noAlarmManager = new NoAlarmManager();
    noAlarmManager.setExact(0, 1000, "", listener, targetHandler); // Compliant
  }

  public void setExactAndAllowWhileIdle(AlarmManager alarmManager, PendingIntent operation) {
    alarmManager.setExactAndAllowWhileIdle(0, 1000, operation); // Noncompliant [[sc=18;ec=43]] {{Use "setAndAllowWhileIdle" instead of "setExactAndAllowWhileIdle".}}
    alarmManager.setAndAllowWhileIdle(0, 1000, operation); // Compliant

    var noAlarmManager = new NoAlarmManager();
    noAlarmManager.setExactAndAllowWhileIdle(0, 1000, operation); // Compliant
  }

  public void setWindow(AlarmManager alarmManager, long triggerTime, PendingIntent pendingIntent) {
    alarmManager.setWindow(0, triggerTime, 300000, pendingIntent); // Noncompliant [[sc=44;ec=50]] {{Don't use alarm windows below 10 minutes.}}
    alarmManager.setWindow(0, triggerTime, 600000, pendingIntent); // Compliant
    alarmManager.setWindow(0, triggerTime, 900000, pendingIntent); // Compliant
    alarmManager.setWindow(0, triggerTime, 300000L, pendingIntent); // Noncompliant
    alarmManager.setWindow(0, triggerTime, 600000L, pendingIntent); // Compliant
    alarmManager.setWindow(0, triggerTime, 900000L, pendingIntent); // Compliant
    alarmManager.setWindow(0, triggerTime, windowLengthMillisTooShort, pendingIntent); // Noncompliant [[sc=44;ec=70]] {{Don't use alarm windows below 10 minutes.}}
    alarmManager.setWindow(0, triggerTime, windowLengthMillisLongEnough, pendingIntent); // Compliant
    alarmManager.setWindow(0, triggerTime, triggerTime, pendingIntent); // Compliant

    var noAlarmManager = new NoAlarmManager();
    noAlarmManager.setWindow(0, triggerTime, 300000, pendingIntent); // Compliant
    noAlarmManager.setWindow(0, triggerTime, 300000L, pendingIntent); // Compliant
    noAlarmManager.setWindow(0, triggerTime, windowLengthMillisTooShort, pendingIntent); // Compliant
  }

  public void setWindow(AlarmManager alarmManager, long triggerTime, AlarmManager.OnAlarmListener listener, Handler targetHandler) {
    alarmManager.setWindow(0, triggerTime, windowLengthMillisTooShort, "", listener, targetHandler); // Noncompliant
    alarmManager.setWindow(0, triggerTime, windowLengthMillisLongEnough, "", listener, targetHandler); // Compliant

    var noAlarmManager = new NoAlarmManager();
    noAlarmManager.setWindow(0, triggerTime, windowLengthMillisTooShort, "", listener, targetHandler); // Compliant
  }

  public void setWindow(AlarmManager alarmManager, long triggerTime, Executor executor, AlarmManager.OnAlarmListener listener) {
    alarmManager.setWindow(0, triggerTime, windowLengthMillisTooShort, "", executor, listener); // Noncompliant
    alarmManager.setWindow(0, triggerTime, windowLengthMillisLongEnough, "", executor, listener); // Compliant

    var noAlarmManager = new NoAlarmManager();
    noAlarmManager.setWindow(0, triggerTime, windowLengthMillisTooShort, "", executor, listener); // Cmpliant
  }

  public static final long windowLengthMillisTooShort = 5 * 60 * 1000; // 5 minutes in milliseconds

  public static final long windowLengthMillisLongEnough = 10 * 60 * 1000; // 10 minutes in milliseconds

  private static class NoAlarmManager {
    public void setExact(int type, long triggerAtMillis, PendingIntent operation) {
    }

    public void setExact(int type, long triggerAtMillis, String tag, AlarmManager.OnAlarmListener listener, Handler targetHandler) {
    }

    public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) {
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, AlarmManager.OnAlarmListener listener,
      Handler targetHandler) {
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, Executor executor,
      AlarmManager.OnAlarmListener listener) {
    }
  }
}
