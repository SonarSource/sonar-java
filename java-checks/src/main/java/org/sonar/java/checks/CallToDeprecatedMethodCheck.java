/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "CallToDeprecatedMethod",
  name = "Deprecated methods should not be used",
  tags = {"cwe", "obsolete", "owasp-a9", "security"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SOFTWARE_RELATED_PORTABILITY)
@SqaleConstantRemediation("15min")
public class CallToDeprecatedMethodCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol symbol;
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      symbol = ((NewClassTree) tree).constructorSymbol();
    } else {
      symbol = ((MethodInvocationTree) tree).symbol();
    }
    if (symbol.metadata().isAnnotatedWith("java.lang.Deprecated")) {
      String name = symbol.name();
      String message;
      if ("<init>".equals(name)) {
        message = "Constructor '" + symbol.owner().name() + "(...)' is deprecated.";
      } else {
        message = "Method '" + symbol.owner().name() + "." + name + "(...)' is deprecated.";
      }
      addIssue(tree, message);
    }
  }
}
