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
package org.sonar.java.classpath;

import java.util.Objects;
import javax.annotation.Nullable;

public record Version(Integer major, @Nullable Integer minor, @Nullable Integer patch, @Nullable String qualifier) implements Comparable<Version> {

  @Override
  public int compareTo(Version o) {
    if (!Objects.equals(major, o.major)) {
      return major - o.major;
    }
    // TODO: complete this
    return 0;
  }

  @Override
  public String toString() {
    return major +
      (minor == null ? "" : "." + minor) +
      (patch == null ? "" : "." + patch) +
      (qualifier == null ? "" : qualifier);
  }
}
