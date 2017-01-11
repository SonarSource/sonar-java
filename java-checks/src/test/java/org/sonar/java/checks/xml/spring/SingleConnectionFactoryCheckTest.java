/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks.xml.spring;

import org.junit.Test;
import org.sonar.java.checks.verifier.XmlCheckVerifier;
import org.sonar.java.xml.XmlCheck;

public class SingleConnectionFactoryCheckTest {

  private static final XmlCheck CHECK = new SingleConnectionFactoryCheck();

  @Test
  public void beans() {
    XmlCheckVerifier.verify("src/test/files/checks/xml/spring/SingleConnectionFactoryCheck/beans.xml", CHECK);
  }

  @Test
  public void not_beans() {
    XmlCheckVerifier.verifyNoIssue("src/test/files/checks/xml/irrelevant.xml", CHECK);
  }
}
