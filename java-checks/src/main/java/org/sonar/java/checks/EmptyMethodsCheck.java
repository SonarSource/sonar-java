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

import com.sonar.sslr.api.AstNode;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(
  key = EmptyMethodsCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class EmptyMethodsCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1186";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (!tree.modifiers().modifiers().contains(Modifier.ABSTRACT)) {
      super.visitClass(tree);
    } else {
      scan(tree.modifiers());
      scan(tree.typeParameters());
      scan(tree.superClass());
      scan(tree.superInterfaces());
      for (Tree memberTree : tree.members()) {
        if (memberTree.is(Kind.METHOD)) {
          super.visitMethod((MethodTree) memberTree);
        } else {
          scan(memberTree);
        }
      }
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);

    BlockTree block = tree.block();
    if (block != null && block.body().isEmpty() && !tree.is(Kind.CONSTRUCTOR) && !containsComment(block)) {
      context.addIssue(tree, ruleKey, "Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or complete the implementation.");
    }
  }

  private static boolean containsComment(BlockTree tree) {
    AstNode blockAstNode = ((JavaTree) tree).getAstNode();
    return blockAstNode.getLastToken().hasTrivia();
  }

}
