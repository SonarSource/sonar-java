/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.verifier.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;

final class InternalConfiguration extends InternalMockedSonarAPI implements Configuration {
  private final Map<String, String> properties = new HashMap<>();

  InternalConfiguration() {
    properties.put(SonarComponents.FAIL_ON_EXCEPTION_KEY, Boolean.toString(true));
  }

  @Override
  public boolean hasKey(String arg0) {
    return properties.containsKey(arg0);
  }

  @Override
  public Optional<String> get(String arg0) {
    return Optional.ofNullable(properties.get(arg0));
  }

  @Override
  public String[] getStringArray(String arg0) {
    throw notSupportedException("getStringArray(String)");
  }
}
