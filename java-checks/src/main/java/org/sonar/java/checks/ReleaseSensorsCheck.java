/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Rule S6889 flags issues when any sensor defined in {@link AcquireReleaseSensor} is not released.
 * The intent of this rule is to make the developer aware of the need to release the sensor when it is not needed anymore.
 * <p>
 * The implemented logic checks that after acquiring a sensor there is at least one release invocation within the same file.
 */
@Rule(key = "S6889")
public class ReleaseSensorsCheck extends IssuableSubscriptionVisitor {

  private enum AcquireReleaseSensor {
    LOCATION_MANAGER("android.location.LocationManager", "requestLocationUpdates", "removeUpdates"),
    SENSOR_MANAGER("android.hardware.SensorManager", "registerListener", "unregisterListener"),
    VIRTUAL_DISPLAY("android.media.projection.MediaProjection", "createVirtualDisplay", "android.hardware.display.VirtualDisplay", RELEASE),
    CAMERA("android.hardware.Camera", "open", RELEASE),
    CAMERA2("android.hardware.camera2.CameraManager", "openCamera", "android.hardware.camera2.CameraDevice", "close"),
    POWER_MANAGER("android.os.PowerManager$WakeLock", ACQUIRE, RELEASE),
    WIFI_MANAGER("android.net.wifi.WifiManager$MulticastLock", ACQUIRE, RELEASE),
    SOUND_POOL("android.media.SoundPool$Builder", "build", "android.media.SoundPool", RELEASE),
    VISUALIZER("android.media.audiofx.Visualizer", MethodMatchers.CONSTRUCTOR, RELEASE),
    MEDIA_PLAYER("android.media.MediaPlayer", MethodMatchers.CONSTRUCTOR, RELEASE),
    MEDIA_RECORDER("android.media.MediaRecorder", MethodMatchers.CONSTRUCTOR, RELEASE);

    private final MethodMatchers acquireMethodMatcher;
    private final MethodMatchers releaseMethodMatcher;

    AcquireReleaseSensor(String sensorClass, String acquireMethod, String releaseMethod) {
      this.acquireMethodMatcher = MethodMatchers.create().ofTypes(sensorClass).names(acquireMethod).withAnyParameters().build();
      this.releaseMethodMatcher = MethodMatchers.create().ofTypes(sensorClass).names(releaseMethod).withAnyParameters().build();
    }

    AcquireReleaseSensor(String acquireSensorClass, String acquireMethod, String releaseSensorClass, String releaseMethod) {
      this.acquireMethodMatcher = MethodMatchers.create().ofTypes(acquireSensorClass).names(acquireMethod).withAnyParameters().build();
      this.releaseMethodMatcher = MethodMatchers.create().ofTypes(releaseSensorClass).names(releaseMethod).withAnyParameters().build();
    }
  }

  private static class AcquireReleaseStatus {
    List<Tree> acquireInvocations = new LinkedList<>();
    boolean released = false;
  }

  private static final String ACQUIRE = "acquire";
  private static final String RELEASE = "release";
  private AcquireReleaseStatus[] statuses;

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    initStatuses();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    Arrays.stream(statuses)
      .filter(status -> !status.released)
      .forEach(status -> status.acquireInvocations.forEach(mit -> reportIssue(mit, "Make sure to release this sensor after use.")));
    statuses = null;
  }

  private void initStatuses() {
    this.statuses = IntStream.range(0, AcquireReleaseSensor.values().length)
      .mapToObj(i -> new AcquireReleaseStatus())
      .toArray(AcquireReleaseStatus[]::new);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    // collect acquire invocations
    Arrays.stream(AcquireReleaseSensor.values())
      .filter(sensor -> isMethodMatched(tree, sensor.acquireMethodMatcher))
      .findAny()
      .ifPresent(sensor -> statuses[sensor.ordinal()].acquireInvocations.add(tree));

    // flag released invocations
    Arrays.stream(AcquireReleaseSensor.values())
      .filter(sensor -> isMethodMatched(tree, sensor.releaseMethodMatcher))
      .findAny()
      .ifPresent(sensor -> statuses[sensor.ordinal()].released = true);
  }

  private static boolean isMethodMatched(Tree tree, MethodMatchers methodMatchers) {
    switch (tree.kind()) {
      case METHOD_INVOCATION:
        return methodMatchers.matches((MethodInvocationTree) tree);
      case NEW_CLASS:
        return methodMatchers.matches((NewClassTree) tree);
      default:
        return false;
    }
  }
}
