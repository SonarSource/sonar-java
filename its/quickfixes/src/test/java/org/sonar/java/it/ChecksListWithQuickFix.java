/*
 * SonarQube Java
 * Copyright (C) 2024-2024 SonarSource SA
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
package org.sonar.java.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.CheckList;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;

public class ChecksListWithQuickFix {

  public static List<JavaFileScanner> checks = null;
  public static final List<String> QUICKFIX_KEYS;
  private static final String METADATA_FOLDER = "../../sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java";

  static {

    QUICKFIX_KEYS = new ArrayList<>();
    try(Stream<Path> paths = Files.walk(Paths.get(METADATA_FOLDER))) {
      paths.filter(Files::isRegularFile)
        .forEach(path -> {
          try {
            String content = Files.readString(path);
            if (content.contains("\"covered\"") || content.contains("\"partial\"")) {
              QUICKFIX_KEYS.add(path.getFileName().toString().replace(".json", ""));
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    checks = new ArrayList<>();
    List<Class<? extends JavaCheck>> withQuickFixes =
      CheckList.getJavaChecks().stream()
        .filter(c -> c.isAnnotationPresent(Rule.class) && hasQuickFixCovered(c.getAnnotation(Rule.class)))
        .toList();
    for (Class c : withQuickFixes) {
      try {
        checks.add((JavaFileScanner) c.getDeclaredConstructor().newInstance());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static boolean hasQuickFixCovered(Rule rule){
    return QUICKFIX_KEYS.contains(rule.key());
  }

}
