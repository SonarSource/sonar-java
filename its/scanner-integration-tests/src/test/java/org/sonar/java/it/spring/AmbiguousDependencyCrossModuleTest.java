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

import com.sonarsource.scanner.integrationtester.dsl.issue.TextRange;
import com.sonarsource.scanner.integrationtester.dsl.issue.TextRangeIssue;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.sonar.java.it.ScannerIntegrationAbstractTest;

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
          "Multiple beans match this dependency (defaultFeatureToggleService, prodFeatureToggleService); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".",
          new TextRange(16, 16, 33, 53)),
        new TextRangeIssue(
          "app/src/main/java/com/example/app/AuditLoggerConsumer.java",
          "java:S9352",
          "Multiple beans match this dependency (defaultAuditLogger, stagingAuditLogger); disambiguate it with \"@Qualifier\" or mark one bean as \"@Primary\".",
          new TextRange(16, 16, 24, 35)));
  }
}
