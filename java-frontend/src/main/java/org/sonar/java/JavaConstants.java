/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public final class JavaConstants {

  public static final String JAVA_CATEGORY = "java";

  public static final Set<String> SECURITY_HOTSPOT_KEYS = ImmutableSet.of(
    "S2255",
    "S3330",
    "S4426",
    "S4434",
    "S4435",
    "S4499",
    "S4347",
    "S2755",
    "S2278",
    "S2277",
    "S2257",
    "S2255",
    "S2245",
    "S2092",
    "S2070",
    "S2068",
    "S1313");

  private JavaConstants() {
  }
}
