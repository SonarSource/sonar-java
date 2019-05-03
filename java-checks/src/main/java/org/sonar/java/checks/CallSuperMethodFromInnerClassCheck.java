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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2388")
public class CallSuperMethodFromInnerClassCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (classSymbol != null && isInnerClass(classSymbol) && !extendsOuterClass(classSymbol)) {
      classTree.accept(new MethodInvocationVisitor(classSymbol));
    }
  }

  private static boolean isInnerClass(Symbol symbol) {
    return symbol.owner().isTypeSymbol();
  }

  private static boolean extendsOuterClass(Symbol.TypeSymbol classSymbol) {
    Type superType = classSymbol.superClass();
    return superType != null && superType.erasure().equals(classSymbol.owner().type().erasure());
  }


  private class MethodInvocationVisitor extends BaseTreeVisitor {
    private final Symbol.TypeSymbol classSymbol;

    public MethodInvocationVisitor(Symbol.TypeSymbol classSymbol) {
      this.classSymbol = classSymbol;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol symbol = tree.symbol();
      if (tree.methodSelect().is(Tree.Kind.IDENTIFIER) && isCallToSuperclassMethod(symbol)) {
        String methodName = ((IdentifierTree) tree.methodSelect()).name();
        reportIssue(ExpressionUtils.methodName(tree), "Prefix this call to \"" + methodName + "\" with \"super.\".");
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isCallToSuperclassMethod(Symbol symbol) {
      return symbol.isMethodSymbol() && !isConstructor(symbol) && isInherited(symbol) && outerClassHasMethodWithSameName(symbol);
    }

    private boolean isConstructor(Symbol symbol) {
      return "<init>".equals(symbol.name());
    }

    private boolean isInherited(Symbol symbol) {
      Type methodOwnerType = symbol.owner().type().erasure();
      Type innerType = classSymbol.type().erasure();
      return !symbol.isStatic() && innerType.isSubtypeOf(methodOwnerType)
        && !classSymbol.owner().type().equals(methodOwnerType) && !innerType.equals(methodOwnerType);
    }

    private boolean outerClassHasMethodWithSameName(Symbol symbol) {
      return !((Symbol.TypeSymbol) classSymbol.owner()).lookupSymbols(symbol.name()).isEmpty();
    }

  }
}
