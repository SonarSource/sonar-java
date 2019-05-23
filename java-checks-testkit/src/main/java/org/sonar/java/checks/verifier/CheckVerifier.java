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
package org.sonar.java.checks.verifier;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.RspecKey;
import org.sonar.java.SonarComponents;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class CheckVerifier {
  private static final Logger LOG = Loggers.get(CheckVerifier.class);

  public static final String ISSUE_MARKER = "Noncompliant";

  public static final Map<String, IssueAttribute> ATTRIBUTE_MAP = ImmutableMap.<String, IssueAttribute>builder()
    .put("message", IssueAttribute.MESSAGE)
    .put("effortToFix", IssueAttribute.EFFORT_TO_FIX)
    .put("sc", IssueAttribute.START_COLUMN)
    .put("startColumn", IssueAttribute.START_COLUMN)
    .put("el", IssueAttribute.END_LINE)
    .put("endLine", IssueAttribute.END_LINE)
    .put("ec", IssueAttribute.END_COLUMN)
    .put("endColumn", IssueAttribute.END_COLUMN)
    .put("secondary", IssueAttribute.SECONDARY_LOCATIONS)
    .build();

  public enum IssueAttribute {
    MESSAGE,
    START_COLUMN,
    END_COLUMN,
    END_LINE,
    EFFORT_TO_FIX,
    SECONDARY_LOCATIONS
  }

  private final ArrayListMultimap<Integer, Map<IssueAttribute, String>> expected = ArrayListMultimap.create();
  private boolean expectNoIssues = false;
  private String expectFileIssue;
  private String expectedProjectIssue;

  public void expectNoIssues() {
    this.expectNoIssues = true;
  }

  public void setExpectedFileIssue(String expectFileIssue) {
    this.expectFileIssue = expectFileIssue;
  }

  public void setExpectedProjectIssue(String expectedProjectIssue) {
    this.expectedProjectIssue = expectedProjectIssue;
  }

  public abstract String getExpectedIssueTrigger();

  protected void collectExpectedIssues(String comment, int line) {
    String expectedStart = getExpectedIssueTrigger();
    if (comment.startsWith(expectedStart)) {
      String cleanedComment = StringUtils.remove(comment, expectedStart);

      EnumMap<IssueAttribute, String> attr = new EnumMap<>(IssueAttribute.class);
      String expectedMessage = StringUtils.substringBetween(cleanedComment, "{{", "}}");
      if (StringUtils.isNotEmpty(expectedMessage)) {
        attr.put(IssueAttribute.MESSAGE, expectedMessage);
      }
      int expectedLine = line;
      String attributesSubstr = extractAttributes(comment, attr);

      cleanedComment = StringUtils.stripEnd(StringUtils.remove(StringUtils.remove(cleanedComment, "[[" + attributesSubstr + "]]"), "{{" + expectedMessage + "}}"), " \t");
      if (StringUtils.startsWith(cleanedComment, "@")) {
        final int lineAdjustment;
        final char firstChar = cleanedComment.charAt(1);
        final int endIndex = cleanedComment.indexOf(' ');
        if (endIndex == -1) {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2));
        } else {
          lineAdjustment = Integer.parseInt(cleanedComment.substring(2, endIndex));
        }
        if (firstChar == '+') {
          expectedLine += lineAdjustment;
        } else if (firstChar == '-') {
          expectedLine -= lineAdjustment;
        } else {
          Fail.fail("Use only '@+N' or '@-N' to shifts messages.");
        }
      }
      updateEndLine(expectedLine, attr);
      expected.put(expectedLine, attr);
    }
  }

  private static String extractAttributes(String comment, Map<IssueAttribute, String> attr) {
    String attributesSubstr = StringUtils.substringBetween(comment, "[[", "]]");
    if (!StringUtils.isEmpty(attributesSubstr)) {
      Iterable<String> attributes = Splitter.on(";").split(attributesSubstr);
      for (String attribute : attributes) {
        String[] split = StringUtils.split(attribute, '=');
        if (split.length == 2 && CheckVerifier.ATTRIBUTE_MAP.containsKey(split[0])) {
          attr.put(CheckVerifier.ATTRIBUTE_MAP.get(split[0]), split[1]);
        } else {
          Fail.fail("// Noncompliant attributes not valid: " + attributesSubstr);
        }
      }
    }
    return attributesSubstr;
  }

  private static void updateEndLine(int expectedLine, EnumMap<IssueAttribute, String> attr) {
    if (attr.containsKey(IssueAttribute.END_LINE)) {
      String endLineStr = attr.get(IssueAttribute.END_LINE);
      if (endLineStr.charAt(0) == '+') {
        int endLine = Integer.parseInt(endLineStr);
        attr.put(IssueAttribute.END_LINE, Integer.toString(expectedLine + endLine));
      } else {
        Fail.fail("endLine attribute should be relative to the line and must be +N with N integer");
      }
    }
  }

  public ArrayListMultimap<Integer, Map<IssueAttribute, String>> getExpected() {
    return expected;
  }

  public void checkIssues(Set<AnalyzerMessage> issues, boolean bypassNoIssue) {
    if (expectNoIssues) {
      assertNoIssues(issues, bypassNoIssue);
    } else if (StringUtils.isNotEmpty(expectFileIssue)) {
      assertSingleIssue(issues, true, expectFileIssue);
    } else if (StringUtils.isNotEmpty(expectedProjectIssue)) {
      assertSingleIssue(issues, false, expectedProjectIssue);
    } else {
      assertMultipleIssue(issues);
    }
  }

  static SonarComponents sonarComponents(InputFile inputFile) {
    SensorContextTester context = SensorContextTester.create(new File("")).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    context.setSettings(new MapSettings().setProperty("sonar.java.failOnException", true));
    SonarComponents sonarComponents = new SonarComponents(null, context.fileSystem(), null, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        return false;
      }
    };
    sonarComponents.setSensorContext(context);
    context.fileSystem().add(inputFile);
    return sonarComponents;
  }

  private void assertMultipleIssue(Set<AnalyzerMessage> issues) throws AssertionError {
    Preconditions.checkState(!issues.isEmpty(), "At least one issue expected");
    List<Integer> unexpectedLines = new LinkedList<>();
    RemediationFunction remediationFunction = remediationFunction(issues.iterator().next());
    for (AnalyzerMessage issue : issues) {
      validateIssue(expected, unexpectedLines, issue, remediationFunction);
    }
    if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
      Collections.sort(unexpectedLines);
      String expectedMsg = expectedMessage();
      String unexpectedMsg = unexpectedMessage(unexpectedLines);
      Fail.fail(expectedMsg + unexpectedMsg);
    }
  }

  private String expectedMessage() {
    return !expected.isEmpty() ? ("Expected " + expected) : "";
  }

  private String unexpectedMessage(List<Integer> unexpectedLines) {
    if (unexpectedLines.isEmpty()) {
      return "";
    }
    return (expected.isEmpty() ? "" : ", ") + "Unexpected at " + unexpectedLines;
  }

  enum RemediationFunction {
    LINEAR, CONST
  }

  static class RuleJSON {
    static class Remediation {
      String func;
    }
    Remediation remediation;
  }

  @CheckForNull
  private static RemediationFunction remediationFunction(AnalyzerMessage issue) {
    String ruleKey = ruleKey(issue);
    try {
      RuleJSON rule = getRuleJSON(ruleKey);
      if (rule.remediation == null) {
        return null;
      }
      switch (rule.remediation.func) {
        case "Linear":
          return RemediationFunction.LINEAR;
        case "Constant/Issue":
          return RemediationFunction.CONST;
        default:
          return null;
      }
    } catch (IOException | JsonParseException e) {
      // Failed to open json file, as this is not part of API yet, we should not fail because of this
      LOG.debug("Remediation function and cost not provided, \"constant\" is assumed.");
      return null;
    }
  }

  private static RuleJSON getRuleJSON(String ruleKey) throws IOException {
    String ruleJson = "/org/sonar/l10n/java/rules/squid/" + ruleKey + "_java.json";
    URL resource = CheckVerifier.class.getResource(ruleJson);
    if(resource == null) {
      throw new IOException(ruleJson + " not found");
    }
    Gson gson = new Gson();
    return gson.fromJson(new InputStreamReader(resource.openStream(), "UTF-8"), RuleJSON.class);
  }

  private static String ruleKey(AnalyzerMessage issue) {
    String ruleKey;
    RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(issue.getCheck().getClass(), RspecKey.class);
    if(rspecKeyAnnotation != null) {
      ruleKey = rspecKeyAnnotation.value();
    } else {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(issue.getCheck().getClass(), Rule.class);
      if (ruleAnnotation != null) {
        ruleKey = ruleAnnotation.key();
      } else {
        Fail.fail("Rules should be annotated with '@Rule(key = \"...\")' annotation (org.sonar.check.Rule).");
        // unreachable
        return null;
      }
    }
    return ruleKey;
  }

  private static void validateIssue(Multimap<Integer, Map<IssueAttribute, String>> expected, List<Integer> unexpectedLines, AnalyzerMessage issue,
    @Nullable RemediationFunction remediationFunction) {
    int line = issue.getLine();
    if (expected.containsKey(line)) {
      Map<IssueAttribute, String> attrs = Iterables.getLast(expected.get(line));
      assertEquals(line, issue.getMessage(), attrs, IssueAttribute.MESSAGE);
      Double cost = issue.getCost();
      if (cost != null) {
        Preconditions.checkState(remediationFunction != RemediationFunction.CONST, "Rule with constant remediation function shall not provide cost");
        assertEquals(line, Integer.toString(cost.intValue()), attrs, IssueAttribute.EFFORT_TO_FIX);
      } else if(remediationFunction == RemediationFunction.LINEAR){
        Fail.fail("A cost should be provided for a rule with linear remediation function");
      }
      validateAnalyzerMessage(line, attrs, issue);
      expected.remove(line, attrs);
    } else {
      unexpectedLines.add(line);
    }
  }

  private static void validateAnalyzerMessage(int line, Map<IssueAttribute, String> attrs, AnalyzerMessage analyzerMessage) {
    Double effortToFix = analyzerMessage.getCost();
    if (effortToFix != null) {
      assertEquals(line, Integer.toString(effortToFix.intValue()), attrs, IssueAttribute.EFFORT_TO_FIX);
    }
    AnalyzerMessage.TextSpan textSpan = analyzerMessage.primaryLocation();
    assertEquals(line, normalizeColumn(textSpan.startCharacter), attrs, IssueAttribute.START_COLUMN);
    assertEquals(line, Integer.toString(textSpan.endLine), attrs, IssueAttribute.END_LINE);
    assertEquals(line, normalizeColumn(textSpan.endCharacter), attrs, IssueAttribute.END_COLUMN);
    if (attrs.containsKey(IssueAttribute.SECONDARY_LOCATIONS)) {
      List<AnalyzerMessage> secondaryLocations = analyzerMessage.flows.stream().map(l -> l.get(0)).collect(Collectors.toList());
      Multiset<String> actualLines = HashMultiset.create();
      for (AnalyzerMessage secondaryLocation : secondaryLocations) {
        actualLines.add(Integer.toString(secondaryLocation.getLine()));
      }
      List<String> expected = Lists.newArrayList(Splitter.on(",").omitEmptyStrings().trimResults().split(attrs.get(IssueAttribute.SECONDARY_LOCATIONS)));
      List<String> unexpected = new ArrayList<>();
      for (String actualLine : actualLines) {
        if (expected.contains(actualLine)) {
          expected.remove(actualLine);
        } else {
          unexpected.add(actualLine);
        }
      }
      if (!expected.isEmpty() || !unexpected.isEmpty()) {
        // Line is not covered by JaCoCo because of thrown exception but effectively covered in UT.
        Fail.fail(String.format("Secondary locations: expected: %s unexpected:%s. In %s:%d", expected, unexpected, normalizedFilePath(analyzerMessage), analyzerMessage.getLine()));
      }
    }
  }

  private static String normalizedFilePath(AnalyzerMessage analyzerMessage) {
    String absolutePath = analyzerMessage.getInputComponent().toString();
    return absolutePath.replace("\\", "/");
  }

  private static String normalizeColumn(int startCharacter) {
    return Integer.toString(startCharacter + 1);
  }

  private static void assertEquals(int line, String value, Map<IssueAttribute, String> attributes, IssueAttribute attribute) {
    if (attributes.containsKey(attribute)) {
      assertThat(value).as("Line: " + line + " attribute mismatch for " + attribute + ": " + attributes).isEqualTo(attributes.get(attribute));
    }
  }

  private static void assertSingleIssue(Set<AnalyzerMessage> issues, boolean issueOnFile, String expectedMessage) {
    Preconditions.checkState(issues.size() == 1, "A single issue is expected on the file");
    AnalyzerMessage issue = Iterables.getFirst(issues, null);
    assertThat(issue.getInputComponent().isFile()).isEqualTo(issueOnFile);
    assertThat(issue.getLine()).isNull();
    assertThat(issue.getMessage()).isEqualTo(expectedMessage);
  }

  private void assertNoIssues(Set<AnalyzerMessage> issues, boolean bypass) {
    assertThat(issues).overridingErrorMessage("No issues expected but got: " + issues).isEmpty();
    if (!bypass) {
      // make sure we do not copy&paste verifyNoIssue call when we intend to call verify
      assertThat(expected.isEmpty()).overridingErrorMessage("The file should not declare noncompliants when no issues are expected").isTrue();
    }
  }
}
