
package checks;

public class ReleaseSensorsCheckSample_non_compliant {
  public void aquireSesnors(
    android.hardware.Camera camera,
    android.location.LocationManager locationManager,
    android.hardware.SensorManager sensorManager,
    android.net.wifi.WifiManager.MulticastLock multicastLock) {

    camera.open(); // Noncompliant {{Make sure to release this sensor when not needed.}}
    locationManager.requestLocationUpdates(); // Noncompliant
    sensorManager.registerListener(); // Noncompliant
    multicastLock.acquire(); // Noncompliant
    android.media.MediaPlayer mediaPlayer = new android.media.MediaPlayer(); // Noncompliant
    android.media.MediaRecorder mediaRecorder = new android.media.MediaRecorder(); // Noncompliant
  }
}
