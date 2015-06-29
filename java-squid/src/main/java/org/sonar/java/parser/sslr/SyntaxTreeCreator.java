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
package org.sonar.java.parser.sslr;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.api.Trivia.TriviaKind;
import org.sonar.java.model.InternalSyntaxSpacing;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.java.parser.sslr.ActionParser.GrammarBuilderInterceptor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
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
        Object child = visit(node.getChildren().get(0));
        if (child instanceof AstNode) {
          ((AstNode) child).hasToBeSkippedFromAst();
        }
        return Optional.of(child);
      }
    }

    List<ParseNode> children = node.getChildren();
    List<Object> convertedChildren = Lists.newArrayList();
    for (ParseNode child : children) {
      Object result = visit(child);
      if (result != null) {
        // FIXME to remove aafter full migration: Allow to skip optional nodes that are supposed to bw skipped from the AST
        if ((result instanceof Optional && ((Optional) result).isPresent() && ((Optional) result).get() instanceof AstNode) && ((AstNode) ((Optional) result).get()).hasToBeSkippedFromAst()){
          for (AstNode resultChild : ((AstNode) ((Optional) result).get()).getChildren()) {
            convertedChildren.add(resultChild);
          }

        } else if (result instanceof AstNode && ((AstNode) result).hasToBeSkippedFromAst()) {
          for (AstNode resultChild : ((AstNode) result).getChildren()) {
            convertedChildren.add(resultChild);
          }
        } else {
          convertedChildren.add(result);
        }
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
      return createNonTerminal(ruleKey, rule, convertedChildren, node.getStartIndex(), node.getEndIndex());
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

  private static Object createNonTerminal(GrammarRuleKey ruleKey, Rule rule, List<Object> children, int startIndex, int endIndex) {
    for (Object child : children) {
      if (child instanceof InternalSyntaxToken) {
        InternalSyntaxToken syntaxToken = (InternalSyntaxToken) child;
        syntaxToken.setGrammarRuleKey(ruleKey);
        return child;
      }
    }
    return new InternalSyntaxSpacing(startIndex, endIndex);
  }

  private InternalSyntaxToken visitTerminal(ParseNode node) {
    TokenType type = null;
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
      type = ruleMatcher.getTokenType();
      if (GenericTokenType.COMMENT == ruleMatcher.getTokenType()) {
        tokenBuilder.setTrivia(Collections.<Trivia>emptyList());
        tokenBuilder.setType(ruleMatcher.getTokenType());
        trivias.add(Trivia.createComment(tokenBuilder.build()));
        return null;
      }
    }
    InternalSyntaxToken internalSyntaxToken =
      createTerminal(input, node.getStartIndex(), node.getEndIndex(), Collections.unmodifiableList(trivias), type);
    trivias.clear();
    return internalSyntaxToken;
  }

  private static InternalSyntaxToken createTerminal(Input input, int startIndex, int endIndex, List<Trivia> trivias, TokenType type) {
    boolean isEof = GenericTokenType.EOF == type;
    LineColumnValue lineColumnValue = tokenPosition(input, startIndex, endIndex);
    return new InternalSyntaxToken(lineColumnValue.line, lineColumnValue.column, lineColumnValue.value,
      createTrivias(trivias), startIndex, endIndex, isEof);
  }

  private static List<SyntaxTrivia> createTrivias(List<Trivia> trivias) {
    List<SyntaxTrivia> result = Lists.newArrayList();
    for (Trivia trivia : trivias) {
      Token trivialToken = trivia.getToken();
      result.add(InternalSyntaxTrivia.create(trivialToken.getValue(), trivialToken.getLine(), trivialToken.getColumn()));
    }
    return result;
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

  private static LineColumnValue tokenPosition(Input input, int startIndex, int endIndex) {
    int[] lineAndColumn = input.lineAndColumnAt(startIndex);
    String value = input.substring(startIndex, endIndex);
    return new LineColumnValue(lineAndColumn[0], lineAndColumn[1] -1, value);
  }

  private static class LineColumnValue {
    final int line;
    final int column;
    final String value;

    private LineColumnValue(int line, int column, String value) {
      this.line = line;
      this.column = column;
      this.value = value;
    }
  }

}
