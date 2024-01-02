/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import java.io.File;
import java.util.List;
import java.util.Optional;
import org.sonar.java.test.classpath.TestClasspathUtils;

public class CommonConstants {

  public static final String AWS_MODULE = "aws";

  public static final String AWS_TEST_CLASSPATH_FILE = "../java-checks-test-sources/aws/target/test-classpath.txt";
  public static final String AWS_TEST_CLASSES_DIRECTORY = "../java-checks-test-sources/aws/target/classes";

  public static final List<File> AWS_CLASSPATH = TestClasspathUtils.loadFromFile(AWS_TEST_CLASSPATH_FILE);

  static {
    Optional.of(new File(AWS_TEST_CLASSES_DIRECTORY)).filter(File::exists).ifPresent(AWS_CLASSPATH::add);
  }
}
