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
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(
  key = "S1206",
  priority = Priority.BLOCKER,
  tags={"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class EqualsOverridenWithHashCodeCheck extends SubscriptionBaseVisitor {

  private static final String HASHCODE = "hashCode";
  private static final String EQUALS = "equals";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }
  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.is(Tree.Kind.CLASS)) {
      MethodTree equalsMethod = null;
      MethodTree hashCodeMethod = null;
      for (Tree memberTree : classTree.members()) {
        if (memberTree.is(Tree.Kind.METHOD)) {
          MethodTree methodTree = (MethodTree) memberTree;
          if (isEquals(methodTree)) {
            equalsMethod = methodTree;
          } else if (isHashCode(methodTree)) {
            hashCodeMethod = methodTree;
          }
        }
      }

      if (equalsMethod != null && hashCodeMethod == null) {
        addIssue(equalsMethod, getMessage(EQUALS, HASHCODE));
      } else if (hashCodeMethod != null && equalsMethod == null) {
        addIssue(hashCodeMethod, getMessage(HASHCODE, EQUALS));
      }
    }
  }

  private boolean isEquals(MethodTree methodTree) {
    return EQUALS.equals(methodTree.simpleName().name()) && hasObjectParam(methodTree) && returnsType(methodTree, Type.BOOLEAN);
  }

  private boolean isHashCode(MethodTree methodTree) {
    return HASHCODE.equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty() && returnsType(methodTree, Type.INT);
  }


  private boolean hasObjectParam(MethodTree tree) {
    if (tree.parameters().size() == 1) {
      Tree type = tree.parameters().get(0).type();
      //FIXME : should rely on type symbol when problem of expression visitor is solved.
      String name = "";
      if(type.is(Tree.Kind.MEMBER_SELECT)){
        name = ((MemberSelectExpressionTree) type).identifier().name();
      }else if(type.is(Tree.Kind.IDENTIFIER)) {
        name = ((IdentifierTree) type).name();
      }
      return name.endsWith("Object");
    }
    return false;
  }

  private boolean returnsType(MethodTree tree, int typeTag) {
    Symbol.MethodSymbol methodSymbol = ((MethodTreeImpl) tree).getSymbol();
    return methodSymbol != null && methodSymbol.getReturnType().getType().isTagged(typeTag);
  }


  private String getMessage(String overridenMethod, String methodToOverride) {
    return "This class overrides \"" + overridenMethod + "()\" and should therefore also override \"" + methodToOverride + "()\".";
  }

}
