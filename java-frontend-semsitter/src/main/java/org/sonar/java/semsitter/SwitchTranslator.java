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
import java.util.stream.Collectors;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.pattern.NullPatternTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.PatternTree;

import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.switchResultType;
import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.keyword;
import static org.sonar.java.semsitter.CstNodes.nextIsGroup;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates try statements, switch statements and switch expressions. */
final class SwitchTranslator extends TranslatorPart {

  SwitchTranslator(TranslationState state) {
    super(state);
  }

  TryStatementTreeImpl tryStatement(CstNode node) {
    ResourceListTreeImpl resources = ResourceListTreeImpl.emptyList();
    CstNode resourceNode = field(node, "resources");
    if (resourceNode != null) {
      addResources(resources, resourceNode);
    }
    List<org.sonar.plugins.java.api.tree.CatchTree> catches = catches(node);
    CstNode finallyBlock = childKind(node, "finally_clause");
    return new TryStatementTreeImpl(
      token(required(node, "try")),
      resources.isEmpty() ? null : token(required(resourceNode, "(")),
      resources,
      resources.isEmpty() ? null : token(required(resourceNode, ")")),
      statements().block(requiredField(node, "body", node)),
      catches,
      finallyBlock == null ? null : token(required(finallyBlock, "finally")),
      finallyBlock == null ? null : statements().block(finallyBlock));
  }

  private void addResources(ResourceListTreeImpl resources, CstNode resourceNode) {
    for (CstNode child : resourceNode.children()) {
      if ("resource".equals(child.k()) && field(child, "name") == null) {
        resources.add(expressions().expression(firstNamed(child)));
      } else if ("resource".equals(child.k())) {
        VariableTreeImpl resource = members().variable(child);
        // A resource variable is implicitly final.
        if (resource.symbol() instanceof JSymbol symbol) {
          symbol.addModifiers(FINAL_MODIFIER);
        }
        resources.add(resource);
      } else if (";".equals(child.text())) {
        resources.separators().add(token(child));
      }
    }
  }

  private List<org.sonar.plugins.java.api.tree.CatchTree> catches(CstNode node) {
    List<org.sonar.plugins.java.api.tree.CatchTree> catches = new ArrayList<>();
    for (CstNode child : node.children()) {
      if ("catch_clause".equals(child.k())) {
        state.scopes.addFirst(new HashMap<>());
        try {
          VariableTreeImpl variable = members().variable(required(child, "catch_formal_parameter"));
          catches.add(new CatchTreeImpl(
            token(required(child, "catch")),
            token(required(child, "(")),
            variable,
            token(required(child, ")")),
            statements().block(requiredField(child, "body", childKind(child, "block")))));
        } finally {
          state.scopes.removeFirst();
        }
      }
    }
    return catches;
  }

  SwitchStatementTreeImpl switchStatement(CstNode node) {
    Parens parens = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    CstNode body = requiredField(node, "body", node);
    return new SwitchStatementTreeImpl(
      token(required(node, "switch")),
      parens.open(),
      parens.expr(),
      parens.close(),
      token(required(body, "{")),
      switchGroups(body),
      token(required(body, "}")));
  }

  SwitchExpressionTreeImpl switchExpression(CstNode node) {
    boolean previous = state.switchExpression;
    state.switchExpression = true;
    Parens parens = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    CstNode body = requiredField(node, "body", node);
    SwitchExpressionTreeImpl tree = new SwitchExpressionTreeImpl(
      token(required(node, "switch")),
      parens.open(),
      parens.expr(),
      parens.close(),
      token(required(body, "{")),
      switchGroups(body),
      token(required(body, "}")));
    state.switchExpression = previous;
    ascribe(tree, switchResultType(tree));
    return tree;
  }

  private List<CaseGroupTreeImpl> switchGroups(CstNode body) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> carried = new ArrayList<>();
    List<CstNode> children = body.children();
    for (int i = 0; i < children.size(); i++) {
      CstNode child = children.get(i);
      if ("switch_rule".equals(child.k()) || "switch_block_statement_group".equals(child.k())) {
        CaseGroupTreeImpl group = switchArm(child, carried);
        if (group.body().isEmpty() && "switch_block_statement_group".equals(child.k()) && nextIsGroup(children, i)) {
          carried = group.labels().stream().map(CaseLabelTreeImpl.class::cast).collect(Collectors.toCollection(ArrayList::new));
        } else {
          groups.add(group);
          carried = new ArrayList<>();
        }
      }
    }
    return groups;
  }

  private CaseGroupTreeImpl switchArm(CstNode node, List<CaseLabelTreeImpl> carried) {
    List<CaseLabelTreeImpl> labels = new ArrayList<>(carried);
    StatementListTreeImpl body = StatementListTreeImpl.emptyList();
    List<CstNode> children = node.children();
    int i = 0;
    while (i < children.size()) {
      CstNode child = children.get(i);
      if ("switch_label".equals(child.k())) {
        i++;
        labels.add(caseLabel(child, token(children.get(i))));
      } else if ("expression_statement".equals(child.k()) && "switch_rule".equals(node.k())) {
        ExpressionStatementTreeImpl statement = statements().expressionStatement(child);
        body.add(new YieldStatementTreeImpl(null, statement.expression(), (InternalSyntaxToken) statement.semicolonToken()));
      } else {
        statements().addStatement(body, child);
      }
      i++;
    }
    return new CaseGroupTreeImpl(labels, body);
  }

  private CaseLabelTreeImpl caseLabel(CstNode node, InternalSyntaxToken separator) {
    InternalSyntaxToken keyword = keyword(node, "case") != null ? token(keyword(node, "case")) : token(required(node, "default"));
    List<ExpressionTree> expressions = new ArrayList<>();
    boolean caseLabel = keyword(node, "case") != null;
    for (CstNode child : node.children()) {
      if (!"case".equals(child.text()) && !",".equals(child.text())) {
        addLabelElement(expressions, child, caseLabel);
      }
    }
    return new CaseLabelTreeImpl(keyword, expressions, separator);
  }

  private void addLabelElement(List<ExpressionTree> expressions, CstNode child, boolean caseLabel) {
    if ("default".equals(child.text())) {
      if (caseLabel) {
        expressions.add(new DefaultPatternTreeImpl(token(child), Type.UNKNOWN));
      }
    } else if ("pattern".equals(child.k()) || "record_pattern".equals(child.k()) || "type_pattern".equals(child.k())) {
      expressions.add(lambdas().pattern(child));
    } else if ("guard".equals(child.k()) && !expressions.isEmpty() && expressions.get(expressions.size() - 1) instanceof PatternTree guarded) {
      GuardedPatternTreeImpl pattern = new GuardedPatternTreeImpl(guarded, token(required(child, "when")), expressions().expression(firstNamed(child)), guarded.symbolType());
      expressions.set(expressions.size() - 1, pattern);
    } else if ("null_literal".equals(child.k())) {
      expressions.add(new NullPatternTreeImpl((LiteralTreeImpl) expressions().expression(child)));
    } else {
      expressions.add(expressions().expression(child));
    }
  }
}
