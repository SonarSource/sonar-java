package android.provider;

import android.content.ContentResolver;

public final class Settings {
  @Deprecated
  public static final String ANDROID_ID = Secure.ANDROID_ID;
  @Deprecated
  public static final String WIFI_WATCHDOG_PING_COUNT = Secure.WIFI_WATCHDOG_PING_COUNT;

  public static final class Secure {
    public static final String ANDROID_ID = "android_id";
    @Deprecated
    public static final String WIFI_WATCHDOG_PING_COUNT = "wifi_watchdog_ping_count";

    public static String getString(ContentResolver resolver, String name) {
      throw new RuntimeException("Stub!");
    }
    public static boolean putString(ContentResolver resolver, String name, String value) {
      throw new RuntimeException("Stub!");
    }
  }
}
