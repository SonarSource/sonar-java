/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.ast;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;
import java.util.List;

public class CheckMessageVerifierCompatibilityTest {

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/ast/CheckMessageVerifierCompatibility.java"), new VisitorsBridge(new CustomRuleCheck()));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(3).withMessage("method message")
      .next().atLine(4).withMessage("variable message")
      .noMore();
  }

  private class CustomRuleCheck extends SubscriptionVisitor {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      super.scanFile(context);
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.VARIABLE);
    }

    @Override
    public void visitNode(Tree tree) {
      if (tree.is(Tree.Kind.METHOD)) {
        context.addIssue(tree, this, "method message");
      } else {
        context.reportIssue(this, tree, "variable message");
      }
    }
  }
}
