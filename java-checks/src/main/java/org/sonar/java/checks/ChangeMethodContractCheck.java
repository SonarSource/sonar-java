/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.JUtils;
import org.sonar.java.se.NullableAnnotationUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2638")
public class ChangeMethodContractCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    List<Symbol.MethodSymbol> overriddenSymbols = methodSymbol.overriddenSymbols();
    if (overriddenSymbols.isEmpty()) {
      return;
    }
    Symbol.MethodSymbol overridee = overriddenSymbols.get(0);
    if (overridee.isMethodSymbol()) {
      checkContractChange(methodTree, overridee);
    }
  }

  private void checkContractChange(MethodTree methodTree, Symbol.MethodSymbol overridee) {
    if (MethodTreeUtils.isEqualsMethod(methodTree)) {
      // Handled by S4454.
      return;
    }
    for (int i = 0; i < methodTree.parameters().size(); i++) {
      VariableTree parameter = methodTree.parameters().get(i);
      checkParameter(parameter, JUtils.parameterAnnotations(overridee, i));
    }
    if (NullableAnnotationUtils.isAnnotatedNonNull(overridee)) {
      NullableAnnotationUtils.nullableAnnotation(methodTree.modifiers())
        .ifPresent(annotation -> reportIssue(annotation, "Remove this \""+ annotation.symbolType().name() +"\" annotation to honor the overridden method's contract."));
    }
  }

  private void checkParameter(VariableTree parameter, SymbolMetadata overrideeParamMetadata) {
    if (NullableAnnotationUtils.isAnnotatedNullable(overrideeParamMetadata)) {
      NullableAnnotationUtils.nonNullAnnotation(parameter.modifiers())
        .ifPresent(annotation -> reportIssue(annotation,
          "Remove this \"" + annotation.annotationType().symbolType().name() + "\" annotation to honor the overridden method's contract."));
    }
  }

}
