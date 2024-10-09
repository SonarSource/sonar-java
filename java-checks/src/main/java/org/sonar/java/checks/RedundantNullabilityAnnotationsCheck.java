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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.NullabilityDataUtils.nullabilityAsString;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.CLASS;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.METHOD;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.VARIABLE;

@Rule(key = "S6665")
public class RedundantNullabilityAnnotationsCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove redundant annotation %s as inside scope annotation %s.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    // check if outer class - this will return default package if necessary
    if (Objects.requireNonNull(classTree.symbol().owner()).isPackageSymbol()) {
      // get nullability from class target level up
      SymbolMetadata.NullabilityData classNullabilityData = classTree.symbol().metadata()
        .nullabilityData(SymbolMetadata.NullabilityTarget.CLASS);
      // if non-null, either directly or inherited from higher scope
      if (classNullabilityData.isNonNull(PACKAGE, false, false)) {
        // then check my members are not directly annotated with non-null
        checkMembers(classNullabilityData, classTree, NULLABILITY_SCOPE.NON_NULLABLE);
      }
      // if nullable, either directly or inherited from higher scope
      else if (classNullabilityData.isNullable(PACKAGE, false, false)) {
        // then check my members are not directly annotated with non-null
        checkMembers(classNullabilityData, classTree, NULLABILITY_SCOPE.NULLABLE);
      }
    }
  }

  private void checkMembers(SymbolMetadata.NullabilityData classNullabilityData,
    ClassTree tree, NULLABILITY_SCOPE scope) {
    // for all members
    tree.members().forEach(member -> {
      if (member.is(Tree.Kind.VARIABLE)) {
        // check field
        VariableTree variableTree = (VariableTree) member;
        checkSymbol(classNullabilityData, variableTree, VARIABLE, variableTree.symbol(), scope);
      } else if (member.is(Tree.Kind.METHOD)) {
        // check method
        checkMethod(classNullabilityData, (MethodTree) member, scope);
      } else if (member.is(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.RECORD)) {
        // check inner class
        checkInnerClass(classNullabilityData, (ClassTree) member, scope);
      }
    });
  }

  private void checkInnerClass(SymbolMetadata.NullabilityData classNullabilityData,
    ClassTree tree, NULLABILITY_SCOPE scope) {
    // check inner object is not directly annotated
    SymbolMetadata.NullabilityData innerNullabilityData = tree.symbol().metadata()
      .nullabilityData(SymbolMetadata.NullabilityTarget.CLASS);
    if (innerNullabilityData.isNonNull(CLASS, false, false)) {
      if (scope.equals(NULLABILITY_SCOPE.NON_NULLABLE)) {
        reportIssue(tree, innerNullabilityData, classNullabilityData);
      }
      // now recurse to check class members
      checkMembers(innerNullabilityData, tree, NULLABILITY_SCOPE.NON_NULLABLE);
    } else if (innerNullabilityData.isNullable(CLASS, false, false)) {
      if (scope.equals(NULLABILITY_SCOPE.NULLABLE)) {
        reportIssue(tree, innerNullabilityData, classNullabilityData);
      }
      // now recurse to check class members
      checkMembers(innerNullabilityData, tree, NULLABILITY_SCOPE.NULLABLE);
    }
  }

  private void checkMethod(SymbolMetadata.NullabilityData classNullabilityData,
    MethodTree method, NULLABILITY_SCOPE scope) {
    // check return type at method level - do not look up hierarchy
    checkSymbol(classNullabilityData, method, METHOD, method.symbol(), scope);
    // check parameters at variable level - do not look up hierarchy
    method.parameters().forEach(parameter ->
      checkSymbol(classNullabilityData, parameter, VARIABLE, parameter.symbol(), scope)
    );
  }

  private void checkSymbol(SymbolMetadata.NullabilityData classNullabilityData, Tree tree,
    SymbolMetadata.NullabilityLevel treeLevel, Symbol symbol, NULLABILITY_SCOPE scope) {
    SymbolMetadata.NullabilityData methodNullabilityData = symbol.metadata().nullabilityData();
    if (methodNullabilityData.isNonNull(treeLevel, false, false) &&
      scope.equals(NULLABILITY_SCOPE.NON_NULLABLE)) {
      reportIssue(tree, methodNullabilityData, classNullabilityData);
    }
    if (methodNullabilityData.isNullable(treeLevel, false, false) &&
      scope.equals(NULLABILITY_SCOPE.NULLABLE)) {
      reportIssue(tree, methodNullabilityData, classNullabilityData);
    }
  }

  // helpful method that handles string conversions of NullabilityData annotations prior to issue reporting
  private void reportIssue(Tree reportLocation,
    SymbolMetadata.NullabilityData directNullabilityData,
    SymbolMetadata.NullabilityData higherNullabilityData) {
    Optional<String> directNullabilityDataAsString = nullabilityAsString(directNullabilityData);
    Optional<String> higherNullabilityDataAsString = nullabilityAsString(higherNullabilityData);
    if (directNullabilityDataAsString.isPresent() && higherNullabilityDataAsString.isPresent()) {
      reportIssue(reportLocation,
        String.format(ISSUE_MESSAGE,
          directNullabilityDataAsString.get(),
          higherNullabilityDataAsString.get()));
    }
  }

  // track class scope nullability state during recursion
  private enum NULLABILITY_SCOPE {
    NULLABLE,
    NON_NULLABLE
  }

}
