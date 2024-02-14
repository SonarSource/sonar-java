package checks;

import android.hardware.camera2.CameraDevice;

public class ReleaseSensorsCheckSample_compliant {

  public static class MockedSensorsExample {
    public void acquireSensors(
      android.location.LocationManager locationManager,
      android.hardware.SensorManager sensorManager,
      android.os.PowerManager.WakeLock wakeLock,
      android.net.wifi.WifiManager.MulticastLock multicastLock,
      android.hardware.camera2.CameraManager cameraManager) {

      locationManager.requestLocationUpdates();
      sensorManager.registerListener();
      wakeLock.acquire();
      multicastLock.acquire();
      new android.media.SoundPool.Builder().build();
      new android.media.audiofx.Visualizer(0);
      android.hardware.Camera.open(1);
      new android.media.MediaPlayer();
      new android.media.MediaRecorder();

      cameraManager.openCamera("id",
        new android.hardware.camera2.CameraDevice.StateCallback() {
          @Override
          public void onDisconnected(CameraDevice camera) {
            camera.close();
          }

          @Override
          public void onError(CameraDevice camera, int error) {
            camera.close();
          }

          @Override
          public void onOpened(android.hardware.camera2.CameraDevice camera) {
            // mock implementation
          }
        },
        null);
    }

    public void releaseSensors(
      android.location.LocationManager locationManager,
      android.hardware.SensorManager sensorManager,
      android.os.PowerManager.WakeLock wakeLock,
      android.net.wifi.WifiManager.MulticastLock multicastLock,
      android.hardware.Camera camera,
      android.media.SoundPool soundPool,
      android.media.audiofx.Visualizer visualizer,
      android.media.MediaPlayer mediaPlayer,
      android.media.MediaRecorder mediaRecorder) {

      camera.release();
      locationManager.removeUpdates();
      sensorManager.unregisterListener();
      wakeLock.release();
      multicastLock.release();
      soundPool.release();
      visualizer.release();
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
