package checks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.AdvertiseSettings;

public class BluetoothLowPowerModeCheckSample {

  private static int connectionPriorityHigh = 1;
  private int advertiseModeBalanced = 1;


  public void nonCompliant(android.bluetooth.BluetoothGatt gatt, android.bluetooth.le.AdvertiseSettings.Builder builder) {
    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH); // Noncompliant {{Use the low power mode for this Bluetooth operation.}}
    gatt.requestConnectionPriority(1); // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    builder.setAdvertiseMode(android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_BALANCED); // Noncompliant {{Use the low power mode for this Bluetooth operation.}}
    builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY); // Noncompliant
    builder.setAdvertiseMode(1); // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^
  }

  public void compliant(android.bluetooth.BluetoothGatt gatt, android.bluetooth.le.AdvertiseSettings.Builder builder) {
    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
    gatt.requestConnectionPriority(2);
    gatt.requestConnectionPriority(connectionPriorityHigh); // FN

    builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
    builder.setAdvertiseMode(0);
    builder.setAdvertiseMode(advertiseModeBalanced); // FN
  }
}
