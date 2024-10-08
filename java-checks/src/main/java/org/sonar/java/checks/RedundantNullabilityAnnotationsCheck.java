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
import org.sonar.java.checks.helpers.NullabilityDataUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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

  private static final String ISSUE_MESSAGE = "Remove redundant nullability annotation %s as already annotated with %s.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    // am I an outer class - will return default package if necessary
    if (Objects.requireNonNull(classTree.symbol().owner()).isPackageSymbol()) {
      // get nullability from class target level up
      SymbolMetadata.NullabilityData classNullabilityData = classTree.symbol().metadata()
        .nullabilityData(SymbolMetadata.NullabilityTarget.CLASS);
      // if non-null, either directly or inherited from higher entity
      if (classNullabilityData.isNonNull(PACKAGE, false, false)) {
        // then check my members are not directly annotated with non-null
        checkIfMembersContainNonNull(classNullabilityData, classTree);
      }
    }
  }

  private void checkIfMembersContainNonNull(SymbolMetadata.NullabilityData classNullabilityData, ClassTree tree) {
    // for all members
    tree.members().forEach(member -> {
      if (member.is(Tree.Kind.VARIABLE)) {
        // check field
        SymbolMetadata.NullabilityData variableNullabilityData = ((VariableTree) member).symbol().metadata().nullabilityData();
        if (variableNullabilityData.isNonNull(VARIABLE, false, false)) {
          reportIssue(member, variableNullabilityData, classNullabilityData);
        }
      } else if (member.is(Tree.Kind.METHOD)) {
        // check method
        checkIfMethodContainsNonNull(classNullabilityData, (MethodTree) member);
      } else if (member.is(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.RECORD)) {
        // check inner object is not directly annotated
        SymbolMetadata.NullabilityData innerNullabilityData = ((ClassTree) member).symbol().metadata().nullabilityData(SymbolMetadata.NullabilityTarget.CLASS);
        if (innerNullabilityData.isNonNull(CLASS, false, false)) {
          reportIssue(member, innerNullabilityData, classNullabilityData);
        }
        // now recurse to check class members
        checkIfMembersContainNonNull(classNullabilityData, (ClassTree) member);
      }
    });
  }

  private void checkIfMethodContainsNonNull(SymbolMetadata.NullabilityData classNullabilityData, MethodTree method) {
    // check return type at method level - do not look up hierarchy
    SymbolMetadata.NullabilityData methodNullabilityData = method.symbol().metadata().nullabilityData();
    if (methodNullabilityData.isNonNull(METHOD, false, false)) {
      reportIssue(method, methodNullabilityData, classNullabilityData);
    }
    // check parameters at variable level - do not look up hierarchy
    method.parameters().forEach(parameter -> {
      SymbolMetadata.NullabilityData parameterNullabilityData = parameter.symbol().metadata().nullabilityData();
      if (parameterNullabilityData.isNonNull(VARIABLE, false, false)) {
        reportIssue(parameter, parameterNullabilityData, classNullabilityData);
      }
    });
  }

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

}
