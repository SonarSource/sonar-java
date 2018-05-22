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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4426")
public class CryptographicKeySizeCheck extends AbstractMethodDetection {

  private static final String KEY_PAIR_GENERATOR = "java.security.KeyPairGenerator";
  private static final String KEY_GENERATOR = "javax.crypto.KeyGenerator";
  private static final String GET_INSTANCE_METHOD = "getInstance";
  private static final String STRING = "java.lang.String";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition(KEY_GENERATOR).name(GET_INSTANCE_METHOD).addParameter(STRING),
      MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name(GET_INSTANCE_METHOD).addParameter(STRING));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree methodTree = findEnclosingMethod(mit);
    ExpressionTree getInstanceArg = mit.arguments().get(0);
    if (methodTree != null && getInstanceArg.is(Tree.Kind.STRING_LITERAL)) {
      MethodVisitor methodVisitor = new MethodVisitor(getInstanceArg);
      methodTree.accept(methodVisitor);
      if (methodVisitor.reportString != null) {
        reportIssue(methodVisitor.treeToReport, methodVisitor.reportString);
      }
    }
  }

  private static MethodTree findEnclosingMethod(Tree tree) {
    while (!tree.is(Tree.Kind.CLASS, Tree.Kind.METHOD)) {
      tree = tree.parent();
    }
    if (tree.is(Tree.Kind.CLASS)) {
      return null;
    }
    return (MethodTree) tree;
  }

  private static class MethodVisitor extends BaseTreeVisitor {

    private String algorithm = null;
    private String reportString = null;
    private MethodInvocationTree treeToReport = null;

    public MethodVisitor(ExpressionTree getInstanceArg) {
      this.algorithm = LiteralUtils.trimQuotes(((LiteralTree) getInstanceArg).value());
    }

    private static final Map<String, Integer> algorithmKeySizeMap = new HashMap<>();
    static {
      algorithmKeySizeMap.put("RSA", 2048);
      algorithmKeySizeMap.put("Blowfish", 128);
    }

    private static final MethodMatcher KEY_GEN_INIT = MethodMatcher.create().typeDefinition(KEY_GENERATOR).name("init").addParameter("int");
    private static final MethodMatcher KEY_PAIR_GEN_INITIALIZE = MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name("initialize").addParameter("int");

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (KEY_GEN_INIT.matches(mit) || KEY_PAIR_GEN_INITIALIZE.matches(mit)) {
        Integer minKeySize = algorithmKeySizeMap.get(algorithm);
        if (minKeySize != null) {
          checkAlgorithmParameterToReport(mit.arguments().get(0), minKeySize)
            .ifPresent(argValue -> reportString = argValue);
          this.treeToReport = mit;
        }
      }
    }

    private static Optional<String> checkAlgorithmParameterToReport(ExpressionTree argument, Integer keySize) {
      String resultString = null;
      if (argument.is(Tree.Kind.INT_LITERAL) && (Integer.parseInt(((LiteralTree) argument).value()) < keySize)) {
        resultString = "Use a key length of at least " + keySize + " bits.";
      }
      return Optional.ofNullable(resultString);
    }
  }
}
