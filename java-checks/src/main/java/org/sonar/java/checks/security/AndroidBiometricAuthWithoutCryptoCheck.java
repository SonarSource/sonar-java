/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
