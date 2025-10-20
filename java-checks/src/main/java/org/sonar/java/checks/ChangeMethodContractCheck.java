/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.NullabilityDataUtils.nullabilityAsString;
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
    var returnType = methodTree.returnType();
    // returnType() returns null in case of constructor: the rule does not support them.
    if (returnType == null) {
      return;
    }

    compareNullability(returnType, methodTree.symbol().metadata(), overridee.metadata(), true);
  }

  private void compareNullability(TypeTree tree, SymbolMetadata upperBound, SymbolMetadata lowerBound, boolean overriddenIsLowerBound) {
    // Check current level
    if (upperBound.nullabilityData().isNullable(PACKAGE, false, false)
        && lowerBound.nullabilityData().isNonNull(PACKAGE, false, false)) {
      reportIssue(tree, lowerBound.nullabilityData(), upperBound.nullabilityData(), overriddenIsLowerBound);
    }

    // Check type parameters
    var upperParams = upperBound.parametersMetadata();
    var lowerParams = lowerBound.parametersMetadata();
    if (upperParams.length != lowerParams.length) {
      return;
    }

    // Compare parameters
    for (var i = 0; i < upperParams.length; i++) {
      compareNullability(getTypeArg(tree, i), upperParams[i], lowerParams[i], overriddenIsLowerBound);
    }
  }

  private static TypeTree getTypeArg(TypeTree tree, int index) {
    if (tree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return ((ParameterizedTypeTree) tree).typeArguments().get(index);
    }

    // That's weird, but ok
    return tree;
  }

  private void checkParameter(VariableTree parameter, SymbolMetadata overrideeParam) {
    compareNullability(parameter.type(), overrideeParam, parameter.symbol().metadata(), false);
  }

  private void reportIssue(Tree reportLocation, NullabilityData upperBound, NullabilityData lowerBound, boolean overriddenIsLowerBound) {
    NullabilityData otherNullability = overriddenIsLowerBound ? lowerBound : upperBound;
    NullabilityData overrideeNullability = overriddenIsLowerBound ? upperBound : lowerBound;

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
