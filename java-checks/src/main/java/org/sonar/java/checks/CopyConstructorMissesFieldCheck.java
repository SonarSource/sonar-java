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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9365")
public class CopyConstructorMissesFieldCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE =
    "This copy constructor leaves eligible fields uninitialized; initialize them explicitly to distinguish omissions from intentional resets.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    MethodTree constructor = (MethodTree) tree;
    if (constructor.parameters().size() != 1 || constructor.block() == null) {
      return;
    }

    Symbol.MethodSymbol constructorSymbol = constructor.symbol();
    Symbol.TypeSymbol owner = constructorSymbol.enclosingClass();
    Type parameterType = constructor.parameters().get(0).symbol().type();
    if (constructorSymbol.isUnknown()
      || owner == null
      || owner.isUnknown()
      || owner.type().isUnknown()
      || parameterType.isUnknown()
      || !owner.type().erasure().equals(parameterType.erasure())) {
      return;
    }

    ClassTree classTree = owner.declaration();
    // Record component fields are initialized implicitly by the canonical constructor.
    if (classTree == null || classTree.is(Tree.Kind.RECORD)) {
      return;
    }

    Map<Symbol, VariableTree> eligibleFields = eligibleFields(classTree, owner);
    if (eligibleFields.isEmpty()) {
      return;
    }

    AnalysisResult result = analyzeInitializers(classTree, owner, eligibleFields.keySet())
      .merge(analyze(constructor, owner, eligibleFields.keySet(), new HashSet<>()));
    if (!result.complete) {
      return;
    }

    List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    eligibleFields.forEach((field, declaration) -> {
      if (!result.assignedFields.contains(field)) {
        secondaries.add(new JavaFileScannerContext.Location(
          "Field \"" + field.name() + "\" is not explicitly initialized by this copy constructor.",
          declaration.simpleName()));
      }
    });
    if (!secondaries.isEmpty()) {
      reportIssue(constructor.simpleName(), ISSUE_MESSAGE, secondaries, null);
    }
  }

  private static Map<Symbol, VariableTree> eligibleFields(ClassTree classTree, Symbol.TypeSymbol owner) {
    Map<Symbol, VariableTree> fields = new LinkedHashMap<>();
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(variable -> variable.initializer() == null)
      .filter(variable -> !ModifiersUtils.hasModifier(variable.modifiers(), Modifier.STATIC))
      .filter(variable -> !ModifiersUtils.hasModifier(variable.modifiers(), Modifier.TRANSIENT))
      .filter(variable -> owner.equals(variable.symbol().owner()))
      .forEach(variable -> fields.put(variable.symbol(), variable));
    return fields;
  }

  private static AnalysisResult analyzeInitializers(ClassTree classTree, Symbol.TypeSymbol owner, Set<Symbol> eligibleFields) {
    AnalysisResult result = AnalysisResult.emptyComplete();
    Set<Symbol.MethodSymbol> activeMethods = new HashSet<>();
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.INITIALIZER)) {
        AssignmentCollector collector = new AssignmentCollector(owner, eligibleFields, activeMethods);
        member.accept(collector);
        result = result.merge(collector.result());
      }
    }
    return result;
  }

  private static AnalysisResult analyze(MethodTree method, Symbol.TypeSymbol owner, Set<Symbol> eligibleFields,
    Set<Symbol.MethodSymbol> activeMethods) {
    Symbol.MethodSymbol methodSymbol = method.symbol();
    if (methodSymbol.isUnknown() || method.block() == null || !activeMethods.add(methodSymbol)) {
      return AnalysisResult.incomplete();
    }
    AssignmentCollector collector = new AssignmentCollector(owner, eligibleFields, activeMethods);
    method.block().accept(collector);
    activeMethods.remove(methodSymbol);
    return collector.result();
  }

  private static final class AssignmentCollector extends BaseTreeVisitor {
    private final Symbol.TypeSymbol owner;
    private final Set<Symbol> eligibleFields;
    private final Set<Symbol.MethodSymbol> activeMethods;
    private final Set<Symbol> assignedFields = new HashSet<>();
    private boolean complete = true;

    private AssignmentCollector(Symbol.TypeSymbol owner, Set<Symbol> eligibleFields, Set<Symbol.MethodSymbol> activeMethods) {
      this.owner = owner;
      this.eligibleFields = eligibleFields;
      this.activeMethods = activeMethods;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      Symbol assignedField = currentInstanceField(tree.variable());
      if (assignedField != null) {
        assignedFields.add(assignedField);
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT)) {
        Symbol assignedField = currentInstanceField(tree.expression());
        if (assignedField != null) {
          assignedFields.add(assignedField);
        }
      }
      super.visitUnaryExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isThisConstructorInvocation(tree)) {
        mergeResolvedTarget(tree.methodSymbol());
      } else if (isInvocationOnThis(tree)) {
        Symbol.MethodSymbol method = tree.methodSymbol();
        if (method.isUnknown()) {
          complete = false;
        } else if (!method.isStatic() && owner.equals(method.enclosingClass())) {
          mergeResolvedTarget(method);
        }
      }
      // Arguments are executed in the current context and may contain assignments or helper calls.
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Local and anonymous class bodies are not executed as part of the current initialization path.
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // Lambda bodies are not executed when the lambda is created.
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (tree.enclosingExpression() != null) {
        tree.enclosingExpression().accept(this);
      }
      tree.arguments().forEach(argument -> argument.accept(this));
      // Deliberately do not visit an anonymous class body.
    }

    private void mergeResolvedTarget(Symbol.MethodSymbol method) {
      if (method.isUnknown() || !owner.equals(method.enclosingClass())) {
        complete = false;
        return;
      }
      MethodTree declaration = method.declaration();
      if (declaration == null || declaration.block() == null) {
        complete = false;
        return;
      }
      AnalysisResult nested = analyze(declaration, owner, eligibleFields, activeMethods);
      assignedFields.addAll(nested.assignedFields);
      complete &= nested.complete;
    }

    private Symbol currentInstanceField(ExpressionTree expression) {
      ExpressionTree variable = ExpressionUtils.skipParentheses(expression);
      if (variable instanceof IdentifierTree identifier) {
        return eligibleFields.contains(identifier.symbol()) ? identifier.symbol() : null;
      }
      if (variable instanceof MemberSelectExpressionTree memberSelect
        && isCurrentInstance(memberSelect.expression())
        && eligibleFields.contains(memberSelect.identifier().symbol())) {
        return memberSelect.identifier().symbol();
      }
      return null;
    }

    private AnalysisResult result() {
      return new AnalysisResult(Set.copyOf(assignedFields), complete);
    }

    private static boolean isThisConstructorInvocation(MethodInvocationTree invocation) {
      return invocation.methodSelect() instanceof IdentifierTree identifier && "this".equals(identifier.name());
    }

    private boolean isInvocationOnThis(MethodInvocationTree invocation) {
      if (invocation.methodSelect() instanceof IdentifierTree identifier) {
        return !"super".equals(identifier.name());
      }
      return invocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect
        && isCurrentInstance(memberSelect.expression());
    }

    private boolean isCurrentInstance(ExpressionTree expression) {
      ExpressionTree receiver = ExpressionUtils.skipParentheses(expression);
      while (receiver instanceof TypeCastTree cast) {
        receiver = ExpressionUtils.skipParentheses(cast.expression());
      }
      if (receiver instanceof IdentifierTree identifier) {
        // An unqualified `this` has no type binding in the syntax tree, but is unambiguous.
        return "this".equals(identifier.name());
      }
      if (!(receiver instanceof MemberSelectExpressionTree qualifiedThis)
        || !ExpressionUtils.isThis(qualifiedThis.identifier())) {
        return false;
      }
      Symbol thisSymbol = qualifiedThis.identifier().symbol();
      return !thisSymbol.isUnknown() && owner.equals(thisSymbol.enclosingClass());
    }
  }

  private record AnalysisResult(Set<Symbol> assignedFields, boolean complete) {
    private static AnalysisResult emptyComplete() {
      return new AnalysisResult(Set.of(), true);
    }

    private static AnalysisResult incomplete() {
      return new AnalysisResult(Set.of(), false);
    }

    private AnalysisResult merge(AnalysisResult other) {
      Set<Symbol> mergedAssignments = new HashSet<>(assignedFields);
      mergedAssignments.addAll(other.assignedFields);
      return new AnalysisResult(Set.copyOf(mergedAssignments), complete && other.complete);
    }
  }
}
