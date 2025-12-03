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
package org.sonar.java.checks.verifier.internal;

import com.sonar.sslr.api.RecognitionException;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;

public class CheckVerifierUtils {

  private CheckVerifierUtils() {
    // utility class
  }

  protected static final String CHECK_OR_CHECKS = "check(s)";
  protected static final String FILE_OR_FILES = "file(s)";

  protected static SonarComponents sonarComponents(boolean isCacheEnabled, ReadCache readCache, WriteCache writeCache) {
    SensorContext sensorContext;
    if (isCacheEnabled) {
      sensorContext = new CacheEnabledSensorContext(readCache, writeCache);
    } else {
      sensorContext = new InternalSensorContext();
    }
    FileSystem fileSystem = sensorContext.fileSystem();
    Configuration config = sensorContext.config();

    ClasspathForMain classpathForMain = new ClasspathForMain(config, fileSystem);
    ClasspathForTest classpathForTest = new ClasspathForTest(config, fileSystem);

    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, classpathForMain, classpathForTest, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        throw new AssertionError(String.format("Should not fail analysis (%s)", re.getMessage()));
      }

      @Override
      public boolean canSkipUnchangedFiles() {
        return isCacheEnabled;
      }
    };
    sonarComponents.setSensorContext(sensorContext);
    return sonarComponents;
  }

  protected static void requiresNull(@Nullable Object obj, String fieldName) {
    if (obj != null) {
      throw new AssertionError(String.format("Do not set %s multiple times!", fieldName));
    }
  }

  protected static void requiresNonNull(@Nullable Object obj, String fieldName) {
    if (obj == null) {
      throw new AssertionError(String.format("Set %s before calling any verification method!", fieldName));
    }
  }

  protected static void requiresNonEmpty(Collection<?> objects, String fieldName) {
    if (objects.isEmpty()) {
      throw new AssertionError(String.format("Provide at least one %s!", fieldName));
    }
  }

}
