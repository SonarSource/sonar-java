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
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
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
        checkField((VariableTree) member, classTree, constructors);
      }
    }
  }

  private void checkField(VariableTree field, ClassTree classTree, List<MethodTree> constructors) {
    if (!isCandidate(field)) {
      return;
    }
    InitializationWrites writes = collectWrites(field, classTree, constructors);
    if (!writes.makesFinalPossible(field, constructors) || isReadBeforeInitialization(field, writes)) {
      return;
    }
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(field)
      .withMessage(MESSAGE)
      .withSecondaries(writes.secondaries())
      .withQuickFixes(() -> quickFixes(field, classTree))
      .report();
  }

  /**
   * Every fragment of a multi-variable declaration such as {@code private int a, b;} shares one modifiers tree,
   * so inserting the modifier there would make all of them final, which may not compile.
   */
  private static List<JavaQuickFix> quickFixes(VariableTree field, ClassTree classTree) {
    boolean sharesDeclaration = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .anyMatch(other -> other != field && other.modifiers() == field.modifiers());
    if (sharesDeclaration) {
      return List.of();
    }
    return List.of(JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
      .addTextEdit(JavaTextEdit.insertBeforeTree(field.type(), "final "))
      .build());
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

  private static InitializationWrites collectWrites(VariableTree field, ClassTree classTree, List<MethodTree> constructors) {
    InitializationWrites writes = new InitializationWrites();
    for (IdentifierTree usage : field.symbol().usages()) {
      AssignmentExpressionTree assignment = plainAssignmentTo(usage);
      if (assignment == null) {
        if (isUnsupportedWrite(usage)) {
          writes.disqualify();
          return writes;
        }
        continue;
      }
      MethodTree constructor = enclosingConstructor(assignment, constructors);
      if (constructor != null) {
        writes.addConstructorWrite(constructor, assignment);
      } else if (isDirectlyInInstanceInitializer(assignment, classTree)) {
        writes.addInitializerBlockWrite(assignment);
      } else {
        writes.disqualify();
        return writes;
      }
    }
    return writes;
  }

  /**
   * Returns the simple assignment the usage is the target of, or null when the usage is not such a target.
   */
  @CheckForNull
  private static AssignmentExpressionTree plainAssignmentTo(IdentifierTree usage) {
    Tree parent = enclosingWriteExpression(usage);
    if (parent instanceof AssignmentExpressionTree assignment
      && parent.is(Tree.Kind.ASSIGNMENT)
      && isOnThisInstance(assignment.variable())) {
      return assignment;
    }
    return null;
  }

  /**
   * Compound assignments and increments read the field before writing it, so they are never compatible with final,
   * and an assignment on another instance proves the field is mutated after construction.
   */
  private static boolean isUnsupportedWrite(IdentifierTree usage) {
    return enclosingWriteExpression(usage) != null;
  }

  /**
   * Returns the expression writing to the given usage, be it an assignment, a compound assignment or an
   * increment, or null when the usage only reads the field.
   */
  @CheckForNull
  private static Tree enclosingWriteExpression(IdentifierTree usage) {
    Tree target = usage;
    Tree parent = usage.parent();
    if (parent != null && parent.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).identifier() == usage) {
      target = parent;
      parent = parent.parent();
    }
    if (parent instanceof AssignmentExpressionTree assignment && assignment.variable() == target) {
      return parent;
    }
    if (parent instanceof UnaryExpressionTree unary && unary.expression() == target && isIncrementOrDecrement(parent)) {
      return parent;
    }
    return null;
  }

  private static boolean isIncrementOrDecrement(Tree tree) {
    return tree.is(Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT);
  }

  /**
   * A blank final field must be definitely assigned before it is read by its simple name, so a read that
   * happens earlier than the initialization would no longer compile once the modifier is added.
   */
  private static boolean isReadBeforeInitialization(VariableTree field, InitializationWrites writes) {
    return field.symbol().usages().stream()
      .filter(usage -> enclosingWriteExpression(usage) == null)
      .anyMatch(read -> readPrecedesInitialization(read, field, writes));
  }

  private static boolean readPrecedesInitialization(IdentifierTree read, VariableTree field, InitializationWrites writes) {
    Tree scope = enclosingExecutableScope(read);
    if (scope != null && scope.is(Tree.Kind.CONSTRUCTOR)) {
      AssignmentExpressionTree assignment = writes.assignmentIn((MethodTree) scope);
      return assignment != null && startsBefore(read, assignment);
    }
    if (scope != null && !scope.is(Tree.Kind.INITIALIZER)) {
      return false;
    }
    // the read happens while the class is being initialized, that is before any constructor body
    if (writes.isAssignedInConstructor()) {
      return true;
    }
    Tree write = field.initializer() != null ? field.initializer() : writes.firstAssignment();
    return write != null && startsBefore(read, write);
  }

  /**
   * Returns the method, constructor or instance initializer block the tree belongs to, or null when it belongs
   * to a field initializer.
   */
  @CheckForNull
  private static Tree enclosingExecutableScope(Tree tree) {
    for (Tree parent = tree.parent(); parent != null; parent = parent.parent()) {
      if (parent.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.INITIALIZER, Tree.Kind.STATIC_INITIALIZER)) {
        return parent;
      }
      if (parent instanceof ClassTree) {
        break;
      }
    }
    return null;
  }

  /**
   * A read nested in the right-hand side of the initializing write still happens before the field is assigned,
   * hence the comparison with the end of the write and not with its start.
   */
  private static boolean startsBefore(Tree read, Tree write) {
    return Position.startOf(read).compareTo(Position.endOf(write)) < 0;
  }

  private static boolean isOnThisInstance(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return ExpressionUtils.isThis(((MemberSelectExpressionTree) variable).expression());
    }
    return true;
  }

  /**
   * A final field can only be assigned by a statement that always runs exactly once, which means a statement
   * directly in the body of a constructor, not nested in any conditional, loop, lambda or inner class.
   * A constructor that can return early is rejected, because the field would not be assigned on that path.
   */
  @CheckForNull
  private static MethodTree enclosingConstructor(AssignmentExpressionTree assignment, List<MethodTree> constructors) {
    BlockTree body = enclosingStatementBlock(assignment);
    if (body == null || !body.is(Tree.Kind.BLOCK)) {
      return null;
    }
    Tree owner = body.parent();
    if (owner != null && owner.is(Tree.Kind.CONSTRUCTOR) && constructors.contains(owner) && !canReturnEarly((MethodTree) owner)) {
      return (MethodTree) owner;
    }
    return null;
  }

  private static boolean isDirectlyInInstanceInitializer(AssignmentExpressionTree assignment, ClassTree classTree) {
    BlockTree block = enclosingStatementBlock(assignment);
    return block != null && block.is(Tree.Kind.INITIALIZER) && block.parent() == classTree;
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

  private static boolean canReturnEarly(MethodTree constructor) {
    ReturnStatementVisitor visitor = new ReturnStatementVisitor();
    constructor.accept(visitor);
    return visitor.found;
  }

  private static List<MethodTree> constructorsOf(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .toList();
  }

  /**
   * Finds return statements belonging to the visited method itself, skipping the bodies of lambdas,
   * anonymous classes and nested classes, whose returns do not affect the enclosing constructor.
   */
  private static class ReturnStatementVisitor extends BaseTreeVisitor {

    private boolean found;

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      found = true;
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // a return inside a lambda leaves the lambda, not the constructor
    }

    @Override
    public void visitClass(ClassTree tree) {
      // a return inside a local, nested or anonymous class belongs to one of its own methods
    }
  }

  private static class InitializationWrites {

    private final List<MethodTree> assigningConstructors = new ArrayList<>();
    private final List<AssignmentExpressionTree> constructorAssignments = new ArrayList<>();
    private final List<AssignmentExpressionTree> assignments = new ArrayList<>();
    private int initializerBlockWrites;
    private boolean disqualified;

    void disqualify() {
      disqualified = true;
    }

    void addConstructorWrite(MethodTree constructor, AssignmentExpressionTree assignment) {
      assigningConstructors.add(constructor);
      constructorAssignments.add(assignment);
      assignments.add(assignment);
    }

    void addInitializerBlockWrite(AssignmentExpressionTree assignment) {
      initializerBlockWrites++;
      assignments.add(assignment);
    }

    boolean isAssignedInConstructor() {
      return !assigningConstructors.isEmpty();
    }

    @CheckForNull
    AssignmentExpressionTree firstAssignment() {
      return assignments.isEmpty() ? null : assignments.get(0);
    }

    @CheckForNull
    AssignmentExpressionTree assignmentIn(MethodTree constructor) {
      for (int i = 0; i < assigningConstructors.size(); i++) {
        if (assigningConstructors.get(i) == constructor) {
          return constructorAssignments.get(i);
        }
      }
      return null;
    }

    List<JavaFileScannerContext.Location> secondaries() {
      return assignments.stream()
        .map(assignment -> new JavaFileScannerContext.Location(SECONDARY_MESSAGE, assignment))
        .toList();
    }

    /**
     * The field can be made final when it is assigned exactly once on every path reaching the end of construction:
     * either by its own initializer, or by a single instance initializer block, or by every constructor that does
     * not delegate to another one. A constructor delegating with {@code this(...)} must not assign the field at all,
     * since the constructor it delegates to already does.
     */
    boolean makesFinalPossible(VariableTree field, List<MethodTree> constructors) {
      if (disqualified) {
        return false;
      }
      if (field.initializer() != null) {
        return assignments.isEmpty();
      }
      if (initializerBlockWrites > 0) {
        return initializerBlockWrites == 1 && assigningConstructors.isEmpty();
      }
      if (assigningConstructors.stream().anyMatch(InitializationWrites::delegatesToAnotherConstructor)) {
        return false;
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
     * Flexible constructor bodies allow statements before the explicit constructor invocation,
     * so the {@code this(...)} call is not necessarily the first statement.
     */
    private static boolean delegatesToAnotherConstructor(MethodTree constructor) {
      BlockTree block = constructor.block();
      return block != null && block.body().stream().anyMatch(InitializationWrites::isThisInvocation);
    }

    private static boolean isThisInvocation(StatementTree statement) {
      if (!statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        return false;
      }
      ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
      if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
        return false;
      }
      ExpressionTree methodSelect = ((MethodInvocationTree) expression).methodSelect();
      return methodSelect.is(Tree.Kind.IDENTIFIER) && "this".equals(((IdentifierTree) methodSelect).name());
    }
  }
}
