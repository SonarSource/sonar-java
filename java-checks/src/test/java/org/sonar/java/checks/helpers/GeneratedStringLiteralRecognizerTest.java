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
package org.sonar.java.checks.helpers;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class GeneratedStringLiteralRecognizerTest {

  private static final String TEST_FILE = "checks/helpers/GeneratedStringLiteralRecognizerSample.java";

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE))
      .withCheck(new TestCheck())
      .verifyIssues();
  }

  @Test
  void test_without_dependencies() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE))
      .withCheck(new TestCheck())
      .withClassPath(List.of())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FILE))
      .withCheck(new TestCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  private static class TestCheck extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return List.of(Tree.Kind.STRING_LITERAL, Tree.Kind.CHAR_LITERAL, Tree.Kind.TEXT_BLOCK);
    }

    @Override
    public void visitNode(Tree tree) {
      if (GeneratedStringLiteralRecognizer.isGenerated((LiteralTree) tree)) {
        reportIssue(tree, "Recognized as generated.");
      }
    }
  }
}
