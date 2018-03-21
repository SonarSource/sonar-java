/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1604")
public class AnonymousClassShouldBeLambdaCheck extends BaseTreeVisitor implements JavaFileScanner, JavaVersionAwareVisitor {

  private JavaFileScannerContext context;
  private List<IdentifierTree> enumConstants;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    enumConstants = Lists.newArrayList();
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
      if (!useThisIdentifier(classBody) && !enumConstants.contains(identifier) && isSAM(classBody, identifier)) {
        context.reportIssue(this, identifier, "Make this anonymous inner class a lambda" + context.getJavaVersion().java8CompatibilityMessage());
      }
    }
  }

  private static boolean isSAM(ClassTree classBody, TypeTree identifier) {
    List<Tree> members = classBody.members();
    if (hasOnlyOneMethod(members) && classBody.symbol().isTypeSymbol()) {
      // Verify class body is a subtype of an interface
      JavaSymbol.TypeJavaSymbol symbol = (JavaSymbol.TypeJavaSymbol) classBody.symbol();
      MethodTree method = (MethodTree) members.get(0);
      JavaSymbol.TypeJavaSymbol interfaceOwner = (JavaSymbol.TypeJavaSymbol) identifier.symbolType().symbol();
      return symbol.getInterfaces().size() == 1 &&
        symbol.getSuperclass().is("java.lang.Object") &&
        notMultipleDefaultMethodsWithSameSignature((JavaSymbol.MethodJavaSymbol) method.symbol(), interfaceOwner);
    }
    return false;
  }

  private static boolean notMultipleDefaultMethodsWithSameSignature(JavaSymbol.MethodJavaSymbol methodSymbol, JavaSymbol.TypeJavaSymbol interfaceOwner) {
    Set<JavaSymbol.MethodJavaSymbol> methods = new HashSet<>();
    List<TypeJavaSymbol> allInterfacesOfHierarchy = interfaceOwner.superTypes().stream().filter(type -> !type.is("java.lang.Object"))
      .map(ClassJavaType::getSymbol).collect(Collectors.toList());
    for (JavaSymbol.TypeJavaSymbol interfaceOfHierarchy : allInterfacesOfHierarchy) {
      methods.addAll(interfaceOfHierarchy.memberSymbols().stream().filter(Symbol::isMethodSymbol).map(method -> ((JavaSymbol.MethodJavaSymbol) method))
        .filter(method -> !method.name().equals(methodSymbol.name()))
        .filter(method -> sameParameters(method, methodSymbol))
        .collect(Collectors.toList()));
    }
    methods.addAll(
      interfaceOwner.memberSymbols().stream().filter(Symbol::isMethodSymbol).map(method -> ((JavaSymbol.MethodJavaSymbol) method))
        .filter(method -> sameParameters(method, methodSymbol))
        .collect(Collectors.toList()));
    return methodsDefaultAndNotStatic(methods);
  }

  private static boolean methodsDefaultAndNotStatic(Set<JavaSymbol.MethodJavaSymbol> methods) {
    List<JavaSymbol.MethodJavaSymbol> methodsRemained = methods.stream().filter(method -> !method.isStatic() && method.isDefault())
      .collect(Collectors.toList());
    return methodsRemained.isEmpty() || methodsRemained.size() <= 1;
  }

  private static boolean sameParameters(JavaSymbol.MethodJavaSymbol method1, JavaSymbol.MethodJavaSymbol method2) {
    return method1.parameterTypes().equals(method2.parameterTypes());
  }

  private static boolean hasOnlyOneMethod(List<Tree> members) {
    MethodTree methodTree = null;
    for (Tree tree : members) {
      if (!tree.is(Tree.Kind.EMPTY_STATEMENT, Tree.Kind.METHOD)) {
        return false;
      } else if (tree.is(Tree.Kind.METHOD)) {
        if (methodTree != null) {
          return false;
        }
        methodTree = (MethodTree) tree;

      }
    }
    // if overriden method declares to throw an exception, refactoring to a lambda might prove tricky
    return methodTree != null && methodTree.throwsClauses().isEmpty();
  }

  private static boolean useThisIdentifier(ClassTree body) {
    ThisIdentifierVisitor visitor = new ThisIdentifierVisitor();
    body.accept(visitor);
    return visitor.usesThisIdentifier;
  }

  private static class ThisIdentifierVisitor extends BaseTreeVisitor {
    boolean usesThisIdentifier = false;
    boolean visitedClassTree = false;

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
    public void visitIdentifier(IdentifierTree tree) {
      usesThisIdentifier |= "this".equals(tree.name());
    }
  }

}
