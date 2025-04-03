package android.telephony;

import android.annotation.NonNull;

public class SubscriptionManager {
  @NonNull
  public String getPhoneNumber(int subscriptionId, int source) {
    throw new RuntimeException("Stub!");
  }
  @NonNull
  public String getPhoneNumber(int subscriptionId) {
    throw new RuntimeException("Stub!");
  }
  public int getActiveSubscriptionInfoCount() {
    throw new RuntimeException("Stub!");
  }
}
