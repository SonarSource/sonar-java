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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1068")
public class UnusedPrivateFieldCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_IGNORE_ANNOTATIONS_KEY = "ignoreAnnotations";
  private static final String DEFAULT_IGNORE_ANNOTATIONS_DESCRIPTION = "Ignore annotations with next names (fully qualified class names separated with \",\").";

  private static final Set<String> OWNER_CLASS_ALLOWED_ANNOTATIONS = Set.of(
    "lombok.Data",
    "lombok.Getter",
    "lombok.Setter",
    "lombok.AllArgsConstructor"
  );

  private static final Tree.Kind[] ASSIGNMENT_KINDS = {
    Tree.Kind.ASSIGNMENT,
    Tree.Kind.MULTIPLY_ASSIGNMENT,
    Tree.Kind.DIVIDE_ASSIGNMENT,
    Tree.Kind.REMAINDER_ASSIGNMENT,
    Tree.Kind.PLUS_ASSIGNMENT,
    Tree.Kind.MINUS_ASSIGNMENT,
    Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
    Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.AND_ASSIGNMENT,
    Tree.Kind.XOR_ASSIGNMENT,
    Tree.Kind.OR_ASSIGNMENT};

  private final List<ClassTree> classes = new ArrayList<>();
  private final Map<Symbol, List<AssignmentExpressionTree>> assignments = new HashMap<>();
  private final Set<String> unknownIdentifiers = new HashSet<>();
  private boolean hasNativeMethod = false;

  @RuleProperty(
    key = DEFAULT_IGNORE_ANNOTATIONS_KEY,
    description = DEFAULT_IGNORE_ANNOTATIONS_DESCRIPTION)
  public String ignoreAnnotations = "";
  private Set<String> ignoredAnnotations;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD, Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    clearState();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (!hasNativeMethod) {
      classes.forEach(this::checkClassFields);
    }
    clearState();
  }

  private void clearState() {
    classes.clear();
    assignments.clear();
    unknownIdentifiers.clear();
    hasNativeMethod = false;
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case METHOD:
        checkIfNativeMethod((MethodTree) tree);
        break;
      case CLASS:
        classes.add((ClassTree) tree);
        break;
      case EXPRESSION_STATEMENT:
        collectAssignment(((ExpressionStatementTree) tree).expression());
        break;
      case IDENTIFIER:
        collectUnknownIdentifier((IdentifierTree) tree);
        break;
      default:
        throw new IllegalStateException("Unexpected subscribed tree.");
    }
  }

  private void collectUnknownIdentifier(IdentifierTree identifier) {
    if (identifier.symbol().isUnknown() && !isMethodIdentifier(identifier)) {
      unknownIdentifiers.add(identifier.name());
    }
  }

  private static boolean isMethodIdentifier(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD_REFERENCE)) {
      parent = parent.parent();
    }
    if (parent == null) {
      return false;
    }
    if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
      return identifier.equals(ExpressionUtils.methodName((MethodInvocationTree) parent));
    }
    return identifier.equals(((MethodReferenceTree) parent).method());
  }

  private void checkIfNativeMethod(MethodTree method) {
    if (ModifiersUtils.hasModifier(method.modifiers(), Modifier.NATIVE)) {
      hasNativeMethod = true;
    }
  }

  private void checkClassFields(ClassTree classTree) {
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .forEach(this::checkIfUnused);
  }

  public void checkIfUnused(VariableTree tree) {
    if (hasOnlyIgnoredAnnotations(tree)) {
      Symbol symbol = tree.symbol();
      String name = symbol.name();
      if (symbol.isPrivate()
        && onlyUsedInVariableAssignment(symbol)
        && !"serialVersionUID".equals(name)
        && !unknownIdentifiers.contains(name)
        && !hasOwnerClassAllowedAnnotations(tree)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(tree.simpleName())
          .withMessage("Remove this unused \"" + name + "\" private field.")
          .withQuickFix(() -> computeQuickFix(tree, assignments.getOrDefault(symbol, Collections.emptyList())))
          .report();
      }
    }
  }

  private static boolean hasOwnerClassAllowedAnnotations(VariableTree variableTree) {
    var ownerClass = (ClassTree) variableTree.parent();
    return ownerClass.modifiers().annotations().stream().anyMatch(
      annotation -> OWNER_CLASS_ALLOWED_ANNOTATIONS.contains(annotation.annotationType().symbolType().fullyQualifiedName())
    );
  }

  private boolean onlyUsedInVariableAssignment(Symbol symbol) {
    return symbol.usages().size() == assignments.getOrDefault(symbol, Collections.emptyList()).size();
  }

  private boolean hasOnlyIgnoredAnnotations(VariableTree tree) {
    return tree.modifiers().annotations().stream().allMatch(
      it -> getIgnoredAnnotations().contains(it.annotationType().symbolType().fullyQualifiedName()));
  }

  private Set<String> getIgnoredAnnotations() {
    if (ignoredAnnotations == null) {
      ignoredAnnotations = Stream.of(ignoreAnnotations.split(","))
        .map(String::trim)
        .filter(it -> !it.isEmpty())
        .collect(Collectors.toSet());
    }
    return ignoredAnnotations;
  }

  private void collectAssignment(ExpressionTree expressionTree) {
    if (expressionTree.is(ASSIGNMENT_KINDS)) {
      AssignmentExpressionTree assignmentExpressionTree = (AssignmentExpressionTree) expressionTree;
      ExpressionTree variable = (assignmentExpressionTree).variable();
      IdentifierTree identifier = null;
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        identifier = (IdentifierTree) variable;
      } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        identifier = ((MemberSelectExpressionTree) variable).identifier();
      } else {
        return;
      }
      Symbol reference = identifier.symbol();
      if (!reference.isUnknown()) {
        List<AssignmentExpressionTree> assignmentsToVariable = assignments.computeIfAbsent(reference, k -> new ArrayList<>());
        assignmentsToVariable.add(assignmentExpressionTree);
      }
    }
  }

  private JavaQuickFix computeQuickFix(VariableTree tree, List<AssignmentExpressionTree> assignments) {
    AnalyzerMessage.TextSpan textSpan = computeTextSpan(tree);
    List<JavaTextEdit> edits = new ArrayList<>(assignments.size() + 1);
    edits.addAll(computeExpressionCaptures(assignments));
    edits.add(JavaTextEdit.removeTextSpan(textSpan));
    return JavaQuickFix.newQuickFix("Remove this unused private field")
      .addTextEdits(edits)
      .reverseSortEdits()
      .build();
  }

  private static AnalyzerMessage.TextSpan computeTextSpan(VariableTree tree) {
    // If the variable is followed by another in a mutli-variable declaration, we remove include the space up to the following variable's name
    Optional<VariableTree> followingVariable = QuickFixHelper.nextVariable(tree);
    if (followingVariable.isPresent()) {
      return AnalyzerMessage.textSpanBetween(tree.simpleName(), true, followingVariable.get().simpleName(), false);
    }
    // If the variable is preceded by another in a multi-variable declaration, we include the space up to the comma that precedes tree
    Optional<SyntaxToken> precedingComma = getPrecedingComma(tree);
    if (precedingComma.isPresent()) {
      SyntaxToken endingSemiColon = tree.lastToken();
      return AnalyzerMessage.textSpanBetween(precedingComma.get(), true, endingSemiColon, false);
    }
    // If the variable is preceded by some related javadoc, we include the javadoc in the span
    List<SyntaxTrivia> trivias = tree.firstToken().trivias();
    if (!trivias.isEmpty()) {
      SyntaxTrivia lastTrivia = trivias.get(trivias.size() - 1);
      if (lastTrivia.comment().startsWith("/**")) {
        SyntaxToken lastToken = tree.lastToken();
        Position start = Position.startOf(lastTrivia);
        Position end = Position.endOf(lastToken);
        return JavaTextEdit.textSpan(start.line(), start.columnOffset(), end.line(), end.columnOffset());
      }
    }
    // By default, we delete the variable's tree
    return AnalyzerMessage.textSpanFor(tree);
  }

  private List<JavaTextEdit> computeExpressionCaptures(List<AssignmentExpressionTree> assignments) {
    List<JavaTextEdit> edits = new ArrayList<>();
    for (int i = 1; i <= assignments.size(); i++) {
      AssignmentExpressionTree assignment = assignments.get(i - 1);
      ExpressionTree variable = assignment.variable();
      String replacement = computeReplacement(variable, i);
      edits.add(
        JavaTextEdit.replaceBetweenTree(variable, true, assignment.expression(), false, replacement)
      );
    }
    return edits;
  }

  private String computeReplacement(ExpressionTree variable, int index) {
    String name = "";
    IdentifierTree identifier;
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      identifier = ((IdentifierTree) variable);
      name = identifier.name() + index;
    } else {
      identifier = ((MemberSelectExpressionTree) variable).identifier();
      name = identifier.name() + index;
    }
    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    TypeTree typeInDeclaration = ((VariableTree) identifier.symbol().declaration()).type();
    String type = QuickFixHelper.contentForTree(typeInDeclaration, context);
    return String.format("%s valueFormerlyAssignedTo%s = ", type, name);
  }

  private static Optional<SyntaxToken> getPrecedingComma(VariableTree variable) {
    return QuickFixHelper.previousVariable(variable).map(VariableTree::lastToken);
  }

}
