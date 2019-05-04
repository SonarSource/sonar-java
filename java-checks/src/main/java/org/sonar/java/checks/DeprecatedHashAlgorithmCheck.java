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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.DeprecatedHashAlgorithmCheck.InsecureAlgorithm.MD2;
import static org.sonar.java.checks.DeprecatedHashAlgorithmCheck.InsecureAlgorithm.MD5;
import static org.sonar.java.checks.DeprecatedHashAlgorithmCheck.InsecureAlgorithm.SHA1;

@Rule(key = "S2070")
public class DeprecatedHashAlgorithmCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String GET_INSTANCE = "getInstance";
  private static final String CONSTRUCTOR = "<init>";

  enum InsecureAlgorithm {
    MD2, MD4, MD5, MD6, RIPEMD,
    HAVAL128 {
      @Override
      public String toString() {
        return "HAVAL-128";
      }
    },
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

  private enum DeprecatedSpringPasswordEncoder {
    MD5("org.springframework.security.authentication.encoding.Md5PasswordEncoder", CONSTRUCTOR),
    SHA("org.springframework.security.authentication.encoding.ShaPasswordEncoder", CONSTRUCTOR),
    LDAP("org.springframework.security.crypto.password.LdapShaPasswordEncoder", CONSTRUCTOR),
    MD4("org.springframework.security.crypto.password.Md4PasswordEncoder", CONSTRUCTOR),
    MESSAGE_DIGEST("org.springframework.security.crypto.password.MessageDigestPasswordEncoder", CONSTRUCTOR),
    STANDARD("org.springframework.security.crypto.password.StandardPasswordEncoder", CONSTRUCTOR),
    NO_OP("org.springframework.security.crypto.password.NoOpPasswordEncoder", GET_INSTANCE);

    private static final String MESSAGE_FORMAT = "Don't rely on %s because it is deprecated and use a stronger hashing algorithm.";
    protected static final Map<String, String> MESSAGE_PER_CLASS;
    static {
      MESSAGE_PER_CLASS = new HashMap<>();
      MESSAGE_PER_CLASS.put(MD5.classFqn, "Use a stronger hashing algorithm than MD5.");
      MESSAGE_PER_CLASS.put(SHA.classFqn, "Don't rely on " + SHA.className + " because it is deprecated.");
      MESSAGE_PER_CLASS.put(LDAP.classFqn, String.format(MESSAGE_FORMAT, LDAP.className));
      MESSAGE_PER_CLASS.put(MD4.classFqn, String.format(MESSAGE_FORMAT, MD4.className));
      MESSAGE_PER_CLASS.put(MESSAGE_DIGEST.classFqn, String.format(MESSAGE_FORMAT, MESSAGE_DIGEST.className));
      MESSAGE_PER_CLASS.put(NO_OP.classFqn, "Use a stronger hashing algorithm than this fake one.");
      MESSAGE_PER_CLASS.put(STANDARD.classFqn, "Use a stronger hashing algorithm.");
    }

    private final String classFqn;
    private final String methodName;
    private final String className;
    DeprecatedSpringPasswordEncoder(String fqn, String methodName) {
      this.classFqn = fqn;
      this.methodName = methodName;
      String[] fqnParts = fqn.split("\\.");
      this.className = fqnParts[fqnParts.length - 1];
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
    ArrayList<MethodMatcher> matchers = new ArrayList<>();
    matchers
      .add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name("getDigest")
        .addParameter(JAVA_LANG_STRING));
    for (String methodName : ALGORITHM_BY_METHOD_NAME.keySet()) {
      matchers.add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name(methodName)
        .withAnyParameters());
    }
    for (String cryptoApi : CRYPTO_APIS) {
      matchers
        .add(MethodMatcher.create()
          .typeDefinition(cryptoApi)
          .name(GET_INSTANCE)
          .addParameter(JAVA_LANG_STRING));
      matchers
        .add(MethodMatcher.create()
          .typeDefinition(cryptoApi)
          .name(GET_INSTANCE)
          .addParameter(JAVA_LANG_STRING)
          .addParameter(TypeCriteria.anyType()));
    }
    for (DeprecatedSpringPasswordEncoder pe : DeprecatedSpringPasswordEncoder.values()) {
      matchers.add(MethodMatcher.create().typeDefinition(pe.classFqn).name(pe.methodName).withAnyParameters());
    }
    for (String methodName : ImmutableList.of("md5", "sha1")) {
      matchers.add(MethodMatcher.create()
        .typeDefinition("com.google.common.hash.Hashing")
        .name(methodName)
        .withoutParameter());
    }
    return matchers;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    String message = DeprecatedSpringPasswordEncoder.MESSAGE_PER_CLASS.get(methodName.symbol().owner().type().fullyQualifiedName());
    if (message != null) {
      reportIssue(methodName, message);
      return;
    }
    InsecureAlgorithm algorithm = ALGORITHM_BY_METHOD_NAME.get(methodName.name());
    if (algorithm == null) {
      algorithm = algorithm(mit.arguments().get(0)).orElse(null);
    }
    if (algorithm != null) {
      reportIssue(methodName, "Use a stronger hashing algorithm than " + algorithm.toString() + ".");
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    String message = DeprecatedSpringPasswordEncoder.MESSAGE_PER_CLASS.get(newClassTree.identifier().symbolType().fullyQualifiedName());
    reportIssue(newClassTree.identifier(), message);
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
