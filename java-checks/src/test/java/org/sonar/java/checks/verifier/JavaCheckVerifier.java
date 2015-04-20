package org.sonar.java.checks.verifier;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Fail;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceCode;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;


/**
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * For example:
 * <pre>
 *   // Noncompliant@+1 {{do not import "java.util.List"}}
 *   import java.util.List;
 * </pre>
 */
public class JavaCheckVerifier extends SubscriptionVisitor {

  private static final String TRIGGER = "// Noncompliant";
  private ArrayListMultimap<Integer, String> expected = ArrayListMultimap.create();
  private boolean expectNoIssues = false;
  private String expectFileIssue;
  private Integer expectFileIssueOnline;

  public static void verify(String filename, JavaFileScanner check) {
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(Lists.newArrayList(check, new JavaCheckVerifier()), Lists.<File>newArrayList(), null));
  }

  public static void verifyNoIssue(String filename, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.expectNoIssues = true;
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(Lists.newArrayList(check, javaCheckVerifier), Lists.<File>newArrayList(), null));
  }

  public static void verifyIssueOnFile(String filename, String message, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.expectFileIssue = message;
    javaCheckVerifier.expectFileIssueOnline = null;
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(Lists.newArrayList(check, javaCheckVerifier), Lists.<File>newArrayList(), null));
  }



  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
  }

  private void collectExpectedIssues(String comment, int line) {
    if (comment.startsWith(TRIGGER)) {
      comment = StringUtils.remove(comment, TRIGGER);

      String expectedMessage = StringUtils.substringBetween(comment, "{{", "}}");

      comment = StringUtils.stripEnd(StringUtils.remove(comment, "{{" + expectedMessage + "}}"), " \t");
      if (StringUtils.startsWith(comment, "@")) {
        final int lineAdjustment;
        final char firstChar = comment.charAt(1);
        final int endIndex = comment.indexOf(' ');
        if (endIndex == -1) {
          lineAdjustment = Integer.parseInt(comment.substring(1));
          comment = "";
        } else {
          lineAdjustment = Integer.parseInt(comment.substring(1, endIndex));
          comment = comment.substring(endIndex + 1).trim();
        }
        if (firstChar == '+') {
          line += lineAdjustment;
        } else if (firstChar == '-') {
          line -= lineAdjustment;
        } else {
          line = lineAdjustment;
        }
      }
      comment = StringUtils.trim(comment);
      int times = StringUtils.isEmpty(comment) ? 1 : Integer.parseInt(comment);
      for (int i = 0; i < times; i++) {
        expected.put(line, expectedMessage);
      }
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    expected.clear();
    super.scanFile(context);
    VisitorsBridge.DefaultJavaFileScannerContext djfsc = (VisitorsBridge.DefaultJavaFileScannerContext) context;
    // leave file.
    checkIssues(djfsc.sourceFile);
    expected.clear();
  }

  private void checkIssues(SourceCode sourceCode) {
    if (expectNoIssues) {
      assertNoIssues(sourceCode);
    } else if (StringUtils.isNotEmpty(expectFileIssue)) {
      assertSingleIssue(sourceCode);
    } else {
      Preconditions.checkState(sourceCode.hasCheckMessages(), "At least one issue expected");
      for (CheckMessage checkMessage : sourceCode.getCheckMessages()) {
        int line = checkMessage.getLine();
        if (!expected.containsKey(line)) {
          throw Fail.fail("Unexpected at " + line);
        }
        List<String> list = expected.get(line);
        String expectedMessage = list.remove(list.size() - 1);
        if (expectedMessage != null) {
          assertThat(checkMessage.getText(Locale.US)).isEqualTo(expectedMessage);
        }
      }
      if (!expected.isEmpty()) {
        throw Fail.fail("Expected " + expected);
      }
    }

  }

  private void assertSingleIssue(SourceCode sourceCode) {
    Set<CheckMessage> checkMessages = sourceCode.getCheckMessages();
    Preconditions.checkState(checkMessages.size() == 1, "A single issue is expected with line " + expectFileIssueOnline);
    CheckMessage checkMessage = Iterables.getFirst(checkMessages, null);
    assertThat(checkMessage.getLine()).isEqualTo(expectFileIssueOnline);
    assertThat(checkMessage.getText(Locale.US)).isEqualTo(expectFileIssue);
  }

  private void assertNoIssues(SourceCode sourceCode) {
    assertThat(sourceCode.getCheckMessages()).overridingErrorMessage("No issues expected").isEmpty();
    // make sure we do not copy&paste verifyNoIssue call when we intend to call verify
    assertThat(expected.isEmpty()).overridingErrorMessage("The file should not declare noncompliants when no issues are expected").isTrue();
  }

}
