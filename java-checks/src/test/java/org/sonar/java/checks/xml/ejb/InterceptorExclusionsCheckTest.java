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
package org.sonar.java.checks.xml.ejb;

import org.junit.Test;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheckVerifier;

public class InterceptorExclusionsCheckTest {

  private static final SonarXmlCheck CHECK = new InterceptorExclusionsCheck();

  @Test
  public void ejb_jar() {
    SonarXmlCheckVerifier.verifyIssues("ejb-jar.xml", CHECK);
  }

  @Test
  public void not_an_ejb_jar() {
    SonarXmlCheckVerifier.verifyNoIssue("../irrelevant.xml", CHECK);
  }
}
