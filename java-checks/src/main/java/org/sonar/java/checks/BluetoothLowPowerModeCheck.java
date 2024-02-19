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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S6926")
public class BluetoothLowPowerModeCheck extends AbstractMethodDetection {

  private static final int CONNECTION_PRIORITY_LOW_POWER = 2;
  private static final int ADVERTISE_MODE_LOW_POWER = 0;

  private static final MethodMatchers REQUEST_CONNECTION_PRIORITY = MethodMatchers.create()
    .ofSubTypes("android.bluetooth.BluetoothGatt")
    .names("requestConnectionPriority")
    .addParametersMatcher("int")
    .build();

  private static final MethodMatchers SET_ADVERTISE_MODE = MethodMatchers.create()
    .ofSubTypes("android.bluetooth.le.AdvertiseSettings$Builder")
    .names("setAdvertiseMode")
    .addParametersMatcher("int")
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(REQUEST_CONNECTION_PRIORITY, SET_ADVERTISE_MODE);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (firstArgumentNotEqualsTo(mit, "requestConnectionPriority", CONNECTION_PRIORITY_LOW_POWER)
      || firstArgumentNotEqualsTo(mit, "setAdvertiseMode", ADVERTISE_MODE_LOW_POWER)) {

      reportIssue(mit.methodSelect(), "Use the low power mode for this Bluetooth operation.");
    }
  }

  private static boolean firstArgumentNotEqualsTo(MethodInvocationTree mit, String methodName, int expectedValue) {
    return methodName.equals(ExpressionUtils.methodName(mit).name())
      && Optional.ofNullable(ExpressionUtils.resolveAsConstant(mit.arguments().get(0)))
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast)
        .filter(value -> value != expectedValue)
        .isPresent();
  }

}
