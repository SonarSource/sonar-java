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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1160", priority = Priority.MAJOR, tags = {"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ThrowsSeveralCheckedExceptionCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && isPublic(methodTree) && !isPublicStaticVoidMain(methodTree)) {
      List<String> thrownCheckedExceptions = getThrownCheckedExceptions(methodTree);
      if (thrownCheckedExceptions.size() > 1 && isNotOverriden(methodTree)) {
        addIssue(methodTree, "Refactor this method to throw at most one checked exception instead of: " + Joiner.on(", ").join(thrownCheckedExceptions));
      }
    }
  }

  private boolean isPublicStaticVoidMain(MethodTree methodTree) {
    return "main".equals(methodTree.simpleName().name()) && hasStringArrayParam(methodTree) && returnsVoid(methodTree) && isStatic(methodTree);
  }

  private boolean hasStringArrayParam(MethodTree methodTree) {
    if(methodTree.parameters().size()==1){
      Tree argType = methodTree.parameters().get(0).type();
      if(argType.is(Tree.Kind.ARRAY_TYPE) && ((ArrayTypeTree) argType).type().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) ((ArrayTypeTree) argType).type();
        return "String".equals(identifierTree.name()) || "java.lang.String".equals(identifierTree.name());
      }
    }
    return false;
  }

  private boolean returnsVoid(MethodTree methodTree) {
    Tree returnType = methodTree.returnType();
    if(returnType != null) {
      return returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
    }
    return false;
  }

  private boolean isStatic(MethodTree methodTree) {
    return methodTree.modifiers().modifiers().contains(Modifier.STATIC);
  }

  private boolean isNotOverriden(MethodTree methodTree) {
    return BooleanUtils.isFalse(((MethodTreeImpl) methodTree).isOverriding());
  }

  private boolean isPublic(MethodTree methodTree) {
    return ((MethodTreeImpl) methodTree).getSymbol().isPublic();
  }

  private List<String> getThrownCheckedExceptions(MethodTree methodTree) {
    List<Symbol.TypeSymbol> thrownClasses = ((MethodTreeImpl) methodTree).getSymbol().getThrownTypes();
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (Symbol.TypeSymbol thrownClass : thrownClasses) {
      if (!isSubClassOfRuntimeException(thrownClass)) {
        builder.add(thrownClass.owner().getName() + "." + thrownClass.getName());
      }
    }
    return builder.build();
  }

  private static boolean isSubClassOfRuntimeException(Symbol.TypeSymbol thrownClass) {
    Symbol.TypeSymbol typeSymbol = thrownClass;
    while (typeSymbol != null) {
      if (isRuntimeException(typeSymbol)) {
        return true;
      }
      Type superType = typeSymbol.getSuperclass();
      if (superType == null) {
        typeSymbol = null;
      } else {
        typeSymbol = ((Type.ClassType) superType).getSymbol();
      }
    }
    return false;
  }

  private static boolean isRuntimeException(Symbol.TypeSymbol thrownClass) {
    return "RuntimeException".equals(thrownClass.getName()) && "java.lang".equals(thrownClass.owner().getName());
  }

}
