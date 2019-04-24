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
package org.sonar.java.checks.security;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.plugins.java.api.tree.ExpressionTree;

public class EmptyDatabasePasswordCheckTest {
  /**
   * Constants used inside "src/test/files/checks/security/EmptyDatabasePasswordCheck.java" file
   * in order to test {@link EmptyDatabasePasswordCheck#getStringValue(ExpressionTree)} resolution
   * of an identifier outside of the compilation unit (static import in this case).
   */
  public final static String EMPTY_PASSWORD = "";
  public final static String NON_EMPTY_PASSWORD = "foo";
  @Test
  public void test() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/security/EmptyDatabasePasswordCheck.java", new EmptyDatabasePasswordCheck());
  }
}
