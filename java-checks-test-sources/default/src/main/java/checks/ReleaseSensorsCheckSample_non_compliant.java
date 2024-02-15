
package checks;

import android.hardware.camera2.CameraDevice;

public class ReleaseSensorsCheckSample_non_compliant {
  public void aquireSensors(
    android.location.LocationManager locationManager,
    android.hardware.SensorManager sensorManager,
    android.os.PowerManager.WakeLock wakeLock,
    android.media.projection.MediaProjection mediaProjection,
    android.net.wifi.WifiManager.MulticastLock multicastLock,
    android.hardware.camera2.CameraManager cameraManager) {

    locationManager.requestLocationUpdates(); // Noncompliant {{Make sure to release this sensor after use.}}
    sensorManager.registerListener(); // Noncompliant
    wakeLock.acquire(); // Noncompliant
    multicastLock.acquire(); // Noncompliant
    mediaProjection.createVirtualDisplay("name", 1, 1, 1, 1, null,null, null); // Noncompliant
    android.hardware.Camera camera = android.hardware.Camera.open(1); // Noncompliant
    android.media.SoundPool soundPool = new android.media.SoundPool.Builder().build(); // Noncompliant
    android.media.audiofx.Visualizer visualizer = new android.media.audiofx.Visualizer(0); // Noncompliant
    android.media.MediaPlayer mediaPlayer = new android.media.MediaPlayer(); // Noncompliant
    android.media.MediaRecorder mediaRecorder = new android.media.MediaRecorder(); // Noncompliant

    cameraManager.openCamera("id", // Noncompliant
      new android.hardware.camera2.CameraDevice.StateCallback() {
        @Override
        public void onDisconnected(CameraDevice camera) {
          // mock implementation
        }

        @Override
        public void onError(CameraDevice camera, int error) {
          // mock implementation
        }

        @Override
        public void onOpened(android.hardware.camera2.CameraDevice camera) {
          // mock implementation
        }
      },
      null);
  }
}
