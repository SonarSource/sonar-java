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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

public abstract class AbstractHashAlgorithmChecker extends AbstractMethodDetection {

  public static final String GET_INSTANCE = "getInstance";
  public static final String JAVA_LANG_STRING = "java.lang.String";

  protected static final Map<String, InsecureAlgorithm> ALGORITHM_BY_METHOD_NAME = MapBuilder.<String, InsecureAlgorithm>newMap()
    .put("getMd2Digest", InsecureAlgorithm.MD2)
    .put("getMd5Digest", InsecureAlgorithm.MD5)
    .put("getShaDigest", InsecureAlgorithm.SHA1)
    .put("getSha1Digest", InsecureAlgorithm.SHA1)
    .put("md2", InsecureAlgorithm.MD2)
    .put("md2Hex", InsecureAlgorithm.MD2)
    .put("md5", InsecureAlgorithm.MD5)
    .put("md5Hex", InsecureAlgorithm.MD5)
    .put("sha1", InsecureAlgorithm.SHA1)
    .put("sha1Hex", InsecureAlgorithm.SHA1)
    .put("sha", InsecureAlgorithm.SHA1)
    .put("shaHex", InsecureAlgorithm.SHA1)
    .put("md5Digest", InsecureAlgorithm.MD5)
    .put("md5DigestAsHex", InsecureAlgorithm.MD5)
    .put("appendMd5DigestAsHex", InsecureAlgorithm.MD5)
    .build();

  private static final String CONSTRUCTOR = "<init>";

  /**
   * These APIs have static getInstance method to get an implementation of some crypto algorithm.
   * javax.crypto.Cipher is missing from this list, because it is covered by rule S5547 {@link StrongCipherAlgorithmCheck}
   * Details can be found here <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html">Security Standard Names</a>
   */
  private static final String[] CRYPTO_APIS = {
    "java.security.AlgorithmParameters",
    "java.security.AlgorithmParameterGenerator",
    "java.security.MessageDigest",
    "java.security.KeyFactory",
    "java.security.KeyPairGenerator",
    "java.security.Signature",
    "javax.crypto.Mac",
    "javax.crypto.KeyGenerator"
  };

  public enum InsecureAlgorithm {
    MD2, MD4, MD5, MD6, RIPEMD,
    HAVAL128 {
      @Override
      public String toString() {
        return "HAVAL-128";
      }
    },
    SHA {
      @Override
      public boolean match(String algorithm) {
        // exact match required for SHA, so it doesn't match compliant SHA-512
        return "SHA".equals(algorithm);
      }
    },
    SHA0 {
      @Override
      public String toString() {
        return "SHA-0";
      }
    },
    SHA1 {
      @Override
      public String toString() {
        return "SHA-1";
      }
    },
    SHA224 {
      @Override
      public String toString() {
        return "SHA-224";
      }
    },
    DSA {
      @Override
      public boolean match(String algorithm) {
        // exact match required for DSA, so it doesn't match ECDSA
        return "DSA".equals(algorithm);
      }
    };

    public boolean match(String algorithm) {
      String normalizedName = algorithm.replace("-", "").toLowerCase(Locale.ENGLISH);
      return normalizedName.contains(name().toLowerCase(Locale.ENGLISH));
    }
  }

  public enum DeprecatedSpringPasswordEncoder {
    MD5("org.springframework.security.authentication.encoding.Md5PasswordEncoder", CONSTRUCTOR),
    SHA("org.springframework.security.authentication.encoding.ShaPasswordEncoder", CONSTRUCTOR),
    LDAP("org.springframework.security.crypto.password.LdapShaPasswordEncoder", CONSTRUCTOR),
    MD4("org.springframework.security.crypto.password.Md4PasswordEncoder", CONSTRUCTOR),
    MESSAGE_DIGEST("org.springframework.security.crypto.password.MessageDigestPasswordEncoder", CONSTRUCTOR),
    STANDARD("org.springframework.security.crypto.password.StandardPasswordEncoder", CONSTRUCTOR),
    NO_OP("org.springframework.security.crypto.password.NoOpPasswordEncoder", GET_INSTANCE);

    public final String classFqn;
    public final String methodName;
    public final String className;

    DeprecatedSpringPasswordEncoder(String fqn, String methodName) {
      this.classFqn = fqn;
      this.methodName = methodName;
      String[] fqnParts = fqn.split("\\.");
      this.className = fqnParts[fqnParts.length - 1];
    }
  }

  protected abstract Optional<String> getMessageForClass(String className);
  protected abstract String getMessageForAlgorithm(String algorithmName);

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return getWeakHashMethodInvocationMatchers();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    Optional<String> message = getMessageForClass(methodName.symbol().owner().type().fullyQualifiedName());
    if (message.isPresent()) {
      reportIssue(methodName, message.get());
      return;
    }
    InsecureAlgorithm algorithm = ALGORITHM_BY_METHOD_NAME.get(methodName.name());
    if (algorithm == null) {
      algorithm = algorithm(mit.arguments().get(0)).orElse(null);
    }
    if (algorithm != null) {
      reportIssue(methodName, getMessageForAlgorithm(algorithm.toString()));
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    getMessageForClass(newClassTree.identifier().symbolType().fullyQualifiedName())
      .ifPresent(message ->  reportIssue(newClassTree.identifier(), message));
  }

  private static MethodMatchers getWeakHashMethodInvocationMatchers() {
    ArrayList<MethodMatchers> matchers = new ArrayList<>();
    matchers
      .add(MethodMatchers.create()
        .ofTypes("org.apache.commons.codec.digest.DigestUtils")
        .names("getDigest")
        .addParametersMatcher(JAVA_LANG_STRING)
        .build());

    matchers
      .add(MethodMatchers.create()
        .ofTypes("org.apache.commons.codec.digest.DigestUtils")
        .name(ALGORITHM_BY_METHOD_NAME::containsKey)
        .withAnyParameters()
        .build());

    matchers
      .add(MethodMatchers.create()
        .ofTypes(CRYPTO_APIS)
        .names(GET_INSTANCE)
        .addParametersMatcher(JAVA_LANG_STRING)
        .addParametersMatcher(JAVA_LANG_STRING, ANY)
        .build());

    matchers
      .add(MethodMatchers.create()
        .ofTypes("org.springframework.util.DigestUtils")
        .names("appendMd5DigestAsHex", "md5Digest", "md5DigestAsHex")
        .withAnyParameters()
        .build());

    for (DeprecatedSpringPasswordEncoder pe : DeprecatedSpringPasswordEncoder.values()) {
      matchers.add(MethodMatchers.create().ofTypes(pe.classFqn).names(pe.methodName).withAnyParameters().build());
    }

    matchers.add(MethodMatchers.create()
      .ofTypes("com.google.common.hash.Hashing")
      .names("md5", "sha1")
      .addWithoutParametersMatcher().build());

    return MethodMatchers.or(matchers);
  }

  private static Optional<InsecureAlgorithm> algorithm(ExpressionTree invocationArgument) {
    ExpressionTree expectedAlgorithm = invocationArgument;
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(invocationArgument);
    if (defaultPropertyValue != null) {
      expectedAlgorithm = defaultPropertyValue;
    }
    Optional<String> stringConstant = expectedAlgorithm.asConstant(String.class);
    if (stringConstant.isPresent()) {
      String algorithmName = stringConstant.get();
      return Arrays.stream(InsecureAlgorithm.values())
        .filter(alg -> alg.match(algorithmName))
        .findFirst();
    }
    return Optional.empty();
  }

}
