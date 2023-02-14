/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1604")
public class AnonymousClassShouldBeLambdaCheck extends BaseTreeVisitor implements JavaFileScanner, JavaVersionAwareVisitor {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private JavaFileScannerContext context;
  private final Set<IdentifierTree> enumConstants = new HashSet<>();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    enumConstants.clear();
    scan(context.getTree());
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    enumConstants.add(tree.simpleName());
    super.visitEnumConstant(tree);
    enumConstants.remove(tree.simpleName());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    super.visitNewClass(tree);
    ClassTree classBody = tree.classBody();
    if (classBody != null) {
      TypeTree identifier = tree.identifier();
      if (!useThisInstance(classBody) && !enumConstants.contains(identifier) && isSAM(classBody)) {
        context.reportIssue(this, identifier, "Make this anonymous inner class a lambda" + context.getJavaVersion().java8CompatibilityMessage());
      }
    }
  }

  private static boolean isSAM(ClassTree classBody) {
    if (hasOnlyOneMethod(classBody.members())) {
      // When overriding only one method of a functional interface, it can only be the single abstract method
      // and not one of the default methods. No need to check that the method signature matches.
      Symbol.TypeSymbol symbol = classBody.symbol();
      // should be anonymous class of interface and not abstract class
      return symbol.interfaces().size() == 1
        && symbol.superClass().is(JAVA_LANG_OBJECT)
        && hasSingleAbstractMethodInHierarchy(JUtils.superTypes(symbol));
    }
    return false;
  }

  private static boolean hasSingleAbstractMethodInHierarchy(Set<Type> superTypes) {
    return superTypes.stream()
      .filter(type -> !type.is(JAVA_LANG_OBJECT))
      .map(Type::symbol)
      // collect all the methods declared in hierarchy
      .flatMap(superType -> superType.memberSymbols().stream().filter(Symbol::isMethodSymbol).filter(Symbol::isAbstract))
      .map(Symbol.MethodSymbol.class::cast)
      // remove objects methods redefined in interfaces
      .filter(symbol -> !isObjectMethod(symbol))
      // remove generic methods, which can not be written as lambda (JLS-11 §15.27)
      .filter(symbol -> !JUtils.isParametrizedMethod(symbol))
      // always take same symbol if method is redeclared over and over in hierarchy
      .map(AnonymousClassShouldBeLambdaCheck::overridenSymbolIfAny)
      .collect(Collectors.toSet())
      .size() == 1;
  }

  private static Symbol.MethodSymbol overridenSymbolIfAny(MethodSymbol symbol) {
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
    // if overriden method declares to throw an exception, refactoring to a lambda might prove tricky
    // if it is annotated with something else than @Override, it is not possible to refactor the code
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
      // visit the class body but ignore inner classes
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
      // ignore identifier, because if it is this, it is a qualified this.
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
