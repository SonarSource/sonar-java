/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S6293")
public class AndroidBiometricAuthWithoutCryptoCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    // no need to check method calls with CryptoObject null, NonNull required
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("android.hardware.biometrics.BiometricPrompt")
        .names("authenticate")
        .addParametersMatcher("android.os.CancellationSignal", "java.util.concurrent.Executor", "android.hardware.biometrics.BiometricPrompt$AuthenticationCallback")
        .build(),
      MethodMatchers.create()
        .ofTypes("androidx.biometric.BiometricPrompt")
        .names("authenticate")
        .addParametersMatcher("androidx.biometric.BiometricPrompt$PromptInfo")
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), "Make sure performing a biometric authentication without a \"CryptoObject\" is safe here.");
  }
}
