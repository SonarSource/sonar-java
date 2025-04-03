package checks.security;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.ads.identifier.internal.HoldingConnectionClient;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class AndroidPersistentUniqueIdentifierCheckSample {

  class BluetoothAdapterTests {
    public static class NotTheRealBluetoothAdapter {
      public String getAddress() {
        return "00:00:00:00:00:00";
      }
    }

    public BluetoothAdapter fieldBluetoothAdapter = new BluetoothAdapter();
    public final BluetoothAdapter finalFieldBluetoothAdapter = new BluetoothAdapter();

    public void nonCompliantScenarios(BluetoothAdapter bluetoothAdapterParam) {
      bluetoothAdapterParam.getAddress(); // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
      //                    ^^^^^^^^^^

      // Noncompliant@+2
      // Noncompliant@+1
      String multiple = bluetoothAdapterParam.getAddress() + (bluetoothAdapterParam.getAddress().toLowerCase());

      String withParentheses = ((bluetoothAdapterParam).getAddress()); // Noncompliant
      //                                                ^^^^^^^^^^

      BluetoothAdapter localBluetoothAdapter = bluetoothAdapterParam;
      localBluetoothAdapter.getAddress(); // Noncompliant
      final BluetoothAdapter localFinalBluetoothAdapter = bluetoothAdapterParam;
      localFinalBluetoothAdapter.getAddress(); // Noncompliant
      String inlineBluetoothAdapter = new BluetoothAdapter().getAddress(); // Noncompliant
      fieldBluetoothAdapter.getAddress(); // Noncompliant
      finalFieldBluetoothAdapter.getAddress(); // Noncompliant

      bluetoothAdapterParam.getAddress(); // Noncompliant
      if (bluetoothAdapterParam != null) {
        bluetoothAdapterParam.getAddress(); // Noncompliant
      }
      Function<BluetoothAdapter, String> inLambda = (BluetoothAdapter ba) -> ba.getAddress(); // Noncompliant
      bluetoothAdapterParam.getAddress(); // Noncompliant

      // Method references
      Supplier<String> localGetAddressMethodRef = bluetoothAdapterParam::getAddress; // Noncompliant
      //                                                                 ^^^^^^^^^^
      localGetAddressMethodRef.get();
      var addressOrNull = Optional.ofNullable(bluetoothAdapterParam).map(BluetoothAdapter::getAddress); // Noncompliant
      //                                                                                   ^^^^^^^^^^
    }

    public void compliantScenarios(
      BluetoothAdapter bluetoothAdapterFunParam,
      NotTheRealBluetoothAdapter notTheRealBluetoothAdapter
    ) {
      bluetoothAdapterFunParam.getState();
      bluetoothAdapterFunParam.toString();
      bluetoothAdapterFunParam.hashCode();
      bluetoothAdapterFunParam.equals(bluetoothAdapterFunParam);

      notTheRealBluetoothAdapter.getAddress();
    }
  }

  class WifiInfoTest {

    public void nonCompliantScenarios(WifiInfo wifiInfo) {
      wifiInfo.getMacAddress(); // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
      //       ^^^^^^^^^^^^^
    }

    public void compliantScenarios(WifiInfo wifiInfo) {
      wifiInfo.getLostTxPacketsPerSecond();
      wifiInfo.setLostTxPacketsPerSecond(0.0);
    }
  }

  class TelephonyManagerTest {

    public void nonCompliantScenarios(TelephonyManager telephonyManager) {
      int slotIndex = 42;
      telephonyManager.getSimSerialNumber(); // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
      //               ^^^^^^^^^^^^^^^^^^
      telephonyManager.getDeviceId(); // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
      telephonyManager.getDeviceId(slotIndex); // Noncompliant
      telephonyManager.getImei(); // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
      telephonyManager.getImei(slotIndex); // Noncompliant
      telephonyManager.getMeid(); // Noncompliant {{Using a hardware identifier puts user privacy at risk. Make sure it is safe here.}}
      telephonyManager.getMeid(slotIndex); // Noncompliant
      telephonyManager.getLine1Number(); // Noncompliant {{Using a phone number puts user privacy at risk. Make sure it is safe here.}}
      telephonyManager.getLine1Number(slotIndex); // Noncompliant
    }

    public void compliantScenarios(TelephonyManager telephonyManager) {
      telephonyManager.getPhoneCount();
      telephonyManager.getActiveModemCount();
    }
  }

  class SubscriptionManagerTest {

    public void nonCompliantScenarios(SubscriptionManager subscriptionManager, int source) {
      subscriptionManager.getPhoneNumber(1); // Noncompliant {{Using a phone number puts user privacy at risk. Make sure it is safe here.}}
      //                  ^^^^^^^^^^^^^^
      subscriptionManager.getPhoneNumber(1, source); // Noncompliant
    }

    public void compliantScenarios(SubscriptionManager subscriptionManager) {
      subscriptionManager.getActiveSubscriptionInfoCount();
    }
  }

  class AdvertisingIdClientTest {

    public void nonCompliantScenarios(
      Context context,
      com.google.android.gms.ads.identifier.AdvertisingIdClient.Info comGoogleAdvertisingIdClientInfo,
      androidx.ads.identifier.AdvertisingIdInfo androidxAdsAdvertisingIdInfo,
      HoldingConnectionClient holdingConnectionClient,
      com.huawei.hms.ads.identifier.AdvertisingIdClient.Info comHuaweiAdvertisingIdClientInfo
    ) {
      comGoogleAdvertisingIdClientInfo.getId(); // Noncompliant {{Using Advertising ID puts user privacy at risk. Make sure it is safe here.}}
      //                               ^^^^^
      com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context).getId(); // Noncompliant

      androidxAdsAdvertisingIdInfo.getId(); // Noncompliant {{Using Advertising ID puts user privacy at risk. Make sure it is safe here.}}
      var advertisingIdInfo = androidx.ads.identifier.AdvertisingIdClient.getIdInfo(holdingConnectionClient);
      var androidxAdsAdvertisingId = advertisingIdInfo.getId(); // Noncompliant

      comHuaweiAdvertisingIdClientInfo.getId(); // Noncompliant {{Using Advertising ID puts user privacy at risk. Make sure it is safe here.}}
      com.huawei.hms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context).getId(); // Noncompliant
    }

    public void compliantScenarios(
      com.google.android.gms.ads.identifier.AdvertisingIdClient.Info comGoogleAdvertisingIdClientInfo,
      androidx.ads.identifier.AdvertisingIdInfo androidxAdsAdvertisingIdInfo,
      com.huawei.hms.ads.identifier.AdvertisingIdClient.Info comHuaweiAdvertisingIdClientInfo
    ) {
      comGoogleAdvertisingIdClientInfo.isLimitAdTrackingEnabled();
      androidxAdsAdvertisingIdInfo.getProviderPackageName();
      comHuaweiAdvertisingIdClientInfo.isLimitAdTrackingEnabled();
    }
  }

  class SettingsSecureTest {

    public static class CustomSettingsSecure {
      public static final String ANDROID_ID = "android_id";

      public static String getString(ContentResolver contentResolver, String name) {
        throw new RuntimeException("not the real Settings.Secure.getString");
      }
    }

    public void nonCompliantScenarios(ContentResolver contentResolver) {
      Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID); // Noncompliant {{Using a non-resettable persistent identifier puts user privacy at risk. Make sure it is safe here.}}
      //              ^^^^^^^^^
      Settings.Secure.getString(contentResolver, (Settings.Secure.ANDROID_ID)); // Noncompliant
      Settings.Secure.getString(contentResolver, "android_id"); // Noncompliant
      Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID); // Noncompliant
      Settings.Secure.getString(contentResolver, Settings.ANDROID_ID); // Noncompliant

      String localSettingName = Settings.Secure.ANDROID_ID;
      Settings.Secure.getString(contentResolver, localSettingName); // FN
      String finalLocalSettingName = Settings.Secure.ANDROID_ID;
      Settings.Secure.getString(contentResolver, finalLocalSettingName); // FN
      Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID.toString()); // FN
      java.util.function.BiFunction<ContentResolver, String, String> getString = Settings.Secure::getString;
      getString.apply(contentResolver, Settings.Secure.ANDROID_ID); // FN
      ((java.util.function.BiFunction<ContentResolver, String, String>) Settings.Secure::getString)
        .apply(contentResolver, Settings.Secure.ANDROID_ID); // FN
    }

    public void compliantScenarios(ContentResolver contentResolver) {
      Settings.Secure.getString(contentResolver, Settings.WIFI_WATCHDOG_PING_COUNT);
      Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID + "suffix");
      CustomSettingsSecure.getString(contentResolver, Settings.Secure.ANDROID_ID);
      Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, "new value");
    }
  }
}
