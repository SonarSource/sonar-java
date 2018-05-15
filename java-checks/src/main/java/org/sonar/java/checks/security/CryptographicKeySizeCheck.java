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
package org.sonar.java.checks.security;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4426")
public class CryptographicKeySizeCheck extends AbstractMethodDetection {

  private static final String KEY_PAIR_GENERATOR = "java.security.KeyPairGenerator";
  private static final String KEY_GENERATOR = "javax.crypto.KeyGenerator";
  private static final String GET_INSTANCE_METHOD = "getInstance";
  private static final String STRING = "java.lang.String";

  private static final GetInstanceMethodInfo getInstanceInfo = new GetInstanceMethodInfo();

  private static final Map<String, Integer> algorithmKeySizeMap = new HashMap<>();
  static {
    algorithmKeySizeMap.put("RSA", 2048);
    algorithmKeySizeMap.put("Blowfish", 128);
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition(KEY_GENERATOR).name(GET_INSTANCE_METHOD).addParameter(STRING),
      MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name(GET_INSTANCE_METHOD).addParameter(STRING),
      MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name("initialize").withAnyParameters(),
      MethodMatcher.create().typeDefinition(KEY_GENERATOR).name("init").withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (GET_INSTANCE_METHOD.equals(mit.symbol().name())) {
      ExpressionTree argument = mit.arguments().get(0);
      if (argument.is(Tree.Kind.STRING_LITERAL)) {
        String algorithm = LiteralUtils.trimQuotes(((LiteralTree) argument).value());
        if (algorithmKeySizeMap.containsKey(algorithm)) {
          storeAlgorithmKeySize(algorithm);
          storeSymbolOfAssignedInstance(mit);
        }
      }
    } else if (initializeIsCalledOnPreviouslyDefinedKeyGen(mit)) {
      checkAlgorithmParameterToReport(mit.arguments().get(0)).ifPresent(reportString -> reportIssue(mit, reportString));
    }
  }

  private static boolean initializeIsCalledOnPreviouslyDefinedKeyGen(MethodInvocationTree mit) {
    boolean isValidKeyGenSymbol = false;
    if (getInstanceInfo.keyGenMethodSymbol != null) {
      ExpressionTree expr = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        isValidKeyGenSymbol = getInstanceInfo.keyGenMethodSymbol.equals(((IdentifierTree) expr).symbol());
      } else if (expr.is(Tree.Kind.MEMBER_SELECT)) {
        isValidKeyGenSymbol = getInstanceInfo.keyGenMethodSymbol.equals(((MemberSelectExpressionTree) expr).identifier().symbol());
      }
    }
    return isValidKeyGenSymbol;
  }

  private static Optional<String> checkAlgorithmParameterToReport(ExpressionTree argument) {
    String reportString = null;
    if (argument.is(Tree.Kind.INT_LITERAL) && (Integer.parseInt(((LiteralTree) argument).value()) < getInstanceInfo.minKeySizeValue)) {
      reportString = "Use a key length of at least " + getInstanceInfo.minKeySizeValue + " bits.";
    }
    getInstanceInfo.nullifyAttributes();
    return Optional.ofNullable(reportString);
  }

  private static void storeSymbolOfAssignedInstance(MethodInvocationTree mit) {
    Tree parentExpression = mit.parent();
    if (parentExpression.is(Tree.Kind.VARIABLE)) {
      getInstanceInfo.keyGenMethodSymbol = ((VariableTree) parentExpression).symbol();
    } else if (parentExpression.is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree variable = ((AssignmentExpressionTree) parentExpression).variable();
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        getInstanceInfo.keyGenMethodSymbol = ((IdentifierTree) variable).symbol();
      } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        getInstanceInfo.keyGenMethodSymbol = ((MemberSelectExpressionTree) variable).identifier().symbol();
      }
    }
  }

  private static void storeAlgorithmKeySize(String algorithm) {
    getInstanceInfo.minKeySizeValue = algorithmKeySizeMap.get(algorithm);
  }

  private static final class GetInstanceMethodInfo {
    private Symbol keyGenMethodSymbol;
    private Integer minKeySizeValue;

    private void nullifyAttributes() {
      this.minKeySizeValue = null;
      this.keyGenMethodSymbol = null;
    }
  }
}
