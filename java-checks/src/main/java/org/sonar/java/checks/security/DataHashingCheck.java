/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonarsource.analyzer.commons.collections.SetUtils;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S4790")
public class DataHashingCheck extends AbstractMethodDetection {

  private static final String GET_INSTANCE = "getInstance";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String CONSTRUCTOR = "<init>";

  private static final Map<String, InsecureAlgorithm> ALGORITHM_BY_METHOD_NAME = MapBuilder.<String, InsecureAlgorithm>newMap()
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

  /**
   * These APIs have static getInstance method to get an implementation of some crypto algorithm.
   * javax.crypto.Cipher is missing from this list, because it is covered by rule S5547 {@link org.sonar.java.checks.StrongCipherAlgorithmCheck}
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

  private enum InsecureAlgorithm {
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

  private enum DeprecatedSpringPasswordEncoder {
    MD5("org.springframework.security.authentication.encoding.Md5PasswordEncoder", CONSTRUCTOR),
    SHA("org.springframework.security.authentication.encoding.ShaPasswordEncoder", CONSTRUCTOR),
    LDAP("org.springframework.security.crypto.password.LdapShaPasswordEncoder", CONSTRUCTOR),
    MD4("org.springframework.security.crypto.password.Md4PasswordEncoder", CONSTRUCTOR),
    MESSAGE_DIGEST("org.springframework.security.crypto.password.MessageDigestPasswordEncoder", CONSTRUCTOR),
    STANDARD("org.springframework.security.crypto.password.StandardPasswordEncoder", CONSTRUCTOR),
    NO_OP("org.springframework.security.crypto.password.NoOpPasswordEncoder", GET_INSTANCE);

    private final String classFqn;
    private final String methodName;

    DeprecatedSpringPasswordEncoder(String fqn, String methodName) {
      this.classFqn = fqn;
      this.methodName = methodName;
    }
  }

  private static final Set<String> DEPRECATED_HASH_CLASSES = SetUtils.immutableSetOf(
    DeprecatedSpringPasswordEncoder.MD5.classFqn,
    DeprecatedSpringPasswordEncoder.SHA.classFqn,
    DeprecatedSpringPasswordEncoder.LDAP.classFqn,
    DeprecatedSpringPasswordEncoder.MD4.classFqn,
    DeprecatedSpringPasswordEncoder.MESSAGE_DIGEST.classFqn,
    DeprecatedSpringPasswordEncoder.NO_OP.classFqn,
    DeprecatedSpringPasswordEncoder.STANDARD.classFqn
  );

  private static final Set<InsecureAlgorithm> EXEMPTABLE_ALGORITHMS = EnumSet.of(InsecureAlgorithm.MD5, InsecureAlgorithm.SHA, InsecureAlgorithm.SHA1);

  private static final Set<String> DATA_SINKS = Set.of("update", "digest", "doFinal", "hashBytes");
  private static final Set<String> FINALIZERS = Set.of("digest", "doFinal");
  private static final Set<String> NEUTRAL_CALLS = Set.of("init", "reset");

  private static final MethodMatchers ONE_SHOT_DIGESTS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("org.apache.commons.codec.digest.DigestUtils")
      .names("md5", "md5Hex", "sha1", "sha1Hex", "sha", "shaHex")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.springframework.util.DigestUtils")
      .names("md5Digest", "md5DigestAsHex", "appendMd5DigestAsHex")
      .withAnyParameters()
      .build());

  private static final MethodMatchers DIGEST_FACTORIES = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.security.MessageDigest", "javax.crypto.Mac")
      .names(GET_INSTANCE)
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.apache.commons.codec.digest.DigestUtils")
      .names("getDigest", "getMd5Digest", "getShaDigest", "getSha1Digest")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("com.google.common.hash.Hashing")
      .names("md5", "sha1")
      .addWithoutParametersMatcher()
      .build());

  private static final MethodMatchers FILE_SOURCES = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.nio.file.Files")
      .names("newInputStream", "readAllBytes", "readString")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.apache.commons.io.FileUtils")
      .names("readFileToByteArray")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.io.FileInputStream", "java.io.FileReader")
      .constructor()
      .withAnyParameters()
      .build());

  private static final MethodMatchers CHANNEL_STREAM = MethodMatchers.create()
    .ofTypes("java.nio.channels.Channels")
    .names("newInputStream")
    .withAnyParameters()
    .build();

  private static final MethodMatchers TO_BYTE_ARRAY = MethodMatchers.create()
    .ofTypes("org.apache.commons.io.IOUtils")
    .names("toByteArray")
    .withAnyParameters()
    .build();

  private static final MethodMatchers STREAM_WRAPPERS = MethodMatchers.create()
    .ofTypes("java.io.BufferedInputStream", "java.security.DigestInputStream")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers DIGEST_INPUT_STREAM = MethodMatchers.create()
    .ofTypes("java.security.DigestInputStream")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers READ_ALL_BYTES = MethodMatchers.create()
    .ofSubTypes("java.io.InputStream")
    .names("readAllBytes")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers AS_BYTE_SOURCE = MethodMatchers.create()
    .ofTypes("com.google.common.io.Files")
    .names("asByteSource")
    .withAnyParameters()
    .build();

  private static final MethodMatchers BYTE_SOURCE_READ = MethodMatchers.create()
    .ofSubTypes("com.google.common.io.ByteSource")
    .names("read")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers BYTE_SOURCE_HASH = MethodMatchers.create()
    .ofSubTypes("com.google.common.io.ByteSource")
    .names("hash")
    .withAnyParameters()
    .build();

  private static final String MESSAGE = "Make sure this weak hash algorithm is not used in a sensitive context here.";

  private enum Use {
    SINK,
    NEUTRAL,
    OTHER
  }

  /**
   * MD5 and SHA-1 are exempted only when every byte reaching the digest is proven to come from a file.
   * Anything that cannot be proven (unknown symbol, field, digest passed to another method, mixed data) is still reported.
   */
  private static boolean isExempt(MethodInvocationTree mit, InsecureAlgorithm algorithm) {
    if (!EXEMPTABLE_ALGORITHMS.contains(algorithm)) {
      return false;
    }
    if (ONE_SHOT_DIGESTS.matches(mit)) {
      return !mit.arguments().isEmpty() && isFileSourced(mit.arguments().get(0));
    }
    return DIGEST_FACTORIES.matches(mit) && isOnlyFedFileData(mit);
  }

  private static boolean isOnlyFedFileData(MethodInvocationTree factoryCall) {
    Tree parent = factoryCall.parent();
    if (parent.is(Tree.Kind.VARIABLE) && ((VariableTree) parent).initializer() == factoryCall) {
      Symbol variable = ((VariableTree) parent).symbol();
      if (!variable.isVariableSymbol() || !variable.owner().isMethodSymbol()) {
        return false;
      }
      boolean hasSink = false;
      for (IdentifierTree usage : variable.usages()) {
        Use use = classify(usage);
        if (use == Use.OTHER) {
          return false;
        }
        hasSink |= use == Use.SINK;
      }
      return hasSink;
    }
    return classify(factoryCall) == Use.SINK;
  }

  private static Use classify(ExpressionTree use) {
    if (use.parent() instanceof MemberSelectExpressionTree memberSelect
      && memberSelect.expression() == use
      && memberSelect.parent() instanceof MethodInvocationTree invocation) {
      return classifyCall(memberSelect.identifier().name(), invocation.arguments());
    }
    if (use.parent().is(Tree.Kind.ARGUMENTS)) {
      return classifyArgument(use, use.parent().parent());
    }
    return Use.OTHER;
  }

  private static Use classifyCall(String name, Arguments arguments) {
    if (DATA_SINKS.contains(name) && !arguments.isEmpty()) {
      return isFileSourced(arguments.get(0)) ? Use.SINK : Use.OTHER;
    }
    if (NEUTRAL_CALLS.contains(name) || (arguments.isEmpty() && FINALIZERS.contains(name))) {
      return Use.NEUTRAL;
    }
    return Use.OTHER;
  }

  private static Use classifyArgument(ExpressionTree argument, Tree owner) {
    if (owner.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClass = (NewClassTree) owner;
      Arguments arguments = newClass.arguments();
      boolean feedsFromFileStream = DIGEST_INPUT_STREAM.matches(newClass)
        && arguments.size() == 2
        && arguments.get(1) == argument
        && isFileSourced(arguments.get(0));
      return feedsFromFileStream ? Use.SINK : Use.OTHER;
    }
    if (owner.is(Tree.Kind.METHOD_INVOCATION) && BYTE_SOURCE_HASH.matches((MethodInvocationTree) owner)) {
      ExpressionTree receiver = receiver((MethodInvocationTree) owner);
      return receiver != null && isFileSourced(receiver) ? Use.SINK : Use.OTHER;
    }
    return Use.OTHER;
  }

  private static boolean isFileSourced(ExpressionTree expression) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      return isFileSourcedVariable(((IdentifierTree) expr).symbol());
    }
    if (expr.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClass = (NewClassTree) expr;
      return FILE_SOURCES.matches(newClass)
        || (STREAM_WRAPPERS.matches(newClass) && !newClass.arguments().isEmpty() && isFileSourced(newClass.arguments().get(0)));
    }
    if (expr.is(Tree.Kind.METHOD_INVOCATION)) {
      return isFileSourcedCall((MethodInvocationTree) expr);
    }
    return false;
  }

  private static boolean isFileSourcedVariable(Symbol symbol) {
    if (!symbol.isVariableSymbol() || !symbol.owner().isMethodSymbol()) {
      return false;
    }
    if (symbol.type().isArray() && symbol.usages().size() != 1) {
      return false;
    }
    ExpressionTree value = ExpressionsHelper.getSingleWriteUsage(symbol);
    return value != null && isFileSourced(value);
  }

  private static boolean isFileSourcedCall(MethodInvocationTree call) {
    if (FILE_SOURCES.matches(call) || AS_BYTE_SOURCE.matches(call)) {
      return true;
    }
    Arguments arguments = call.arguments();
    if (CHANNEL_STREAM.matches(call)) {
      return !arguments.isEmpty() && arguments.get(0).symbolType().isSubtypeOf("java.nio.channels.FileChannel");
    }
    if (TO_BYTE_ARRAY.matches(call)) {
      return !arguments.isEmpty() && isFileSourced(arguments.get(0));
    }
    if (READ_ALL_BYTES.matches(call) || BYTE_SOURCE_READ.matches(call)) {
      ExpressionTree receiver = receiver(call);
      return receiver != null && isFileSourced(receiver);
    }
    return false;
  }

  @CheckForNull
  private static ExpressionTree receiver(MethodInvocationTree call) {
    return call.methodSelect().is(Tree.Kind.MEMBER_SELECT) ? ((MemberSelectExpressionTree) call.methodSelect()).expression() : null;
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return getWeakHashMethodInvocationMatchers();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    IdentifierTree methodName = ExpressionUtils.methodName(mit);
    if (DEPRECATED_HASH_CLASSES.contains(methodName.symbol().owner().type().fullyQualifiedName())) {
      reportIssue(methodName, MESSAGE);
      return;
    }
    InsecureAlgorithm algorithm = ALGORITHM_BY_METHOD_NAME.get(methodName.name());
    if (algorithm == null) {
      algorithm = algorithm(mit.arguments().get(0)).orElse(null);
    }
    if (algorithm != null && !isExempt(mit, algorithm)) {
      reportIssue(methodName, MESSAGE);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (DEPRECATED_HASH_CLASSES.contains(newClassTree.identifier().symbolType().fullyQualifiedName())) {
      reportIssue(newClassTree.identifier(), MESSAGE);
    }
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
