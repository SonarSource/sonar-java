/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4790")
public class DataHashingCheck extends AbstractMethodDetection {

  private static final String DIGEST_UTILS = "org.apache.commons.codec.digest.DigestUtils";
  private static final String MESSAGE = "Make sure that hashing data is safe here.";
  private static final String SECRET_KEY_FACTORY = "javax.crypto.SecretKeyFactory";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    List<MethodMatcher> matchers = new ArrayList<>();
    matchers.add(MethodMatcher.create().typeDefinition("java.security.MessageDigest").name("getInstance").withAnyParameters());
    matchers.add(MethodMatcher.create().typeDefinition(DIGEST_UTILS).name("<init>").withAnyParameters());
    matchers.addAll(
      Stream.of("Md2", "Md5", "Sha", "Sha1", "Sha256", "Sha384", "Sha512")
        .flatMap(alg -> Stream.of("get" + alg + "Digest", alg.toLowerCase(Locale.ENGLISH), alg.toLowerCase(Locale.ENGLISH) + "Hex"))
        .map(name -> MethodMatcher.create().typeDefinition(DIGEST_UTILS).name(name).withAnyParameters())
        .collect(Collectors.toList()));
    matchers.addAll(
      Stream.of("md5", "sha1", "sha256", "sha384", "sha512")
        .map(alg -> MethodMatcher.create().typeDefinition("com.google.common.hash.Hashing").name(alg).withoutParameter())
        .collect(Collectors.toList()));
    matchers.add(MethodMatcher.create().typeDefinition(SECRET_KEY_FACTORY).name("getInstance").withAnyParameters());
    return matchers;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.symbol().owner().type().is(SECRET_KEY_FACTORY)) {
      String algorithm = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
      if (algorithm == null || !algorithm.startsWith("PBKDF2")) {
        return;
      }
    }
    reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree.identifier(), MESSAGE);
  }
}
