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
package org.sonar.java.externalreport;

import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.AbstractAssert;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;

import static org.assertj.core.api.Assertions.fail;

public class ExternalIssueAssert extends AbstractAssert<ExternalIssueAssert, ExternalIssueAssert.ExternalIssueData> {
  static ExternalIssueAssert assertThat(ExternalIssue issue) {
    return new ExternalIssueAssert(convertToData(issue));
  }

  private final List<String> errorMessages = new ArrayList<>();

  public ExternalIssueAssert(ExternalIssueData actual) {
    super(actual, ExternalIssueAssert.class);
  }
  public static ExternalIssueData convertToData(ExternalIssue issue) {
    ExternalIssueData data = new ExternalIssueData();
    data.fileName = issue.primaryLocation().inputComponent().key();
    data.engineId = issue.engineId();
    data.ruleId = issue.ruleId();
    data.ruleKey = issue.ruleKey().rule();
    data.ruleType = issue.type();
    data.severity = issue.severity();
    data.message = issue.primaryLocation().message();
    data.textRange = issue.primaryLocation().textRange();
    data.textRangeStartLine = issue.primaryLocation().textRange().start().line();
    data.remediationEffort = issue.remediationEffort();
    return data;
  }

  public ExternalIssueAssert hasFileName(String fileName) {
    if (!fileName.equals(actual.fileName)) {
      errorMessages.add("Unexpected fileName. Expected: " + fileName + ", but was: " + actual.fileName + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasEngineId(String engineId) {
    if (!engineId.equals(actual.engineId)) {
      errorMessages.add("Unexpected engineId. Expected: " + engineId + ", but was: " + actual.engineId + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasRuleId(String ruleId) {
    if (!ruleId.equals(actual.ruleId)) {
      errorMessages.add("Unexpected ruleId. Expected: " + ruleId + ", but was: " + actual.ruleId + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasRuleKey(String ruleKey) {
    if (!ruleKey.equals(actual.ruleKey)) {
      errorMessages.add("Unexpected ruleKey. Expected: " + ruleKey + ", but was: " + actual.ruleKey + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasRuleType(RuleType ruleType) {
    if (!ruleType.equals(actual.ruleType)) {
      errorMessages.add("Unexpected ruleType. Expected: " + ruleType + ", but was: " + actual.ruleType + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasSeverity(Severity severity) {
    if (!severity.equals(actual.severity)) {
      errorMessages.add("Unexpected severity. Expected: " + severity + ", but was: " + actual.severity + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasMessage(String message) {
    if (!message.equals(actual.message)) {
      errorMessages.add("Unexpected message. Expected: " + message + ", but was: " + actual.message + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasTextRange(TextRange textRange) {
    if (!textRange.equals(actual.textRange)) {
      errorMessages.add("Unexpected textRange. Expected: " + textRange + ", but was: " + actual.textRange + ".");
    }
    return this;
  }

  public ExternalIssueAssert hasTextRangeStartLine(int textRangeStartLine) {
    if (!(textRangeStartLine == actual.textRangeStartLine)) {
      errorMessages.add(String.format("Unexpected textRangeStartLine. Expected: %d, but was: %d.", textRangeStartLine, actual.textRangeStartLine));
    }
    return this;
  }

  public ExternalIssueAssert hasRemediationEffort(long remediationEffort) {
    if (!(remediationEffort == actual.remediationEffort)) {
      errorMessages.add(String.format("Unexpected remediationEffort. Expected: %d, but was: %d.", remediationEffort, actual.remediationEffort));
    }
    return this;
  }

  public void verify() {
    if (!errorMessages.isEmpty()) {
      fail(String.join("\n", errorMessages));
    }
  }

  static class ExternalIssueData {
    private String fileName;
    private String engineId;
    private String ruleId;
    private String ruleKey;
    private RuleType ruleType;
    private Severity severity;
    private String message;
    private TextRange textRange;
    private int textRangeStartLine;
    private Long remediationEffort;
  }
}
