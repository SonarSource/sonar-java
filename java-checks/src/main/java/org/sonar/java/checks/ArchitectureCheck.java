/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.NoSqale;
import org.sonar.squidbridge.annotations.RuleTemplate;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Rule(
  key = "ArchitecturalConstraint",
  name = "Architectural constraints should be followed",
  priority = Priority.MAJOR)
@RuleTemplate
@NoSqale
public class ArchitectureCheck extends SubscriptionBaseVisitor {

  @RuleProperty(description = "Optional. If this property is not defined, all classes should adhere to this constraint. Ex : **.web.**")
  String fromClasses = "";

  @RuleProperty(description = "Mandatory. Ex : java.util.Vector, java.util.Hashtable, java.util.Enumeration")
  String toClasses = "";

  private WildcardPattern[] fromPatterns;
  private WildcardPattern[] toPatterns;

  private Deque<String> shouldCheck = new LinkedList<>();
  private Deque<Set<String>> issues = new LinkedList<>();
  private Deque<Symbol> currentType = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      check((IdentifierTree) tree);
    } else {
      initClass((ClassTree) tree);
    }
  }

  private void check(IdentifierTree tree) {
    String shouldCheckId = shouldCheck.peekFirst();
    if (shouldCheckId == null) {
      return;
    }
    Symbol symbol = tree.symbol();
    if (!symbol.isUnknown() && !currentType.contains(symbol.owner())) {
      Type type = symbol.type();
      if (type != null) {
        String fullyQualifiedName = type.fullyQualifiedName();
        Set<String> currentIssues = issues.peekFirst();
        if (!currentIssues.contains(fullyQualifiedName) && WildcardPattern.match(getToPatterns(), fullyQualifiedName)) {
          addIssue(tree, shouldCheckId + " must not use " + fullyQualifiedName);
          currentIssues.add(fullyQualifiedName);
        }
      }
    }
  }

  private void initClass(ClassTree tree) {
    String fullyQualifiedName = ((JavaSymbol.TypeJavaSymbol) tree.symbol()).getFullyQualifiedName();
    if (WildcardPattern.match(getFromPatterns(), fullyQualifiedName)) {
      shouldCheck.addFirst(fullyQualifiedName);
      issues.addFirst(new HashSet<String>());
    } else {
      shouldCheck.addFirst(null);
      issues.addFirst(null);
    }
    currentType.push(tree.symbol());
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!tree.is(Tree.Kind.IDENTIFIER)) {
      shouldCheck.removeFirst();
      issues.removeFirst();
      currentType.pop();
    }
  }

  private WildcardPattern[] getFromPatterns() {
    if (fromPatterns == null) {
      fromPatterns = PatternUtils.createPatterns(StringUtils.defaultIfEmpty(fromClasses, "**"));
    }
    return fromPatterns;
  }

  private WildcardPattern[] getToPatterns() {
    if (toPatterns == null) {
      toPatterns = PatternUtils.createPatterns(toClasses);
    }
    return toPatterns;
  }
}
