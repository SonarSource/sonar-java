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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S2111",
    priority = Priority.CRITICAL,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class BigDecimalDoubleConstructorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      NewClassTree nct = (NewClassTree) tree;
      if (((AbstractTypedTree) nct).getSymbolType().is("java.math.BigDecimal") && isDoubleConstructor(nct)) {
        addIssue(tree, "Use \"BigDecimal.valueOf\" instead.");
      }
    }
  }

  private boolean isDoubleConstructor(NewClassTree nct) {
    if (!nct.arguments().isEmpty() && nct.arguments().size() <= 2) {
      Type argumentType = ((AbstractTypedTree) nct.arguments().get(0)).getSymbolType();
      return argumentType.is("double") || argumentType.is("float");
    }
    return false;
  }
}
