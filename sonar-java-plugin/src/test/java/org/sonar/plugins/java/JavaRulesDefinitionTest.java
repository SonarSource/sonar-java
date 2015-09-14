/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.checks.CheckList;

import static org.fest.assertions.Assertions.assertThat;

public class JavaRulesDefinitionTest {

  @Test
  public void test() {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    assertThat(repository.name()).isEqualTo("SonarQube");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size());

    RulesDefinition.Rule unusedLabelRule = repository.rule("S1065");
    assertThat(unusedLabelRule).isNotNull();
    assertThat(unusedLabelRule.internalKey()).isEqualTo(unusedLabelRule.key());
    assertThat(unusedLabelRule.internalKey()).isEqualTo("S1065");
    assertThat(unusedLabelRule.name()).isEqualTo("Unused labels should be removed");
  }

}
