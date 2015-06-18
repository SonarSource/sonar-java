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
package org.sonar.java.parser.sslr;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.api.Trivia.TriviaKind;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.parser.sslr.ActionParser2.GrammarBuilderInterceptor;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.internal.grammar.MutableParsingRule;
import org.sonar.sslr.internal.matchers.ParseNode;
import org.sonar.sslr.internal.vm.TokenExpression;
import org.sonar.sslr.internal.vm.TriviaExpression;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class SyntaxTreeCreator<T> {

  private static final TokenType UNDEFINED_TOKEN_TYPE = new TokenType() {

    @Override
    public String getName() {
      return "TOKEN";
    }

    @Override
    public String getValue() {
      return getName();
    }

    @Override
    public boolean hasToBeSkippedFromAst(AstNode node) {
      return false;
    }

    @Override
    public String toString() {
      return SyntaxTreeCreator.class.getSimpleName();
    }

  };

  private final Object treeFactory;
  private final GrammarBuilderInterceptor mapping;

  private final Token.Builder tokenBuilder = Token.builder();
  private final List<Trivia> trivias = Lists.newArrayList();

  private Input input;

  public SyntaxTreeCreator(Object treeFactory, GrammarBuilderInterceptor mapping) {
    this.treeFactory = treeFactory;
    this.mapping = mapping;
  }

  public T create(ParseNode node, Input input) {
    this.input = input;
    this.trivias.clear();
    return (T) visit(node);
  }

  private Object visit(ParseNode node) {
    if (node.getMatcher() instanceof MutableParsingRule) {
      return visitNonTerminal(node);
    } else {
      return visitTerminal(node);
    }
  }

  private Object visitNonTerminal(ParseNode node) {
    MutableParsingRule rule = (MutableParsingRule) node.getMatcher();
    GrammarRuleKey ruleKey = rule.getRuleKey();

    if (mapping.hasMethodForRuleKey(ruleKey)) {
      // TODO Drop useless intermediate nodes
      Preconditions.checkState(node.getChildren().size() == 1);
      return visit(node.getChildren().get(0));
    }

    if (mapping.isOptionalRule(ruleKey)) {
      Preconditions.checkState(node.getChildren().size() <= 1);
      if (node.getChildren().isEmpty()) {
        return Optional.absent();
      } else {
        return Optional.of(visit(node.getChildren().get(0)));
      }
    }

    List<ParseNode> children = node.getChildren();
    List<Object> convertedChildren = Lists.newArrayList();
    for (ParseNode child : children) {
      Object result = visit(child);
      if (result != null) {
        convertedChildren.add(result);
      }
    }

    if (mapping.isOneOrMoreRule(ruleKey)) {
      return Lists.newArrayList(convertedChildren);
    }

    if (mapping.isZeroOrMoreRule(ruleKey)) {
      return convertedChildren.isEmpty() ? Optional.absent() : Optional.of(Lists.newArrayList(convertedChildren));
    }

    Method method = mapping.actionForRuleKey(ruleKey);
    if (method == null) {
      Token token = null;

      for (Object child : convertedChildren) {
        if(child instanceof InternalSyntaxToken) {
          return child;
        }
        if (child instanceof AstNode && ((AstNode) child).hasToken()) {
          token = ((AstNode) child).getToken();
          break;
        }
      }
      AstNode astNode = new AstNode(rule.getRealAstNodeType(), rule.getName(), token);
      for (Object child : convertedChildren) {
        astNode.addChild((AstNode) child);
      }

      astNode.setFromIndex(node.getStartIndex());
      astNode.setToIndex(node.getEndIndex());

      return astNode;
    }

    try {
      return method.invoke(treeFactory, convertedChildren.toArray(new Object[convertedChildren.size()]));
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e);
    } catch (IllegalArgumentException e) {
      throw Throwables.propagate(e);
    } catch (InvocationTargetException e) {
      throw Throwables.propagate(e);
    }
  }

  private InternalSyntaxToken visitTerminal(ParseNode node) {
    if (node.getMatcher() instanceof TriviaExpression) {
      TriviaExpression ruleMatcher = (TriviaExpression) node.getMatcher();
      if (ruleMatcher.getTriviaKind() == TriviaKind.SKIPPED_TEXT) {
        return null;
      } else if (ruleMatcher.getTriviaKind() == TriviaKind.COMMENT) {
        updateTokenPositionAndValue(node);
        tokenBuilder.setTrivia(Collections.<Trivia>emptyList());
        tokenBuilder.setType(GenericTokenType.COMMENT);
        trivias.add(Trivia.createComment(tokenBuilder.build()));
        return null;
      } else {
        throw new IllegalStateException("Unexpected trivia kind: " + ruleMatcher.getTriviaKind());
      }
    } else if (node.getMatcher() instanceof TokenExpression) {
      updateTokenPositionAndValue(node);
      TokenExpression ruleMatcher = (TokenExpression) node.getMatcher();
      tokenBuilder.setType(ruleMatcher.getTokenType());
      if (ruleMatcher.getTokenType() == GenericTokenType.COMMENT) {
        tokenBuilder.setTrivia(Collections.<Trivia>emptyList());
        trivias.add(Trivia.createComment(tokenBuilder.build()));
        return null;
      }
    } else {
      updateTokenPositionAndValue(node);
      tokenBuilder.setType(UNDEFINED_TOKEN_TYPE);
    }
    Token token = tokenBuilder.setTrivia(trivias).build();
    trivias.clear();
    return new InternalSyntaxToken(token);
  }

  private void updateTokenPositionAndValue(ParseNode node) {
    tokenBuilder.setGeneratedCode(false);
    int[] lineAndColumn = input.lineAndColumnAt(node.getStartIndex());
    tokenBuilder.setLine(lineAndColumn[0]);
    tokenBuilder.setColumn(lineAndColumn[1] - 1);
    tokenBuilder.setURI(input.uri());
    String value = input.substring(node.getStartIndex(), node.getEndIndex());
    tokenBuilder.setValueAndOriginalValue(value);
  }

}
