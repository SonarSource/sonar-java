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
package org.sonar.java.checks.verifier;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;

class TestProfileRegistrarContextTest {

  @Test
  void store_registration() {
    TestProfileRegistrarContext context = new TestProfileRegistrarContext();
    context.registerDefaultQualityProfileRules(List.of(RuleKey.of("java", "S1234")));
    context.registerRules("Sonar way", List.of(RuleKey.of("java", "S1235")));
    context.registerRules("Sonar agentic AI", List.of(RuleKey.of("java", "S4321")));
    assertThat(context.rulesByQualityProfile.get("Sonar way")).containsOnly(RuleKey.of("java", "S1234"), RuleKey.of("java", "S1235"));
    assertThat(context.rulesByQualityProfile.get("Sonar agentic AI")).containsExactly(RuleKey.of("java", "S4321"));
  }

}
