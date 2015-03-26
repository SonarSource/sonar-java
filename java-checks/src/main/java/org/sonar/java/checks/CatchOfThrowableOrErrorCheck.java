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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1181",
  name = "Throwable and Error should not be caught",
  tags = {"cert", "cwe", "error-handling"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.EXCEPTION_HANDLING)
@SqaleConstantRemediation("20min")
public class CatchOfThrowableOrErrorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeTree typeTree = ((CatchTree) tree).parameter().type();

    if (typeTree.is(Kind.UNION_TYPE)) {
      UnionTypeTree unionTypeTree = (UnionTypeTree) typeTree;
      for (TypeTree typeAlternativeTree : unionTypeTree.typeAlternatives()) {
        checkType(typeAlternativeTree);
      }
    } else {
      checkType(typeTree);
    }
  }

  private void checkType(TypeTree tree) {
    Type type = tree.symbolType();
    if (type.is("java.lang.Throwable") || type.is("java.lang.Error")) {
      addIssue(tree, "Catch Exception instead of " + type.name() + ".");
    }
  }
}
