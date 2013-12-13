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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;
import com.sonar.sslr.api.AstNode;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Map;

@Rule(
  key = SynchronizedClassUsageCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SynchronizedClassUsageCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  public static final String RULE_KEY = "S1149";
  private static final RuleKey RULE_KEY_FOR_REPOSITORY = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String>builder()
    .put("Vector", "\"ArrayList\" or \"LinkedList\"")
    .put("Hashtable", "\"HashMap\"")
    .put("StringBuffer", "\"StringBuilder\"")
    .put("Stack", "\"Deque\"")
    .build();


  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    String declaredType = getTypeName(tree.type());

    if (REPLACEMENTS.containsKey(declaredType)) {
      reportIssue(tree.type(), declaredType);
    } else {
      ExpressionTree init = tree.initializer();
      if (init != null && init.is(Tree.Kind.NEW_CLASS)) {
        String initType = getTypeName(((NewClassTree) tree.initializer()).identifier());
        reportIssueIfDeprecatedType(tree.initializer(), initType);
      }
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    scan(tree.modifiers());
    scan(tree.typeParameters());
    scan(tree.returnType());
    scan(tree.defaultValue());
    scan(tree.block());
    if (!isOverriding(tree)) {
      for (VariableTree param : tree.parameters()) {
        reportIssueIfDeprecatedType(param, getTypeName(param.type()));
      }
      reportIssueIfDeprecatedType(tree, getTypeName(tree.returnType()));
    }

  }

  @Override
  public void visitClass(ClassTree tree) {
    super.visitClass(tree);
    for (Tree parent : tree.superInterfaces()) {
      reportIssueIfDeprecatedType(parent, getTypeName(parent));
    }

  }

  private void reportIssueIfDeprecatedType(Tree tree, String type) {
    if (REPLACEMENTS.containsKey(type)) {
      reportIssue(tree, type);
    }
  }

  private void reportIssue(Tree tree, String type) {
    context.addIssue(tree, RULE_KEY_FOR_REPOSITORY, "Replace the synchronized class \"" + type + "\" by an unsynchronized one such as " + REPLACEMENTS.get(type) + ".");
  }

  private String getTypeName(Tree typeTree) {
    if (typeTree != null) {

      if (typeTree.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) typeTree).name();
      } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) typeTree).identifier().name();
      } else if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
        return getTypeName(((ParameterizedTypeTree) typeTree).type());
      }
    }
    return "";
  }

  private boolean isOverriding(MethodTree tree) {
    AstNode memberDec = ((JavaTree) tree).getAstNode().getParent().getParent();
    for (AstNode modifier : memberDec.getChildren(JavaGrammar.MODIFIER)) {
      if (AstNodeTokensMatcher.matches(modifier, "@Override")) {
        return true;
      }
    }
    return false;
  }
}
