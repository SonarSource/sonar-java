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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SyntaxNodePredicates;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S2653",
  name = "Web applications should not have a \"main\" method",
  priority = Priority.CRITICAL,
  tags = {Tag.CWE, Tag.JEE, Tag.SECURITY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("5min")
public class MainInServletCheck extends SubscriptionBaseVisitor {

  private static final Set<String> SERVLET_AND_EJB_CLASSES = ImmutableSet.of(
    "javax.servlet.http.HttpServlet",
    "org.apache.struts.action.Action",
    "javax.ejb.EnterpriseBean"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree node = (ClassTree) tree;
    Symbol.TypeSymbol symbol = node.symbol();
    if (isServletOrEjb(symbol)) {
      for (Tree member : node.members()) {
        if (member.is(Tree.Kind.METHOD) && ((MethodTreeImpl) member).isMainMethod()) {
          addIssue(member, "Remove this unwanted \"main\" method.");
        }
      }
    }
  }

  private static boolean isServletOrEjb(Symbol symbol) {
    if (Iterables.any(SERVLET_AND_EJB_CLASSES, SyntaxNodePredicates.isSubtypeOf(symbol.type()))) {
      return true;
    }
    for (SymbolMetadata.AnnotationInstance annotation : symbol.metadata().annotations()) {
      if (annotation.symbol().type().fullyQualifiedName().startsWith("javax.ejb.")) {
        return true;
      }
    }
    return false;
  }

}
