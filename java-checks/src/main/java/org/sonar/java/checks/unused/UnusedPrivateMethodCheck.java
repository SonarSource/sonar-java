/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@DeprecatedRuleKey(ruleKey = "UnusedPrivateMethod", repositoryKey = "squid")
@Rule(key = "S1144")
public class UnusedPrivateMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    reportUnusedPrivateMethods(findUnusedPrivateMethods((ClassTree) tree));
  }

  Stream<MethodTree> findUnusedPrivateMethods(ClassTree tree) {
    var collector = new UnusedMethodCollector();
    tree.members().forEach(it -> it.accept(collector));
    var unusedPrivateMethods = collector.unusedPrivateMethods;
    if (unusedPrivateMethods.isEmpty()) {
      return Stream.empty();
    }

    var methodNames = unusedPrivateMethods.stream().map(it -> it.simpleName().name()).collect(Collectors.toSet());
    methodNames.removeAll(collector.unresolvedMethodNames);
    var filter = new MethodsUsedInAnnotationsFilter(methodNames);
    tree.accept(filter);
    return unusedPrivateMethods.stream().filter(it -> filter.filteredNames.contains(it.simpleName().name()));
  }

  private void reportUnusedPrivateMethods(Stream<MethodTree> methods) {
    methods
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

  private static class UnusedMethodCollector extends BaseTreeVisitor {

    public final List<MethodTree> unusedPrivateMethods = new ArrayList<>();
    public final Set<String> unresolvedMethodNames = new HashSet<>();

    private static final Set<String> PARAM_ANNOTATION_EXCEPTIONS = Set.of(
      "javax.enterprise.event.Observes",
      "jakarta.enterprise.event.Observes"
    );

    @Override
    public void visitClass(ClassTree tree) {
      // cut visitation of inner classes
    }

    @Override
    public void visitMethod(MethodTree methodTree) {
      super.visitMethod(methodTree);
      Symbol symbol = methodTree.symbol();
      if (isUnusedPrivate(symbol) && hasNoAnnotation(methodTree) && (isConstructorWithParameters(methodTree) || isNotMethodFromSerializable(methodTree, symbol))) {
        unusedPrivateMethods.add(methodTree);
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      super.visitMethodInvocation(mit);
      String name = ExpressionUtils.methodName(mit).name();
      addIfArgumentsAreUnknown(mit.arguments(), name);
      addIfUnknownOrAmbiguous(mit.methodSymbol(), name);
    }

    @Override
    public void visitMethodReference(MethodReferenceTree mref) {
      super.visitMethodReference(mref);
      IdentifierTree methodIdentifier = mref.method();
      addIfUnknownOrAmbiguous(methodIdentifier.symbol(), methodIdentifier.name());
    }

    @Override
    public void visitNewClass(NewClassTree nct) {
      super.visitNewClass(nct);
      String name = constructorName(nct.identifier());
      addIfArgumentsAreUnknown(nct.arguments(), name);
      addIfUnknownOrAmbiguous(nct.methodSymbol(), name);
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
      return switch (typeTree.kind()) {
        case PARAMETERIZED_TYPE -> constructorName(((ParameterizedTypeTree) typeTree).type());
        case MEMBER_SELECT -> ((MemberSelectExpressionTree) typeTree).identifier().name();
        case IDENTIFIER -> ((IdentifierTree) typeTree).name();
        default -> throw new IllegalStateException("Unexpected TypeTree used as constructor.");
      };
    }

    private static boolean isUnusedPrivate(Symbol symbol) {
      return symbol.isPrivate() && symbol.usages().isEmpty();
    }

    private static boolean hasNoAnnotation(MethodTree methodTree) {
      return methodTree.modifiers().annotations().isEmpty() && methodTree.parameters().stream().noneMatch(UnusedMethodCollector::hasAllowedAnnotation);
    }

    private static boolean hasAllowedAnnotation(VariableTree variableTree) {
      List<AnnotationTree> annotations = variableTree.modifiers().annotations();
      return !annotations.isEmpty() && annotations.stream().anyMatch(UnusedMethodCollector::isAllowedAnnotation);
    }

    private static boolean isAllowedAnnotation(AnnotationTree annotation) {
      Type annotationSymbolType = annotation.symbolType();
      if (PARAM_ANNOTATION_EXCEPTIONS.stream().anyMatch(annotationSymbolType::is)) {
        return true;
      }
      if (annotationSymbolType.isUnknown()) {
        TypeTree annotationType = annotation.annotationType();
        if (annotationType.is(Tree.Kind.IDENTIFIER)) {
          return "Observes".equals(((IdentifierTree) annotationType).name());
        }
        if (annotationType.is(Tree.Kind.MEMBER_SELECT)) {
          String concatenatedAnnotation = ExpressionsHelper.concatenate((MemberSelectExpressionTree) annotationType);
          return PARAM_ANNOTATION_EXCEPTIONS.stream().anyMatch(concatenatedAnnotation::equals);
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

  private static class MethodsUsedInAnnotationsFilter extends BaseTreeVisitor {

    public MethodsUsedInAnnotationsFilter(Set<String> methodNames) {
      this.filteredNames = methodNames;
    }

    private Set<String> filteredNames;

    private static boolean isNameIndicatingMethod(String name) {
      return name.toLowerCase(Locale.getDefault()).contains("method");
    }

    private void removeMethodName(LiteralTree literal) {
      filteredNames.remove(removeQuotes(literal.value()));
    }

    private static String removeQuotes(String withQuotes) {
      return withQuotes.substring(1, withQuotes.length() - 1);
    }

    @Override
    public void visitAnnotation(AnnotationTree annotationTree) {
      var isMethodAnnotation = isNameIndicatingMethod(annotationTree.annotationType().symbolType().name());
      for (var arg : annotationTree.arguments()) {
        if (arg.is(Tree.Kind.STRING_LITERAL)) {
          if (isMethodAnnotation) {
            removeMethodName((LiteralTree) arg);
          }
        } else if (arg instanceof AssignmentExpressionTree asgn && asgn.expression().is(Tree.Kind.STRING_LITERAL) && (
          isMethodAnnotation || isNameIndicatingMethod(((IdentifierTree) asgn.variable()).name())
        )) {
          removeMethodName((LiteralTree) asgn.expression());
        }
      }
    }
  }
}
