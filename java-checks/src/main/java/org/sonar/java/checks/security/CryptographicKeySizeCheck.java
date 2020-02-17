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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.extractIdentifierSymbol;
import static org.sonar.java.model.ExpressionUtils.getAssignedSymbol;

@Rule(key = "S4426")
public class CryptographicKeySizeCheck extends AbstractMethodDetection {

  private static final String KEY_PAIR_GENERATOR = "java.security.KeyPairGenerator";
  private static final String KEY_GENERATOR = "javax.crypto.KeyGenerator";
  private static final String EC_GEN_PARAMETER_SPEC = "java.security.spec.ECGenParameterSpec";
  private static final String GET_INSTANCE_METHOD = "getInstance";
  private static final String STRING = "java.lang.String";

  private static final int EC_MIN_KEY = 224;
  private static final Pattern EC_KEY_PATTERN = Pattern.compile("^(secp|prime|sect|c2tnb)(\\d+)");

  private static final Map<String, Integer> ALGORITHM_KEY_SIZE_MAP = ImmutableMap.of(
    "RSA", 2048,
    "DH", 2048,
    "DIFFIEHELLMAN", 2048,
    "DSA", 2048,
    "AES", 128);

  private static final MethodMatcher KEY_GEN_INIT = MethodMatcher.create().typeDefinition(KEY_GENERATOR).name("init").addParameter("int");
  private static final MethodMatcher KEY_PAIR_GEN_INITIALIZE = MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name("initialize").addParameter("int");
  private static final MethodMatcher KEY_PAIR_GEN_INITIALIZE_WITH_SOURCE = KEY_PAIR_GEN_INITIALIZE.copy().addParameter("java.security.SecureRandom");

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(KEY_GENERATOR).name(GET_INSTANCE_METHOD).addParameter(STRING),
      MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name(GET_INSTANCE_METHOD).addParameter(STRING),
      MethodMatcher.create().typeDefinition(EC_GEN_PARAMETER_SPEC).name("<init>").addParameter(STRING));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree methodTree = ExpressionUtils.getEnclosingMethod(mit);
    String getInstanceArg = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
    if (methodTree != null && getInstanceArg != null) {
      Optional<Symbol> assignedSymbol = getAssignedSymbol(mit);
      assignedSymbol.ifPresent(symbol -> {
        MethodVisitor methodVisitor = new MethodVisitor(getInstanceArg, symbol);
        methodTree.accept(methodVisitor);
      });
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    String firstArgument = ExpressionsHelper.getConstantValueAsString(newClassTree.arguments().get(0)).value();
    if (firstArgument != null) {
      Matcher matcher = EC_KEY_PATTERN.matcher(firstArgument);
      if (matcher.find() && Integer.valueOf(matcher.group(2)) < EC_MIN_KEY) {
        reportIssue(newClassTree, "Use a key length of at least " + EC_MIN_KEY + " bits for EC cipher algorithm.");
      }
    }
  }

  private class MethodVisitor extends BaseTreeVisitor {

    private final String algorithm;
    private final Integer minKeySize;
    private final Symbol variable;

    public MethodVisitor(String getInstanceArg, Symbol variable) {
      this.algorithm = getInstanceArg;
      this.minKeySize = ALGORITHM_KEY_SIZE_MAP.get(this.algorithm.toUpperCase(Locale.ENGLISH));
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (minKeySize != null && (KEY_GEN_INIT.matches(mit) || KEY_PAIR_GEN_INITIALIZE.matches(mit) || KEY_PAIR_GEN_INITIALIZE_WITH_SOURCE.matches(mit))) {
        Integer keySize = LiteralUtils.intLiteralValue(mit.arguments().get(0));
        if (keySize != null && keySize < minKeySize && isSameVariableSymbol(mit)) {
          reportIssue(mit, "Use a key length of at least " + minKeySize + " bits for " + algorithm + " cipher algorithm.");
        }
      }
    }

    private boolean isSameVariableSymbol(MethodInvocationTree mit) {
      ExpressionTree methodSelect = mit.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        return extractIdentifierSymbol(((MemberSelectExpressionTree)methodSelect).expression()).filter(s -> s.equals(variable)).isPresent();
      }
      return false;
    }
  }
}
