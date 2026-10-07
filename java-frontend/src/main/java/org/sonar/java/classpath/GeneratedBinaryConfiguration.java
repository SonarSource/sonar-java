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
package org.sonar.java.classpath;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.config.Configuration;

final class GeneratedBinaryConfiguration implements Configuration {

  private final Configuration delegate;
  private final String binariesProperty;
  private final List<Path> directories;

  GeneratedBinaryConfiguration(Configuration delegate, String binariesProperty, List<Path> directories) {
    this.delegate = delegate;
    this.binariesProperty = binariesProperty;
    this.directories = directories;
  }

  @Override
  public Optional<String> get(String key) {
    if (binariesProperty.equals(key) && !directories.isEmpty()) {
      return Optional.of(Arrays.stream(getStringArray(key))
        .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
        .collect(Collectors.joining(",")));
    }
    return delegate.get(key);
  }

  @Override
  public boolean hasKey(String key) {
    return (binariesProperty.equals(key) && !directories.isEmpty()) || delegate.hasKey(key);
  }

  @Override
  public String[] getStringArray(String key) {
    if (binariesProperty.equals(key) && !directories.isEmpty()) {
      return Stream.concat(Arrays.stream(delegate.getStringArray(key)), directories.stream().map(Path::toString))
        .distinct().toArray(String[]::new);
    }
    return delegate.getStringArray(key);
  }
}
