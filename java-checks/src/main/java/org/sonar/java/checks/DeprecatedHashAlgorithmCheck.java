/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.sonar.java.checks.DeprecatedHashAlgorithmCheck.InsecureAlgorithm.MD2;
import static org.sonar.java.checks.DeprecatedHashAlgorithmCheck.InsecureAlgorithm.MD5;
import static org.sonar.java.checks.DeprecatedHashAlgorithmCheck.InsecureAlgorithm.SHA1;

@Rule(key = "S2070")
public class DeprecatedHashAlgorithmCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  enum InsecureAlgorithm {
    MD2, MD4, MD5, MD6, RIPEMD160,
    SHA1 {
      @Override
      public String toString() {
        return "SHA-1";
      }
    },
    DSA {
      @Override
      boolean match(String algorithm) {
        // exact match required for DSA, so it doesn't match ECDSA
        return "DSA".equals(algorithm);
      }
    };

    boolean match(String algorithm) {
      String normalizedName = algorithm.replaceAll("-", "").toLowerCase(Locale.ENGLISH);
      return normalizedName.contains(name().toLowerCase(Locale.ENGLISH));
    }
  }

  /**
   * These APIs have static getInstance method to get an implementation of some crypto algorithm.
   * javax.crypto.Cipher is missing from this list, because it is covered by rule S2278 {@link AvoidDESCheck}
   * Details can be found here <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html">Security Standard Names</a>
   */
  private static final List<String> CRYPTO_APIS = Arrays.asList(
    "java.security.AlgorithmParameters",
    "java.security.AlgorithmParameterGenerator",
    "java.security.MessageDigest",
    "java.security.KeyFactory",
    "java.security.KeyPairGenerator",
    "java.security.Signature",
    "javax.crypto.Mac",
    "javax.crypto.KeyGenerator"
  );

  private static final Map<String, InsecureAlgorithm> ALGORITHM_BY_METHOD_NAME = ImmutableMap.<String, InsecureAlgorithm>builder()
    .put("getMd2Digest", MD2)
    .put("getMd5Digest", MD5)
    .put("getShaDigest", SHA1)
    .put("getSha1Digest", SHA1)
    .put("md2", MD2)
    .put("md2Hex", MD2)
    .put("md5", MD5)
    .put("md5Hex", MD5)
    .put("sha1", SHA1)
    .put("sha1Hex", SHA1)
    .put("sha", SHA1)
    .put("shaHex", SHA1)
    .build();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    Builder<MethodMatcher> builder = ImmutableList.<MethodMatcher>builder()
      .add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name("getDigest")
        .addParameter(JAVA_LANG_STRING));
    for (String methodName : ALGORITHM_BY_METHOD_NAME.keySet()) {
      builder.add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name(methodName)
        .withAnyParameters());
    }
    for (String cryptoApi : CRYPTO_APIS) {
      builder
        .add(MethodMatcher.create()
          .typeDefinition(cryptoApi)
          .name("getInstance")
          .addParameter(JAVA_LANG_STRING))
        .add(MethodMatcher.create()
          .typeDefinition(cryptoApi)
          .name("getInstance")
          .addParameter(JAVA_LANG_STRING)
          .addParameter(TypeCriteria.anyType()));
    }
    for (String methodName : ImmutableList.of("md5", "sha1")) {
      builder.add(MethodMatcher.create()
        .typeDefinition("com.google.common.hash.Hashing")
        .name(methodName)
        .withoutParameter());
    }
    return builder.build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = MethodsHelper.methodName(mit).name();
    InsecureAlgorithm algorithm = ALGORITHM_BY_METHOD_NAME.get(methodName);
    if (algorithm == null) {
      algorithm = algorithm(mit.arguments().get(0)).orElse(null);
    }
    if (algorithm != null) {
      reportIssue(MethodsHelper.methodName(mit), "Use a stronger hashing algorithm than " + algorithm.toString() + ".");
    }
  }

  private static Optional<InsecureAlgorithm> algorithm(ExpressionTree invocationArgument) {
    ExpressionTree expectedAlgorithm = invocationArgument;
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(invocationArgument);
    if (defaultPropertyValue != null) {
      expectedAlgorithm = defaultPropertyValue;
    }
    if (expectedAlgorithm.is(Tree.Kind.STRING_LITERAL)) {
      String algorithmName = LiteralUtils.trimQuotes(((LiteralTree) expectedAlgorithm).value());
      return Arrays.stream(InsecureAlgorithm.values())
        .filter(alg -> alg.match(algorithmName))
        .findFirst();
    }
    return Optional.empty();
  }

}
