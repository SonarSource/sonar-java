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
package org.sonar.java.checks.xml.hibernate;

import org.junit.Test;
import org.sonar.java.checks.verifier.XmlCheckVerifier;
import org.sonar.java.xml.XmlCheck;

public class DatabaseSchemaUpdateCheckTest {

  private static final XmlCheck CHECK = new DatabaseSchemaUpdateCheck();

  private static final String TEST_FILES_ROOT = "src/test/files/checks/xml/hibernate/DatabaseSchemaUpdateCheck";

  @Test
  public void test() {
    XmlCheckVerifier.verify(TEST_FILES_ROOT + "/hibernate11.cfg.xml", CHECK);
    XmlCheckVerifier.verify(TEST_FILES_ROOT + "/hibernate20.cfg.xml", CHECK);
    XmlCheckVerifier.verify(TEST_FILES_ROOT + "/hibernate30.cfg.xml", CHECK);
  }

  @Test
  public void no_issue() {
    XmlCheckVerifier.verifyNoIssue(TEST_FILES_ROOT + "/hibernate30NoProperty.cfg.xml", CHECK);
    XmlCheckVerifier.verifyNoIssue("src/test/files/checks/xml/irrelevant.xml", CHECK);
  }
}
