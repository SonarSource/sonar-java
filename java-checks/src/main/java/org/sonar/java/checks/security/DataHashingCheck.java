/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4790")
public class DataHashingCheck extends AbstractMethodDetection {

  private static final String DIGEST_UTILS = "org.apache.commons.codec.digest.DigestUtils";
  private static final String MESSAGE = "Make sure that hashing data is safe here.";
  private static final String SECRET_KEY_FACTORY = "javax.crypto.SecretKeyFactory";

  private static final List<String> HASH_NAMES = Arrays.asList("md5", "sha1", "sha256", "sha384", "sha512");

  private static final Set<String> DIGEST_HASH_NAMES = Stream.of("Md2", "Md5", "Sha", "Sha1", "Sha256", "Sha384", "Sha512")
    .flatMap(alg -> Stream.of("get" + alg + "Digest", alg.toLowerCase(Locale.ENGLISH), alg.toLowerCase(Locale.ENGLISH) + "Hex"))
    .collect(Collectors.toSet());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes("java.security.MessageDigest").names("getInstance").withAnyParameters().build(),
      MethodMatchers.create().ofTypes(DIGEST_UTILS).constructor().withAnyParameters().build(),
      MethodMatchers.create().ofTypes(DIGEST_UTILS).name(DIGEST_HASH_NAMES::contains).withAnyParameters().build(),
      MethodMatchers.create().ofTypes("com.google.common.hash.Hashing").name(HASH_NAMES::contains).addWithoutParametersMatcher().build(),
      MethodMatchers.create().ofTypes(SECRET_KEY_FACTORY).names("getInstance").withAnyParameters().build()
    );
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
