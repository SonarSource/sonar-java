/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.LINE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.QUICK_FIXES;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.SECONDARY_LOCATIONS;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.START_COLUMN;

class ExpectationsTest {

  private static final int TEST_LINE = 42;

  @Test
  void issue_without_details() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant", TEST_LINE).issue;
    assertThat(issue).containsEntry(LINE, TEST_LINE);
  }

  @Test
  void issue_with_message() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant {{message}}", TEST_LINE).issue;
    assertThat(issue).containsEntry(MESSAGE, "message");
  }

  @Test
  void issue_with_attributes() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant [[flows=f1,f2,f3;sc=3;ec=7;el=4;secondary=5]]", TEST_LINE).issue;
    assertThat(issue)
      .containsEntry(FLOWS, Arrays.asList("f1", "f2", "f3"))
      .containsEntry(START_COLUMN, 3)
      .containsEntry(END_COLUMN, 7)
      .containsEntry(END_LINE, Expectations.Parser.LineRef.fromString("4"))
      .containsEntry(SECONDARY_LOCATIONS, Collections.singletonList(5))
      .doesNotContainKey(MESSAGE);
  }

  @Test
  void relative_end_line_attribute() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant [[el=+1]]", TEST_LINE).issue;
    assertThat(issue).containsEntry(END_LINE, Expectations.Parser.LineRef.fromString(String.valueOf(TEST_LINE + 1)));
  }

  @Test
  void relative_secondary_location_attribute() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant [[secondary=-2,+5,31]]", TEST_LINE).issue;
    assertThat(issue)
      .containsEntry(SECONDARY_LOCATIONS, Arrays.asList(TEST_LINE - 2, TEST_LINE + 5, 31));
  }

  @Test
  void invalid_attribute_name() {
    Expectations.Parser parser = new Expectations().parser();
    try {
      parser.parseIssue("// Noncompliant [[invalid]]", TEST_LINE);
      Fail.fail("exception expected");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: 'invalid'");
    }
  }

  @Test
  void issue_with_attributes_and_comment() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant [[sc=1;ec=6]] {{message}}", TEST_LINE).issue;
    assertThat(issue)
      .containsEntry(MESSAGE, "message")
      .containsEntry(START_COLUMN, 1)
      .containsEntry(END_COLUMN, 6);
  }

  @Test
  void issue_with_attributes_and_comment_switched() {
    Expectations.Issue issue = new Expectations().parser().parseIssue("// Noncompliant {{message}} [[sc=1;ec=6]]", TEST_LINE).issue;
    assertThat(issue).containsEntry(MESSAGE, "message")
      .containsEntry(START_COLUMN, 1)
      .containsEntry(END_COLUMN, 6);
  }

  @Test
  void end_line_attribute() {
    Expectations.Parser parser = new Expectations().parser();
    try {
      parser.parseIssue("// Noncompliant [[endLine=-1]] {{message}}", 0);
      Fail.fail("exception expected");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("endLine attribute should be relative to the line and must be +N with N integer");
    }
  }

  @Test
  void line_shifting() {
    Map<Expectations.IssueAttribute, Object> issue = new Expectations().parser().parseIssue("// Noncompliant@-1", TEST_LINE).issue;
    assertThat(issue).containsEntry(Expectations.IssueAttribute.LINE, 41);
  }

  @Test
  void no_flows() {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows(null, 0);
    assertThat(flows).isEmpty();
  }

  @Test
  void flow_without_details() {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows("// flow@npe1", TEST_LINE);
    assertThat(flows).hasSize(1);
    Expectations.FlowComment flow = flows.iterator().next();
    assertThat(flow.id).isEqualTo("npe1");
    assertThat(flow.line).isEqualTo(TEST_LINE);
  }

  @Test
  void flow_with_message() throws Exception {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows("// flow@npe1 {{message}}", TEST_LINE);
    assertThat(flows).hasSize(1);
    Expectations.FlowComment flow = flows.iterator().next();
    assertThat(flow.id).isEqualTo("npe1");
    assertThat(flow.line).isEqualTo(TEST_LINE);
    assertThat(flow.attributes).containsEntry(MESSAGE, "message");
  }

  @Test
  void flow_with_attributes_and_message() throws Exception {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows("// flow@npe1 [[sc=1;ec=6]] {{message}}", TEST_LINE);
    assertThat(flows).hasSize(1);
    Expectations.FlowComment flow = flows.iterator().next();
    assertThat(flow.id).isEqualTo("npe1");
    assertThat(flow.line).isEqualTo(TEST_LINE);
    assertThat(flow.attributes)
      .containsEntry(START_COLUMN, 1)
      .containsEntry(END_COLUMN, 6)
      .containsEntry(MESSAGE, "message");
  }

  @Test
  void issue_and_flow_on_the_same_line() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser().parseIssue("// Noncompliant [[flows=id]] flow@id", TEST_LINE);
    assertThat(iaf.issue).containsEntry(Expectations.IssueAttribute.LINE, TEST_LINE);
    assertThat((List) iaf.issue.get(FLOWS)).contains("id");
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.id).isEqualTo("id");
    assertThat(flow.line).isEqualTo(TEST_LINE);
  }

  @Test
  void issue_and_flow_message() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser().parseIssue("// Noncompliant {{issue msg}} flow@id {{flow msg}}", TEST_LINE);
    assertThat(iaf.issue).containsEntry(MESSAGE, "issue msg");
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.attributes).containsEntry(MESSAGE, "flow msg");
  }

  @Test
  void issue_and_flow_message2() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser().parseIssue("// Noncompliant flow@id {{flow msg}}", TEST_LINE);
    assertThat(iaf.issue.get(MESSAGE)).isNull();
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.attributes).containsEntry(MESSAGE, "flow msg");
  }

  @Test
  void issue_and_flow_attr() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser().parseIssue("// Noncompliant [[sc=1;ec=2]] bla bla flow@id [[sc=3;ec=4]] bla bla", TEST_LINE);
    assertThat(iaf.issue).containsEntry(START_COLUMN, 1)
      .containsEntry(END_COLUMN, 2);
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.attributes)
      .containsEntry(START_COLUMN, 3)
      .containsEntry(END_COLUMN, 4);
  }

  @Test
  void issue_and_flow_all_options() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser()
      .parseIssue("// Noncompliant [[flows;sc=1;ec=2]] bla {{issue msg}} bla flow@id [[sc=3;ec=4]] bla {{flow msg}} bla", TEST_LINE);
    assertThat(iaf.issue).isEqualTo(MapBuilder.<Expectations.IssueAttribute, Object>newMap()
      .put(LINE, 42)
      .put(MESSAGE, "issue msg")
      .put(START_COLUMN, 1)
      .put(END_COLUMN, 2)
      .put(FLOWS, Collections.emptyList()).build());
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.line).isEqualTo(TEST_LINE);
    assertThat(flow.attributes).isEqualTo(MapBuilder.<Expectations.IssueAttribute, Object>newMap()
      .put(MESSAGE, "flow msg")
      .put(START_COLUMN, 3)
      .put(END_COLUMN, 4)
      .build());
  }

  @Test
  void issue_and_multiple_flows_on_the_same_line() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser().parseIssue("// Noncompliant [[flows=id]] {{issue msg}} flow@id1,id2 {{flow msg12}} flow@id3 {{flow msg3}}",
      TEST_LINE);
    assertThat(iaf.issue).containsEntry(MESSAGE, "issue msg");
    assertThat(iaf.flows).hasSize(3);
    List<Integer> lines = iaf.flows.stream().map(f -> f.line).collect(Collectors.toList());
    assertThat(lines).isEqualTo(Arrays.asList(TEST_LINE, TEST_LINE, TEST_LINE));
    Map<String, Object> idMsgMap = iaf.flows.stream().collect(Collectors.toMap(f -> f.id, f -> MESSAGE.get(f.attributes)));
    assertThat(idMsgMap).isEqualTo(MapBuilder.<String, Object>newMap()
      .put("id1", "flow msg12")
      .put("id2", "flow msg12")
      .put("id3", "flow msg3")
      .build());
  }

  @Nested
  class QuickFixes {
    private Expectations.Parser parser;
    private Expectations expectations;

    @BeforeEach
    void setup() {
      expectations = new Expectations();
      expectations.setCollectQuickFixes();
      parser = expectations.parser();
    }

    @Test
    void quickFix_id_is_part_of_usual_atributes() {
      Expectations.Parser.ParsedComment parsedComment = parser.parseIssue("// Noncompliant [[sc=2;ec=4;quickfixes=qf1]]", TEST_LINE);
      assertThat(parsedComment.issue).containsEntry(QUICK_FIXES, Collections.singletonList("qf1"));
    }

    @Test
    void there_can_be_multiple_quickfixes() {
      Expectations.Parser.ParsedComment parsedComment = parser.parseIssue("// Noncompliant [[sc=2;ec=4;quickfixes=qf1,abc,myFix]]", TEST_LINE);
      assertThat(parsedComment.issue).containsEntry(QUICK_FIXES, Arrays.asList("qf1", "abc", "myFix"));
    }

    @Test
    void quick_fix_simple() {
      parser.parseIssue("// Noncompliant [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      // Actual line of the comment does not matter, the final value will be computed from the issue line.
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[sc=2;ec=4]] {{Do something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();

      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 42,9);

      assertThat(quickFixes).hasSize(1);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertJavaQuickFix(quickFix, "(42:1)-(42:3)");
    }


    @Test
    void quick_fix_relative_lines() {
      parser.parseIssue("// Noncompliant@+1 [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[sl=+1;sc=2;ec=4]] {{Do something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();

      TextSpan expectedTextSpanIssue = new TextSpan(43,4, 43,9);

      assertThat(quickFixes).hasSize(1);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertJavaQuickFix(quickFix, "(44:1)-(43:3)");
    }

    @Test
    void quick_fix_edit_with_absolute_lines() {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[sl=10;el=13;sc=2;ec=4]] {{Do something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();

      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 42,9);

      assertThat(quickFixes).hasSize(1);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertJavaQuickFix(quickFix, "(10:1)-(13:3)");
    }

    @Test
    void quick_fix_edit_with_relative_end_lines() {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[el=+2;sc=2;ec=4]] {{Do something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();

      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 42,9);

      assertThat(quickFixes).hasSize(1);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertJavaQuickFix(quickFix, "(42:1)-(44:3)");
    }

    @Test
    void quick_fix_edit_with_relative_start_lines() {
      parser.parseIssue("// Noncompliant [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[sl=+2;sc=2;ec=4]] {{Do something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();

      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 42,9);

      assertThat(quickFixes).hasSize(1);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertJavaQuickFix(quickFix, "(44:1)-(42:3)");
    }

    @Test
    void quick_fix_edit_with_relative_end_line_in_both() {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;el=+5;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[el=+2;sc=2;ec=4]] {{Do something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();

      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 47,9);

      assertThat(quickFixes).hasSize(1);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertJavaQuickFix(quickFix, "(42:1)-(44:3)");
    }

    private void assertJavaQuickFix(JavaQuickFix quickFix, String editTextSpan) {
      assertThat(quickFix.getDescription()).isEqualTo("message");
      List<JavaTextEdit> textEdits = quickFix.getTextEdits();
      assertThat(textEdits).hasSize(1);
      JavaTextEdit javaTextEdit = textEdits.get(0);

      assertThat(javaTextEdit.getReplacement()).isEqualTo("Do something");
      TextSpan textSpan = javaTextEdit.getTextSpan();

      // Issue is on line 43, then edit is on next line: 44.
      assertThat(textSpan).hasToString(editTextSpan);
    }

    @Test
    void quick_fix_edit_fails_without_end_delimiter() {
      parser.parseIssue("// Noncompliant@ [[sc=3;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{It goes like this:}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[sc=5;ec=10]] {{boom}", TEST_LINE + 2);
      assertThatThrownBy(() -> parser.consolidateQuickFixes())
        .isInstanceOf(AssertionError.class);
    }

    @Test
    void quick_fix_without_message() {
      parser.parseIssue("// Noncompliant@+1 [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      assertThatThrownBy(() -> parser.consolidateQuickFixes()).isInstanceOf(AssertionError.class)
        .hasMessage("Missing message for quick fix: qf1");
    }

    @Test
    void quick_fix_without_edits() {
      parser.parseIssue("// Noncompliant@+1 [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE);
      assertThatThrownBy(() -> parser.consolidateQuickFixes()).isInstanceOf(AssertionError.class)
        .hasMessage("Missing edits for quick fix: qf1");
    }

    @Test
    void quick_fix_edit_without_start_column() {
      assertEditCommentThrows("// edit@qf1 [[ec=4]] {{Do something}}",
        "start column not specified for quick fix edit at line 44.");
    }

    @Test
    void quick_fix_edit_without_end_column() {
      assertEditCommentThrows("// edit@qf1 [[sc=4]] {{Do something}}",
        "end column not specified for quick fix edit at line 44.");
    }

    @Test
    void quick_fix_unnecessary_edit() {
      assertEditCommentThrows("// edit@qf1 [[sc=2;ec=2]] {{}}",
        "Unnecessary edit for quick fix id qf1. TextEdits should not have empty range and text.");
    }

    @Test
    void quick_fix_unnecessary_edit_same_line() {
      assertEditCommentThrows("// edit@qf1 [[sc=2;ec=2;sl=2;el=2]] {{}}",
        "Unnecessary edit for quick fix id qf1. TextEdits should not have empty range and text.");
    }

    private void assertEditCommentThrows(String editComment, String expectedMessage) {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix(editComment, TEST_LINE + 2);
      assertThatThrownBy(() -> parser.consolidateQuickFixes()).isInstanceOf(AssertionError.class)
        .hasMessage(expectedMessage);
    }

    @Test
    void quick_fix_necessary_empty_edit() {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      // Empty edit with non-empty text range is a deletion.
      parser.parseQuickFix("// edit@qf1 [[sc=2;ec=3]] {{}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();
      assertThat(quickFixes).hasSize(1);
      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 42,9);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertThat(quickFix.getTextEdits()).hasSize(1);
    }

    @Test
    void quick_fix_necessary_empty_range() {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      // Empty edit with non-empty text range is an addition.
      parser.parseQuickFix("// edit@qf1 [[sc=2;ec=2]] {{something}}", TEST_LINE + 2);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();
      assertThat(quickFixes).hasSize(1);
      TextSpan expectedTextSpanIssue = new TextSpan(42,4, 42,9);
      JavaQuickFix quickFix = quickFixes.get(expectedTextSpanIssue).get(0);
      assertThat(quickFix.getTextEdits()).hasSize(1);
    }

    @Test
    void quick_fix_message_without_issue() {
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE);
      assertThatThrownBy(() -> parser.consolidateQuickFixes()).isInstanceOf(AssertionError.class)
        .hasMessage("Missing issue for quick fix id: qf1");
    }

    @Test
    void quick_fix_edit_without_issue() {
      parser.parseQuickFix("// edit@qf1 [[sc=2;ec=4]] {{Do something}}", TEST_LINE);
      assertThatThrownBy(() -> parser.consolidateQuickFixes()).isInstanceOf(AssertionError.class)
        .hasMessage("Missing issue for quick fix id: qf1");
    }

    @Test
    void quick_fix_edit_without_replacement() {
      parser.parseIssue("// Noncompliant@ [[sc=5;ec=10;quickfixes=qf1]]", TEST_LINE);
      parser.parseQuickFix("// fix@qf1 {{message}}", TEST_LINE + 1);
      parser.parseQuickFix("// edit@qf1 [[sc=2;ec=4]]", TEST_LINE);
      assertThatThrownBy(() -> parser.consolidateQuickFixes()).isInstanceOf(AssertionError.class)
        .hasMessage("Quickfix edit should contain a replacement.");
    }

    @Test
    void quick_fix_without_start_column() {
      assertThatThrownBy(() -> parser.parseIssue("// Noncompliant@ [[ec=10;quickfixes=qf1]]", TEST_LINE)).isInstanceOf(AssertionError.class)
        .hasMessage("An issue with quick fixes must set the start column ([Line 42]).");
    }

    @Test
    void quick_fix_without_end_column() {
      assertThatThrownBy(() -> parser.parseIssue("// Noncompliant@ [[sc=10;quickfixes=qf1]]", TEST_LINE)).isInstanceOf(AssertionError.class)
        .hasMessage("An issue with quick fixes must set the end column ([Line 42]).");
    }

    @Test
    void unrelated_comment() {
      parser.parseQuickFix("// myMail@abc.com {{message}}", TEST_LINE);
      parser.consolidateQuickFixes();
      Map<TextSpan, List<JavaQuickFix>> quickFixes = expectations.quickFixes();
      assertThat(quickFixes).isEmpty();
    }
  }
}
