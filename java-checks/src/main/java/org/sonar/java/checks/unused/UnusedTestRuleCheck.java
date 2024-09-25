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
package org.sonar.java.checks.unused;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S2924")
public class UnusedTestRuleCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> CHECKED_RULE = SetUtils.immutableSetOf(
    "org.junit.rules.TemporaryFolder",
    "org.junit.rules.TestName"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    boolean isAbstract = classTree.modifiers().modifiers().stream().anyMatch(m -> Modifier.ABSTRACT == m.modifier());
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        Symbol symbol = variableTree.symbol();
        if ((isTestNameOrTemporaryFolderRule(symbol) || hasTempDirAnnotation(symbol)) && symbol.usages().isEmpty()) {
          // if class is abstract, then we need to check modifier - if not private, then it's okay
          if (isAbstract && variableTree.modifiers().modifiers().stream().noneMatch(m -> Modifier.PRIVATE == m.modifier())) {
              continue;
          }
          reportIssue(variableTree.simpleName(), "Remove this unused \"" + getSymbolType(symbol) + "\".");
        }
      } else if (member.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        checkJUnit5((MethodTree) member);
      }
    }
  }

  private void checkJUnit5(MethodTree member) {
    for (VariableTree param : member.parameters()) {
      Symbol symbol = param.symbol();
      if ((hasTempDirAnnotation(symbol) || symbol.type().is("org.junit.jupiter.api.TestInfo")) && symbol.usages().isEmpty()) {
        reportIssue(param.simpleName(), "Remove this unused \"" + getSymbolType(symbol) + "\".");
      }
    }
  }

  private static boolean isTestNameOrTemporaryFolderRule(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith("org.junit.Rule") && CHECKED_RULE.contains(symbol.type().fullyQualifiedName());
  }

  private static boolean hasTempDirAnnotation(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith("org.junit.jupiter.api.io.TempDir");
  }

  private static String getSymbolType(Symbol symbol) {
    return hasTempDirAnnotation(symbol) ? "TempDir" : symbol.type().toString();
  }

}
