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
package org.sonar.java.ast.parser;

import com.sonar.sslr.api.typed.ActionParser;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import java.io.File;
import java.nio.charset.Charset;

public class JavaParser extends ActionParser<Tree> {

  private JavaParser(Charset charset, LexerlessGrammarBuilder grammarBuilder, Class<JavaGrammar> javaGrammarClass,
    TreeFactory treeFactory, JavaNodeBuilder javaNodeBuilder, JavaLexer compilationUnit) {
    super(charset, grammarBuilder, javaGrammarClass, treeFactory, javaNodeBuilder, compilationUnit);
  }

  public static ActionParser<Tree> createParser(Charset charset) {
    return new JavaParser(
      charset,
      JavaLexer.createGrammarBuilder(),
      JavaGrammar.class,
      new TreeFactory(),
      new JavaNodeBuilder(),
      JavaLexer.COMPILATION_UNIT);
  }

  @Override
  public Tree parse(File file) {
    return createParentLink((JavaTree) super.parse(file));
  }

  @Override
  public Tree parse(String source) {
    return createParentLink((JavaTree) super.parse(source));
  }

  private static Tree createParentLink(JavaTree parent) {
    if (!parent.isLeaf()) {
      for (Tree nextTree : parent.getChildren()) {
        JavaTree next = (JavaTree) nextTree;
        if (next != null) {
          next.setParent(parent);
          createParentLink(next);
        }
      }
    }
    return parent;
  }
}
