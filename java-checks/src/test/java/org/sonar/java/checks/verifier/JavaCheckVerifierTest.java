package org.sonar.java.checks.verifier;

import com.google.common.collect.ImmutableList;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaCheckVerifierTest {

  public static final String FILENAME = "src/test/files/JavaCheckVerifier.java";

  @Test
  public void verify_line_issues() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssue(1, "message");
        addIssue(3, "message1");
      }
    };

    JavaCheckVerifier.verify("src/test/files/JavaCheckVerifier.java", visitor);
  }

  @Test
  public void verify_unexpected_issue() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssue(1, "message");
        addIssue(3, "message1");
        addIssue(4, "message1");
      }
    };

    try {
      JavaCheckVerifier.verify(FILENAME, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Unexpected at 4");
    }
  }

  @Test
  public void verify_issue_on_file() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        super.scanFile(context);
        addIssueOnFile("messageOnFile");
      }
    };
    JavaCheckVerifier.verifyIssueOnFile(FILENAME, "messageOnFile", visitor);

  }

  @Test
  public void verify_no_issue_fail_if_noncompliant() {
    IssuableSubscriptionVisitor visitor = new IssuableSubscriptionVisitor() {
      @Override
      public List<Tree.Kind> nodesToVisit() {
        return ImmutableList.of();
      }
    };
    try {
      JavaCheckVerifier.verifyNoIssue(FILENAME, visitor);
      Fail.fail();
    } catch (AssertionError e) {
      assertThat(e).hasMessage("The file should not declare noncompliants when no issues are expected");
    }
  }
}
