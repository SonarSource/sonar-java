/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityData;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.se.NullabilityDataUtils.nullabilityAsString;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;

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

    // If the method from the parent claims to never return null, the method from the child
    // that can actually be executed at runtime should not return null.
    NullabilityData overrideeNullability = overridee.metadata().nullabilityData();
    if (overrideeNullability.isNonNull(PACKAGE, false, false)) {
      NullabilityData methodNullability = methodTree.symbol().metadata().nullabilityData();
      if (methodNullability.isNullable(PACKAGE, false, false)) {
        // returnType() returns null in case of constructor: the rule does not support them.
        reportIssue(methodTree.returnType(), overrideeNullability, methodNullability);
      }
    }
  }

  private void checkParameter(VariableTree parameter, SymbolMetadata overrideeParamMetadata) {
    // Annotations on parameters is the opposite of return value: if arguments of the parent can be null, the child method has to accept null value.
    NullabilityData overrideeParamNullability = overrideeParamMetadata.nullabilityData();
    if (overrideeParamNullability.isNullable(PACKAGE, false, false)) {
      NullabilityData paramNullability = parameter.symbol().metadata().nullabilityData();
      if (paramNullability.isNonNull(PACKAGE, false, false)) {
        reportIssue(parameter.simpleName(), overrideeParamNullability, paramNullability);
      }
    }
  }

  private void reportIssue(Tree reportLocation, NullabilityData overrideeNullability, NullabilityData otherNullability) {
    Optional<String> overrideeAsString = nullabilityAsString(otherNullability);
    Optional<String> otherAsString = nullabilityAsString(overrideeNullability);
    if (overrideeAsString.isPresent() && otherAsString.isPresent()) {
      reportIssue(reportLocation,
        String.format("Fix the incompatibility of the annotation %s to honor %s of the overridden method.",
          overrideeAsString.get(),
          otherAsString.get()),
        getSecondariesForAnnotations(otherNullability, overrideeNullability),
        null);
    }
  }

  private static List<JavaFileScannerContext.Location> getSecondariesForAnnotations(NullabilityData childData, NullabilityData parentData) {
    List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    Tree childDeclaration = childData.declaration();
    if (childDeclaration != null) {
      secondaries.add(new JavaFileScannerContext.Location("Child annotation", childDeclaration));
    }
    Tree parentDeclaration = parentData.declaration();
    if (parentDeclaration != null) {
      secondaries.add(new JavaFileScannerContext.Location("Overridden annotation", parentDeclaration));
    }
    return secondaries;
  }

}
