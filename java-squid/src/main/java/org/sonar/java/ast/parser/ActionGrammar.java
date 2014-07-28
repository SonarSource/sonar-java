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
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.java.model.KindMaps;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.List;

import static org.sonar.java.ast.api.JavaPunctuator.LWING;
import static org.sonar.java.ast.api.JavaPunctuator.RWING;

public class ActionGrammar {

  // TODO Visibility
  public final GrammarBuilder b;
  public final TreeFactory f;

  public ActionGrammar(GrammarBuilder b, TreeFactory f) {
    this.b = b;
    this.f = f;
  }

  public ModifiersTree DSL_MODIFIERS() {
    return b.<ModifiersTree>nonterminal(JavaGrammar.DSL_MODIFIERS)
      .is(f.modifiers(b.zeroOrMore(b.invokeRule(JavaGrammar.MODIFIER))));
  }

  public BlockTree BLOCK() {
    return b.<BlockTree>nonterminal(JavaGrammar.BLOCK)
      .is(f.block(b.invokeRule(LWING), b.invokeRule(JavaGrammar.BLOCK_STATEMENTS), b.invokeRule(RWING)));
  }

  // 14.9. The if Statement
  public IfStatementTree IF_STATEMENT() {
    return b.<IfStatementTree>nonterminal(JavaGrammar.IF_STATEMENT)
      .is(
        f.completeIf(
          b.invokeRule(JavaKeyword.IF), b.invokeRule(JavaGrammar.PAR_EXPRESSION), b.invokeRule(JavaGrammar.STATEMENT),
          b.optional(
            f.newIfWithElse(b.invokeRule(JavaKeyword.ELSE), b.invokeRule(JavaGrammar.STATEMENT)))));
  }

  public static class TreeFactory {

    private final KindMaps kindMaps = new KindMaps();

    private final JavaTreeMaker treeMaker = new JavaTreeMaker();

    public ModifiersTree modifiers(Optional<List<AstNode>> modifierNodes) {
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

    public BlockTree block(AstNode lwing, AstNode statements, AstNode rwing) {
      return new BlockTreeImpl(Tree.Kind.BLOCK, treeMaker.blockStatements(statements), lwing, statements, rwing);
    }

    public IfStatementTree completeIf(AstNode ifToken, AstNode condition, AstNode statement, Optional<IfStatementTreeImpl> elseClause) {
      if (elseClause.isPresent()) {
        return elseClause.get().complete(treeMaker.expression(condition), treeMaker.statement(statement), ifToken, condition, statement);
      } else {
        return new IfStatementTreeImpl(treeMaker.expression(condition), treeMaker.statement(statement), ifToken, condition, statement);
      }
    }

    public IfStatementTreeImpl newIfWithElse(AstNode elseToken, AstNode elseStatement) {
      return new IfStatementTreeImpl(treeMaker.statement(elseStatement), elseToken, elseStatement);
    }

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
