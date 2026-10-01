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

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonarsource.analyzer.commons.appsec.CryptographicKeySizeConfiguration;

import static org.sonar.java.model.ExpressionUtils.getAssignedSymbol;
import static org.sonar.java.model.ExpressionUtils.isInvocationOnVariable;

@Rule(key = "S4426")
public class CryptographicKeySizeCheck extends AbstractMethodDetection {

  private static final String KEY_PAIR_GENERATOR = "java.security.KeyPairGenerator";
  private static final String KEY_GENERATOR = "javax.crypto.KeyGenerator";
  private static final String EC_GEN_PARAMETER_SPEC = "java.security.spec.ECGenParameterSpec";
  private static final String GET_INSTANCE_METHOD = "getInstance";
  private static final String STRING = "java.lang.String";

  @RuleProperty(
    key = "minimumKeySizes",
    description = "Comma-separated list of algorithm:minKeySize pairs (e.g. \"RSA:4096,AES:256\"). " +
      "Patches the default minimum key sizes — only the listed algorithms are overridden; others keep their defaults.",
    defaultValue = CryptographicKeySizeConfiguration.DEFAULT_KEY_SIZES)
  public String minimumKeySizes = CryptographicKeySizeConfiguration.DEFAULT_KEY_SIZES;

  private Map<String, Integer> effectiveKeySizeMap;

  private Map<String, Integer> getEffectiveKeySizeMap() {
    if (effectiveKeySizeMap == null) {
      effectiveKeySizeMap = CryptographicKeySizeConfiguration.effectiveKeySizes(minimumKeySizes);
    }
    return effectiveKeySizeMap;
  }

  private static final MethodMatchers KEY_GEN = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(KEY_GENERATOR)
      .names("init")
      .addParametersMatcher("int")
      .build(),
    MethodMatchers.create()
      .ofTypes(KEY_PAIR_GENERATOR)
      .names("initialize")
      .addParametersMatcher("int")
      .addParametersMatcher("int", "java.security.SecureRandom")
      .build());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(KEY_GENERATOR, KEY_PAIR_GENERATOR)
        .names(GET_INSTANCE_METHOD)
        .addParametersMatcher(STRING)
        .build(),
      MethodMatchers.create()
        .ofTypes(EC_GEN_PARAMETER_SPEC)
        .constructor()
        .addParametersMatcher(STRING)
        .build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree methodTree = ExpressionUtils.getEnclosingMethod(mit);
    String getInstanceArg = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
    if (methodTree != null && getInstanceArg != null) {
      Optional<Symbol> assignedSymbol = getAssignedSymbol(mit);
      MethodVisitor methodVisitor = new MethodVisitor(getInstanceArg, assignedSymbol.orElse(null));
      methodTree.accept(methodVisitor);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    String firstArgument = ExpressionsHelper.getConstantValueAsString(newClassTree.arguments().get(0)).value();
    if (firstArgument != null) {
      Integer ecMinKey = getEffectiveKeySizeMap().get("EC");
      if (ecMinKey != null) {
        CryptographicKeySizeConfiguration.extractEcKeySize(firstArgument).ifPresent(keySize -> {
          if (keySize < ecMinKey) {
            reportIssue(newClassTree, "Use a key length of at least " + ecMinKey + " bits for EC cipher algorithm.");
          }
        });
      }
    }
  }

  private class MethodVisitor extends BaseTreeVisitor {

    private final String algorithm;
    private final Integer minKeySize;
    private final Symbol variable;

    public MethodVisitor(String getInstanceArg, @Nullable Symbol variable) {
      this.algorithm = getInstanceArg;
      this.minKeySize = getEffectiveKeySizeMap().get(this.algorithm.toUpperCase(Locale.ROOT));
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (minKeySize != null && KEY_GEN.matches(mit)) {
        Integer keySize = LiteralUtils.intLiteralValue(mit.arguments().get(0));
        if (keySize != null && keySize < minKeySize && isInvocationOnVariable(mit, variable, false)) {
          reportIssue(mit, "Use a key length of at least " + minKeySize + " bits for " + algorithm + " cipher algorithm.");
        }
      }
    }
  }
}
