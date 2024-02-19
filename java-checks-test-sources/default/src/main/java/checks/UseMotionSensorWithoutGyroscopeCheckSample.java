package checks;

import android.hardware.Sensor;
import android.hardware.SensorManager;

public class UseMotionSensorWithoutGyroscopeCheckSample {

  void compliant(SensorManager sensorManager) {
    sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR); // Noncompliant {{Replace `TYPE_ROTATION_VECTOR` (11) with `TYPE_GEOMAGNETIC_ROTATION_VECTOR` (20) to optimize battery life.}}
    sensorManager.getDefaultSensor(11); // Noncompliant [[sc=5;ec=39]]
    sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR, true); // Noncompliant
    sensorManager.getDefaultSensor(11, false); // Noncompliant [[sc=5;ec=46]]
  }

  void nonCompliant(SensorManager sensorManager) {
    sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
    sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, true);
    sensorManager.getDefaultSensor(20);
    sensorManager.getDefaultSensor(20, false);
    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    sensorManager.getDefaultSensor(19);
  }
}
