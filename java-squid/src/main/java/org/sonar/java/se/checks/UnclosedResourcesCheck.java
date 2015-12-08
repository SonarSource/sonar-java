/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.se.checks;

import com.google.common.collect.Multimap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ConstraintManager;
import org.sonar.java.se.ConstraintManager.NullConstraint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2095",
  name = "Resources should be closed",
  priority = Priority.BLOCKER,
  tags = {"bug", "cert", "cwe", "denial-of-service", "leak", "security"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class UnclosedResourcesCheck extends SECheck implements JavaFileScanner {

  abstract static class SingleTreeNodeVisitor extends BaseTreeVisitor {

    private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
    private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
      "java.io.ByteArrayOutputStream",
      "java.io.ByteArrayInputStream",
      "java.io.StringReader",
      "java.io.StringWriter",
      "java.io.CharArrayReader",
      "java.io.CharArrayWriter",
      "com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream"
    };

    ProgramState programState;

    protected SingleTreeNodeVisitor(ProgramState programState) {
      this.programState = programState;
    }

    @Override
    protected void scan(Tree tree) {
      // Cut recursive processing
    }

    @Override
    protected void scan(List<? extends Tree> trees) {
      // Cut recursive processing
    }

    @Override
    protected void scan(ListTree<? extends Tree> listTree) {
      // Cut recursive processing
    }

    protected static boolean needsClosing(Type type) {
      for (String ignoredTypes : IGNORED_CLOSEABLE_SUBTYPES) {
        if (type.is(ignoredTypes)) {
          return false;
        }
      }
      return isCloseable(type);
    }

    protected static boolean isCloseable(Type type) {
      return type.isSubtypeOf(JAVA_IO_CLOSEABLE);
    }

    protected static boolean isOpeningResource(NewClassTree syntaxNode) {
      if (isWithinTryHeader(syntaxNode)) {
        return false;
      }
      final Type type = syntaxNode.symbolType();
      return needsClosing(type);
    }

    protected static boolean isWithinTryHeader(NewClassTree syntaxNode) {
      final Tree parent = syntaxNode.parent();
      if (parent != null) {
        final Tree grandParent = parent.parent();
        if (grandParent != null) {
          final Tree greatGrandParent = grandParent.parent();
          return greatGrandParent != null && greatGrandParent.is(Tree.Kind.TRY_STATEMENT);
        }
      }
      return false;
    }

  }

  private static class PostStatementVisitor extends SingleTreeNodeVisitor {

    public PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      if (isOpeningResource(syntaxNode)) {
        final SymbolicValue instanceValue = programState.peekValue();
        programState = programState.addConstraint(instanceValue, NullConstraint.OPENED);
      }
    }
  }

  static class PreStatementVisitor extends SingleTreeNodeVisitor {

    private static final String CLOSE_METHOD_NAME = "close";

    private final ConstraintManager constraintManager;

    PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      final List<SymbolicValue> arguments = programState.peekValues(syntaxNode.arguments().size());
      if (isOpeningResource(syntaxNode)) {
        Iterator<SymbolicValue> iterator = arguments.iterator();
        for (ExpressionTree argument : syntaxNode.arguments()) {
          if (!iterator.hasNext()) {
            throw new IllegalStateException("Mismatch between declared constructor arguments and argument values!");
          }
          final Type type = argument.symbolType();
          final SymbolicValue value = iterator.next();
          if (isCloseable(type)) {
            constraintManager.setWrappedValue(value);
            break;
          }
        }
      } else {
        closeArguments(syntaxNode.arguments(), 0);
      }
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree syntaxNode) {
      SymbolicValue currentVal = programState.peekValue();
      if (currentVal != null) {
        final ExpressionTree expression = syntaxNode.expression();
        if (expression != null) {
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            final IdentifierTree identifier = (IdentifierTree) expression;
            currentVal = programState.getValue(identifier.symbol());
          } else {
            currentVal = programState.peekValue();
          }
          programState = closeResource(programState, currentVal);
        }
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree syntaxNode) {
      final ExpressionTree variable = syntaxNode.variable();
      if (variable.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
        List<SymbolicValue> stackedValues = programState.peekValues(2);
        SymbolicValue value = stackedValues.get(1);
        if (isOpened(programState, value)) {
          programState = closeResource(programState, value);
        }
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxNode) {
      if (isClosingResource(syntaxNode.symbol())) {
        ExpressionTree methodSelect = syntaxNode.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          final ExpressionTree targetExpression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (targetExpression.is(Tree.Kind.IDENTIFIER)) {
            final IdentifierTree identifier = (IdentifierTree) targetExpression;
            final SymbolicValue target = programState.getValue(identifier.symbol());
            programState = closeResource(programState, target);
          }
        }
      } else {
        closeArguments(syntaxNode.arguments(), 1);
      }
    }

    private void closeArguments(final Arguments arguments, int stackOffset) {
      final List<SymbolicValue> values = programState.peekValues(arguments.size() + stackOffset);
      final List<SymbolicValue> argumentValues = values.subList(stackOffset, values.size());
      for (SymbolicValue target : argumentValues) {
        programState = closeResource(programState, target);
      }
    }

    private static ProgramState closeResource(ProgramState programState, final SymbolicValue target) {
      if (target != null && isOpened(programState, target)) {
        return programState.addConstraint(target.wrappedValue(), NullConstraint.CLOSED);
      }
      return programState;
    }

    private static boolean isOpened(ProgramState programState, SymbolicValue value) {
      return NullConstraint.OPENED.equals(programState.getConstraint(value.wrappedValue()));
    }

    private static boolean isClosingResource(Symbol constructor) {
      return constructor.isMethodSymbol() && CLOSE_METHOD_NAME.equals(constructor.name());
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    Multimap<Tree, String> issues = ((DefaultJavaFileScannerContext) context).getSEIssues(UnclosedResourcesCheck.class);
    for (Map.Entry<Tree, String> issue : issues.entries()) {
      context.reportIssue(this, issue.getKey(), issue.getValue());
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    final SingleTreeNodeVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    final PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    final List<Tree> openedResources = context.getState().getConstrainedSyntaxNodes(NullConstraint.OPENED);
    for (Tree syntaxNode : openedResources) {
      if (syntaxNode instanceof NewClassTree) {
        context.reportIssue(syntaxNode, this, "Close this \"" + ((NewClassTree) syntaxNode).identifier() + "\".");
      }
    }
  }
}
