/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type.ClassType;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(
  key = "S2226",
  priority = Priority.CRITICAL,
  tags = {"bug", "cert", "multi-threading", "struts"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ServletInstanceFieldCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variable = (VariableTree) tree;
    if (hasSemantic() && isOwnedByAServlet(variable) && !isStaticOrFinal(variable)) {
      addIssue(tree, "Remove this misleading mutable servlet instance fields or make it \"static\" and/or \"final\"");
    }
  }

  private boolean isOwnedByAServlet(VariableTree variable) {
    VariableTreeImpl vti = (VariableTreeImpl) variable;
    Symbol owner = vti.getSymbol().owner();
    if (owner.isKind(Symbol.TYP)) {
      TypeSymbol ownerType = (TypeSymbol) owner;
      for (ClassType classType : ownerType.superTypes()) {
        if (classType.is("javax.servlet.http.HttpServlet") || classType.is("org.apache.struts.action.Action")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isStaticOrFinal(VariableTree variable) {
    List<Modifier> modifiers = variable.modifiers().modifiers();
    return modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.FINAL);
  }

}
