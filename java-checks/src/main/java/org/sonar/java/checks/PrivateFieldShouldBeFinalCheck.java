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
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9438")
public class PrivateFieldShouldBeFinalCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make this private field \"final\", it is never reassigned after construction.";
  private static final String SECONDARY_MESSAGE = "Assigned during initialization.";
  private static final String QUICK_FIX_MESSAGE = "Add the \"final\" modifier";

  private static final List<String> PERSISTENCE_ANNOTATIONS = List.of(
    "javax.persistence.Entity",
    "javax.persistence.Embeddable",
    "javax.persistence.MappedSuperclass",
    "jakarta.persistence.Entity",
    "jakarta.persistence.Embeddable",
    "jakarta.persistence.MappedSuperclass");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (isManagedByPersistenceFramework(classTree)) {
      return;
    }
    List<MethodTree> constructors = constructorsOf(classTree);
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        checkField((VariableTree) member, constructors);
      }
    }
  }

  private void checkField(VariableTree field, List<MethodTree> constructors) {
    if (!isCandidate(field)) {
      return;
    }
    InitializationWrites writes = collectWrites(field, constructors);
    if (writes.isDisqualified() || !writes.makesFinalPossible(field, constructors)) {
      return;
    }
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(field.simpleName())
      .withMessage(MESSAGE)
      .withSecondaries(writes.secondaries())
      .withQuickFix(() -> JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
        .addTextEdit(JavaTextEdit.insertBeforeTree(field.type(), "final "))
        .build())
      .report();
  }

  private static boolean isCandidate(VariableTree field) {
    Symbol symbol = field.symbol();
    return symbol.isVariableSymbol()
      && !symbol.isUnknown()
      && symbol.isPrivate()
      && !symbol.isStatic()
      && !symbol.isFinal()
      && !symbol.isVolatile()
      && field.modifiers().annotations().isEmpty();
  }

  /**
   * Fields of persistence entities are written reflectively by the framework, outside of any constructor,
   * so they cannot be made final even when the analyzed code never reassigns them.
   */
  private static boolean isManagedByPersistenceFramework(ClassTree classTree) {
    return PERSISTENCE_ANNOTATIONS.stream().anyMatch(classTree.symbol().metadata()::isAnnotatedWith);
  }

  private static InitializationWrites collectWrites(VariableTree field, List<MethodTree> constructors) {
    InitializationWrites writes = new InitializationWrites();
    for (IdentifierTree usage : field.symbol().usages()) {
      AssignmentExpressionTree assignment = assignmentTo(usage);
      if (assignment == null) {
        if (isNonAssigningWrite(usage)) {
          writes.disqualify();
          return writes;
        }
        continue;
      }
      MethodTree constructor = enclosingConstructor(assignment, constructors);
      if (constructor != null) {
        writes.addConstructorWrite(constructor, assignment);
      } else if (isDirectlyInInstanceInitializer(assignment)) {
        writes.addInitializerBlockWrite(assignment);
      } else {
        writes.disqualify();
        return writes;
      }
    }
    return writes;
  }

  /**
   * Returns the assignment the given usage is the target of, or null when the usage is not a plain assignment target.
   * Writes through another instance are reported as unsupported rather than ignored, because they prove the field
   * is mutated outside of the initialization of {@code this}.
   */
  @CheckForNull
  private static AssignmentExpressionTree assignmentTo(IdentifierTree usage) {
    Tree target = usage;
    Tree parent = usage.parent();
    if (parent != null && parent.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).identifier() == usage) {
      target = parent;
      parent = parent.parent();
    }
    if (parent != null && parent.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) parent;
      if (assignment.variable() == target && isOnThisInstance(assignment.variable())) {
        return assignment;
      }
    }
    return null;
  }

  private static boolean isOnThisInstance(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return ExpressionUtils.isThis(((MemberSelectExpressionTree) variable).expression());
    }
    return variable.is(Tree.Kind.IDENTIFIER);
  }

  /**
   * Compound assignments and increments read the field before writing it, so they are never compatible with final,
   * and an assignment on another instance proves the field is mutated after construction.
   */
  private static boolean isNonAssigningWrite(IdentifierTree usage) {
    Tree target = usage;
    Tree parent = usage.parent();
    if (parent != null && parent.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).identifier() == usage) {
      target = parent;
      parent = parent.parent();
    }
    if (parent == null) {
      return false;
    }
    if (parent.is(Tree.Kind.ASSIGNMENT)) {
      return ((AssignmentExpressionTree) parent).variable() == target;
    }
    return isCompoundAssignmentTarget(parent, target) || isIncrementOrDecrementTarget(parent, target);
  }

  private static boolean isCompoundAssignmentTarget(Tree parent, Tree target) {
    return parent instanceof AssignmentExpressionTree assignment
      && !parent.is(Tree.Kind.ASSIGNMENT)
      && assignment.variable() == target;
  }

  private static boolean isIncrementOrDecrementTarget(Tree parent, Tree target) {
    return parent instanceof UnaryExpressionTree unary
      && unary.expression() == target;
  }

  /**
   * A final field can only be assigned by a statement that always runs exactly once, which means a statement
   * directly in the body of a constructor, not nested in any conditional, loop, lambda or inner class.
   */
  @CheckForNull
  private static MethodTree enclosingConstructor(AssignmentExpressionTree assignment, List<MethodTree> constructors) {
    BlockTree body = enclosingStatementBlock(assignment);
    if (body == null || !body.is(Tree.Kind.BLOCK)) {
      return null;
    }
    Tree owner = body.parent();
    if (owner != null && owner.is(Tree.Kind.CONSTRUCTOR) && constructors.contains(owner)) {
      return (MethodTree) owner;
    }
    return null;
  }

  private static boolean isDirectlyInInstanceInitializer(AssignmentExpressionTree assignment) {
    BlockTree block = enclosingStatementBlock(assignment);
    return block != null && block.is(Tree.Kind.INITIALIZER);
  }

  @CheckForNull
  private static BlockTree enclosingStatementBlock(AssignmentExpressionTree assignment) {
    Tree statement = assignment.parent();
    if (statement == null || !statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return null;
    }
    Tree block = statement.parent();
    if (block instanceof BlockTree blockTree) {
      return blockTree;
    }
    return null;
  }

  private static List<MethodTree> constructorsOf(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .toList();
  }

  private static class InitializationWrites {

    private final List<MethodTree> assigningConstructors = new ArrayList<>();
    private final List<AssignmentExpressionTree> assignments = new ArrayList<>();
    private int initializerBlockWrites;
    private boolean disqualified;

    void disqualify() {
      disqualified = true;
    }

    boolean isDisqualified() {
      return disqualified;
    }

    void addConstructorWrite(MethodTree constructor, AssignmentExpressionTree assignment) {
      assigningConstructors.add(constructor);
      assignments.add(assignment);
    }

    void addInitializerBlockWrite(AssignmentExpressionTree assignment) {
      initializerBlockWrites++;
      assignments.add(assignment);
    }

    List<JavaFileScannerContext.Location> secondaries() {
      return assignments.stream()
        .map(assignment -> new JavaFileScannerContext.Location(SECONDARY_MESSAGE, assignment))
        .toList();
    }

    /**
     * The field can be made final when it is assigned exactly once on every path reaching the end of construction:
     * either by its own initializer, or by a single instance initializer block, or by every constructor that does
     * not delegate to another one.
     */
    boolean makesFinalPossible(VariableTree field, List<MethodTree> constructors) {
      boolean hasInitializer = field.initializer() != null;
      if (hasInitializer) {
        return assignments.isEmpty();
      }
      if (initializerBlockWrites > 0) {
        return initializerBlockWrites == 1 && assigningConstructors.isEmpty();
      }
      List<MethodTree> required = constructors.stream()
        .filter(constructor -> !delegatesToAnotherConstructor(constructor))
        .toList();
      return !required.isEmpty()
        && required.stream().allMatch(this::assignedExactlyOnceIn);
    }

    private boolean assignedExactlyOnceIn(MethodTree constructor) {
      return assigningConstructors.stream().filter(constructor::equals).count() == 1;
    }

    /**
     * A constructor delegating to another one of the same class must not assign a final field itself,
     * so it does not take part in the "every constructor assigns the field" requirement.
     */
    private static boolean delegatesToAnotherConstructor(MethodTree constructor) {
      BlockTree block = constructor.block();
      if (block == null || block.body().isEmpty()) {
        return false;
      }
      StatementTree first = block.body().get(0);
      if (!first.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        return false;
      }
      ExpressionTree expression = ((ExpressionStatementTree) first).expression();
      if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
        return false;
      }
      ExpressionTree methodSelect = ((MethodInvocationTree) expression).methodSelect();
      return methodSelect.is(Tree.Kind.IDENTIFIER) && "this".equals(((IdentifierTree) methodSelect).name());
    }
  }
}
