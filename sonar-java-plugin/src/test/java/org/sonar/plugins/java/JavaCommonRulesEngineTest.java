/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java;

import org.junit.Test;
import org.sonar.commonrules.api.CommonRulesRepository;

import static org.fest.assertions.Assertions.assertThat;

public class JavaCommonRulesEngineTest {

  @Test
  public void provide_extensions() {
    JavaCommonRulesEngine engine = new JavaCommonRulesEngine();
    assertThat(engine.provide()).isNotEmpty();
  }

  @Test
  public void enable_common_rules() {
    JavaCommonRulesEngine provider = new JavaCommonRulesEngine();
    CommonRulesRepository repo = provider.newRepository();
    assertThat(repo.rules()).hasSize(6);
    assertThat(repo.rule(CommonRulesRepository.RULE_INSUFFICIENT_COMMENT_DENSITY)).isNotNull();
    assertThat(repo.rule(CommonRulesRepository.RULE_FAILED_UNIT_TESTS)).isNotNull();
    assertThat(repo.rule(CommonRulesRepository.RULE_INSUFFICIENT_COMMENT_DENSITY)).isNotNull();
    assertThat(repo.rule(CommonRulesRepository.RULE_INSUFFICIENT_BRANCH_COVERAGE)).isNotNull();
    assertThat(repo.rule(CommonRulesRepository.RULE_INSUFFICIENT_LINE_COVERAGE)).isNotNull();
    assertThat(repo.rule(CommonRulesRepository.RULE_SKIPPED_UNIT_TESTS)).isNotNull();
  }
}


