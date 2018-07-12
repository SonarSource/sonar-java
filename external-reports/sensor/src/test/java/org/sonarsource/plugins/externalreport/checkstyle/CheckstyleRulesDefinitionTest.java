/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonarsource.plugins.externalreport.checkstyle;

import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckstyleRulesDefinitionTest {

  @Test
  public void external_repositories_not_supported() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    CheckstyleRulesDefinition rulesDefinition = new CheckstyleRulesDefinition(false);
    rulesDefinition.define(context);
    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void checkstyle_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    CheckstyleRulesDefinition rulesDefinition = new CheckstyleRulesDefinition(true);
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository("external_checkstyle");
    assertThat(repository.name()).isEqualTo("Checkstyle");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.isExternal()).isEqualTo(true);

    assertThat(repository.rules().size()).isEqualTo(155);

    RulesDefinition.Rule classNaming = repository.rule("ArrayTypeStyleCheck");
    assertThat(classNaming).isNotNull();
    assertThat(classNaming.name()).isEqualTo("Array Type Style");
    assertThat(classNaming.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(classNaming.severity()).isEqualTo("MINOR");
    assertThat(classNaming.htmlDescription()).isEqualTo("Checks the style of array type definitions. Some like Java-style:" +
      " public static void main(String[] args) and some like C-style: public static void main(String args[])\n\n<p>\n</p>");
    assertThat(classNaming.tags()).isEmpty();
    assertThat(classNaming.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

}
