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
package org.sonar.java;

import com.google.common.collect.ImmutableList;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

public class JavaClasspathProperties {

  public static final String SONAR_JAVA_BINARIES = "sonar.java.binaries";
  public static final String SONAR_JAVA_LIBRARIES = "sonar.java.libraries";
  public static final String SONAR_JAVA_TEST_BINARIES = "sonar.java.test.binaries";
  public static final String SONAR_JAVA_TEST_LIBRARIES = "sonar.java.test.libraries";

  private JavaClasspathProperties() {
  }

  public static List<PropertyDefinition> getProperties() {
    ImmutableList.Builder<PropertyDefinition> extensions = ImmutableList.builder();
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_BINARIES)
            .description("Comma-separated paths to directories containing the binary files (directories with class files).")
            .hidden()
            .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_LIBRARIES)
            .description("Comma-separated paths to libraries required by the project.")
            .hidden()
            .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_TEST_BINARIES)
            .description("Comma-separated paths to directories containing the binary files (directories with class files).")
            .hidden()
            .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_TEST_LIBRARIES)
            .description("Comma-separated paths to libraries required by the project.")
            .hidden()
            .build()
    );
    return extensions.build();
  }
}
