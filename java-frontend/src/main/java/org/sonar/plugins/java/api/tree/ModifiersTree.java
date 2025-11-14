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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;

import java.util.List;

/**
 * Modifiers.
 *
 * JLS 8.1.1, 8.3.1, 8.4.3, 8.5.1, 8.8.3, 9.1.1, 9.7
 *
 * @since Java 1.3
 */
@Beta
public interface ModifiersTree extends ListTree<ModifierTree> {

  /**
   * @since Java 1.5
   */
  List<AnnotationTree> annotations();

  List<ModifierKeywordTree> modifiers();

}
