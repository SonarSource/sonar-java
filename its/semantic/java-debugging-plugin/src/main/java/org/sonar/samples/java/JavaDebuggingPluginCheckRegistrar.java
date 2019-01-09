/*
 * SonarQube Java
 * Copyright (C) 2017-2019 SonarSource SA
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
package org.sonar.samples.java;

import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

import java.util.Arrays;

public class JavaDebuggingPluginCheckRegistrar implements CheckRegistrar {

  @Override
  public void register(RegistrarContext registrarContext) {
    registrarContext.registerClassesForRepository(
      JavaDebuggingPluginRulesDefinition.REPOSITORY_KEY,
      Arrays.asList(checkClasses()),
      Arrays.asList(testCheckClasses()));
  }

  public static Class<? extends JavaCheck>[] checkClasses() {
    return new Class[] {
      UnknownMethodInvocationsCheck.class,
      UnknownConstructorCallCheck.class
    };
  }

  public static Class<? extends JavaCheck>[] testCheckClasses() {
    return new Class[0];
  }

}
