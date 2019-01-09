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
package org.sonar.java.checks.xml.struts;

import org.junit.Before;
import org.junit.Test;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheckVerifier;

public class ActionNumberCheckTest {

  private ActionNumberCheck check;

  @Before
  public void setup() {
    check = new ActionNumberCheck();
  }

  @Test
  public void struts_config_with_too_many_forwards() {
    SonarXmlCheckVerifier.verifyIssues("tooManyActionsDefault/struts-config.xml", check);
  }

  @Test
  public void struts_config_with_too_many_forwards_custom() {
    check.maximumForwards = 3;
    SonarXmlCheckVerifier.verifyIssues("tooManyActionsCustom/struts-config.xml", check);
  }

  @Test
  public void not_a_struts_config_xml() {
    SonarXmlCheckVerifier.verifyNoIssue("../irrelevant.xml", check);
  }
}
