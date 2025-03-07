/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.verifier.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.java.classpath.ClasspathForMain;

public class InternalClasspathForMain extends ClasspathForMain {

  private List<File> classpath = new ArrayList<>();

  public InternalClasspathForMain(Configuration settings, FileSystem fs) {
    super(settings, fs);
  }

  public InternalClasspathForMain(Configuration settings, FileSystem fs, List<File> classpath) {
    super(settings, fs);
    this.classpath = classpath;
  }

  @Override
  protected void init() {
    elements.addAll(getJdkJars());
    binaries.addAll(classpath);
    elements.addAll(binaries);
  }
}
