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

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.AbstractHashAlgorithmChecker;
import org.sonar.java.checks.helpers.ExpressionsHelper;
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
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S4790")
public class DataHashingCheck extends AbstractHashAlgorithmChecker {

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
      .ofSubTypes("org.springframework.web.multipart.MultipartFile")
      .names("getBytes")
      .addWithoutParametersMatcher()
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

  @Override
  protected Optional<String> getMessageForClass(String className) {
    return DEPRECATED_HASH_CLASSES.contains(className) ? Optional.of(MESSAGE) : Optional.empty();
  }

  @Override
  protected String getMessageForAlgorithm(String algorithmName) {
    return MESSAGE;
  }

  /**
   * MD5 and SHA-1 are exempted only when every byte reaching the digest is proven to come from a file.
   * Anything that cannot be proven (unknown symbol, field, digest passed to another method, mixed data) is still reported.
   */
  @Override
  protected boolean isExempt(MethodInvocationTree mit, InsecureAlgorithm algorithm) {
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
    Tree parent = use.parent();
    if (parent.is(Tree.Kind.MEMBER_SELECT)
      && ((MemberSelectExpressionTree) parent).expression() == use
      && parent.parent().is(Tree.Kind.METHOD_INVOCATION)
      && ((MethodInvocationTree) parent.parent()).methodSelect() == parent) {
      return classifyCall(((MemberSelectExpressionTree) parent).identifier().name(), ((MethodInvocationTree) parent.parent()).arguments());
    }
    if (parent.is(Tree.Kind.ARGUMENTS)) {
      return classifyArgument(use, parent.parent());
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
}
