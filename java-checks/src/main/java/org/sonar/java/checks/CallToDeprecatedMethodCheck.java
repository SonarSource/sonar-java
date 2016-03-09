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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "CallToDeprecatedMethod",
  name = "\"@Deprecated\" code should not be used",
  priority = Priority.MINOR,
  tags = {Tag.CWE, Tag.OBSOLETE, Tag.OWASP_A9, Tag.SECURITY})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SOFTWARE_RELATED_PORTABILITY)
@SqaleConstantRemediation("15min")
public class CallToDeprecatedMethodCheck extends IssuableSubscriptionVisitor {

  private int nestedDeprecationLevel = 0;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    nestedDeprecationLevel = 0;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.IDENTIFIER, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) tree).symbol();
      if (isDeprecated(symbol) && nestedDeprecationLevel == 0) {
        String name;
        if (isConstructor(symbol)) {
          name = symbol.owner().name();
        } else {
          name = symbol.name();
        }
        reportIssue(tree, "Remove this use of \"" + name + "\"; it is deprecated.");
      }
    } else if (isDeprecatedMethod(tree) || isDeprecatedClassTree(tree)) {
      nestedDeprecationLevel++;
    }

  }

  private static boolean isDeprecated(Symbol symbol) {
    return symbol.isDeprecated() || (isConstructor(symbol) && symbol.owner().isDeprecated());
  }

  private static boolean isConstructor(Symbol symbol) {
    return symbol.isMethodSymbol() && "<init>".equals(symbol.name());
  }

  @Override
  public void leaveNode(Tree tree) {
    if (isDeprecatedMethod(tree) || isDeprecatedClassTree(tree)) {
      nestedDeprecationLevel--;
    }
  }

  private static boolean isDeprecatedMethod(Tree tree) {
    return tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR) && ((MethodTree) tree).symbol().isDeprecated();
  }

  private static boolean isDeprecatedClassTree(Tree tree) {
    return tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) && ((ClassTree) tree).symbol().isDeprecated();
  }
}
