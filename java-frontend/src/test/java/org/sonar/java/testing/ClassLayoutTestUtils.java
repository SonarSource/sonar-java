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
package org.sonar.java.testing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ClassLayoutTestUtils {

  private ClassLayoutTestUtils() {
  }

  /** Count number of fields in instances, including inherited fields. */
  public static int countFields(Class<?> clazz) {
    int count = 0;
    while (clazz != Object.class) {
      for (Field f : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(f.getModifiers())) {
          count++;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return count;
  }
}
