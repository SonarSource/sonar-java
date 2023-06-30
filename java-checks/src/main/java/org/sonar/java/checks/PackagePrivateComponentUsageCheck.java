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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.model.JavaTree.ParameterizedTypeTreeImpl;
import org.sonar.java.model.PackageUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "PackagePrivateComponentUsageCheck")
public class PackagePrivateComponentUsageCheck extends IssuableSubscriptionVisitor implements EndOfAnalysis {

  private static final String ISSUE_MESSAGE = "This %s is package private but is never used within the package";
  private final HashMap<String, AnalyzerMessage> packagePrivateComponents = new HashMap<>();

  private final HashSet<String> visitedPackagePrivateComponents = new HashSet<>();

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.CLASS, Kind.METHOD, Kind.VARIABLE,
      Kind.METHOD_INVOCATION, Kind.MEMBER_SELECT, Kind.NEW_CLASS, Kind.NEW_ARRAY);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.METHOD)) {
      visitMethod((MethodTree) tree);
    } else if (tree.is(Kind.CLASS)) {
      visitClass((ClassTree) tree);
    } else if (tree.is(Kind.VARIABLE)) {
      visitVariable((VariableTree) tree);
    } else if (tree.is(Kind.METHOD_INVOCATION)) {
      visitMethodInvocation((MethodInvocationTree) tree);
    } else if (tree.is(Kind.MEMBER_SELECT)) {
      visitMemberSelectExpression((MemberSelectExpressionTree) tree);
    } else if (tree.is(Kind.NEW_CLASS)) {
      visitNewClass((NewClassTree) tree);
    } else if (tree.is(Kind.NEW_ARRAY)) {
      visitNewArray((NewArrayTree) tree);
    }
  }

  private void visitMethod(MethodTree tree) {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    if (tree.symbol().isPackageVisibility()) {
      String fn = tree.symbol().signature();
      packagePrivateComponents.put(fn, defaultContext.createAnalyzerMessage(this, tree, String.format(ISSUE_MESSAGE, "method")));
    }
  }

  private void visitClass(ClassTree tree) {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    if (tree.symbol().isPackageVisibility()) {
      String fn = tree.symbol().type().fullyQualifiedName();
      packagePrivateComponents.put(fn, defaultContext.createAnalyzerMessage(this, tree, String.format(ISSUE_MESSAGE, "class")));
    }
  }

  private void visitVariable(VariableTree tree) {
    DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;
    if (tree.symbol().isPackageVisibility() && tree.parent().is(Kind.CLASS)) {
      var parentClass = (ClassTree) tree.parent();
      String fn = parentClass.symbol().type().fullyQualifiedName() + "#" + tree.simpleName();
      packagePrivateComponents.put(fn, defaultContext.createAnalyzerMessage(this, tree, String.format(ISSUE_MESSAGE, "variable")));
    }
  }
  
  private void visitMethodInvocation(MethodInvocationTree tree) {
    String signature = tree.methodSymbol().signature();
    if (isPackagePrivateAndFromCurrentPackage(tree.methodSymbol(), signature)) {
      String key = tree.methodSymbol().signature();
      visitedPackagePrivateComponents.add(key);
    }
    if(tree.arguments() != null) {
      for(ExpressionTree expression : tree.arguments()) {
        if(expression instanceof MethodReferenceTree) {
          var methodRef = (MethodReferenceTree)expression;
          var methodOwnerClass = (IdentifierTree)methodRef.expression();
          var className = methodOwnerClass.symbolType().fullyQualifiedName();
          if(isPackagePrivateAndFromCurrentPackage(methodOwnerClass.symbol(), className)) {
            String key = className + "#" + methodRef.method().toString();
            visitedPackagePrivateComponents.add(key);
            visitedPackagePrivateComponents.add(className);
          }
        }
      }
    }
  }

  private void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    ExpressionTree expression = tree.expression();
    String className = expression.symbolType().fullyQualifiedName();
    boolean shouldBeConsidered = false;
    if (expression.is(Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expression;
      shouldBeConsidered = isPackagePrivateAndFromCurrentPackage(identifier.symbol(), className);
    }
    if (shouldBeConsidered) {
      String key = className + "#" + tree.identifier().name();
      visitedPackagePrivateComponents.add(key);
      visitedPackagePrivateComponents.add(className);
    }
  }

  private void visitNewClass(NewClassTree tree) {
    String fn = tree.identifier().symbolType().fullyQualifiedName();
    if (isPackagePrivateAndFromCurrentPackage(tree.constructorSymbol(), fn)) {
      visitedPackagePrivateComponents.add(fn);
    }
    if (tree.identifier() instanceof ParameterizedTypeTreeImpl) {
      ParameterizedTypeTreeImpl paramType = (ParameterizedTypeTreeImpl) tree.identifier();
      for (TypeTree type : paramType.typeArguments()) {
        String tfn = type.symbolType().fullyQualifiedName();
        if (isPackagePrivateAndFromCurrentPackage(type.symbolType().symbol(), tfn)) {
          visitedPackagePrivateComponents.add(tfn);
        }
      }
    }
  }

  private void visitNewArray(NewArrayTree tree) {
    if(tree.type() != null) {
      String fn = tree.type().symbolType().fullyQualifiedName();
      if (isPackagePrivateAndFromCurrentPackage(tree.type().symbolType().symbol(), fn)) {
        visitedPackagePrivateComponents.add(fn);
      }
    }
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    DefaultModuleScannerContext defaultContext = (DefaultModuleScannerContext) context;
    visitedPackagePrivateComponents.forEach(packagePrivateComponents::remove);
    packagePrivateComponents.values().forEach(defaultContext::reportIssue);
  }

  private boolean isPackagePrivateAndFromCurrentPackage(Symbol symbol, String signature) {
    return symbol.isPackageVisibility() && belongsToCurrentlyAnalyzedPackage(signature);
  }

  private boolean belongsToCurrentlyAnalyzedPackage(String signature) {
    var packageDeclaration = context.getTree().packageDeclaration();
    String currentFilePackage = PackageUtils.packageName(packageDeclaration, ".");
    return signature.contains(currentFilePackage);
  }

}

