/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.se.checks;

import org.junit.Test;
import org.sonar.java.se.JavaCheckVerifier;

public class UnclosedResourcesCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/se/UnclosedResourcesCheck.java", new UnclosedResourcesCheck());
  }

  @Test
  public void nonReproducible() {
    JavaCheckVerifier.verify("src/test/files/se/IrreproducibleUnclosedResourcesTestFile.java", new UnclosedResourcesCheck());
  }

  // Failing test @Test
  public void reproducible() {
    JavaCheckVerifier.verify("src/test/files/se/ReproducibleUnclosedResourcesTestFile.java", new UnclosedResourcesCheck());
  }

  // Failing test @Test
  public void foreign() {
    JavaCheckVerifier.verify("src/test/files/se/CloseResourceTestFile.java", new UnclosedResourcesCheck());
  }
}
