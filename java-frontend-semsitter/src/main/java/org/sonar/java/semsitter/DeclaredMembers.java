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
package org.sonar.java.semsitter;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonarsource.semsitter.wire.Fact;

/** What the {@code types} answers declare about members and packages, by symbol id, for one {@link JSema}. */
final class DeclaredMembers {
  final Map<String, List<DeclaredAnnotation>> libraryAnnotations = new HashMap<>();
  final Map<String, List<String>> projectThrows = new HashMap<>();
  final Map<String, Fact> projectMemberFacts = new HashMap<>();
  final Map<String, Boolean> projectVarArgs = new HashMap<>();
  final Map<String, List<DeclaredAnnotation>> packageAnnotations = new HashMap<>();
  final Map<String, JsonObject> projectFieldTypes = new HashMap<>();
  final Map<String, Object> annotationDefaults = new HashMap<>();
}
