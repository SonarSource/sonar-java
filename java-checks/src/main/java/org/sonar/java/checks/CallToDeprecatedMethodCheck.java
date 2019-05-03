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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "CallToDeprecatedMethod")
@RspecKey("S1874")
public class CallToDeprecatedMethodCheck extends IssuableSubscriptionVisitor {

  private int nestedDeprecationLevel = 0;

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    nestedDeprecationLevel = 0;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IDENTIFIER, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (nestedDeprecationLevel == 0) {
      if (tree.is(Tree.Kind.IDENTIFIER)) {
        checkIdentifierIssue((IdentifierTree) tree);
      } else if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        checkMethodIssue((MethodTree) tree);
      }
    }
    if (isDeprecatedMethod(tree) || isDeprecatedClassTree(tree)) {
      nestedDeprecationLevel++;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (isDeprecatedMethod(tree) || isDeprecatedClassTree(tree)) {
      nestedDeprecationLevel--;
    }
  }

  private void checkIdentifierIssue(IdentifierTree identifierTree) {
    if (isSimpleNameOfVariableTreeOrVariableIsDeprecated(identifierTree)) {
      return;
    }
    Symbol symbol = identifierTree.symbol();
    if (isDeprecated(symbol)) {
      String name;
      if (isConstructor(symbol)) {
        name = symbol.owner().name();
      } else {
        name = symbol.name();
      }
      reportIssue(identifierTree, "Remove this use of \"" + name + "\"; it is deprecated.");
    }
  }

  private static boolean isSimpleNameOfVariableTreeOrVariableIsDeprecated(IdentifierTree identifierTree) {
    Tree parent = identifierTree.parent();
    return parent.is(Tree.Kind.VARIABLE) && (identifierTree.equals(((VariableTree) parent).simpleName()) || ((VariableTree) parent).symbol().isDeprecated());
  }

  private void checkMethodIssue(MethodTree methodTree) {
    if(!methodTree.symbol().isDeprecated() && isOverridingDeprecatedConcreteMethod(methodTree.symbol())) {
      reportIssue(methodTree.simpleName(), "Don't override a deprecated method or explicitly mark it as \"@Deprecated\".");
    }
  }

  private static boolean isDeprecated(Symbol symbol) {
    return symbol.isDeprecated() || (isConstructor(symbol) && symbol.owner().isDeprecated()) || isDeprecatedEnumConstant(symbol);
  }

  private static boolean isDeprecatedEnumConstant(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.isEnum() && symbol.type().symbol().isDeprecated();
  }

  private static boolean isConstructor(Symbol symbol) {
    return symbol.isMethodSymbol() && "<init>".equals(symbol.name());
  }

  private static boolean isOverridingDeprecatedConcreteMethod(Symbol.MethodSymbol symbol) {
    Symbol.MethodSymbol overriddenMethod = symbol.overriddenSymbol();
    while(overriddenMethod != null && !overriddenMethod.isUnknown()) {
      if (overriddenMethod.isAbstract()) {
        return false;
      }
      if (overriddenMethod.isDeprecated()) {
        return true;
      }
      overriddenMethod = overriddenMethod.overriddenSymbol();
    }
    return false;
  }

  private static boolean isDeprecatedMethod(Tree tree) {
    return tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR) && ((MethodTree) tree).symbol().isDeprecated();
  }

  private static boolean isDeprecatedClassTree(Tree tree) {
    return tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) && ((ClassTree) tree).symbol().isDeprecated();
  }
}
