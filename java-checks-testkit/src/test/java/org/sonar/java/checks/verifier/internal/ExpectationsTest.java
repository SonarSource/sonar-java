/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.sonar.java.collections.MapBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.LINE;
import static org.sonar.java.checks.verifier.internal.Expectations.IssueAttribute.MESSAGE;
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
    assertThat(issue.get(MESSAGE)).isEqualTo("message");
    assertThat(issue.get(START_COLUMN)).isEqualTo(1);
    assertThat(issue.get(END_COLUMN)).isEqualTo(6);
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
    assertThat(iaf.issue.get(Expectations.IssueAttribute.LINE)).isEqualTo(TEST_LINE);
    assertThat((List) iaf.issue.get(FLOWS)).contains("id");
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.id).isEqualTo("id");
    assertThat(flow.line).isEqualTo(TEST_LINE);
  }

  @Test
  void issue_and_flow_message() {
    Expectations.Parser.ParsedComment iaf = new Expectations().parser().parseIssue("// Noncompliant {{issue msg}} flow@id {{flow msg}}", TEST_LINE);
    assertThat(iaf.issue.get(MESSAGE)).isEqualTo("issue msg");
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
    assertThat(iaf.issue.get(START_COLUMN)).isEqualTo(1);
    assertThat(iaf.issue.get(END_COLUMN)).isEqualTo(2);
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
    assertThat(iaf.issue.get(MESSAGE)).isEqualTo("issue msg");
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
}
