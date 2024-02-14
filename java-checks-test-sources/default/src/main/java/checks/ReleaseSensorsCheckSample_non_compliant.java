
package checks;

public class ReleaseSensorsCheckSample_non_compliant {
  public void aquireSensors(
    android.location.LocationManager locationManager,
    android.hardware.SensorManager sensorManager,
    android.net.wifi.WifiManager.MulticastLock multicastLock) {

    locationManager.requestLocationUpdates(); // Noncompliant {{Make sure to release this sensor after use.}}
    sensorManager.registerListener(); // Noncompliant
    multicastLock.acquire(); // Noncompliant
    android.hardware.Camera camera = android.hardware.Camera.open(1); // Noncompliant
    android.media.MediaPlayer mediaPlayer = new android.media.MediaPlayer(); // Noncompliant
    android.media.MediaRecorder mediaRecorder = new android.media.MediaRecorder(); // Noncompliant
  }
}
