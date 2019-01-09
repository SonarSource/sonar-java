/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.AccessorsUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S4144")
public class MethodIdenticalImplementationsCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MSG = "Update this method so that its implementation is not identical to \"%s\" on line %d.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    List<MethodWithUsedVariables> methods = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(methodTree -> isDuplicateCandidate(methodTree, classTree))
      .map(MethodWithUsedVariables::new)
      .collect(Collectors.toList());
    if (methods.size() <= 1) {
      return;
    }
    Set<MethodTree> reported = new HashSet<>();
    for (int i = 0; i < methods.size(); i++) {
      MethodWithUsedVariables methodWithVariables = methods.get(i);
      MethodTree method = methodWithVariables.method;
      SyntaxToken methodIdentifier = method.simpleName().identifierToken();
      List<StatementTree> methodBody = method.block().body();
      methods.stream()
        .skip(i + 1L)
        // avoid reporting multiple times
        .filter(otherMethodWithVariables -> !reported.contains(otherMethodWithVariables.method))
        // skip overloads
        .filter(otherMethodWithVariables -> !methodIdentifier.text().equals(otherMethodWithVariables.method.simpleName().name()))
        // only consider method syntactically equivalent
        .filter(otherMethodWithVariables -> SyntacticEquivalence.areEquivalent(methodBody, otherMethodWithVariables.method.block().body()))
        // only consider method having same types for their variables
        .filter(methodWithVariables::isUsingSameVariablesWithSameTypes)
        .forEach(otherMethodWithVariables -> {
          MethodTree otherMethod = otherMethodWithVariables.method;
          reportIssue(
            otherMethod.simpleName(),
            String.format(ISSUE_MSG, methodIdentifier.text(), methodIdentifier.line()),
            Collections.singletonList(new JavaFileScannerContext.Location("original implementation", methodIdentifier)),
            null);
          reported.add(otherMethod);
        });
    }
  }

  private static boolean isDuplicateCandidate(MethodTree methodTree, ClassTree classTree) {
    BlockTree block = methodTree.block();
    return AccessorsUtils.isAccessor(classTree, methodTree)
      || (block != null && block.body().size() >= 2);
  }

  private static class MethodWithUsedVariables extends BaseTreeVisitor {
    private final MethodTree method;
    private final Map<String, Type> usedVariablesByNameAndType = new HashMap<>();
    private boolean hasUnknownVariableType = false;

    public MethodWithUsedVariables(MethodTree method) {
      this.method = method;
      method.accept(this);
    }

    public boolean isUsingSameVariablesWithSameTypes(MethodWithUsedVariables otherMethod) {
      return !hasUnknownVariableType
        && !otherMethod.hasUnknownVariableType
        && usedVariablesByNameAndType.equals(otherMethod.usedVariablesByNameAndType);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();
      Type type = symbol.type();
      if (symbol.isVariableSymbol()) {
        if (type.isUnknown()) {
          hasUnknownVariableType = true;
        } else {
          usedVariablesByNameAndType.putIfAbsent(tree.name(), type);
        }
      }
      super.visitIdentifier(tree);
    }

  }
}
