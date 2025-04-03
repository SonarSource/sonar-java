package androidx.ads.identifier;

import android.annotation.NonNull;

public abstract class AdvertisingIdInfo {
  @NonNull
  public abstract String getId();
  @NonNull
  public abstract String getProviderPackageName();
}
