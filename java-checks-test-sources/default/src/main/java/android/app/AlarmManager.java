package android.app;

import android.os.Handler;
import android.os.Parcelable;
import java.util.concurrent.Executor;

public class AlarmManager {

  public void set(int type, long triggerAtMillis, PendingIntent operation) {
  }

  public void set(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) {
  }

  public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
  }

  public void setExact(int type, long triggerAtMillis, PendingIntent operation) {
  }

  public void setExact(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler) {
  }

  public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
  }

  public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) {
  }

  public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, OnAlarmListener listener,
    Handler targetHandler) {
  }

  public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, Executor executor,
    OnAlarmListener listener) {
  }

  public static final class AlarmClockInfo implements Parcelable {
  }

  public interface OnAlarmListener {
  }
}
