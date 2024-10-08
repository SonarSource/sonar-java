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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.CLASS;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.METHOD;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.VARIABLE;

@Rule(key = "S6665")
public class RedundantNullabilityAnnotationsCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove redundant nullability annotation.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    // am I an outer class - will return default package if necessary
    if (Objects.requireNonNull(classTree.symbol().owner()).isPackageSymbol()) {
      // get nullability directly at class target level
      SymbolMetadata.NullabilityData highestData = classTree.symbol().metadata()
        .nullabilityData(SymbolMetadata.NullabilityTarget.CLASS);
      // if non-null, either directly or inherited from higher entity
      if (highestData.isNonNull(PACKAGE, false, false)) {
        // then check my members are not directly annotated with non-null
        checkMembersAreNotNonNull(classTree);
      }
    }
  }

  private void checkMembersAreNotNonNull(ClassTree tree) {
    // for all members
    tree.members().forEach(member -> {
      if (member.is(Tree.Kind.METHOD)) {
        // check method
        checkMethod((MethodTree) member);
      } else if (member.is(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.RECORD)) {
        // check inner class is not directly annotated
        if (((ClassTree) member).symbol().metadata().nullabilityData(SymbolMetadata.NullabilityTarget.CLASS)
          .isNonNull(CLASS, false, false)) {
          reportIssue(member, ISSUE_MESSAGE);
        }
        checkMembersAreNotNonNull((ClassTree) member);
      }
    });
  }

  private void checkMethod(MethodTree method) {
    // check return type at method level - do not look up hierarchy
    if (method.symbol().metadata().nullabilityData()
      .isNonNull(METHOD, false, false)) {
      reportIssue(method, ISSUE_MESSAGE);
    }
    // check parameters at variable level - do not look up hierarchy
    method.parameters().forEach(parameter -> {
      if (parameter.symbol().metadata().nullabilityData()
        .isNonNull(VARIABLE, false, false)) {
        reportIssue(parameter, ISSUE_MESSAGE);
      }
    });
  }

}
