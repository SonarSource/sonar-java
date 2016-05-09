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

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import java.util.List;

@Rule(key = "S3020")
public class ToArrayCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher COLLECTION_TO_ARRAY = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.util.Collection")).name("toArray");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (COLLECTION_TO_ARRAY.matches(mit)) {
      Tree parent = tree.parent();
      if (parent.is(Tree.Kind.TYPE_CAST)) {
        checkCast(((TypeCastTree) parent).symbolType(), mit);
      }
    }
  }

  private void checkCast(Type type, MethodInvocationTree mit) {
    if (type.isArray() && !type.is("java.lang.Object[]")) {
      Type elementType = ((Type.ArrayType) type).elementType();
      if (!((JavaType) elementType).isTagged(JavaType.TYPEVAR)) {
        reportIssue(mit, "Pass \"new " + elementType.name() + "[0]\" as argument to \"toArray\".");
      }
    }
  }

}
