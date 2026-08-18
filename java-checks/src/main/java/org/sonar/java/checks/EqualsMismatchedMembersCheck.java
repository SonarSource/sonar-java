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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Flags {@code equals} implementations that compare a field or getter of {@code this}
 * with a different field or getter of the other instance.
 */
@Rule(key = "S9350")
public class EqualsMismatchedMembersCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE =
    "This equals() implementation compares mismatched members; pairing \"%s\" with \"%s\" breaks the equality contract.";
  private static final String SECONDARY_THIS = "Compared member on this";
  private static final String SECONDARY_OTHER = "Compared member on the other instance";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final String EQUALS_METHOD_NAME = "equals";

  private static final MethodMatchers OBJECTS_EQUALS = MethodMatchers.create()
    .ofTypes("java.util.Objects")
    .names(EQUALS_METHOD_NAME)
    .addParametersMatcher(JAVA_LANG_OBJECT, JAVA_LANG_OBJECT)
    .build();

  private static final MethodMatchers GUAVA_OBJECTS_EQUAL = MethodMatchers.create()
    .ofTypes("com.google.common.base.Objects")
    .names("equal")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ARRAYS_EQUALS = MethodMatchers.create()
    .ofTypes("java.util.Arrays")
    .names(EQUALS_METHOD_NAME)
    .withAnyParameters()
    .build();

  private static final MethodMatchers INSTANCE_EQUALS = MethodMatchers.create()
    .ofAnyType()
    .names(EQUALS_METHOD_NAME)
    .addParametersMatcher(JAVA_LANG_OBJECT)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!MethodTreeUtils.isEqualsMethod(methodTree) || methodTree.block() == null) {
      return;
    }
    Symbol owner = methodTree.symbol().owner();
    if (owner == null || !owner.isTypeSymbol() || owner.isUnknown() || owner.type().isUnknown()) {
      return;
    }
    ComparisonCollector collector = new ComparisonCollector(owner);
    methodTree.block().accept(collector);
    for (ComparisonSite comparison : collector.comparisons) {
      // Order-independent equality: (a, b) || (b, a) in the same statement is not a mismatch.
      if (collector.pairsByStatement.get(comparison.statement).contains(comparison.pair().reversed())) {
        continue;
      }
      reportIssue(
        comparison.tree,
        String.format(ISSUE_MESSAGE, comparison.thisMember.displayName, comparison.otherMember.displayName),
        List.of(
          new JavaFileScannerContext.Location(SECONDARY_THIS, comparison.thisMember.tree),
          new JavaFileScannerContext.Location(SECONDARY_OTHER, comparison.otherMember.tree)),
        null);
    }
  }

  private static final class ComparisonCollector extends BaseTreeVisitor {
    private final Symbol enclosingClass;
    private final List<ComparisonSite> comparisons = new ArrayList<>();
    private final Map<Tree, Set<MemberPair>> pairsByStatement = new HashMap<>();

    private ComparisonCollector(Symbol enclosingClass) {
      this.enclosingClass = enclosingClass;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Nested types are visited independently through METHOD subscription.
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
        addIfDubious(tree, tree.leftOperand(), tree.rightOperand());
      }
      super.visitBinaryExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      List<ExpressionTree> arguments = tree.arguments();
      if (isTwoArgEqualityHelper(tree)) {
        addIfDubious(tree, arguments.get(0), arguments.get(1));
      } else if (ARRAYS_EQUALS.matches(tree) && arguments.size() >= 2) {
        // 2-arg compares the two arrays; 6-arg subrange overloads compare arguments 0 and 3.
        addIfDubious(tree, arguments.get(0), arguments.get(arguments.size() / 2));
      } else if (INSTANCE_EQUALS.matches(tree) && arguments.size() == 1) {
        ExpressionTree receiver = receiver(tree);
        if (receiver != null && !isSuper(receiver)) {
          addIfDubious(tree, receiver, arguments.get(0));
        }
      }
      super.visitMethodInvocation(tree);
    }

    /**
     * Records a candidate when one operand is a member of {@code this} and the other is a
     * differently named member of the same kind on a local or parameter (the other instance).
     */
    private void addIfDubious(Tree comparisonTree, ExpressionTree lhs, ExpressionTree rhs) {
      Optional<MemberRef> left = member(lhs);
      Optional<MemberRef> right = member(rhs);
      if (left.isEmpty() || right.isEmpty()) {
        return;
      }
      MemberRef leftMember = left.get();
      MemberRef rightMember = right.get();
      // Skip field-vs-getter, same member names, and same-object or two-foreign-object pairings.
      if (leftMember.kind != rightMember.kind
        || leftMember.displayName.equals(rightMember.displayName)
        || leftMember.onThis == rightMember.onThis) {
        return;
      }
      MemberRef thisMember = leftMember.onThis ? leftMember : rightMember;
      MemberRef otherMember = leftMember.onThis ? rightMember : leftMember;
      ComparisonSite comparison = new ComparisonSite(comparisonTree, enclosingStatement(comparisonTree), thisMember, otherMember);
      comparisons.add(comparison);
      pairsByStatement.computeIfAbsent(comparison.statement, key -> new HashSet<>()).add(comparison.pair());
    }

    private Optional<MemberRef> member(ExpressionTree expression) {
      ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
      Optional<Boolean> onThis = receiverIsThis(expr);
      if (onThis.isEmpty()) {
        // Not reached through this or a local/parameter (for example a static or a nested selection).
        return Optional.empty();
      }
      if (expr instanceof IdentifierTree identifierTree) {
        return field(identifierTree.symbol(), expr, onThis.get());
      }
      if (expr instanceof MemberSelectExpressionTree memberSelect) {
        return field(memberSelect.identifier().symbol(), expr, onThis.get());
      }
      if (expr instanceof MethodInvocationTree invocation && invocation.arguments().isEmpty()) {
        return getter(invocation, onThis.get());
      }
      return Optional.empty();
    }

    private Optional<MemberRef> field(Symbol symbol, ExpressionTree tree, boolean onThis) {
      if (symbol.isUnknown() || !symbol.isVariableSymbol() || symbol.isStatic() || !ownedByEnclosing(symbol)) {
        return Optional.empty();
      }
      return Optional.of(new MemberRef(MemberKind.FIELD, symbol.name(), tree, onThis));
    }

    private Optional<MemberRef> getter(MethodInvocationTree invocation, boolean onThis) {
      Symbol.MethodSymbol method = invocation.methodSymbol();
      if (method.isUnknown() || method.isStatic() || !ownedByEnclosing(method)) {
        return Optional.empty();
      }
      Symbol.TypeSymbol returnType = method.returnType();
      if (returnType == null || returnType.isUnknown() || returnType.type().isVoid() || !method.parameterTypes().isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new MemberRef(MemberKind.METHOD, method.name() + "()", invocation, onThis));
    }

    private boolean ownedByEnclosing(Symbol symbol) {
      Symbol owner = symbol.owner();
      // Compare erasures so a field of Holder<?> still belongs to Holder<T>.
      return owner != null && !owner.isUnknown() && owner.isTypeSymbol()
        && enclosingClass.type().erasure().equals(owner.type().erasure());
    }

    /**
     * {@code Optional.of(true)} for {@code this} (implicit or explicit), {@code Optional.of(false)}
     * for a local or parameter, empty when the receiver is neither.
     */
    private static Optional<Boolean> receiverIsThis(ExpressionTree access) {
      if (access instanceof IdentifierTree) {
        return Optional.of(true);
      }
      if (access instanceof MemberSelectExpressionTree memberSelect) {
        return classifyReceiver(memberSelect.expression());
      }
      if (access instanceof MethodInvocationTree invocation) {
        if (invocation.methodSelect() instanceof IdentifierTree) {
          return Optional.of(true);
        }
        if (invocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect) {
          return classifyReceiver(memberSelect.expression());
        }
      }
      return Optional.empty();
    }

    private static Optional<Boolean> classifyReceiver(ExpressionTree receiverExpr) {
      ExpressionTree expr = ExpressionUtils.skipParentheses(receiverExpr);
      if (!(expr instanceof IdentifierTree identifierTree)) {
        return Optional.empty();
      }
      if ("this".equals(identifierTree.name())) {
        return Optional.of(true);
      }
      Symbol symbol = identifierTree.symbol();
      if (symbol.isUnknown() || !symbol.isVariableSymbol() || symbol.isStatic()) {
        return Optional.empty();
      }
      Symbol owner = symbol.owner();
      if (owner == null || !owner.isMethodSymbol()) {
        // A field of this type used as receiver is not the other instance.
        return Optional.empty();
      }
      return Optional.of(false);
    }

    private static boolean isTwoArgEqualityHelper(MethodInvocationTree tree) {
      if (tree.arguments().size() != 2) {
        return false;
      }
      if (OBJECTS_EQUALS.matches(tree) || GUAVA_OBJECTS_EQUAL.matches(tree)) {
        return true;
      }
      if (!"equal".equals(ExpressionUtils.methodName(tree).name())) {
        return false;
      }
      Symbol.MethodSymbol method = tree.methodSymbol();
      // Unresolved `equal` covers Guava when it is absent from the classpath.
      if (method.isUnknown()) {
        return true;
      }
      Symbol.TypeSymbol returnType = method.returnType();
      // Resolved user helpers are equality comparisons only when they return boolean.
      return method.isStatic()
        && returnType != null
        && !returnType.isUnknown()
        && returnType.type().isPrimitive(Type.Primitives.BOOLEAN);
    }

    private static ExpressionTree receiver(MethodInvocationTree invocation) {
      if (invocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect) {
        return memberSelect.expression();
      }
      return null;
    }

    private static boolean isSuper(ExpressionTree expression) {
      ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
      return expr instanceof IdentifierTree identifierTree && "super".equals(identifierTree.name());
    }

    private static Tree enclosingStatement(Tree tree) {
      Tree current = tree;
      while (current != null && !(current instanceof StatementTree)) {
        current = current.parent();
      }
      return current != null ? current : tree;
    }
  }

  private enum MemberKind {
    FIELD,
    METHOD
  }

  private record MemberRef(MemberKind kind, String displayName, ExpressionTree tree, boolean onThis) {
  }

  private record ComparisonSite(Tree tree, Tree statement, MemberRef thisMember, MemberRef otherMember) {
    private MemberPair pair() {
      return new MemberPair(thisMember.displayName, otherMember.displayName);
    }
  }

  private record MemberPair(String thisName, String otherName) {
    private MemberPair reversed() {
      return new MemberPair(otherName, thisName);
    }
  }
}
