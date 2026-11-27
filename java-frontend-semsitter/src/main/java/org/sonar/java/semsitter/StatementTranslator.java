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
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JLabelSymbol;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bindMethod;
import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.firstOf;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.lastKeyword;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.nameNode;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates blocks and statements. */
final class StatementTranslator extends TranslatorPart {

  private static final String BLOCK = "block";
  private static final String LOCAL_VARIABLE_DECLARATION = "local_variable_declaration";
  private static final String PARENTHESIZED_EXPRESSION = "parenthesized_expression";
  private static final String CONDITION = "condition";

  StatementTranslator(TranslationState state) {
    super(state);
  }

  BlockTreeImpl block(CstNode node) {
    state.scopes.addFirst(new HashMap<>());
    try {
      return blockInScope(node);
    } finally {
      state.scopes.removeFirst();
    }
  }

  private BlockTreeImpl blockInScope(CstNode node) {
    if (!BLOCK.equals(node.k())) {
      CstNode nested = childKind(node, BLOCK);
      if (nested != null) {
        node = nested;
      }
    }
    InternalSyntaxToken open = token(required(node, "{"));
    InternalSyntaxToken close = token(required(node, "}"));
    List<StatementTree> statements = new ArrayList<>();
    for (CstNode child : node.children()) {
      if ("{".equals(child.text()) || "}".equals(child.text())) {
        continue;
      }
      addStatement(statements, child);
    }
    return new BlockTreeImpl(open, statements, close);
  }

  void addStatement(List<StatementTree> statements, CstNode node) {
    if (LOCAL_VARIABLE_DECLARATION.equals(node.k())) {
      statements.addAll(members().variables(node));
      return;
    }
    if (";".equals(node.text())) {
      statements.add(new EmptyStatementTreeImpl(token(node)));
      return;
    }
    CstNode switchNode = childKind(node, "switch_expression");
    if ("expression_statement".equals(node.k()) && switchNode != null) {
      statements.add(switches().switchStatement(switchNode));
      statements.add(new EmptyStatementTreeImpl(token(required(node, ";"))));
      return;
    }
    statements.add(statement(node));
  }

  private StatementTree statement(CstNode node) {
    return switch (node.k()) {
      case BLOCK -> block(node);
      case "expression_statement" -> expressionStatement(node);
      case "explicit_constructor_invocation" -> constructorInvocation(node);
      case "if_statement" -> ifStatement(node);
      case "while_statement" -> whileStatement(node);
      case "do_statement" -> doStatement(node);
      case "for_statement" -> forStatement(node);
      case "enhanced_for_statement" -> forEach(node);
      case "break_statement" -> breakStatement(node);
      case "continue_statement" -> continueStatement(node);
      case "return_statement" -> returnStatement(node);
      case "yield_statement" -> yieldStatement(node);
      case "throw_statement" -> throwStatement(node);
      case "assert_statement" -> assertStatement(node);
      case "labeled_statement" -> labeled(node);
      case "try_statement", "try_with_resources_statement" -> switches().tryStatement(node);
      case "synchronized_statement" -> synchronizedStatement(node);
      case "switch_statement", "switch_expression" -> switches().switchStatement(node);
      case LOCAL_VARIABLE_DECLARATION -> members().variables(node).get(0);
      case "class_declaration", "interface_declaration", "enum_declaration", "record_declaration" -> declarations().typeDeclaration(node);
      default -> new EmptyStatementTreeImpl(";".equals(node.text()) ? token(node) : token(required(node, ";")));
    };
  }

  ExpressionStatementTreeImpl expressionStatement(CstNode node) {
    return new ExpressionStatementTreeImpl(expressions().expression(node.children().get(0)), optionalToken(node, ";"));
  }

  private ExpressionStatementTreeImpl constructorInvocation(CstNode node) {
    CstNode constructor = requiredField(node, "constructor", firstOf(node, "super", "this"));
    IdentifierTreeImpl identifier = names().identifier(constructor);
    bindUse(identifier, constructor);
    CstNode object = field(node, "object");
    ExpressionTree select = object == null
      ? identifier
      : new MemberSelectExpressionTreeImpl(expressions().expression(object), token(required(node, ".")), identifier);
    CstNode typeArguments = field(node, "type_arguments");
    MethodInvocationTreeImpl invocation = new MethodInvocationTreeImpl(select, typeArguments == null ? null : typeTrees().typeArguments(typeArguments),
      expressions().arguments(requiredField(node, "arguments", childKind(node, "argument_list"))));
    if (identifier.symbol() instanceof Symbol.MethodSymbol method) {
      bindMethod(invocation, method);
      ascribe(invocation, method.owner().type());
      ascribe(identifier, Type.UNKNOWN);
    }
    Symbol.TypeSymbol enclosing = state.types.peek();
    if (invocation.symbolType().isUnknown() && enclosing != null) {
      Type constructed = "this".equals(identifier.name()) ? enclosing.type() : enclosing.superClass();
      ascribe(invocation, constructed == null ? Type.UNKNOWN : constructed);
    }
    return new ExpressionStatementTreeImpl(invocation, token(required(node, ";")));
  }

  private IfStatementTreeImpl ifStatement(CstNode node) {
    Parens condition = parens(requiredField(node, CONDITION, childKind(node, PARENTHESIZED_EXPRESSION)));
    CstNode alternative = field(node, "alternative");
    return new IfStatementTreeImpl(
      token(required(node, "if")),
      condition.open(),
      condition.expr(),
      condition.close(),
      statement(requiredField(node, "consequence", lastNamed(node))),
      alternative == null ? null : token(required(node, "else")),
      alternative == null ? null : statement(alternative));
  }

  private WhileStatementTreeImpl whileStatement(CstNode node) {
    Parens condition = parens(requiredField(node, CONDITION, childKind(node, PARENTHESIZED_EXPRESSION)));
    return new WhileStatementTreeImpl(
      token(required(node, "while")),
      condition.open(),
      condition.expr(),
      condition.close(),
      statement(requiredField(node, "body", lastNamed(node))));
  }

  private DoWhileStatementTreeImpl doStatement(CstNode node) {
    Parens condition = parens(requiredField(node, CONDITION, childKind(node, PARENTHESIZED_EXPRESSION)));
    return new DoWhileStatementTreeImpl(
      token(required(node, "do")),
      statement(requiredField(node, "body", firstNamed(node))),
      token(required(node, "while")),
      condition.open(),
      condition.expr(),
      condition.close(),
      token(lastKeyword(node, ";")));
  }

  private ForStatementTreeImpl forStatement(CstNode node) {
    state.scopes.addFirst(new HashMap<>());
    try {
      return forStatementInScope(node);
    } finally {
      state.scopes.removeFirst();
    }
  }

  private ForStatementTreeImpl forStatementInScope(CstNode node) {
    ForClauses clauses = new ForClauses();
    for (CstNode child : node.children()) {
      if (")".equals(child.text())) {
        break;
      }
      clauses.add(child);
    }
    return new ForStatementTreeImpl(
      token(required(node, "for")),
      token(required(node, "(")),
      clauses.init,
      Objects.requireNonNull(clauses.firstSemicolon),
      clauses.condition,
      Objects.requireNonNull(clauses.secondSemicolon),
      clauses.update,
      token(required(node, ")")),
      statement(requiredField(node, "body", lastNamed(node))));
  }

  /** Accumulates the init, condition and update clauses of a for statement while its children are visited. */
  private final class ForClauses {
    private final StatementListTreeImpl init = StatementListTreeImpl.emptyList();
    private final StatementListTreeImpl update = StatementListTreeImpl.emptyList();
    @Nullable
    private InternalSyntaxToken firstSemicolon = null;
    @Nullable
    private InternalSyntaxToken secondSemicolon = null;
    @Nullable
    private ExpressionTree condition = null;
    private int semicolons = 0;

    void add(CstNode child) {
      String text = child.text();
      if ("for".equals(text) || "(".equals(text)) {
        return;
      }
      if (";".equals(text)) {
        addSemicolon(child);
      } else if (",".equals(text)) {
        (semicolons == 0 ? init : update).separators().add(token(child));
      } else if (LOCAL_VARIABLE_DECLARATION.equals(child.k())) {
        addDeclaration(child);
      } else {
        addExpression(child);
      }
    }

    private void addSemicolon(CstNode child) {
      semicolons++;
      if (semicolons == 1) {
        firstSemicolon = token(child);
      } else {
        secondSemicolon = token(child);
      }
    }

    private void addDeclaration(CstNode child) {
      List<VariableTreeImpl> declared = members().variables(child);
      init.addAll(declared);
      declared.get(declared.size() - 1).setEndToken(null);
      firstSemicolon = token(required(child, ";"));
      semicolons = 1;
    }

    private void addExpression(CstNode child) {
      switch (semicolons) {
        case 0 -> init.add(new ExpressionStatementTreeImpl(expressions().expression(child), null));
        case 1 -> condition = expressions().expression(child);
        default -> update.add(new ExpressionStatementTreeImpl(expressions().expression(child), null));
      }
    }
  }

  private ForEachStatementImpl forEach(CstNode node) {
    state.scopes.addFirst(new HashMap<>());
    try {
      return forEachInScope(node);
    } finally {
      state.scopes.removeFirst();
    }
  }

  private ForEachStatementImpl forEachInScope(CstNode node) {
    TypeTree type = typeTrees().arrayType(typeTrees().type(requiredField(node, "type", firstType(node))), field(node, "dimensions"));
    IdentifierTreeImpl name = names().identifier(requiredField(node, "name", firstOf(node, "identifier")));
    VariableTreeImpl variable = new VariableTreeImpl(members().modifiers(node), type, name);
    bindDeclaration(variable, requiredField(node, "name", nameNode(node)), name);
    return new ForEachStatementImpl(
      token(required(node, "for")),
      token(required(node, "(")),
      variable,
      token(required(node, ":")),
      expressions().expression(requiredField(node, "value", firstNamed(node))),
      token(required(node, ")")),
      statement(requiredField(node, "body", lastNamed(node))));
  }

  private BreakStatementTreeImpl breakStatement(CstNode node) {
    IdentifierTreeImpl label = labelUse(node);
    return new BreakStatementTreeImpl(token(required(node, "break")), label, token(required(node, ";")));
  }

  private ContinueStatementTreeImpl continueStatement(CstNode node) {
    return new ContinueStatementTreeImpl(token(required(node, "continue")), labelUse(node), token(required(node, ";")));
  }

  private ReturnStatementTreeImpl returnStatement(CstNode node) {
    ExpressionTree expression = null;
    for (CstNode child : node.children()) {
      if (!"return".equals(child.text()) && !";".equals(child.text())) {
        expression = expressions().expression(child);
      }
    }
    if (expression instanceof NewClassTreeImpl newClass && !state.returnTargets.isEmpty()) {
      expressions().inferDiamond(newClass, state.returnTargets.peek());
    }
    if (expression != null
      && expression.is(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD_REFERENCE, Tree.Kind.METHOD_INVOCATION)
      && !state.returnTargets.isEmpty()
      && !state.returnTargets.peek().isUnknown()) {
      expressions().ascribeTarget(expression, state.returnTargets.peek());
    }
    return new ReturnStatementTreeImpl(token(required(node, "return")), expression, token(required(node, ";")));
  }

  private YieldStatementTreeImpl yieldStatement(CstNode node) {
    ExpressionTree expression = null;
    for (CstNode child : node.children()) {
      if (!"yield".equals(child.text()) && !";".equals(child.text())) {
        expression = expressions().expression(child);
      }
    }
    return new YieldStatementTreeImpl(token(required(node, "yield")), Objects.requireNonNull(expression), token(required(node, ";")));
  }

  private ThrowStatementTreeImpl throwStatement(CstNode node) {
    return new ThrowStatementTreeImpl(token(required(node, "throw")), expressions().expression(firstNamed(node)), token(required(node, ";")));
  }

  private AssertStatementTreeImpl assertStatement(CstNode node) {
    List<ExpressionTree> expressions = new ArrayList<>();
    InternalSyntaxToken colon = null;
    for (CstNode child : node.children()) {
      if (":".equals(child.text())) {
        colon = token(child);
      } else if (!"assert".equals(child.text()) && !";".equals(child.text())) {
        expressions.add(expressions().expression(child));
      }
    }
    AssertStatementTreeImpl tree = new AssertStatementTreeImpl(token(required(node, "assert")), expressions.get(0), token(required(node, ";")));
    if (colon != null) {
      tree.complete(colon, expressions.get(1));
    }
    return tree;
  }

  private LabeledStatementTreeImpl labeled(CstNode node) {
    CstNode label = node.children().get(0);
    IdentifierTreeImpl name = names().identifier(label);
    JLabelSymbol symbol = new JLabelSymbol(name.name());
    state.labels.push(symbol);
    List<CstNode> children = node.children();
    LabeledStatementTreeImpl tree = new LabeledStatementTreeImpl(name, token(required(node, ":")), statement(children.get(children.size() - 1)));
    state.labels.pop();
    symbol.setDeclaration(tree);
    tree.labelSymbol = symbol;
    String id = label.decl();
    if (id != null) {
      sema.declare(id, tree);
    }
    return tree;
  }

  private SynchronizedStatementTreeImpl synchronizedStatement(CstNode node) {
    Parens parens = parens(requiredField(node, CONDITION, childKind(node, PARENTHESIZED_EXPRESSION)));
    return new SynchronizedStatementTreeImpl(
      token(required(node, "synchronized")),
      parens.open(),
      parens.expr(),
      parens.close(),
      block(requiredField(node, "body", childKind(node, BLOCK))));
  }

  @Nullable
  private IdentifierTreeImpl labelUse(CstNode node) {
    CstNode name = firstOf(node, "identifier");
    if (name == null) {
      return null;
    }
    IdentifierTreeImpl identifier = names().identifier(name);
    for (JLabelSymbol label : state.labels) {
      if (label.name().equals(identifier.name())) {
        label.addUsage(identifier);
        identifier.labelSymbol = label;
        break;
      }
    }
    return identifier;
  }
}
