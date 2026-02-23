/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks;

import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S8461")
public class UseKdfForKeyDerivationCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Use the KDF API instead of %s for key derivation.";

  private static final List<String> PRF = List.of(
    "HmacSHA1",
    "HmacSHA224",
    "HmacSHA256",
    "HmacSHA384",
    "HmacSHA512",
    "HmacSHA512/224",
    "HmacSHA512/256",
    "HmacSHA3-224",
    "HmacSHA3-256",
    "HmacSHA3-384",
    "HmacSHA3-512"
  );
  private static final List<String> KFD_ALGORITHMS = Stream.concat(
    Stream.of("HKDF-SHA256", "HKDF-SHA384", "HKDF-SHA512"),
    PRF.stream().map("PBKDF2With%s"::formatted)
  ).toList();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes("javax.crypto.KeyGenerator").names("getInstance").withAnyParameters().build(),
      MethodMatchers.create().ofTypes("javax.crypto.SecretKeyFactory").names("getInstance").withAnyParameters().build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArg = mit.arguments().get(0);
    String algorithm = ExpressionsHelper.getConstantValueAsString(firstArg).value();
    if (algorithm != null && KFD_ALGORITHMS.contains(algorithm)) {
      String className = mit.methodSymbol().owner().name();
      reportIssue(mit, String.format(MESSAGE, className));
    }
  }

}
