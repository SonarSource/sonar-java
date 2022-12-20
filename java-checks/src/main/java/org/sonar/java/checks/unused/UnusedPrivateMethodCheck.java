/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.unused;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@DeprecatedRuleKey(ruleKey = "UnusedPrivateMethod", repositoryKey = "squid")
@Rule(key = "S1144")
public class UnusedPrivateMethodCheck extends IssuableSubscriptionVisitor {

  private final List<MethodTree> unusedPrivateMethods = new ArrayList<>();
  private final Set<String> unresolvedMethodNames = new HashSet<>();
  
  private static final String PARAM_ANNOTATION_EXCEPTION = "javax.enterprise.event.Observes";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      // declarations
      Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR,
      // usages
      Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD_REFERENCE, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    reportUnusedPrivateMethods();
    unusedPrivateMethods.clear();
    unresolvedMethodNames.clear();
  }

  private void reportUnusedPrivateMethods() {
    unusedPrivateMethods.stream()
      .filter(methodTree -> !unresolvedMethodNames.contains(methodTree.simpleName().name()))
      .forEach(methodTree -> {
        IdentifierTree simpleName = methodTree.simpleName();
        String methodType = methodTree.is(Tree.Kind.CONSTRUCTOR) ? "constructor" : "method";
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(simpleName)
          .withMessage("Remove this unused private \"%s\" %s.", simpleName.name(), methodType)
          .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove the unused %s", methodType)
            .addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(QuickFixHelper.previousToken(methodTree), false, methodTree, true)))
            .build())
          .report();
      });
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case METHOD:
      case CONSTRUCTOR:
        checkIfUnused((MethodTree) tree);
        break;
      case NEW_CLASS:
        checkIfUnknown((NewClassTree) tree);
        break;
      case METHOD_INVOCATION:
        checkIfUnknown((MethodInvocationTree) tree);
        break;
      case METHOD_REFERENCE:
        checkIfUnknown((MethodReferenceTree) tree);
        break;
      default:
    }
  }

  private void checkIfUnknown(MethodInvocationTree mit) {
    String name = ExpressionUtils.methodName(mit).name();
    addIfArgumentsAreUnknown(mit.arguments(), name);
    addIfUnknownOrAmbiguous(mit.methodSymbol(), name);
  }

  private void checkIfUnknown(NewClassTree nct) {
    String name = constructorName(nct.identifier());
    addIfArgumentsAreUnknown(nct.arguments(), name);
    addIfUnknownOrAmbiguous(nct.methodSymbol(), name);
  }

  private void checkIfUnknown(MethodReferenceTree mref) {
    IdentifierTree methodIdentifier = mref.method();
    addIfUnknownOrAmbiguous(methodIdentifier.symbol(), methodIdentifier.name());
  }

  private void addIfArgumentsAreUnknown(Arguments arguments, String name) {
    // In case of broken semantic, if the argument is unknown, the method call will not have the correct reference.
    if (arguments.stream().anyMatch(arg -> arg.symbolType().isUnknown())) {
      unresolvedMethodNames.add(name);
    }
  }

  private void addIfUnknownOrAmbiguous(Symbol symbol, String name) {
    // In case of broken semantic (overload with unknown args), ECJ wrongly link the symbol to the good overload.
    if (symbol.isUnknown() || (symbol.isMethodSymbol() && ((Symbol.MethodSymbol) symbol).parameterTypes().stream().anyMatch(Type::isUnknown))) {
      unresolvedMethodNames.add(name);
    }
  }

  private static String constructorName(TypeTree typeTree) {
    switch (typeTree.kind()) {
      case PARAMETERIZED_TYPE:
        return constructorName(((ParameterizedTypeTree) typeTree).type());
      case MEMBER_SELECT:
        return ((MemberSelectExpressionTree) typeTree).identifier().name();
      case IDENTIFIER:
        return ((IdentifierTree) typeTree).name();
      default:
        throw new IllegalStateException("Unexpected TypeTree used as constructor.");
    }
  }

  private void checkIfUnused(MethodTree methodTree) {
    Symbol symbol = methodTree.symbol();
    if (isUnusedPrivate(symbol) && hasNoAnnotation(methodTree) && (isConstructorWithParameters(methodTree) || isNotMethodFromSerializable(methodTree, symbol))) {
      unusedPrivateMethods.add(methodTree);
    }
  }

  private static boolean isUnusedPrivate(Symbol symbol) {
    return symbol.isPrivate() && symbol.usages().isEmpty();
  }

  private static boolean hasNoAnnotation(MethodTree methodTree) {
    return methodTree.modifiers().annotations().isEmpty() && methodTree.parameters().stream().noneMatch(UnusedPrivateMethodCheck::hasAllowedAnnotation);
  }
  
  private static boolean hasAllowedAnnotation(VariableTree variableTree) {
    List<AnnotationTree> annotations = variableTree.modifiers().annotations();
    return !annotations.isEmpty() && annotations.stream().anyMatch(UnusedPrivateMethodCheck::isAllowedAnnotation);
  }
  
  private static boolean isAllowedAnnotation(AnnotationTree annotation) {
    Type annotationSymbolType = annotation.symbolType();
    if (annotationSymbolType.is(PARAM_ANNOTATION_EXCEPTION)) {
      return true;
    }
    if (annotationSymbolType.isUnknown()) {
      TypeTree annotationType = annotation.annotationType();
      if (annotationType.is(Tree.Kind.IDENTIFIER)) {
        return "Observes".equals(((IdentifierTree) annotationType).name());
      }
      if (annotationType.is(Tree.Kind.MEMBER_SELECT)) {
        return PARAM_ANNOTATION_EXCEPTION.equals(ExpressionsHelper.concatenate((MemberSelectExpressionTree) annotationType));
      }
    }
    return false;
  }

  private static boolean isConstructorWithParameters(MethodTree methodTree) {
    return methodTree.is(Tree.Kind.CONSTRUCTOR) && !methodTree.parameters().isEmpty();
  }

  private static boolean isNotMethodFromSerializable(MethodTree methodTree, Symbol symbol) {
    return methodTree.is(Tree.Kind.METHOD) && !SerializableContract.SERIALIZABLE_CONTRACT_METHODS.contains(symbol.name());
  }
}
