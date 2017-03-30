/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "RedundantThrowsDeclarationCheck")
@RspecKey("S1130")
public class RedundantThrowsDeclarationCheck extends IssuableSubscriptionVisitor {

  public static final Set<String> SERIALIZABLE_CONTRACT_METHODS = ImmutableSet.of(
    "writeObject",
    "writeReplace",
    "readObject",
    "readResolve",
    "readObjectNoData");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    ListTree<TypeTree> thrownList = methodTree.throwsClauses();

    Set<Type> thrownExceptions = thrownExceptionsFromBody(methodTree);

    Set<String> reported = new HashSet<>();
    for (TypeTree typeTree : thrownList) {
      Type exceptionType = typeTree.symbolType();
      String fullyQualifiedName = exceptionType.fullyQualifiedName();
      if (!reported.contains(fullyQualifiedName)) {
        String superTypeName = isSubclassOfAny(exceptionType, thrownList);
        if (superTypeName != null) {
          reportIssue(typeTree, String.format("Remove the declaration of thrown exception '%s' which is a subclass of '%s'.", fullyQualifiedName, superTypeName));
        } else if (exceptionType.isSubtypeOf("java.lang.RuntimeException")) {
          reportIssue(typeTree, String.format("Remove the declaration of thrown exception '%s' which is a runtime exception.", fullyQualifiedName));
        } else if (declaredMoreThanOnce(fullyQualifiedName, thrownList)) {
          reportIssue(typeTree, String.format("Remove the redundant '%s' thrown exception declaration(s).", fullyQualifiedName));
        } else if (canNotBeThrown(methodTree, exceptionType, thrownExceptions)) {
          reportIssue(typeTree, String.format("Remove the declaration of thrown exception '%s', as it cannot be thrown from %s's body.", fullyQualifiedName,
            tree.is(Tree.Kind.CONSTRUCTOR) ? "constructor" : "method"));
        }
        reported.add(fullyQualifiedName);
      }
    }
  }

  private static boolean canNotBeThrown(MethodTree methodTree, Type exceptionType, @Nullable Set<Type> thrownExceptions) {
    if (isOverridingOrDesignedForExtension(methodTree)
      || !exceptionType.isSubtypeOf("java.lang.Exception")
      || exceptionType.isSubtypeOf("java.lang.RuntimeException")
      || thrownExceptions == null) {
      return false;
    }

    if (thrownExceptions.stream().anyMatch(t -> ((JavaType) t).isTagged(JavaType.TYPEVAR))) {
      // kill the noise due to SONARJAVA-1778 - type substitution not applied on thrown type when parameterized on parametric methods
      return false;
    }

    return thrownExceptions.stream().noneMatch(t -> t.isSubtypeOf(exceptionType));
  }

  private static boolean isOverridingOrDesignedForExtension(MethodTree methodTree) {
    // we need to be sure that it's not an override
    return !Boolean.FALSE.equals(((MethodTreeImpl) methodTree).isOverriding())
      || SERIALIZABLE_CONTRACT_METHODS.contains(methodTree.simpleName().name())
      // kill the noise - usually designed for extension
      || isDesignedForExtension(methodTree);
  }

  private static boolean isDesignedForExtension(MethodTree methodTree) {
    return !ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE)
      && (emptyBody(methodTree) || onlyReturnLiteralsOrThrowException(methodTree));
  }

  private static boolean onlyReturnLiteralsOrThrowException(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block == null) {
      return false;
    }
    List<StatementTree> body = block.body();
    if (body.size() != 1) {
      return false;
    }
    StatementTree singleStatement = body.get(0);
    return singleStatement.is(Tree.Kind.THROW_STATEMENT) 
      || (singleStatement.is(Tree.Kind.RETURN_STATEMENT) && ExpressionUtils.skipParentheses(((ReturnStatementTree) singleStatement).expression()).is(
      Tree.Kind.NULL_LITERAL,
      Tree.Kind.STRING_LITERAL,
      Tree.Kind.BOOLEAN_LITERAL,
      Tree.Kind.CHAR_LITERAL,
      Tree.Kind.DOUBLE_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.INT_LITERAL));
  }

  private static boolean emptyBody(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    return block != null && block.body().isEmpty();
  }

  @Nullable
  private static Set<Type> thrownExceptionsFromBody(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null) {
      MethodInvocationVisitor visitor = new MethodInvocationVisitor();
      block.accept(visitor);
      return visitor.thrownExceptions();
    }
    return null;
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {
    private Set<Type> thrownExceptions = new HashSet<>();
    private boolean visitedUnknown = false;

    @Nullable
    public Set<Type> thrownExceptions() {
      if (visitedUnknown || thrownExceptions.stream().anyMatch(Type::isUnknown)) {
        // as soon as there is an unknown type, we discard any attempt to find an issue
        return null;
      }
      return thrownExceptions;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      addThrownTypes(tree.symbol());
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      addThrownTypes(tree.constructorSymbol());
      super.visitNewClass(tree);
    }

    private void addThrownTypes(Symbol methodSymbol) {
      if (!visitedUnknown) {
        if (methodSymbol.isUnknown() || !methodSymbol.isMethodSymbol()) {
          visitedUnknown = true;
        } else {
          thrownExceptions.addAll(((Symbol.MethodSymbol) methodSymbol).thrownTypes());
        }
      }
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      Type exceptionType = tree.expression().symbolType();
      thrownExceptions.add(exceptionType);
      super.visitThrowStatement(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip anonymous classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }
  }

  private static boolean declaredMoreThanOnce(String fullyQualifiedName, ListTree<TypeTree> thrown) {
    boolean firstOccurrenceFound = false;
    for (TypeTree typeTree : thrown) {
      if (typeTree.symbolType().is(fullyQualifiedName)) {
        if (firstOccurrenceFound) {
          return true;
        } else {
          firstOccurrenceFound = true;
        }
      }
    }
    return false;
  }

  private static String isSubclassOfAny(Type type, ListTree<TypeTree> thrownList) {
    for (TypeTree thrown : thrownList) {
      String name = thrown.symbolType().fullyQualifiedName();
      if (!type.is(name) && type.isSubtypeOf(name)) {
        return name;
      }
    }
    return null;
  }
}
