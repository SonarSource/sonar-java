/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.plugins.java.api;

import org.sonar.java.annotations.Beta;
import java.util.Optional;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Represents source map between generated Java code and JSP file
 */
@Beta
public interface SourceMap {

  /**
   * Return location in JSP file corresponding to the AST node
   *
   * @return location in JSP file or null
   */
  Optional<Location> sourceMapLocationFor(Tree tree);

  interface Location {

    InputFile file();

    int startLine();

    int endLine();
  }
}
