/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Fail;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.Expectations.IssueAttribute.END_COLUMN;
import static org.sonar.java.se.Expectations.IssueAttribute.END_LINE;
import static org.sonar.java.se.Expectations.IssueAttribute.FLOWS;
import static org.sonar.java.se.Expectations.IssueAttribute.MESSAGE;
import static org.sonar.java.se.Expectations.IssueAttribute.SECONDARY_LOCATIONS;
import static org.sonar.java.se.Expectations.IssueAttribute.START_COLUMN;

public class ExpectationsParserTest {

  private static final int LINE = 42;

  @Test
  public void issue_without_details() {
    Map<Expectations.IssueAttribute, Object> issue = Expectations.Parser.parseIssue("// Noncompliant", LINE).issue;
    assertThat(issue.get(Expectations.IssueAttribute.LINE)).isEqualTo(LINE);
  }

  @Test
  public void issue_with_message() {
    Map<Expectations.IssueAttribute, Object> issue = Expectations.Parser.parseIssue("// Noncompliant {{message}}", LINE).issue;
    assertThat(issue.get(MESSAGE)).isEqualTo("message");
  }

  @Test
  public void issue_with_attributes() {
    Map<Expectations.IssueAttribute, Object> issue = Expectations.Parser.parseIssue("// Noncompliant [[flows=f1,f2,f3;sc=3;ec=7;el=4;secondary=5]]", LINE).issue;
    assertThat(issue.get(FLOWS)).isEqualTo(ImmutableList.of("f1", "f2", "f3"));
    assertThat(issue.get(START_COLUMN)).isEqualTo(3);
    assertThat(issue.get(END_COLUMN)).isEqualTo(7);
    assertThat(issue.get(END_LINE)).isEqualTo(Expectations.Parser.LineRef.fromString("4"));
    assertThat(issue.get(SECONDARY_LOCATIONS)).isEqualTo(Collections.singletonList(5));
    assertThat(issue.get(MESSAGE)).isNull();
  }

  @Test
  public void relative_end_line_attribute() {
    Map<Expectations.IssueAttribute, Object> issue = Expectations.Parser.parseIssue("// Noncompliant [[el=+1]]", LINE).issue;
    assertThat(issue.get(END_LINE)).isEqualTo(Expectations.Parser.LineRef.fromString(String.valueOf(LINE + 1)));
  }

  @Test
  public void invalid_attribute_name() {
    try {
      Expectations.Parser.parseIssue("// Noncompliant [[invalid]]", LINE);
      Fail.fail("exception expected");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("// Noncompliant attributes not valid: invalid");
    }
  }

  @Test
  public void issue_with_attributes_and_comment() {
    Map<Expectations.IssueAttribute, Object> issue = Expectations.Parser.parseIssue("// Noncompliant [[sc=1;ec=6]] {{message}}", LINE).issue;
    assertThat(issue.get(MESSAGE)).isEqualTo("message");
    assertThat(issue.get(START_COLUMN)).isEqualTo(1);
    assertThat(issue.get(END_COLUMN)).isEqualTo(6);
  }

  @Test
  public void issue_with_attributes_and_comment_switched() {
    Expectations.Issue issue = Expectations.Parser.parseIssue("// Noncompliant {{message}} [[sc=1;ec=6]]", LINE).issue;
    assertThat(issue.get(MESSAGE)).isEqualTo("message");
    assertThat(issue.get(START_COLUMN)).isEqualTo(1);
    assertThat(issue.get(END_COLUMN)).isEqualTo(6);
  }

  @Test
  public void end_line_attribute() {
    try {
      Expectations.Parser.parseIssue("// Noncompliant [[endLine=-1]] {{message}}", 0);
      Fail.fail("exception expected");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("endLine attribute should be relative to the line and must be +N with N integer");
    }
  }

  @Test
  public void line_shifting() {
    Map<Expectations.IssueAttribute, Object> issue = Expectations.Parser.parseIssue("// Noncompliant@-1", LINE).issue;
    assertThat(issue.get(Expectations.IssueAttribute.LINE)).isEqualTo(41);
  }

  @Test
  public void no_flows() {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows(null, 0);
    assertThat(flows).isEmpty();
  }

  @Test
  public void flow_without_details() {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows("// flow@npe1", LINE);
    assertThat(flows).hasSize(1);
    Expectations.FlowComment flow = flows.iterator().next();
    assertThat(flow.id).isEqualTo("npe1");
    assertThat(flow.line).isEqualTo(LINE);
  }

  @Test
  public void flow_with_message() throws Exception {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows("// flow@npe1 {{message}}", LINE);
    assertThat(flows).hasSize(1);
    Expectations.FlowComment flow = flows.iterator().next();
    assertThat(flow.id).isEqualTo("npe1");
    assertThat(flow.line).isEqualTo(LINE);
    assertThat(flow.attributes.get(MESSAGE)).isEqualTo("message");
  }

  @Test
  public void flow_with_attributes_and_message() throws Exception {
    List<Expectations.FlowComment> flows = Expectations.Parser.parseFlows("// flow@npe1 [[sc=1;ec=6]] {{message}}", LINE);
    assertThat(flows).hasSize(1);
    Expectations.FlowComment flow = flows.iterator().next();
    assertThat(flow.id).isEqualTo("npe1");
    assertThat(flow.line).isEqualTo(LINE);
    assertThat(flow.attributes.get(START_COLUMN)).isEqualTo(1);
    assertThat(flow.attributes.get(END_COLUMN)).isEqualTo(6);
    assertThat(flow.attributes.get(MESSAGE)).isEqualTo("message");
  }

  @Test
  public void issue_and_flow_on_the_same_line() {
    Expectations.Parser.ParsedComment iaf = Expectations.Parser.parseIssue("// Noncompliant [[flows=id]] flow@id", LINE);
    assertThat(iaf.issue.get(Expectations.IssueAttribute.LINE)).isEqualTo(LINE);
    assertThat((List) iaf.issue.get(FLOWS)).contains("id");
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.id).isEqualTo("id");
    assertThat(flow.line).isEqualTo(LINE);
  }

  @Test
  public void issue_and_flow_message() {
    Expectations.Parser.ParsedComment iaf = Expectations.Parser.parseIssue("// Noncompliant {{issue msg}} flow@id {{flow msg}}", LINE);
    assertThat(iaf.issue.get(Expectations.IssueAttribute.MESSAGE)).isEqualTo("issue msg");
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.attributes.get(MESSAGE)).isEqualTo("flow msg");
  }

  @Test
  public void issue_and_flow_message2() {
    Expectations.Parser.ParsedComment iaf = Expectations.Parser.parseIssue("// Noncompliant flow@id {{flow msg}}", LINE);
    assertThat(iaf.issue.get(Expectations.IssueAttribute.MESSAGE)).isNull();
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.attributes.get(MESSAGE)).isEqualTo("flow msg");
  }

  @Test
  public void issue_and_flow_attr() {
    Expectations.Parser.ParsedComment iaf = Expectations.Parser.parseIssue("// Noncompliant [[sc=1;ec=2]] bla bla flow@id [[sc=3;ec=4]] bla bla", LINE);
    assertThat(iaf.issue.get(Expectations.IssueAttribute.START_COLUMN)).isEqualTo(1);
    assertThat(iaf.issue.get(Expectations.IssueAttribute.END_COLUMN)).isEqualTo(2);
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.attributes.get(START_COLUMN)).isEqualTo(3);
    assertThat(flow.attributes.get(END_COLUMN)).isEqualTo(4);
  }

  @Test
  public void issue_and_flow_all_options() {
    Expectations.Parser.ParsedComment iaf = Expectations.Parser
      .parseIssue("// Noncompliant [[flows;sc=1;ec=2]] bla {{issue msg}} bla flow@id [[sc=3;ec=4]] bla {{flow msg}} bla", LINE);
    assertThat(iaf.issue).isEqualTo(ImmutableMap.of(
      Expectations.IssueAttribute.LINE, 42,
      MESSAGE, "issue msg",
      START_COLUMN, 1,
      END_COLUMN, 2,
      FLOWS, Collections.emptyList()
    ));
    assertThat(iaf.flows).hasSize(1);
    Expectations.FlowComment flow = iaf.flows.iterator().next();
    assertThat(flow.line).isEqualTo(LINE);
    assertThat(flow.attributes).isEqualTo(ImmutableMap.of(
      MESSAGE, "flow msg",
      START_COLUMN, 3,
      END_COLUMN, 4
    ));
  }

  @Test
  public void issue_and_multiple_flows_on_the_same_line() {
    Expectations.Parser.ParsedComment iaf = Expectations.Parser.parseIssue("// Noncompliant [[flows=id]] {{issue msg}} flow@id1,id2 {{flow msg12}} flow@id3 {{flow msg3}}", LINE);
    assertThat(iaf.issue.get(Expectations.IssueAttribute.MESSAGE)).isEqualTo("issue msg");
    assertThat(iaf.flows).hasSize(3);
    List<Integer> lines = iaf.flows.stream().map(f -> f.line).collect(Collectors.toList());
    assertThat(lines).isEqualTo(ImmutableList.of(LINE, LINE, LINE));
    Map<String, Object> idMsgMap = iaf.flows.stream().collect(Collectors.toMap(f -> f.id, f -> MESSAGE.get(f.attributes)));
    assertThat(idMsgMap).isEqualTo(ImmutableMap.of(
      "id1", "flow msg12",
      "id2", "flow msg12",
      "id3", "flow msg3"
    ));
  }
}
