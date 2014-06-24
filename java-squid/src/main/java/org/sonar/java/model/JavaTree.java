/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.List;

public abstract class JavaTree implements Tree {

  protected final AstNode astNode;

  public JavaTree(AstNode astNode) {
    this.astNode = astNode;
  }

  public AstNode getAstNode() {
    return astNode;
  }

  public int getLine() {
    return astNode.getTokenLine();
  }

  @Override
  public final boolean is(Kind kind) {
    return getKind() == null ? false : getKind() == kind;
  }

  public abstract Kind getKind();

  public abstract static class AbstractExpressionTree extends JavaTree {
    private Type type;

    public AbstractExpressionTree(AstNode astNode) {
      super(astNode);
    }

    public Type getType() {
      return type;
    }

    public void setType(Type type) {
      this.type = type;
    }
  }

  public static class PrimitiveTypeTreeImpl extends AbstractExpressionTree implements PrimitiveTypeTree {
    public PrimitiveTypeTreeImpl(AstNode astNode) {
      super(astNode);
    }

    @Override
    public Kind getKind() {
      return Kind.PRIMITIVE_TYPE;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitPrimitiveType(this);
    }
  }

  public static class IdentifierTreeImpl extends AbstractExpressionTree implements IdentifierTree {
    private final String name;

    public IdentifierTreeImpl(AstNode astNode, String name) {
      super(astNode);
      this.name = Preconditions.checkNotNull(name);
    }

    @Override
    public Kind getKind() {
      return Kind.IDENTIFIER;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitIdentifier(this);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static class CompilationUnitTreeImpl extends JavaTree implements CompilationUnitTree {
    @Nullable
    private final ExpressionTree packageName;
    private final List<ImportTree> imports;
    private final List<Tree> types;
    private final List<AnnotationTree> packageAnnotations;

    public CompilationUnitTreeImpl(AstNode astNode, @Nullable ExpressionTree packageName, List<ImportTree> imports, List<Tree> types, List<AnnotationTree> packageAnnotations) {
      super(astNode);
      this.packageName = packageName;
      this.imports = Preconditions.checkNotNull(imports);
      this.types = Preconditions.checkNotNull(types);
      this.packageAnnotations = Preconditions.checkNotNull(packageAnnotations);
    }

    @Override
    public Kind getKind() {
      return Kind.COMPILATION_UNIT;
    }

    @Override
    public List<AnnotationTree> packageAnnotations() {
      return packageAnnotations;
    }

    @Nullable
    @Override
    public ExpressionTree packageName() {
      return packageName;
    }

    @Override
    public List<ImportTree> imports() {
      return imports;
    }

    @Override
    public List<Tree> types() {
      return types;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCompilationUnit(this);
    }
  }

  public static class ImportTreeImpl extends JavaTree implements ImportTree {
    private final boolean isStatic;
    private final Tree qualifiedIdentifier;

    public ImportTreeImpl(AstNode astNode, boolean aStatic, Tree qualifiedIdentifier) {
      super(astNode);
      isStatic = aStatic;
      this.qualifiedIdentifier = qualifiedIdentifier;
    }

    @Override
    public Kind getKind() {
      return null;
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }

    @Override
    public Tree qualifiedIdentifier() {
      return qualifiedIdentifier;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitImport(this);
    }
  }

  public static class ClassTreeImpl extends JavaTree implements ClassTree {
    private final Kind kind;
    private final ModifiersTree modifiers;
    private final IdentifierTree simpleName;
    @Nullable
    private final Tree superClass;
    private final List<Tree> superInterfaces;
    private final List<Tree> members;
    @Nullable
    private Symbol.TypeSymbol symbol;

    public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, @Nullable IdentifierTree simpleName, @Nullable Tree superClass, List<Tree> superInterfaces,
                         List<Tree> members) {
      super(astNode);
      this.kind = Preconditions.checkNotNull(kind);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.simpleName = simpleName;
      this.superClass = superClass;
      this.superInterfaces = Preconditions.checkNotNull(superInterfaces);
      this.members = Preconditions.checkNotNull(members);
    }

    // TODO remove:
    public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, List<Tree> members) {
      this(astNode, kind, modifiers, null, null, ImmutableList.<Tree>of(), members);
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Nullable
    @Override
    public IdentifierTree simpleName() {
      return simpleName;
    }

    @Override
    public List<Tree> typeParameters() {
      // TODO implement
      return ImmutableList.of();
    }

    @Override
    public ModifiersTree modifiers() {
      return modifiers;
    }

    @Nullable
    @Override
    public Tree superClass() {
      return superClass;
    }

    @Override
    public List<Tree> superInterfaces() {
      return superInterfaces;
    }

    @Override
    public List<Tree> members() {
      return members;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitClass(this);
    }

    @Nullable
    public Symbol.TypeSymbol getSymbol() {
      return symbol;
    }

    public void setSymbol(Symbol.TypeSymbol symbol) {
      this.symbol = symbol;
    }
  }

  public static class MethodTreeImpl extends JavaTree implements MethodTree {
    private final ModifiersTree modifiers;
    @Nullable
    private final Tree returnType;
    private final IdentifierTree simpleName;
    private final List<VariableTree> parameters;
    @Nullable
    private final BlockTree block;
    private final List<ExpressionTree> throwsClauses;
    private final ExpressionTree defaultValue;

    public MethodTreeImpl(AstNode astNode, ModifiersTree modifiers, @Nullable Tree returnType, IdentifierTree simpleName, List<VariableTree> parameters,
                          @Nullable BlockTree block,
                          List<ExpressionTree> throwsClauses, @Nullable ExpressionTree defaultValue) {
      super(astNode);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.returnType = returnType;
      this.simpleName = Preconditions.checkNotNull(simpleName);
      this.parameters = Preconditions.checkNotNull(parameters);
      this.block = block;
      this.throwsClauses = Preconditions.checkNotNull(throwsClauses);
      this.defaultValue = defaultValue;
    }

    @Override
    public Kind getKind() {
      return returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD;
    }

    @Override
    public ModifiersTree modifiers() {
      return modifiers;
    }

    @Override
    public List<Tree> typeParameters() {
      // TODO implement
      return ImmutableList.of();
    }

    @Nullable
    @Override
    public Tree returnType() {
      return returnType;
    }

    @Override
    public IdentifierTree simpleName() {
      return simpleName;
    }

    @Override
    public List<VariableTree> parameters() {
      return parameters;
    }

    @Override
    public List<ExpressionTree> throwsClauses() {
      return throwsClauses;
    }

    @Nullable
    @Override
    public BlockTree block() {
      return block;
    }

    @Nullable
    @Override
    public ExpressionTree defaultValue() {
      return defaultValue;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitMethod(this);
    }
  }

  public static class BlockTreeImpl extends JavaTree implements BlockTree {
    private final Kind kind;
    private final List<StatementTree> body;

    public BlockTreeImpl(AstNode astNode, Kind kind, List<StatementTree> body) {
      super(astNode);
      this.kind = kind;
      this.body = Preconditions.checkNotNull(body);
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public List<StatementTree> body() {
      return body;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitBlock(this);
    }
  }

  public static class IfStatementTreeImpl extends JavaTree implements IfStatementTree {
    private final ExpressionTree condition;
    private final StatementTree thenStatement;
    @Nullable
    private final StatementTree elseStatement;

    public IfStatementTreeImpl(AstNode astNode, ExpressionTree condition, StatementTree thenStatement, @Nullable StatementTree elseStatement) {
      super(astNode);
      this.condition = Preconditions.checkNotNull(condition);
      this.thenStatement = Preconditions.checkNotNull(thenStatement);
      this.elseStatement = elseStatement;
    }

    @Override
    public Kind getKind() {
      return Kind.IF_STATEMENT;
    }

    @Override
    public ExpressionTree condition() {
      return condition;
    }

    @Override
    public StatementTree thenStatement() {
      return thenStatement;
    }

    @Nullable
    @Override
    public StatementTree elseStatement() {
      return elseStatement;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitIfStatement(this);
    }
  }

  public static class WhileStatementTreeImpl extends JavaTree implements WhileStatementTree {
    private final ExpressionTree condition;
    private final StatementTree statement;

    public WhileStatementTreeImpl(AstNode astNode, ExpressionTree condition, StatementTree statement) {
      super(astNode);
      this.condition = Preconditions.checkNotNull(condition);
      this.statement = Preconditions.checkNotNull(statement);
    }

    @Override
    public Kind getKind() {
      return Kind.WHILE_STATEMENT;
    }

    @Override
    public ExpressionTree condition() {
      return condition;
    }

    @Override
    public StatementTree statement() {
      return statement;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitWhileStatement(this);
    }
  }

  public static class ForStatementTreeImpl extends JavaTree implements ForStatementTree {
    private final List<StatementTree> initializer;
    @Nullable
    private final ExpressionTree condition;
    private final List<StatementTree> update;
    private final StatementTree statement;

    public ForStatementTreeImpl(AstNode astNode, List<StatementTree> initializer, @Nullable ExpressionTree condition, List<StatementTree> update,
                                StatementTree statement) {
      super(astNode);
      this.initializer = Preconditions.checkNotNull(initializer);
      this.condition = condition;
      this.update = Preconditions.checkNotNull(update);
      this.statement = Preconditions.checkNotNull(statement);
    }

    @Override
    public Kind getKind() {
      return Kind.FOR_STATEMENT;
    }

    @Override
    public List<StatementTree> initializer() {
      return initializer;
    }

    @Nullable
    @Override
    public ExpressionTree condition() {
      return condition;
    }

    @Override
    public List<StatementTree> update() {
      return update;
    }

    @Override
    public StatementTree statement() {
      return statement;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitForStatement(this);
    }
  }

  public static class ForEachStatementImpl extends JavaTree implements ForEachStatement {
    private final VariableTree variable;
    private final ExpressionTree expression;
    private final StatementTree statement;

    public ForEachStatementImpl(AstNode astNode, VariableTree variable, ExpressionTree expression, StatementTree statement) {
      super(astNode);
      this.variable = Preconditions.checkNotNull(variable);
      this.expression = Preconditions.checkNotNull(expression);
      this.statement = Preconditions.checkNotNull(statement);
    }

    @Override
    public Kind getKind() {
      return Kind.FOR_EACH_STATEMENT;
    }

    @Override
    public VariableTree variable() {
      return variable;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public StatementTree statement() {
      return statement;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitForEachStatement(this);
    }
  }

  public static class EmptyStatementTreeImpl extends JavaTree implements EmptyStatementTree {
    public EmptyStatementTreeImpl(AstNode astNode) {
      super(astNode);
    }

    @Override
    public Kind getKind() {
      return Kind.EMPTY_STATEMENT;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitEmptyStatement(this);
    }
  }

  public static class ExpressionStatementTreeImpl extends JavaTree implements ExpressionStatementTree {
    private final ExpressionTree expression;

    public ExpressionStatementTreeImpl(AstNode astNode, ExpressionTree expression) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
    }

    @Override
    public Kind getKind() {
      return Kind.EXPRESSION_STATEMENT;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitExpressionStatement(this);
    }
  }

  public static class AssertStatementTreeImpl extends JavaTree implements AssertStatementTree {
    private final ExpressionTree condition;
    @Nullable
    private final ExpressionTree detail;

    public AssertStatementTreeImpl(AstNode astNode, ExpressionTree condition, @Nullable ExpressionTree detail) {
      super(astNode);
      this.condition = Preconditions.checkNotNull(condition);
      this.detail = detail;
    }

    @Override
    public Kind getKind() {
      return Kind.ASSERT_STATEMENT;
    }

    @Override
    public ExpressionTree condition() {
      return condition;
    }

    @Nullable
    @Override
    public ExpressionTree detail() {
      return detail;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitAssertStatement(this);
    }
  }

  public static class SwitchStatementTreeImpl extends JavaTree implements SwitchStatementTree {
    private final ExpressionTree expression;
    private final List<CaseGroupTree> cases;

    public SwitchStatementTreeImpl(AstNode astNode, ExpressionTree expression, List<CaseGroupTree> cases) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
      this.cases = Preconditions.checkNotNull(cases);
    }

    @Override
    public Kind getKind() {
      return Kind.SWITCH_STATEMENT;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public List<CaseGroupTree> cases() {
      return cases;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitSwitchStatement(this);
    }
  }

  public static class CaseGroupTreeImpl extends JavaTree implements CaseGroupTree {
    private final List<CaseLabelTree> labels;
    private final List<StatementTree> body;

    public CaseGroupTreeImpl(AstNode astNode, List<CaseLabelTree> labels, List<StatementTree> body) {
      super(astNode);
      this.labels = Preconditions.checkNotNull(labels);
      this.body = Preconditions.checkNotNull(body);
    }

    @Override
    public Kind getKind() {
      return Kind.CASE_GROUP;
    }

    @Override
    public List<CaseLabelTree> labels() {
      return labels;
    }

    @Override
    public List<StatementTree> body() {
      return body;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCaseGroup(this);
    }
  }

  public static class CaseLabelTreeImpl extends JavaTree implements CaseLabelTree {
    @Nullable
    private final ExpressionTree expression;

    public CaseLabelTreeImpl(AstNode astNode, @Nullable ExpressionTree expression) {
      super(astNode);
      this.expression = expression;
    }

    @Override
    public Kind getKind() {
      return Kind.CASE_LABEL;
    }

    @Nullable
    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCaseLabel(this);
    }
  }

  public static class DoWhileStatementTreeImpl extends JavaTree implements DoWhileStatementTree {
    private final StatementTree statement;
    private final ExpressionTree condition;

    public DoWhileStatementTreeImpl(AstNode astNode, StatementTree statement, ExpressionTree condition) {
      super(astNode);
      this.statement = Preconditions.checkNotNull(statement);
      this.condition = Preconditions.checkNotNull(condition);
    }

    @Override
    public Kind getKind() {
      return Kind.DO_STATEMENT;
    }

    @Override
    public StatementTree statement() {
      return statement;
    }

    @Override
    public ExpressionTree condition() {
      return condition;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitDoWhileStatement(this);
    }
  }

  public static class BreakStatementTreeImpl extends JavaTree implements BreakStatementTree {
    @Nullable
    private final IdentifierTree label;

    public BreakStatementTreeImpl(AstNode astNode, @Nullable IdentifierTree label) {
      super(astNode);
      this.label = label;
    }

    @Override
    public Kind getKind() {
      return Kind.BREAK_STATEMENT;
    }

    @Nullable
    @Override
    public IdentifierTree label() {
      return label;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitBreakStatement(this);
    }
  }

  public static class ContinueStatementTreeImpl extends JavaTree implements ContinueStatementTree {
    @Nullable
    private final IdentifierTree label;

    public ContinueStatementTreeImpl(AstNode astNode, @Nullable IdentifierTree label) {
      super(astNode);
      this.label = label;
    }

    @Override
    public Kind getKind() {
      return Kind.CONTINUE_STATEMENT;
    }

    @Nullable
    @Override
    public IdentifierTree label() {
      return label;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitContinueStatement(this);
    }
  }

  public static class ReturnStatementTreeImpl extends JavaTree implements ReturnStatementTree {
    @Nullable
    private final ExpressionTree expression;

    public ReturnStatementTreeImpl(AstNode astNode, @Nullable ExpressionTree expression) {
      super(astNode);
      this.expression = expression;
    }

    @Override
    public Kind getKind() {
      return Kind.RETURN_STATEMENT;
    }

    @Nullable
    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitReturnStatement(this);
    }
  }

  public static class SynchronizedStatementTreeImpl extends JavaTree implements SynchronizedStatementTree {
    private final ExpressionTree expression;
    private final BlockTree block;

    public SynchronizedStatementTreeImpl(AstNode astNode, ExpressionTree expression, BlockTree block) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
      this.block = Preconditions.checkNotNull(block);
    }

    @Override
    public Kind getKind() {
      return Kind.SYNCHRONIZED_STATEMENT;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public BlockTree block() {
      return block;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitSynchronizedStatement(this);
    }
  }

  public static class ThrowStatementTreeImpl extends JavaTree implements ThrowStatementTree {
    private final ExpressionTree expression;

    public ThrowStatementTreeImpl(AstNode astNode, ExpressionTree expression) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
    }

    @Override
    public Kind getKind() {
      return Kind.THROW_STATEMENT;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitThrowStatement(this);
    }
  }

  public static class TryStatementTreeImpl extends JavaTree implements TryStatementTree {
    private final List<VariableTree> resources;
    private final BlockTree block;
    private final List<CatchTree> catches;
    @Nullable
    private final BlockTree finallyBlock;

    public TryStatementTreeImpl(AstNode astNode, List<VariableTree> resources, BlockTree block, List<CatchTree> catches, @Nullable BlockTree finallyBlock) {
      super(astNode);
      this.resources = Preconditions.checkNotNull(resources);
      this.block = Preconditions.checkNotNull(block);
      this.catches = Preconditions.checkNotNull(catches);
      this.finallyBlock = finallyBlock;
    }

    @Override
    public Kind getKind() {
      return Kind.TRY_STATEMENT;
    }

    @Override
    public List<VariableTree> resources() {
      return resources;
    }

    @Override
    public BlockTree block() {
      return block;
    }

    @Override
    public List<CatchTree> catches() {
      return catches;
    }

    @Nullable
    @Override
    public BlockTree finallyBlock() {
      return finallyBlock;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitTryStatement(this);
    }
  }

  public static class CatchTreeImpl extends JavaTree implements CatchTree {
    private final VariableTree parameter;
    private final BlockTree block;

    public CatchTreeImpl(AstNode astNode, VariableTree parameter, BlockTree block) {
      super(astNode);
      this.parameter = Preconditions.checkNotNull(parameter);
      this.block = Preconditions.checkNotNull(block);
    }

    @Override
    public Kind getKind() {
      return Kind.CATCH;
    }

    @Override
    public VariableTree parameter() {
      return parameter;
    }

    @Override
    public BlockTree block() {
      return block;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCatch(this);
    }
  }

  public static class LabeledStatementTreeImpl extends JavaTree implements LabeledStatementTree {
    private final IdentifierTree label;
    private final StatementTree statement;

    public LabeledStatementTreeImpl(AstNode astNode, IdentifierTree label, StatementTree statement) {
      super(astNode);
      this.label = Preconditions.checkNotNull(label);
      this.statement = Preconditions.checkNotNull(statement);
    }

    @Override
    public Kind getKind() {
      return Kind.LABELED_STATEMENT;
    }

    @Override
    public IdentifierTree label() {
      return label;
    }

    @Override
    public StatementTree statement() {
      return statement;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitLabeledStatement(this);
    }
  }

  public static class EnumConstantTreeImpl extends VariableTreeImpl implements EnumConstantTree {
    public EnumConstantTreeImpl(AstNode astNode, ModifiersTree modifiers, Tree type, IdentifierTree simpleName, ExpressionTree initializer) {
      super(astNode, modifiers, type, simpleName, Preconditions.checkNotNull(initializer));
    }

    @Override
    public Kind getKind() {
      return Kind.ENUM_CONSTANT;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitEnumConstant(this);
    }
  }

  public static class VariableTreeImpl extends JavaTree implements VariableTree {
    private final ModifiersTree modifiers;
    private final Tree type;
    private final IdentifierTree simpleName;
    @Nullable
    private final ExpressionTree initializer;
    private Symbol.VariableSymbol symbol;

    public VariableTreeImpl(AstNode astNode, ModifiersTree modifiers, Tree type, IdentifierTree simpleName, @Nullable ExpressionTree initializer) {
      super(astNode);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.type = Preconditions.checkNotNull(type);
      this.simpleName = Preconditions.checkNotNull(simpleName);
      this.initializer = initializer;
    }

    @Override
    public Kind getKind() {
      return Kind.VARIABLE;
    }

    @Override
    public ModifiersTree modifiers() {
      return modifiers;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public IdentifierTree simpleName() {
      return simpleName;
    }

    @Nullable
    @Override
    public ExpressionTree initializer() {
      return initializer;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitVariable(this);
    }

    public Symbol.VariableSymbol getSymbol() {
      return symbol;
    }

    public void setSymbol(Symbol.VariableSymbol symbol) {
      this.symbol = symbol;
    }
  }

  public static class LiteralTreeImpl extends AbstractExpressionTree implements LiteralTree {
    private final Kind kind;

    public LiteralTreeImpl(AstNode astNode, Kind kind) {
      super(astNode);
      this.kind = Preconditions.checkNotNull(kind);
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public String value() {
      return super.astNode.getTokenOriginalValue();
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitLiteral(this);
    }
  }

  public static class BinaryExpressionTreeImpl extends AbstractExpressionTree implements BinaryExpressionTree {
    private final ExpressionTree leftOperand;
    private final Kind kind;
    private final ExpressionTree rightOperand;

    public BinaryExpressionTreeImpl(AstNode astNode, ExpressionTree leftOperand, Kind kind, ExpressionTree rightOperand) {
      super(astNode);
      this.leftOperand = Preconditions.checkNotNull(leftOperand);
      this.kind = Preconditions.checkNotNull(kind);
      this.rightOperand = Preconditions.checkNotNull(rightOperand);
    }

    @Override
    public ExpressionTree leftOperand() {
      return leftOperand;
    }

    @Override
    public ExpressionTree rightOperand() {
      return rightOperand;
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitBinaryExpression(this);
    }
  }

  public static class UnaryExpressionTreeImpl extends AbstractExpressionTree implements UnaryExpressionTree {
    private final Kind kind;
    private final ExpressionTree expression;

    public UnaryExpressionTreeImpl(AstNode astNode, Tree.Kind kind, ExpressionTree expression) {
      super(astNode);
      this.kind = Preconditions.checkNotNull(kind);
      this.expression = Preconditions.checkNotNull(expression);
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitUnaryExpression(this);
    }
  }

  public static class ParenthesizedTreeImpl extends AbstractExpressionTree implements ParenthesizedTree {
    private final ExpressionTree expression;

    public ParenthesizedTreeImpl(AstNode astNode, ExpressionTree expression) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
    }

    @Override
    public Kind getKind() {
      return Kind.PARENTHESIZED_EXPRESSION;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitParenthesized(this);
    }
  }

  public static class ConditionalExpressionTreeImpl extends AbstractExpressionTree implements ConditionalExpressionTree {
    private final ExpressionTree condition;
    private final ExpressionTree trueExpression;
    private final ExpressionTree falseExpression;

    public ConditionalExpressionTreeImpl(AstNode astNode, ExpressionTree condition, ExpressionTree trueExpression, ExpressionTree falseExpression) {
      super(astNode);
      this.condition = Preconditions.checkNotNull(condition);
      this.trueExpression = Preconditions.checkNotNull(trueExpression);
      this.falseExpression = Preconditions.checkNotNull(falseExpression);
    }

    @Override
    public Kind getKind() {
      return Kind.CONDITIONAL_EXPRESSION;
    }

    @Override
    public ExpressionTree condition() {
      return condition;
    }

    @Override
    public ExpressionTree trueExpression() {
      return trueExpression;
    }

    @Override
    public ExpressionTree falseExpression() {
      return falseExpression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitConditionalExpression(this);
    }
  }

  public static class InstanceOfTreeImpl extends AbstractExpressionTree implements InstanceOfTree {
    private final ExpressionTree expression;
    private final Tree type;

    public InstanceOfTreeImpl(AstNode astNode, ExpressionTree expression, Tree type) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
      this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public Kind getKind() {
      return Kind.INSTANCE_OF;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitInstanceOf(this);
    }
  }

  public static class TypeCastExpressionTreeImpl extends AbstractExpressionTree implements TypeCastTree {
    private final Tree type;
    private final ExpressionTree expression;

    public TypeCastExpressionTreeImpl(AstNode astNode, Tree type, ExpressionTree expression) {
      super(astNode);
      this.type = Preconditions.checkNotNull(type);
      this.expression = Preconditions.checkNotNull(expression);
    }

    @Override
    public Kind getKind() {
      return Kind.TYPE_CAST;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitTypeCast(this);
    }
  }

  public static class AssignmentExpressionTreeImpl extends AbstractExpressionTree implements AssignmentExpressionTree {
    private final ExpressionTree variable;
    private final Kind kind;
    private final ExpressionTree expression;

    public AssignmentExpressionTreeImpl(AstNode astNode, ExpressionTree variable, Kind kind, ExpressionTree expression) {
      super(astNode);
      this.variable = Preconditions.checkNotNull(variable);
      this.kind = Preconditions.checkNotNull(kind);
      this.expression = Preconditions.checkNotNull(expression);
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public ExpressionTree variable() {
      return variable;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitAssignmentExpression(this);
    }
  }

  public static class MethodInvocationTreeImpl extends AbstractExpressionTree implements MethodInvocationTree {
    private final ExpressionTree methodSelect;
    private final List<ExpressionTree> arguments;

    public MethodInvocationTreeImpl(AstNode astNode, ExpressionTree methodSelect, List<ExpressionTree> arguments) {
      super(astNode);
      this.methodSelect = Preconditions.checkNotNull(methodSelect);
      this.arguments = Preconditions.checkNotNull(arguments);
    }

    @Override
    public Kind getKind() {
      return Kind.METHOD_INVOCATION;
    }

    @Override
    public List<Tree> typeArguments() {
      // TODO implement
      return ImmutableList.of();
    }

    @Override
    public ExpressionTree methodSelect() {
      return methodSelect;
    }

    @Override
    public List<ExpressionTree> arguments() {
      return arguments;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitMethodInvocation(this);
    }
  }

  public static class NewArrayTreeImpl extends AbstractExpressionTree implements NewArrayTree {
    private final Tree type;
    private final List<ExpressionTree> dimensions;
    private final List<ExpressionTree> initializers;

    public NewArrayTreeImpl(AstNode astNode, Tree type, List<ExpressionTree> dimensions, List<ExpressionTree> initializers) {
      super(astNode);
      // TODO maybe type should not be null?
      this.type = type;
      this.dimensions = Preconditions.checkNotNull(dimensions);
      this.initializers = Preconditions.checkNotNull(initializers);
    }

    @Override
    public Kind getKind() {
      return Kind.NEW_ARRAY;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public List<ExpressionTree> dimensions() {
      return dimensions;
    }

    @Override
    public List<ExpressionTree> initializers() {
      return initializers;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitNewArray(this);
    }
  }

  public static class NewClassTreeImpl extends AbstractExpressionTree implements NewClassTree {
    private final ExpressionTree enclosingExpression;
    private final ExpressionTree identifier;
    private final List<ExpressionTree> arguments;
    @Nullable
    private final ClassTree classBody;

    public NewClassTreeImpl(AstNode astNode, @Nullable ExpressionTree enclosingExpression, ExpressionTree identifier, List<ExpressionTree> arguments,
                            @Nullable ClassTree classBody) {
      super(astNode);
      this.enclosingExpression = enclosingExpression;
      this.identifier = Preconditions.checkNotNull(identifier);
      this.arguments = Preconditions.checkNotNull(arguments);
      this.classBody = classBody;
    }

    @Override
    public Kind getKind() {
      return Kind.NEW_CLASS;
    }

    @Nullable
    @Override
    public ExpressionTree enclosingExpression() {
      return enclosingExpression;
    }

    @Override
    public List<Tree> typeArguments() {
      // TODO implement
      return ImmutableList.of();
    }

    @Override
    public Tree identifier() {
      return identifier;
    }

    @Override
    public List<ExpressionTree> arguments() {
      return arguments;
    }

    @Nullable
    @Override
    public ClassTree classBody() {
      return classBody;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitNewClass(this);
    }
  }

  public static class MemberSelectExpressionTreeImpl extends AbstractExpressionTree implements MemberSelectExpressionTree {
    private final ExpressionTree expression;
    private final IdentifierTree identifier;

    public MemberSelectExpressionTreeImpl(AstNode astNode, ExpressionTree expression, IdentifierTree identifier) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
      this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public Kind getKind() {
      return Kind.MEMBER_SELECT;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public IdentifierTree identifier() {
      return identifier;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitMemberSelectExpression(this);
    }
  }

  public static class ArrayAccessExpressionTreeImpl extends AbstractExpressionTree implements ArrayAccessExpressionTree {
    private final ExpressionTree expression;
    private final ExpressionTree index;

    public ArrayAccessExpressionTreeImpl(AstNode astNode, ExpressionTree expression, ExpressionTree index) {
      super(astNode);
      this.expression = Preconditions.checkNotNull(expression);
      this.index = Preconditions.checkNotNull(index);
    }

    @Override
    public Kind getKind() {
      return Kind.ARRAY_ACCESS_EXPRESSION;
    }

    @Override
    public ExpressionTree expression() {
      return expression;
    }

    @Override
    public ExpressionTree index() {
      return index;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitArrayAccessExpression(this);
    }
  }

  public static class ArrayTypeTreeImpl extends AbstractExpressionTree implements ArrayTypeTree {
    private final Tree type;

    public ArrayTypeTreeImpl(AstNode astNode, Tree type) {
      super(astNode);
      this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public Kind getKind() {
      return Kind.ARRAY_TYPE;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitArrayType(this);
    }
  }

  public static class WildcardTreeImpl extends JavaTree implements WildcardTree {
    private final Kind kind;
    @Nullable
    private final Tree bound;

    public WildcardTreeImpl(AstNode astNode, Kind kind, @Nullable Tree bound) {
      super(astNode);
      this.kind = Preconditions.checkNotNull(kind);
      this.bound = bound;
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Nullable
    @Override
    public Tree bound() {
      return bound;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitWildcard(this);
    }
  }

  public static class ParameterizedTypeTreeImpl extends AbstractExpressionTree implements ParameterizedTypeTree, ExpressionTree {
    private final ExpressionTree type;
    private final List<Tree> typeArguments;

    public ParameterizedTypeTreeImpl(AstNode child, ExpressionTree type, List<Tree> typeArguments) {
      super(child);
      this.type = Preconditions.checkNotNull(type);
      this.typeArguments = Preconditions.checkNotNull(typeArguments);
    }

    @Override
    public Kind getKind() {
      return Kind.PARAMETERIZED_TYPE;
    }

    @Override
    public Tree type() {
      return type;
    }

    @Override
    public List<Tree> typeArguments() {
      return typeArguments;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitParameterizedType(this);
    }
  }

  public static class UnionTypeTreeImpl extends JavaTree implements UnionTypeTree {
    private final List<Tree> typeAlternatives;

    public UnionTypeTreeImpl(AstNode astNode, List<Tree> typeAlternatives) {
      super(astNode);
      this.typeAlternatives = Preconditions.checkNotNull(typeAlternatives);
    }

    @Override
    public Kind getKind() {
      return Kind.UNION_TYPE;
    }

    @Override
    public List<Tree> typeAlternatives() {
      return typeAlternatives;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitUnionType(this);
    }
  }

  public static class ModifiersTreeImpl extends JavaTree implements ModifiersTree {
    // TODO remove:
    public static final ModifiersTreeImpl EMPTY = new ModifiersTreeImpl(null, ImmutableList.<Modifier>of(), ImmutableList.<AnnotationTree>of());

    private final List<Modifier> modifiers;
    private final List<AnnotationTree> annotations;

    public ModifiersTreeImpl(AstNode astNode, List<Modifier> modifiers, List<AnnotationTree> annotations) {
      super(astNode);
      this.modifiers = Preconditions.checkNotNull(modifiers);
      this.annotations = Preconditions.checkNotNull(annotations);
    }

    @Override
    public Kind getKind() {
      return Kind.MODIFIERS;
    }

    @Override
    public List<Modifier> modifiers() {
      return modifiers;
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitModifier(this);
    }
  }

  public static class AnnotationTreeImpl extends AbstractExpressionTree implements AnnotationTree {

    private final List<ExpressionTree> arguments;
    private final Tree annotationType;

    public AnnotationTreeImpl(AstNode astNode, Tree annotationType, List<ExpressionTree> arguments) {
      super(astNode);
      this.annotationType = annotationType;
      this.arguments = arguments;
    }

    @Override
    public Tree annotationType() {
      return annotationType;
    }

    @Override
    public List<ExpressionTree> arguments() {
      return arguments;
    }

    @Override
    public Kind getKind() {
      return Kind.ANNOTATION;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitAnnotation(this);

    }
  }

  public static class LambdaExpressionTreeImpl extends AbstractExpressionTree implements LambdaExpressionTree {

    private final List<VariableTree> parameters;
    private final Tree body;

    public LambdaExpressionTreeImpl(AstNode astNode, List<VariableTree> parameters, Tree body) {
      super(astNode);
      this.parameters = parameters;
      this.body = body;
    }

    @Override
    public Kind getKind() {
      return Kind.LAMBDA_EXPRESSION;
    }

    @Override
    public List<VariableTree> parameters() {
      return parameters;
    }

    @Override
    public Tree body() {
      return body;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitLambdaExpression(this);
    }
  }

  public static class NotImplementedTreeImpl extends AbstractExpressionTree implements ExpressionTree{
    private String name;

    public NotImplementedTreeImpl(AstNode astNode, String name) {
      super(astNode);
      this.name = name;
    }

    @Override
    public Kind getKind() {
      return Kind.OTHER;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitOther(this);
    }

    public String getName() {
      return name;
    }
  }

}
