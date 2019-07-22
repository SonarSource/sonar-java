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
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.ArrayList;
import java.util.List;

@Rule(key = "S2226")
public class ServletInstanceFieldCheck extends IssuableSubscriptionVisitor {

  private List<VariableTree> issuableVariables = new ArrayList<>();
  private List<VariableTree> excludedVariables = new ArrayList<>();

  private static final List<String> ANNOTATIONS_EXCLUDING_FIELDS = Arrays.asList(
    "javax.inject.Inject",
    "javax.ejb.EJB",
    "org.springframework.beans.factory.annotation.Autowired",
    "javax.annotation.Resource");

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.VARIABLE, Kind.METHOD);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    reportIssuesOnVariable();
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    if (tree.is(Kind.METHOD) && isServletInit((MethodTree) tree)) {
      tree.accept(new AssignmentVisitor());
    } else if (tree.is(Kind.VARIABLE)) {
      VariableTree variable = (VariableTree) tree;
      if (isOwnedByAServlet(variable) && !isExcluded(variable)) {
        issuableVariables.add(variable);
      }
    }
  }

  private static boolean isExcluded(VariableTree variable) {
    SymbolMetadata varMetadata = variable.symbol().metadata();
    return isStaticOrFinal(variable) || ANNOTATIONS_EXCLUDING_FIELDS.stream().anyMatch(varMetadata::isAnnotatedWith);
  }

  private static boolean isServletInit(MethodTree tree) {
    return "init".equals(tree.simpleName().name()) && tree.parameters().size() == 1 && tree.parameters().get(0).symbol().type().is("javax.servlet.ServletConfig");
  }

  private void reportIssuesOnVariable() {
    issuableVariables.removeAll(excludedVariables);
    for (VariableTree variable : issuableVariables) {
      reportIssue(variable.simpleName(), "Remove this misleading mutable servlet instance field or make it \"static\" and/or \"final\"");
    }
    issuableVariables.clear();
    excludedVariables.clear();
  }

  private class AssignmentVisitor extends BaseTreeVisitor {
    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable().is(Kind.IDENTIFIER)) {
        Tree declaration = ((IdentifierTree) tree.variable()).symbol().declaration();
        if (declaration != null && declaration.is(Kind.VARIABLE)) {
          excludedVariables.add((VariableTree) declaration);
        }
      }
    }
  }

  private static boolean isOwnedByAServlet(VariableTree variable) {
    Symbol owner = variable.symbol().owner();
    return owner.isTypeSymbol()
      && variable.parent().is(Tree.Kind.CLASS)
      && (owner.type().isSubtypeOf("javax.servlet.http.HttpServlet")
      || owner.type().isSubtypeOf("org.apache.struts.action.Action"));
  }

  private static boolean isStaticOrFinal(VariableTree variable) {
    ModifiersTree modifiers = variable.modifiers();
    return ModifiersUtils.hasModifier(modifiers, Modifier.STATIC) || ModifiersUtils.hasModifier(modifiers, Modifier.FINAL);
  }

}
