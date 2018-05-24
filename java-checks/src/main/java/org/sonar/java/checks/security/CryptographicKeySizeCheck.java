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
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
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
    String getInstanceArg = ConstantUtils.resolveAsStringConstant(mit.arguments().get(0));
    if (methodTree != null && getInstanceArg != null) {
      MethodVisitor methodVisitor = new MethodVisitor(getInstanceArg);
      methodTree.accept(methodVisitor);
    }
  }

  @CheckForNull
  public static MethodTree findEnclosingMethod(Tree tree) {
    while (!tree.is(Tree.Kind.CLASS, Tree.Kind.METHOD)) {
      tree = tree.parent();
    }
    if (tree.is(Tree.Kind.CLASS)) {
      return null;
    }
    return (MethodTree) tree;
  }

  private class MethodVisitor extends BaseTreeVisitor {

    private final String algorithm;

    public MethodVisitor(String getInstanceArg) {
      this.algorithm = getInstanceArg;
    }

    private final Map<String, Integer> algorithmKeySizeMap = ImmutableMap.of("Blowfish", 128, "RSA", 2048);

    private final MethodMatcher keyGenInit = MethodMatcher.create().typeDefinition(KEY_GENERATOR).name("init").addParameter("int");
    private final MethodMatcher keyPairGenInitialize = MethodMatcher.create().typeDefinition(KEY_PAIR_GENERATOR).name("initialize").addParameter("int");

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (keyGenInit.matches(mit) || keyPairGenInitialize.matches(mit)) {
        Integer minKeySize = algorithmKeySizeMap.get(this.algorithm);
        if (minKeySize != null) {
          Integer keySize = LiteralUtils.intLiteralValue(mit.arguments().get(0));
          if (keySize != null && keySize < minKeySize) {
            reportIssue(mit, "Use a key length of at least " + minKeySize + " bits.");
          }
        }
      }
    }
  }
}
