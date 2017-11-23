/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaFrontend {

  public static class ScannedFile {

    private final CompilationUnitTree tree;
    private final SemanticModel semantic;

    public ScannedFile(CompilationUnitTree tree, SemanticModel semantic) {
      this.tree = tree;
      this.semantic = semantic;
    }

    public CompilationUnitTree tree() {
      return tree;
    }

    public SemanticModel semantic() {
      return semantic;
    }
  }

  private final ActionParser<Tree> parser = JavaParser.createParser();

  public ScannedFile scan(File source, ClassLoader classLoader) {
    CompilationUnitTree tree = (CompilationUnitTree) parser.parse(source);
    SemanticModel model = SemanticModel.createFor(tree, classLoader);

    return new ScannedFile(tree, model);
  }

}
