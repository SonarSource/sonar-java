package checks;

import android.media.MediaPlayer;
import android.media.MediaRecorder;

public class ReleaseSensorsCheckSample_compliant {

  public static class MockedSensorsExample {
    public void acquireSensors(
      android.hardware.Camera camera,
      android.location.LocationManager locationManager,
      android.hardware.SensorManager sensorManager,
      android.net.wifi.WifiManager.MulticastLock multicastLock) {

      camera.open();
      locationManager.requestLocationUpdates();
      sensorManager.registerListener();
      multicastLock.acquire();
      android.media.MediaPlayer mediaPlayer = new MediaPlayer();
      android.media.MediaRecorder mediaRecorder = new MediaRecorder();
    }

    public void releaseSensors(
      android.hardware.Camera camera,
      android.location.LocationManager locationManager,
      android.hardware.SensorManager sensorManager,
      android.net.wifi.WifiManager.MulticastLock multicastLock,
      android.media.MediaPlayer mediaPlayer,
      android.media.MediaRecorder mediaRecorder) {

      camera.release();
      locationManager.removeUpdates();
      sensorManager.unregisterListener();
      multicastLock.release();
      mediaPlayer.release();
      mediaRecorder.release();
    }
  }

  public static class FakeSensorExample {
    public void test() {
      FakeSensor fakeSensor = new FakeSensor();
      fakeSensor.acquire(); // Compliant
    }

    public static class FakeSensor {
      public void acquire() {
        // mock implementation
      }

      public void release() {
        // mock implementation
      }
    }
  }
}
