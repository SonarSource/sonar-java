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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.java.model.KindMaps;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
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
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.Collections;
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

  public ModifiersTreeImpl DSL_MODIFIERS() {
    return b.<ModifiersTreeImpl>nonterminal(JavaGrammar.DSL_MODIFIERS)
      .is(f.modifiers(b.zeroOrMore(b.invokeRule(JavaGrammar.MODIFIER))));
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
          b.invokeRule(JavaKeyword.IF), b.invokeRule(JavaGrammar.PAR_EXPRESSION), b.invokeRule(JavaGrammar.STATEMENT),
          b.optional(
            f.newIfWithElse(b.invokeRule(JavaKeyword.ELSE), b.invokeRule(JavaGrammar.STATEMENT)))));
  }

  public WhileStatementTreeImpl WHILE_STATEMENT() {
    return b.<WhileStatementTreeImpl>nonterminal(JavaGrammar.WHILE_STATEMENT)
      .is(f.whileStatement(b.invokeRule(JavaKeyword.WHILE), b.invokeRule(JavaGrammar.PAR_EXPRESSION), b.invokeRule(JavaGrammar.STATEMENT)));
  }

  public DoWhileStatementTreeImpl DO_WHILE_STATEMENT() {
    return b.<DoWhileStatementTreeImpl>nonterminal(JavaGrammar.DO_STATEMENT)
      .is(
        f.doWhileStatement(b.invokeRule(JavaKeyword.DO), b.invokeRule(JavaGrammar.STATEMENT), b.invokeRule(JavaKeyword.WHILE), b.invokeRule(JavaGrammar.PAR_EXPRESSION),
          b.invokeRule(JavaPunctuator.SEMI)));
  }

  public SwitchStatementTreeImpl SWITCH_STATEMENT() {
    return b.<SwitchStatementTreeImpl>nonterminal(JavaGrammar.SWITCH_STATEMENT)
      .is(
        f.switchStatement(
          b.invokeRule(JavaKeyword.SWITCH), b.invokeRule(JavaGrammar.PAR_EXPRESSION), b.invokeRule(JavaPunctuator.LWING),
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
      .is(f.synchronizedStatement(b.invokeRule(JavaKeyword.SYNCHRONIZED), b.invokeRule(JavaGrammar.PAR_EXPRESSION), BLOCK()));
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

  public static class TreeFactory {

    private final KindMaps kindMaps = new KindMaps();

    private final JavaTreeMaker treeMaker = new JavaTreeMaker();

    public ModifiersTreeImpl modifiers(Optional<List<AstNode>> modifierNodes) {
      if (!modifierNodes.isPresent()) {
        return ModifiersTreeImpl.EMPTY_MODIFIERS;
      }

      ImmutableList.Builder<Modifier> modifiers = ImmutableList.builder();
      ImmutableList.Builder<AnnotationTree> annotations = ImmutableList.builder();
      for (AstNode astNode : modifierNodes.get()) {
        Preconditions.checkArgument(astNode.is(JavaGrammar.MODIFIER), "Unexpected AstNodeType: %s", astNode.getType().toString());
        astNode = astNode.getFirstChild();
        if (astNode.is(JavaGrammar.ANNOTATION)) {
          annotations.add(treeMaker.annotation(astNode));
        } else {
          JavaKeyword keyword = (JavaKeyword) astNode.getType();
          modifiers.add(kindMaps.getModifier(keyword));
        }
      }

      return new ModifiersTreeImpl(modifierNodes.get(), modifiers.build(), annotations.build());
    }

    // Statements

    public BlockTreeImpl block(AstNode leftCurlyBraceToken, AstNode statements, AstNode rightCurlyBraceToken) {
      return new BlockTreeImpl(Tree.Kind.BLOCK, treeMaker.blockStatements(statements), leftCurlyBraceToken, statements, rightCurlyBraceToken);
    }

    public AssertStatementTreeImpl completeAssertStatement(AstNode assertToken, AstNode expression, Optional<AssertStatementTreeImpl> detailExpression, AstNode semicolonToken) {
      return detailExpression.isPresent() ?
        detailExpression.get().complete(treeMaker.expression(expression),
          assertToken, expression, semicolonToken) :
        new AssertStatementTreeImpl(treeMaker.expression(expression),
          assertToken, expression, semicolonToken);
    }

    public AssertStatementTreeImpl newAssertStatement(AstNode colonToken, AstNode expression) {
      return new AssertStatementTreeImpl(treeMaker.expression(expression),
        colonToken, expression);
    }

    public IfStatementTreeImpl completeIf(AstNode ifToken, AstNode condition, AstNode statement, Optional<IfStatementTreeImpl> elseClause) {
      if (elseClause.isPresent()) {
        return elseClause.get().complete(treeMaker.expression(condition), treeMaker.statement(statement), ifToken, condition, statement);
      } else {
        return new IfStatementTreeImpl(treeMaker.expression(condition), treeMaker.statement(statement), ifToken, condition, statement);
      }
    }

    public IfStatementTreeImpl newIfWithElse(AstNode elseToken, AstNode elseStatement) {
      return new IfStatementTreeImpl(treeMaker.statement(elseStatement), elseToken, elseStatement);
    }

    public WhileStatementTreeImpl whileStatement(AstNode whileToken, AstNode expression, AstNode statement) {
      return new WhileStatementTreeImpl(treeMaker.expression(expression), treeMaker.statement(statement),
        whileToken, expression, statement);
    }

    public DoWhileStatementTreeImpl doWhileStatement(AstNode doToken, AstNode statement, AstNode whileToken, AstNode expression, AstNode semicolonToken) {
      return new DoWhileStatementTreeImpl(treeMaker.statement(statement), treeMaker.expression(expression),
        doToken, statement, whileToken, expression, semicolonToken);
    }

    public SwitchStatementTreeImpl switchStatement(
      AstNode switchToken, AstNode expression, AstNode leftCurlyBraceToken, Optional<List<CaseGroupTreeImpl>> optionalGroups, AstNode rightCurlyBraceToken) {

      List<CaseGroupTreeImpl> groups = optionalGroups.isPresent() ? optionalGroups.get() : Collections.<CaseGroupTreeImpl>emptyList();

      ImmutableList.Builder<AstNode> children = ImmutableList.builder();
      children.add(switchToken, expression, leftCurlyBraceToken);
      children.addAll(groups);
      children.add(rightCurlyBraceToken);

      return new SwitchStatementTreeImpl(treeMaker.expression(expression), groups,
        children.build());
    }

    public CaseGroupTreeImpl switchGroup(List<CaseLabelTreeImpl> labels, Optional<List<AstNode>> optionalBlockStatements) {
      List<AstNode> blockStatements = optionalBlockStatements.isPresent() ? optionalBlockStatements.get() : Collections.<AstNode>emptyList();

      ImmutableList.Builder<StatementTree> builder = ImmutableList.builder();
      for (AstNode blockStatement : blockStatements) {
        builder.addAll(treeMaker.blockStatement(blockStatement));
      }

      return new CaseGroupTreeImpl(labels, builder.build(), blockStatements);
    }

    public CaseLabelTreeImpl newCaseSwitchLabel(AstNode caseToken, AstNode expression, AstNode colonToken) {
      return new CaseLabelTreeImpl(treeMaker.expression(expression),
        caseToken, expression, colonToken);
    }

    public CaseLabelTreeImpl newDefaultSwitchLabel(AstNode defaultToken, AstNode colonToken) {
      return new CaseLabelTreeImpl(null,
        defaultToken, colonToken);
    }

    public SynchronizedStatementTreeImpl synchronizedStatement(AstNode synchronizedToken, AstNode expression, BlockTreeImpl block) {
      return new SynchronizedStatementTreeImpl(treeMaker.expression(expression), block,
        synchronizedToken, expression, block);
    }

    public BreakStatementTreeImpl breakStatement(AstNode breakToken, Optional<AstNode> identifier, AstNode semicolonToken) {
      return identifier.isPresent() ?
        new BreakStatementTreeImpl(treeMaker.identifier(identifier.get()),
          breakToken, identifier.get(), semicolonToken) :
        new BreakStatementTreeImpl(null,
          breakToken, semicolonToken);
    }

    public ContinueStatementTreeImpl continueStatement(AstNode continueToken, Optional<AstNode> identifier, AstNode semicolonToken) {
      return identifier.isPresent() ?
        new ContinueStatementTreeImpl(treeMaker.identifier(identifier.get()),
          continueToken, identifier.get(), semicolonToken) :
        new ContinueStatementTreeImpl(null,
          continueToken, semicolonToken);
    }

    public ReturnStatementTreeImpl returnStatement(AstNode returnToken, Optional<AstNode> expression, AstNode semicolonToken) {
      return expression.isPresent() ?
        new ReturnStatementTreeImpl(treeMaker.expression(expression.get()),
          returnToken, expression.get(), semicolonToken) :
        new ReturnStatementTreeImpl(null,
          returnToken, semicolonToken);
    }

    public ThrowStatementTreeImpl throwStatement(AstNode throwToken, AstNode expression, AstNode semicolonToken) {
      return new ThrowStatementTreeImpl(treeMaker.expression(expression),
        throwToken, expression, semicolonToken);
    }

    public LabeledStatementTreeImpl labeledStatement(AstNode identifier, AstNode colon, AstNode statement) {
      return new LabeledStatementTreeImpl(treeMaker.identifier(identifier), treeMaker.statement(statement),
        identifier, colon, statement);
    }

    public ExpressionStatementTreeImpl expressionStatement(AstNode expression, AstNode semicolonToken) {
      return new ExpressionStatementTreeImpl(treeMaker.expression(expression),
        expression, semicolonToken);
    }

    public EmptyStatementTreeImpl emptyStatement(AstNode semicolon) {
      return new EmptyStatementTreeImpl(semicolon);
    }

    // End of statements

  }

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
