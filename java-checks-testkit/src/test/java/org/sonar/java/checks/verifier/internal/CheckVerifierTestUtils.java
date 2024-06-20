/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java.checks.verifier.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public class CheckVerifierTestUtils {

  private CheckVerifierTestUtils() {
    // utility class
  }

  protected static final String TEST_FILE = "src/test/files/testing/Compliant.java";
  protected static final String TEST_FILE_PARSE_ERROR = "src/test/files/testing/ParsingError.java";
  protected static final String TEST_FILE_NONCOMPLIANT = "src/test/files/testing/Noncompliant.java";
  protected static final String TEST_FILE_NONCOMPLIANT_ISSUE_ON_FILE = "src/test/files/java-check-verifier/CommonsJavaCheckVerifierOnFile.java";
  protected static final String TEST_FILE_WITH_QUICK_FIX = "src/test/files/testing/IssueWithQuickFix.java";
  protected static final String TEST_FILE_WITH_NO_EXPECTED = "src/test/files/testing/IssueWithNoQuickFixExpected.java";
  protected static final String TEST_FILE_WITH_QUICK_FIX_ON_MULTIPLE_LINE = "src/test/files/testing/IssueWithQuickFixMultipleLine.java";
  protected static final String TEST_FILE_WITH_TWO_QUICK_FIX = "src/test/files/testing/IssueWithTwoQuickFixes.java";
  protected static final String TEST_FILE_WITH_PREVIEW_FEATURES = "src/test/files/testing/NeedJava21PreviewFeaturesEnabled.java";

  protected static final JavaFileScanner FAILING_CHECK = new FailingCheck();
  protected static final JavaFileScanner NO_EFFECT_CHECK = new NoEffectCheck();
  protected static final JavaFileScanner FILE_LINE_ISSUE_CHECK = new FileLineIssueCheck();
  protected static final JavaFileScanner PROJECT_ISSUE_CHECK = new ProjectIssueCheck();
  protected static final JavaFileScanner FILE_ISSUE_CHECK = new FileIssueCheck();
  protected static final JavaFileScanner FILE_ISSUE_CHECK_IN_ANDROID = new FileIssueCheckInAndroidContext();

  @Rule(key = "FailingCheck")
  protected static final class FailingCheck implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      throw new RuntimeException("This checks fails systematically with a RuntimeException");
    }
  }

  @Rule(key = "NoEffectCheck")
  protected static final class NoEffectCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      // do nothing
    }
  }

  @Rule(key = "NoEffectEndOfAnalysisCheck")
  protected static class NoEffectEndOfAnalysisCheck implements JavaFileScanner, EndOfAnalysis {
    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      // do nothing
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      // do nothing
    }
  }

  @Rule(key = "FileIssueCheck")
  protected static final class FileIssueCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssueOnFile(this, "issueOnFile");
    }
  }

  @Rule(key = "FileLineIssueCheck")
  protected static final class FileLineIssueCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssue(1, this, "issueOnLine");
    }
  }

  @Rule(key = "ProjectIssueCheck")
  protected static final class ProjectIssueCheck implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      context.addIssueOnProject(this, "issueOnProject");
    }
  }

  @Rule(key = "MultipleIssuePerLineCheck")
  protected static final class MultipleIssuePerLineCheck implements JavaFileScanner {

    private final String msg1;
    private final String msg2;
    private boolean flipOrder;

    MultipleIssuePerLineCheck() {
      this("msg 1", "msg 2");
    }

    MultipleIssuePerLineCheck(String msg1, String msg2) {
      this.msg1 = msg1;
      this.msg2 = msg2;
      this.flipOrder = false;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      String[] msgs = {msg1, msg2};
      report(context, 4, msgs);

      if (flipOrder) {
        msgs = new String[] {msg2, msg1};
      }
      report(context, 7, msgs);
    }

    private void report(JavaFileScannerContext context, int line, String... messages) {
      Stream.of(messages).forEach(msg -> context.addIssue(line, this, msg));
    }

    public void setFlipOrder(boolean flipOrder) {
      this.flipOrder = flipOrder;
    }
  }

  @Rule(key = "IssueWithQuickFix")
  protected static final class IssueWithQuickFix extends IssuableSubscriptionVisitor {
    Supplier<List<JavaQuickFix>> quickFixes;

    IssueWithQuickFix(Supplier<List<JavaQuickFix>> quickFixes) {
      this.quickFixes = quickFixes;
    }

    static IssueWithQuickFix of(Supplier<JavaQuickFix> quickFixes) {
      return new IssueWithQuickFix(() -> Collections.singletonList(quickFixes.get()));
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      ClassTree classTree = (ClassTree) tree;
      ((InternalJavaIssueBuilder) ((JavaFileScannerContextForTests) context).newIssue())
        .forRule(this)
        .onTree(classTree.declarationKeyword())
        .withMessage("message")
        .withQuickFixes(quickFixes)
        .report();
    }
  }

  @Rule(key = "FileIssueAndroidCheck")
  protected static final class FileIssueCheckInAndroidContext implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      if (context.inAndroidContext()) {
        context.addIssueOnFile(this, "issueOnFile");
      }
    }
  }

  protected static boolean equivalent(CacheContext a, CacheContext b) {
    return a.isCacheEnabled() == b.isCacheEnabled() &&
      a.getReadCache().equals(b.getReadCache()) &&
      a.getWriteCache().equals(b.getWriteCache());
  }

}
