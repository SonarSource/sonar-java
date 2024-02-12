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
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6889")
public class ReleaseSensorsCheck extends IssuableSubscriptionVisitor {

  public static final String RELEASE = "release";
  private AcquireReleaseStatus[] statuses;

  public ReleaseSensorsCheck() {
    initStatuses();
  }

  private void initStatuses() {
    this.statuses = IntStream.range(0, AcquireReleaseSensor.values().length)
      .mapToObj(i -> new AcquireReleaseStatus())
      .toArray(AcquireReleaseStatus[]::new);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;

    // collect acquire invocations
    Arrays.stream(AcquireReleaseSensor.values())
      .filter(sensor -> sensor.acquireMethodMatcher.matches(mit))
      .forEach(sensor -> statuses[sensor.ordinal()].acquireInvocations.add(mit));

    // flag released invocations
    Arrays.stream(AcquireReleaseSensor.values())
      .filter(sensor -> sensor.releaseMethodMatcher.matches(mit))
      .forEach(sensor -> statuses[sensor.ordinal()].released = true);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    Arrays.stream(statuses)
      .filter(status -> !status.released)
      .forEach(status -> status.acquireInvocations.forEach(mit -> reportIssue(mit, "Make sure to release this sensor.")));

    initStatuses();
  }

  private static class AcquireReleaseStatus {
    List<MethodInvocationTree> acquireInvocations = new LinkedList<>();
    boolean released = false;
  }

  enum AcquireReleaseSensor {
    CAMERA("android.hardware.Camera", "open", "close"),
    LOCATION_MANAGER("android.location.LocationManager", "requestLocationUpdates", "removeUpdates"),
    SENSOR_MANAGER("android.hardware.SensorManager", "registerListener", "unregisterListener"),
    WIFI_MANAGER("android.net.wifi.WifiManager$MulticastLock", "acquire", RELEASE),
    MEDIA_PLAYER("android.media.MediaPlayer", "MediaPlayer", RELEASE),
    // todo verify if the method matcher can match the constructor!
    MEDIA_RECORDER("android.media.MediaRecorder", "MediaRecorder", RELEASE);
    // todo verify if the method matcher can match the constructor!

    private final MethodMatchers acquireMethodMatcher;
    private final MethodMatchers releaseMethodMatcher;

    AcquireReleaseSensor(String sensorClass, String acquireMethod, String releaseMethod) {
      this.acquireMethodMatcher = MethodMatchers.create().ofTypes(sensorClass).names(acquireMethod).withAnyParameters().build();
      this.releaseMethodMatcher = MethodMatchers.create().ofTypes(sensorClass).names(releaseMethod).withAnyParameters().build();
    }
  }
}
