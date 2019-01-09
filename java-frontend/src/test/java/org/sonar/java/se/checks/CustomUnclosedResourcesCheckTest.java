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
package org.sonar.java.se.checks;

import org.junit.Test;
import org.sonar.java.se.JavaCheckVerifier;

public class CustomUnclosedResourcesCheckTest {

  @Test
  public void constructorClosed() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.constructor = "org.sonar.custom.GenericResource(java.lang.String)";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    JavaCheckVerifier.verify("src/test/files/se/customresources/ConstructorClosed.java", check);
  }

  @Test
  public void constructorClosedAny() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.constructor = "org.sonar.custom.GenericResource";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource";
    JavaCheckVerifier.verify("src/test/files/se/customresources/ConstructorClosedAny.java", check);
  }

  @Test
  public void factoryClosed() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.factoryMethod = "org.sonar.custom.ResourceFactory#createResource(java.lang.String)";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    JavaCheckVerifier.verify("src/test/files/se/customresources/FactoryClosed.java", check);
  }

  @Test
  public void factoryClosedAny() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.factoryMethod = "org.sonar.custom.ResourceFactory#createResource";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource";
    JavaCheckVerifier.verify("src/test/files/se/customresources/FactoryClosedAny.java", check);
  }

  @Test
  public void openedClosed() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.openingMethod = "org.sonar.custom.GenericResource#open(java.lang.String)";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    JavaCheckVerifier.verify("src/test/files/se/customresources/OpenedClosed.java", check);
  }

  @Test
  public void openedClosedAny() {
    CustomUnclosedResourcesCheck check = new CustomUnclosedResourcesCheck();
    check.openingMethod = "org.sonar.custom.GenericResource#open";
    check.closingMethod = "org.sonar.custom.GenericResource#closeResource";
    JavaCheckVerifier.verify("src/test/files/se/customresources/OpenedClosedAny.java", check);
  }

  /**
   * {@link CustomUnclosedResourcesCheck.CustomResourceConstraint} class needs to be different, otherwise issued will be duplicated. See SONARJAVA-1624
   */
  @Test
  public void check_status_is_different_instance() {
    CustomUnclosedResourcesCheck check1 = new CustomUnclosedResourcesCheck();
    check1.constructor = "org.sonar.custom.GenericResource(java.lang.String)";
    check1.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    CustomUnclosedResourcesCheck check2 = new CustomUnclosedResourcesCheck();
    check2.constructor = "org.sonar.custom.GenericResource(java.lang.String)";
    check2.closingMethod = "org.sonar.custom.GenericResource#closeResource(java.lang.String)";
    JavaCheckVerifier.verify("src/test/files/se/customresources/ConstructorClosed.java", check1, check2);
  }

}
