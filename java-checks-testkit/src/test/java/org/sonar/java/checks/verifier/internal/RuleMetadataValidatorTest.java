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
package org.sonar.java.checks.verifier.internal;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.check.Rule;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RuleMetadataValidatorTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"func\":\"Constant/Issue\",\"constantCost\":\"5min\"}",
    "{\"func\":\"Linear\",\"linearFactor\":\"2h\"}",
    "{\"func\":\"Linear with offset\",\"linearFactor\":\"1min\",\"linearOffset\":\"0min\"}"
  })
  void valid_remediation(String remediation) {
    assertThat(RuleMetadataValidator.readFunction(new StringReader("{\"remediation\":" + remediation + "}"), "Test"))
      .isIn("Constant/Issue", "Linear", "Linear with offset");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "null", "{}", "{\"func\":null}", "{\"func\":42}", "{\"func\":\"Unknown\"}",
    "{\"func\":\"Constant/Issue\"}",
    "{\"func\":\"Constant/Issue\",\"constantCost\":5}",
    "{\"func\":\"Constant/Issue\",\"constantCost\":\"-5min\"}",
    "{\"func\":\"Constant/Issue\",\"constantCost\":\"5minutes\"}",
    "{\"func\":\"Constant/Issue\",\"constantCost\":\"2147483648min\"}",
    "{\"func\":\"Constant/Issue\",\"constantCost\":\"5min\",\"linearFactor\":\"1min\"}",
    "{\"func\":\"Linear\",\"constantCost\":\"5min\"}",
    "{\"func\":\"Linear with offset\",\"linearFactor\":\"5min\"}",
    "{\"func\":\"Linear\",\"linearFactor\":\"5min\",\"linearDesc\":42}"
  })
  void invalid_remediation(String remediation) {
    assertThatThrownBy(() -> RuleMetadataValidator.readFunction(new StringReader("{\"remediation\":" + remediation + "}"), "Test"))
      .isInstanceOf(AssertionError.class).hasMessageStartingWith("Invalid remediation metadata for rule 'Test'");
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "", "[]", "42"})
  void invalid_metadata_is_not_a_missing_resource(String metadata) {
    assertThatThrownBy(() -> RuleMetadataValidator.readFunction(new StringReader(metadata), "Test"))
      .isInstanceOf(AssertionError.class).hasMessageContaining("expected a JSON object");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 1, 42})
  void linear_allows_explicit_gap_and_default_gap(int cost) {
    RuleMetadataValidator.validate(issue(new LinearCheck(), cost));
    RuleMetadataValidator.validate(issue(new OffsetCheck(), cost));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void constant_allows_costs_that_are_not_reported_as_gaps(int cost) {
    RuleMetadataValidator.validate(issue(new ConstantCheck(), cost));
  }

  @Test
  void rejects_gaps_without_remediation() {
    assertThatThrownBy(() -> RuleMetadataValidator.validate(issue(new NoRemediationCheck(), 42)))
      .isInstanceOf(AssertionError.class).hasMessageContaining("has no remediation function");
    RuleMetadataValidator.validate(issue(new NoRemediationCheck(), 0));
  }

  @Test
  void missing_metadata_and_annotation_remain_supported() {
    RuleMetadataValidator.validate(issue(new MissingMetadataCheck(), 42));
    RuleMetadataValidator.validate(issue(mock(JavaCheck.class), 42));
  }

  @Test
  void malformed_resource_fails_clearly() {
    assertThatThrownBy(() -> RuleMetadataValidator.validate(issue(new BrokenMetadataCheck(), 0)))
      .isInstanceOf(AssertionError.class).hasMessageContaining("Failed to read rule metadata for 'BrokenJSON'");
  }

  private static AnalyzerMessage issue(JavaCheck check, int cost) {
    return new AnalyzerMessage(check, mock(InputComponent.class), 1, "message", cost);
  }

  @Rule(key = "ConstantJSON")
  static class ConstantCheck implements JavaCheck { }

  @Rule(key = "LinearJSON")
  static class LinearCheck implements JavaCheck { }

  @Rule(key = "LinearOffsetJSON")
  static class OffsetCheck implements JavaCheck { }

  @Rule(key = "UndefinedRemediationFunc")
  static class NoRemediationCheck implements JavaCheck { }

  @Rule(key = "MissingMetadata")
  static class MissingMetadataCheck implements JavaCheck { }

  @Rule(key = "BrokenJSON")
  static class BrokenMetadataCheck implements JavaCheck { }
}
