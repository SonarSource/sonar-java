/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class AnonymousClassToLambdaUtils {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private AnonymousClassToLambdaUtils() {
  }

  public static boolean canBeConvertedToLambda(ClassTree classBody, Set<IdentifierTree> enumConstants) {
    var identifier = ((NewClassTree) classBody.parent()).identifier();
    return !useThisInstance(classBody) && !enumConstants.contains(identifier) && isSAM(classBody);
  }

  private static boolean isSAM(ClassTree classBody) {
    if (hasOnlyOneMethod(classBody.members())) {
      Symbol.TypeSymbol symbol = classBody.symbol();
      return symbol.interfaces().size() == 1
        && symbol.superClass().is(JAVA_LANG_OBJECT)
        && hasSingleAbstractMethodInHierarchy(symbol.superTypes());
    }
    return false;
  }

  private static boolean hasSingleAbstractMethodInHierarchy(Set<Type> superTypes) {
    return superTypes.stream()
      .filter(type -> !type.is(JAVA_LANG_OBJECT))
      .map(Type::symbol)
      .flatMap(superType -> superType.memberSymbols().stream().filter(Symbol::isMethodSymbol).filter(Symbol::isAbstract))
      .map(Symbol.MethodSymbol.class::cast)
      .filter(symbol -> !isObjectMethod(symbol))
      .filter(symbol -> !symbol.isParametrizedMethod())
      .map(AnonymousClassToLambdaUtils::overriddenSymbolIfAny)
      .collect(Collectors.toSet())
      .size() == 1;
  }

  private static Symbol.MethodSymbol overriddenSymbolIfAny(MethodSymbol symbol) {
    return symbol.overriddenSymbols().stream()
      .findFirst()
      .orElse(symbol);
  }

  private static boolean isObjectMethod(Symbol.MethodSymbol methodSymbol) {
    return methodSymbol.overriddenSymbols().stream()
      .map(Symbol::owner)
      .map(Symbol::type)
      .anyMatch(t -> t.is(JAVA_LANG_OBJECT));
  }

  private static boolean hasOnlyOneMethod(List<Tree> members) {
    MethodTree methodTree = null;
    for (Tree tree : members) {
      if (!tree.is(Tree.Kind.EMPTY_STATEMENT, Tree.Kind.METHOD)) {
        return false;
      }
      if (tree.is(Tree.Kind.METHOD)) {
        if (methodTree != null) {
          return false;
        }
        methodTree = (MethodTree) tree;
      }
    }
    return methodTree != null && canRefactorMethod(methodTree);
  }

  private static boolean canRefactorMethod(MethodTree methodTree) {
    return methodTree.throwsClauses().isEmpty()
      && methodTree.symbol().metadata().annotations().stream()
      .allMatch(annotation -> annotation.symbol().type().is("java.lang.Override"));
  }

  private static boolean useThisInstance(ClassTree body) {
    UsesThisInstanceVisitor visitor = new UsesThisInstanceVisitor(body.symbol().type());
    body.accept(visitor);
    return visitor.usesThisInstance;
  }

  private static class UsesThisInstanceVisitor extends BaseTreeVisitor {
    private final Type instanceType;
    boolean usesThisInstance = false;
    boolean visitedClassTree = false;

    public UsesThisInstanceVisitor(Type instanceType) {
      this.instanceType = instanceType;
    }

    @Override
    public void visitClass(ClassTree tree) {
      if (!visitedClassTree) {
        visitedClassTree = true;
        super.visitClass(tree);
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // ignore anonymous classes
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      scan(tree.expression());
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.methodSelect().is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) tree.methodSelect()).symbol();
        usesThisInstance |= symbol.isMethodSymbol() &&
          !symbol.isStatic() &&
          instanceType.isSubtypeOf(symbol.owner().type());
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      usesThisInstance |= "this".equals(tree.name());
    }
  }

}
