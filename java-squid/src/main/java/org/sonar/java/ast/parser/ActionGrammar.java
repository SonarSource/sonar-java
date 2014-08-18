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
package org.sonar.java.ast.parser;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.List;

import static org.sonar.java.ast.api.JavaPunctuator.COLON;
import static org.sonar.java.ast.api.JavaTokenType.IDENTIFIER;

public class ActionGrammar {

  private final GrammarBuilder b;
  private final TreeFactory f;

  public ActionGrammar(GrammarBuilder b, TreeFactory f) {
    this.b = b;
    this.f = f;
  }

  public ModifiersTreeImpl MODIFIERS() {
    return b.<ModifiersTreeImpl>nonterminal(JavaGrammar.MODIFIERS)
      .is(f.modifiers(b.zeroOrMore(b.invokeRule(JavaGrammar.MODIFIER))));
  }

  // Literals

  public ExpressionTree LITERAL() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.LITERAL)
      .is(
        f.literal(
          b.firstOf(
            b.invokeRule(JavaKeyword.TRUE),
            b.invokeRule(JavaKeyword.FALSE),
            b.invokeRule(JavaKeyword.NULL),
            b.invokeRule(JavaTokenType.CHARACTER_LITERAL),
            b.invokeRule(JavaTokenType.LITERAL),
            b.invokeRule(JavaTokenType.FLOAT_LITERAL),
            b.invokeRule(JavaTokenType.DOUBLE_LITERAL),
            b.invokeRule(JavaTokenType.LONG_LITERAL),
            b.invokeRule(JavaTokenType.INTEGER_LITERAL))));
  }

  // Statements

  public BlockTreeImpl BLOCK() {
    return b.<BlockTreeImpl>nonterminal(JavaGrammar.BLOCK)
      .is(f.block(b.invokeRule(JavaPunctuator.LWING), b.invokeRule(JavaGrammar.BLOCK_STATEMENTS), b.invokeRule(JavaPunctuator.RWING)));
  }

  public AssertStatementTreeImpl ASSERT_STATEMENT() {
    return b.<AssertStatementTreeImpl>nonterminal(JavaGrammar.ASSERT_STATEMENT)
      .is(f.completeAssertStatement(
        b.invokeRule(JavaKeyword.ASSERT), b.invokeRule(JavaGrammar.EXPRESSION),
        b.optional(
          f.newAssertStatement(b.invokeRule(JavaPunctuator.COLON), b.invokeRule(JavaGrammar.EXPRESSION))),
        b.invokeRule(JavaPunctuator.SEMI)));
  }

  public IfStatementTreeImpl IF_STATEMENT() {
    return b.<IfStatementTreeImpl>nonterminal(JavaGrammar.IF_STATEMENT)
      .is(
        f.completeIf(
          b.invokeRule(JavaKeyword.IF), PARENTHESIZED_EXPRESSION(), b.invokeRule(JavaGrammar.STATEMENT),
          b.optional(
            f.newIfWithElse(b.invokeRule(JavaKeyword.ELSE), b.invokeRule(JavaGrammar.STATEMENT)))));
  }

  public WhileStatementTreeImpl WHILE_STATEMENT() {
    return b.<WhileStatementTreeImpl>nonterminal(JavaGrammar.WHILE_STATEMENT)
      .is(f.whileStatement(b.invokeRule(JavaKeyword.WHILE), PARENTHESIZED_EXPRESSION(), b.invokeRule(JavaGrammar.STATEMENT)));
  }

  public DoWhileStatementTreeImpl DO_WHILE_STATEMENT() {
    return b.<DoWhileStatementTreeImpl>nonterminal(JavaGrammar.DO_STATEMENT)
      .is(
        f.doWhileStatement(b.invokeRule(JavaKeyword.DO), b.invokeRule(JavaGrammar.STATEMENT), b.invokeRule(JavaKeyword.WHILE), PARENTHESIZED_EXPRESSION(),
          b.invokeRule(JavaPunctuator.SEMI)));
  }

  public SwitchStatementTreeImpl SWITCH_STATEMENT() {
    return b.<SwitchStatementTreeImpl>nonterminal(JavaGrammar.SWITCH_STATEMENT)
      .is(
        f.switchStatement(
          b.invokeRule(JavaKeyword.SWITCH), PARENTHESIZED_EXPRESSION(), b.invokeRule(JavaPunctuator.LWING),
          b.zeroOrMore(SWITCH_GROUP()),
          b.invokeRule(JavaPunctuator.RWING)));
  }

  public CaseGroupTreeImpl SWITCH_GROUP() {
    return b.<CaseGroupTreeImpl>nonterminal(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP)
      .is(f.switchGroup(b.oneOrMore(SWITCH_LABEL()), b.zeroOrMore(b.invokeRule(JavaGrammar.BLOCK_STATEMENT))));
  }

  public CaseLabelTreeImpl SWITCH_LABEL() {
    return b.<CaseLabelTreeImpl>nonterminal(JavaGrammar.SWITCH_LABEL)
      .is(
        b.firstOf(
          f.newCaseSwitchLabel(b.invokeRule(JavaKeyword.CASE), b.invokeRule(JavaGrammar.CONSTANT_EXPRESSION), b.invokeRule(JavaPunctuator.COLON)),
          f.newDefaultSwitchLabel(b.invokeRule(JavaKeyword.DEFAULT), b.invokeRule(JavaPunctuator.COLON))));
  }

  public SynchronizedStatementTreeImpl SYNCHRONIZED_STATEMENT() {
    return b.<SynchronizedStatementTreeImpl>nonterminal(JavaGrammar.SYNCHRONIZED_STATEMENT)
      .is(f.synchronizedStatement(b.invokeRule(JavaKeyword.SYNCHRONIZED), PARENTHESIZED_EXPRESSION(), BLOCK()));
  }

  public BreakStatementTreeImpl BREAK_STATEMENT() {
    return b.<BreakStatementTreeImpl>nonterminal(JavaGrammar.BREAK_STATEMENT)
      .is(f.breakStatement(b.invokeRule(JavaKeyword.BREAK), b.optional(b.invokeRule(JavaTokenType.IDENTIFIER)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ContinueStatementTreeImpl CONTINUE_STATEMENT() {
    return b.<ContinueStatementTreeImpl>nonterminal(JavaGrammar.CONTINUE_STATEMENT)
      .is(f.continueStatement(b.invokeRule(JavaKeyword.CONTINUE), b.optional(b.invokeRule(JavaTokenType.IDENTIFIER)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ReturnStatementTreeImpl RETURN_STATEMENT() {
    return b.<ReturnStatementTreeImpl>nonterminal(JavaGrammar.RETURN_STATEMENT)
      .is(f.returnStatement(b.invokeRule(JavaKeyword.RETURN), b.optional(b.invokeRule(JavaGrammar.EXPRESSION)), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public ThrowStatementTreeImpl THROW_STATEMENT() {
    return b.<ThrowStatementTreeImpl>nonterminal(JavaGrammar.THROW_STATEMENT)
      .is(f.throwStatement(b.invokeRule(JavaKeyword.THROW), b.invokeRule(JavaGrammar.EXPRESSION), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public LabeledStatementTreeImpl LABELED_STATEMENT() {
    return b.<LabeledStatementTreeImpl>nonterminal(JavaGrammar.LABELED_STATEMENT)
      .is(f.labeledStatement(b.invokeRule(IDENTIFIER), b.invokeRule(COLON), b.invokeRule(JavaGrammar.STATEMENT)));
  }

  public ExpressionStatementTreeImpl EXPRESSION_STATEMENT() {
    return b.<ExpressionStatementTreeImpl>nonterminal(JavaGrammar.EXPRESSION_STATEMENT)
      .is(f.expressionStatement(b.invokeRule(JavaGrammar.STATEMENT_EXPRESSION), b.invokeRule(JavaPunctuator.SEMI)));
  }

  public EmptyStatementTreeImpl EMPTY_STATEMENT() {
    return b.<EmptyStatementTreeImpl>nonterminal(JavaGrammar.EMPTY_STATEMENT)
      .is(f.emptyStatement(b.invokeRule(JavaPunctuator.SEMI)));
  }

  // End of statements

  // Expressions

  public ExpressionTree LAMBDA_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.LAMBDA_EXPRESSION)
      .is(f.lambdaExpression(b.invokeRule(JavaGrammar.LAMBDA_PARAMETERS), b.invokeRule(JavaGrammar.ARROW), b.invokeRule(JavaGrammar.LAMBDA_BODY)));
  }

  public ParenthesizedTreeImpl PARENTHESIZED_EXPRESSION() {
    return b.<ParenthesizedTreeImpl>nonterminal(JavaGrammar.PAR_EXPRESSION)
      .is(f.parenthesizedExpression(b.invokeRule(JavaPunctuator.LPAR), b.invokeRule(JavaGrammar.EXPRESSION), b.invokeRule(JavaPunctuator.RPAR)));
  }

  public ExpressionTree EXPLICIT_GENERIC_INVOCATION_EXPRESSION() {
    // TODO Own tree node?
    return b.<ExpressionTree>nonterminal(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_EXPRESSION)
      .is(
        f.completeExplicityGenericInvocation(
          b.invokeRule(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS),
          b.firstOf(
            f.newExplicitGenericInvokation(b.invokeRule(JavaGrammar.EXPLICIT_GENERIC_INVOCATION_SUFFIX)),
            f.newExplicitGenericInvokation(b.invokeRule(JavaKeyword.THIS), b.invokeRule(JavaGrammar.ARGUMENTS)))));
  }

  public ExpressionTree THIS_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.THIS_EXPRESSION)
      .is(f.thisExpression(b.invokeRule(JavaKeyword.THIS), b.optional(b.invokeRule(JavaGrammar.ARGUMENTS))));
  }

  public ExpressionTree SUPER_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.SUPER_EXPRESSION)
      .is(f.superExpression(b.invokeRule(JavaKeyword.SUPER), b.invokeRule(JavaGrammar.SUPER_SUFFIX)));
  }

  public ExpressionTree NEW_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.NEW_EXPRESSION)
      .is(f.newExpression(b.invokeRule(JavaKeyword.NEW), b.zeroOrMore(b.invokeRule(JavaGrammar.ANNOTATION)), CREATOR()));
  }

  public ExpressionTree CREATOR() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.CREATOR)
      .is(
        f.completeCreator(
          b.optional(b.invokeRule(JavaGrammar.NON_WILDCARD_TYPE_ARGUMENTS)),
          b.firstOf(
            f.newClassCreator(b.invokeRule(JavaGrammar.CREATED_NAME), b.invokeRule(JavaGrammar.CLASS_CREATOR_REST)),
            f.newArrayCreator(
              b.firstOf(
                b.invokeRule(JavaGrammar.CLASS_TYPE),
                b.invokeRule(JavaGrammar.BASIC_TYPE)),
              ARRAY_CREATOR_REST()))));
  }

  public NewArrayTreeImpl ARRAY_CREATOR_REST() {
    return b.<NewArrayTreeImpl>nonterminal(JavaGrammar.ARRAY_CREATOR_REST)
      .is(
        f.completeArrayCreator(
          b.zeroOrMore(b.invokeRule(JavaGrammar.ANNOTATION)),
          b.firstOf(
            f.newArrayCreatorWithInitializer(
              b.invokeRule(JavaPunctuator.LBRK), b.invokeRule(JavaPunctuator.RBRK), b.zeroOrMore(b.invokeRule(JavaGrammar.DIM)), b.invokeRule(JavaGrammar.ARRAY_INITIALIZER)),
            f.newArrayCreatorWithDimension(
              b.invokeRule(JavaPunctuator.LBRK), b.invokeRule(JavaGrammar.EXPRESSION), b.invokeRule(JavaPunctuator.RBRK),
              b.zeroOrMore(b.invokeRule(JavaGrammar.DIM_EXPR)),
              b.zeroOrMore(f.newWrapperAstNode(b.zeroOrMore(b.invokeRule(JavaGrammar.ANNOTATION)), b.invokeRule(JavaGrammar.DIM)))))));
  }

  public ExpressionTree BASIC_CLASS_EXPRESSION() {
    return b
      .<ExpressionTree>nonterminal(JavaGrammar.BASIC_CLASS_EXPRESSION)
      .is(
        f.basicClassExpression(b.invokeRule(JavaGrammar.BASIC_TYPE), b.zeroOrMore(b.invokeRule(JavaGrammar.DIM)), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS)));
  }

  public ExpressionTree VOID_CLASS_EXPRESSION() {
    return b.<ExpressionTree>nonterminal(JavaGrammar.VOID_CLASS_EXPRESSION)
      .is(f.voidClassExpression(b.invokeRule(JavaKeyword.VOID), b.invokeRule(JavaPunctuator.DOT), b.invokeRule(JavaKeyword.CLASS)));
  }

  // End of expressions

  public interface GrammarBuilder {

    <T> NonterminalBuilder<T> nonterminal();

    <T> NonterminalBuilder<T> nonterminal(GrammarRuleKey ruleKey);

    <T> T firstOf(T... methods);

    <T> Optional<T> optional(T method);

    <T> List<T> oneOrMore(T method);

    <T> Optional<List<T>> zeroOrMore(T method);

    AstNode invokeRule(GrammarRuleKey ruleKey);

    AstNode token(String value);

  }

  public interface NonterminalBuilder<T> {

    T is(T method);

  }

}
