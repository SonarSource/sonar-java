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
package org.sonar.java.it.spring;

import com.google.gson.JsonParser;
import com.sonarsource.scanner.integrationtester.dsl.issue.FileIssue;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRange;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import org.junit.jupiter.api.Test;
import org.sonar.java.it.ScannerIntegrationAbstractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AmbiguousDependencyCrossModuleTest extends ScannerIntegrationAbstractTest {

  @Test
  void test() {
    var issues = analyze(Path.of("ambiguous-dependencies-should-be-resolved"), "S9352");
    assertThat(issues)
      .hasSize(4)
      .contains(new TextRangeIssue(
        "app/src/main/java/com/example/app/PaymentConsumer.java",
        "java:S9352",
        "Multiple beans match this dependency (creditCardPaymentGateway, digitalWalletPaymentGateway); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".",
        new TextRange(15, 15, 27, 41)),
        new TextRangeIssue(
          "app/src/main/java/com/example/app/InventoryConsumer.java",
          "java:S9352",
          "Multiple beans match this dependency (storeInventoryService, warehouseInventoryService); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".",
          new TextRange(12, 12, 29, 45)),
        new TextRangeIssue(
          "app/src/main/java/com/example/app/FeatureToggleConsumer.java",
          "java:S9352",
          "Multiple beans match this dependency (defaultFeatureToggleService, prodFeatureToggleService) in profile 'prod'; disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".",
          new TextRange(16, 16, 33, 53)),
        new TextRangeIssue(
          "app/src/main/java/com/example/app/AuditLoggerConsumer.java",
          "java:S9352",
          "Multiple beans match this dependency (defaultAuditLogger, stagingAuditLogger) in profile 'staging'; disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".",
          new TextRange(16, 16, 24, 35)));
  }

  @Test
  void spring_context_model_json_is_created() throws IOException {
    analyze(Path.of("ambiguous-dependencies-should-be-resolved"), "S9352");

    var modelPath = projectBaseDir.resolve(".sonar/spring-context-model.json");
    assertThat(modelPath).isRegularFile().isNotEmptyFile();

    var model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
    var filesData = model.getAsJsonObject("filesData");
    var expectedBeans = Map.of(
      "app", "paymentConsumer",
      "module-a", "creditCardPaymentGateway",
      "module-b", "digitalWalletPaymentGateway");

    assertThat(filesData.keySet()).containsAll(expectedBeans.keySet());
    expectedBeans.forEach((moduleKey, beanName) ->
      assertThat(filesData.getAsJsonObject(moduleKey).asMap().values())
        .as("Beans persisted for module %s", moduleKey)
        .flatExtracting(file -> file.getAsJsonObject().getAsJsonArray("beans").asList())
        .extracting(bean -> bean.getAsJsonObject().get("name").getAsString())
        .contains(beanName));
  }

  /**
   * {@code app/src/test/java} declares a third PaymentGateway bean inside a nested {@code @Configuration} of a JUnit
   * test class. Spring Boot's TestTypeExcludeFilter keeps it out of component scanning, so it must not be collected:
   * were it, PaymentConsumer's issue above would name three candidates instead of two.
   */
  @Test
  void beans_declared_in_test_scope_are_not_candidates() {
    var issues = analyze(Path.of("ambiguous-dependencies-should-be-resolved"), "S9352");
    assertThat(issues)
      .extracting(FileIssue::message)
      .noneMatch(message -> message.contains("testPaymentGateway"));
  }
}
