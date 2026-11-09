/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.psi;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;

final class PsiRuntimeTestSupport {
  private PsiRuntimeTestSupport() {
  }

  static void isolated(Class<?> test, String method, Path temporary) throws Exception {
    var classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    URL[] urls = Arrays.stream(classpath.split(Pattern.quote(File.pathSeparator))).map(entry -> {
      try {
        return Path.of(entry).toUri().toURL();
      } catch (java.net.MalformedURLException failure) {
        throw new IllegalArgumentException(failure);
      }
    }).toArray(URL[]::new);
    try (var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
      var action = loader.loadClass(test.getName()).getDeclaredMethod(method, Path.class);
      action.setAccessible(true);
      action.invoke(null, temporary);
    }
  }
}
