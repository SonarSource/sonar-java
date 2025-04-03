package com.google.android.gms.ads.identifier;

import android.content.Context;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class AdvertisingIdClient {
  public static final class Info {
    public final String getId() {
      throw new RuntimeException("Stub!");
    }
    public boolean isLimitAdTrackingEnabled () {
      throw new RuntimeException("Stub!");
    }
  }

  public static Info getAdvertisingIdInfo(Context context) {
    throw new RuntimeException("Stub!");
  }
}
