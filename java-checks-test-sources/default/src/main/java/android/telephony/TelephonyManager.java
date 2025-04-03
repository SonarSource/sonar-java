package android.telephony;

public class TelephonyManager {
  public String getSimSerialNumber() {
    throw new RuntimeException("Stub!");
  }
  @Deprecated
  public String getDeviceId() {
    throw new RuntimeException("Stub!");
  }
  @Deprecated
  public String getDeviceId(int slotIndex) {
    throw new RuntimeException("Stub!");
  }
  public String getImei() {
    throw new RuntimeException("Stub!");
  }
  public String getImei(int slotIndex) {
    throw new RuntimeException("Stub!");
  }
  @Deprecated
  public String getMeid() {
    throw new RuntimeException("Stub!");
  }
  @Deprecated
  public String getMeid(int slotIndex) {
    throw new RuntimeException("Stub!");
  }
  @Deprecated
  public String getLine1Number() {
    throw new RuntimeException("Stub!");
  }
  public String getLine1Number(int subId) {
    throw new RuntimeException("Stub!");
  }
  @Deprecated
  public int getPhoneCount() {
    throw new RuntimeException("Stub!");
  }
  public int getActiveModemCount() {
    throw new RuntimeException("Stub!");
  }
}
