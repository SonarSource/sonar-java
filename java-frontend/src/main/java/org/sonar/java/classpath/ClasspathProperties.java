/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import java.util.ArrayList;
import java.util.Collections;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

public class ClasspathProperties {

  public static final String EMPTY_LIBRARIES_WARNING_TEMPLATE = "Dependencies/libraries were not provided for analysis of %s files. The '%s' property is empty." +
    " Verify your configuration, as you might end up with less precise results.";

  public static final String SONAR_JAVA_JDK_HOME = "sonar.java.jdkHome";

  public static final String SONAR_JAVA_BINARIES = "sonar.java.binaries";
  public static final String SONAR_JAVA_LIBRARIES = "sonar.java.libraries";

  public static final String SONAR_JAVA_TEST_BINARIES = "sonar.java.test.binaries";
  public static final String SONAR_JAVA_TEST_LIBRARIES = "sonar.java.test.libraries";

  private ClasspathProperties() {
  }

  public static List<PropertyDefinition> getProperties() {
    List<PropertyDefinition> extensions = new ArrayList<>();
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_JDK_HOME)
      .description("Path to jdk directory used by the project under analysis.")
      .hidden()
      .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_BINARIES)
      .description("Comma-separated paths to directories containing the binary files (directories with class files).")
      .multiValues(true)
      .hidden()
      .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_LIBRARIES)
      .description("Comma-separated paths to libraries required by the project.")
      .multiValues(true)
      .hidden()
      .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_TEST_BINARIES)
      .description("Comma-separated paths to directories containing the binary files (directories with class files).")
      .multiValues(true)
      .hidden()
      .build()
    );
    extensions.add(PropertyDefinition.builder(SONAR_JAVA_TEST_LIBRARIES)
      .description("Comma-separated paths to libraries required by the project.")
      .multiValues(true)
      .hidden()
      .build()
    );
    return Collections.unmodifiableList(extensions);
  }
}
