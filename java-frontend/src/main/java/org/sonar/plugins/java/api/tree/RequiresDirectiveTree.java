/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;

/**
 * The 'requires' directive from java 9 module directives
 * 
 * JLS9 - §7.7.1
 * 
 * <pre>
 *   requires {@link #moduleName()} ;
 *   requires static {@link #moduleName()} ;
 *   requires transitive {@link #moduleName()} ;
 * </pre>
 * 
 * @since Java 9
 */
@Beta
public interface RequiresDirectiveTree extends ModuleDirectiveTree {

  ModifiersTree modifiers();

  ModuleNameTree moduleName();

}
