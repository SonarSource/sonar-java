/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import java.util.HashMap;
import javax.annotation.Nullable;


public final class ShannonEntropy {

  private static final double LOG_2 = Math.log(2.0d);

  private ShannonEntropy() {
    // utility class
  }

  public static double calculate(@Nullable String str) {
    if (str == null || str.isEmpty()) {
      return 0.0d;
    }
    int length = str.length();
    return str.chars()
      .collect(HashMap<Integer, Integer>::new, (map, ch) -> map.merge(ch, 1, Integer::sum), HashMap::putAll)
      .values().stream()
      .mapToDouble(count -> ((double) count) / length)
      .map(frequency -> -(frequency * Math.log(frequency) / LOG_2))
      .sum();
  }

}
