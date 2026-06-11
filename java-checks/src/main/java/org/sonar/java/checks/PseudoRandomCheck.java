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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S2245")
public class PseudoRandomCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure that using this pseudorandom number generator is safe here.";

  private static final String LANG3_RANDOM_STRING_UTILS = "org.apache.commons.lang3.RandomStringUtils";

  private static final MethodMatchers STATIC_RANDOM_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.lang.Math").names("random").addWithoutParametersMatcher().build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.concurrent.ThreadLocalRandom",
        "org.apache.commons.lang.math.RandomUtils",
        "org.apache.commons.lang3.RandomUtils",
        "org.apache.commons.lang.RandomStringUtils",
        LANG3_RANDOM_STRING_UTILS)
      .anyName()
      .withAnyParameters()
      .build()
  );

  private static final MethodMatchers RANDOM_STRING_UTILS_SECURE_INSTANCES = MethodMatchers.create()
    .ofSubTypes(LANG3_RANDOM_STRING_UTILS)
      .names("secure", "secureStrong")
      .withAnyParameters()
      .build();

  private static final MethodMatchers RANDOM_STRING_UTILS_RANDOM_WITH_RANDOM_SOURCE = MethodMatchers.create()
    .ofSubTypes("org.apache.commons.lang.RandomStringUtils", LANG3_RANDOM_STRING_UTILS)
    .names("random")
    .addParametersMatcher("int", "int", "int", "boolean", "boolean", "char[]", "java.util.Random")
    .build();

  private static final Set<String> RANDOM_CONSTRUCTOR_TYPES = SetUtils.immutableSetOf(
    "java.util.Random",
    "org.apache.commons.lang.math.JVMRandom"
  );

  private static final List<String> CRYPTO_IMPORT_PREFIXES = List.of(
    "java.security.",
    "javax.crypto.",
    "org.springframework.security.",
    "org.bouncycastle.",
    "io.jsonwebtoken.",
    "com.auth0.jwt.",
    "at.favre.lib.crypto.",
    "de.mkammerer.argon2."
  );

  private static final Set<String> SECURITY_KEYWORDS = SetUtils.immutableSetOf(
    "aes", "asymmetric", "auth", "certificate", "chacha20", "cipher", "crypto", "cryptography",
    "decrypt", "ecc", "encrypt", "encryption", "hmac", "iv", "nonce", "password", "pbkdf2",
    "poly1305", "randombytes", "rsa", "salt", "scrypt", "secret", "secure", "security",
    "sessionid", "signature", "token", "verify"
  );

  private boolean cryptoImportPresent = false;
  private final Map<Tree, Boolean> scopeSecurityContextCache = new IdentityHashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      cryptoImportPresent = hasCryptoImport((CompilationUnitTree) tree);
      return;
    }
    if (tree instanceof MethodInvocationTree mit) {
      if (isStaticCallToInsecureRandomMethod(mit) && isInSecurityContext(mit)) {
        reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
      }
    } else {
      NewClassTree newClass = (NewClassTree) tree;
      if (RANDOM_CONSTRUCTOR_TYPES.contains(newClass.symbolType().fullyQualifiedName())
        && isInSecurityContext(newClass)) {
        reportIssue(newClass.identifier(), MESSAGE);
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      cryptoImportPresent = false;
      scopeSecurityContextCache.clear();
    }
  }

  private static boolean isStaticCallToInsecureRandomMethod(MethodInvocationTree mit) {
    return STATIC_RANDOM_METHODS.matches(mit)
      && !RANDOM_STRING_UTILS_RANDOM_WITH_RANDOM_SOURCE.matches(mit)
      && !RANDOM_STRING_UTILS_SECURE_INSTANCES.matches(mit)
      && mit.methodSymbol().isStatic();
  }

  private static boolean hasCryptoImport(CompilationUnitTree cut) {
    for (ImportClauseTree importClause : cut.imports()) {
      if (importClause.is(Tree.Kind.IMPORT)
        && ((ImportTree) importClause).qualifiedIdentifier() instanceof ExpressionTree exprTree
        && matchesCryptoPrefix(ExpressionsHelper.concatenate(exprTree))) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesCryptoPrefix(String importName) {
    for (String prefix : CRYPTO_IMPORT_PREFIXES) {
      if (importName.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInSecurityContext(Tree tree) {
    if (cryptoImportPresent) {
      return true;
    }
    Tree scope = findDeclarationScope(tree);
    if (scope == null) {
      // Defensive: shouldn't happen in valid Java (every PRNG call has an enclosing
      // method or class). Fail open and flag to avoid silent false negatives.
      return true;
    }
    return scopeSecurityContextCache.computeIfAbsent(scope, PseudoRandomCheck::scopeHasSecurityKeyword);
  }

  private static boolean scopeHasSecurityKeyword(Tree scope) {
    IdentifierCollector collector = new IdentifierCollector();
    scope.accept(collector);
    for (String identifier : collector.identifiers) {
      for (String token : tokenizeIdentifier(identifier)) {
        if (SECURITY_KEYWORDS.contains(token)) {
          return true;
        }
      }
    }
    return false;
  }

  // Mirrors Dart's `declarationScope`: the closest enclosing MethodTree (method/constructor) for
  // local code; falls back to the enclosing ClassTree for field/initializer-block code.
  private static Tree findDeclarationScope(Tree tree) {
    Tree current = tree.parent();
    while (current != null) {
      if (current instanceof MethodTree methodTree) {
        return methodTree;
      }
      if (current instanceof ClassTree classTree) {
        return classTree;
      }
      current = current.parent();
    }
    return null;
  }

  // Mirrors Dart's `_splitIntoWords`: split on underscores first; for each part either keep it as
  // a single lowercase word when all-uppercase, or split further on capital-letter boundaries.
  static List<String> tokenizeIdentifier(String identifier) {
    List<String> words = new ArrayList<>();
    for (String part : identifier.split("_")) {
      if (part.isEmpty()) {
        continue;
      }
      if (isAllUppercaseWithLetter(part)) {
        words.add(part.toLowerCase(Locale.ROOT));
      } else {
        for (String sub : part.split("(?=[A-Z])")) {
          if (!sub.isEmpty()) {
            words.add(sub.toLowerCase(Locale.ROOT));
          }
        }
      }
    }
    return words;
  }

  private static boolean isAllUppercaseWithLetter(String part) {
    boolean hasLetter = false;
    for (int i = 0; i < part.length(); i++) {
      char c = part.charAt(i);
      if (Character.isLetter(c)) {
        hasLetter = true;
        if (Character.isLowerCase(c)) {
          return false;
        }
      }
    }
    return hasLetter;
  }

  private static class IdentifierCollector extends BaseTreeVisitor {
    private final Set<String> identifiers = new HashSet<>();

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiers.add(tree.name());
      super.visitIdentifier(tree);
    }
  }

}
