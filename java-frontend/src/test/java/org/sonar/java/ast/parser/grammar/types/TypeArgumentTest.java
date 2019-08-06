/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.ast.parser.grammar.types;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import static org.junit.Assert.assertEquals;
import static org.sonar.sslr.tests.Assertions.assertThat;

public class TypeArgumentTest {

  @Test
  public void ok() {
    assertThat(JavaLexer.TYPE_ARGUMENT)
      .matches("referenceType")
      .matches("?")
      .matches("? extends referenceType")
      .matches("? super referenceType")
      .matches("@Foo referenceType")
      .matches("@Foo ?")
      .matches("@Foo ? extends @Foo referenceType")
      .matches("@Foo ? super @Foo referenceType")
      .matches("referenceType[]")
      .matches("@Foo referenceType[]");
  }

  @Test
  public void annotations() {
    {
      WildcardTree t = (WildcardTree) parseTypeArgument("@A ?");
      assertEquals(1, t.annotations().size());
    }
    {
      ArrayTypeTree t = (ArrayTypeTree) parseTypeArgument("Object @A []");
      assertEquals(1, t.annotations().size());
      assertEquals(0, t.type().annotations().size());
    }
    {
      ArrayTypeTree t = (ArrayTypeTree) parseTypeArgument("@A Object []");
      assertEquals(0, t.annotations().size());
      assertEquals(1, t.type().annotations().size());
    }
  }

  private static Tree parseTypeArgument(String s) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) JavaParser.createParser().parse("class C { List<" + s + "> f; }");
    ClassTree cls = (ClassTree) compilationUnit.types().get(0);
    VariableTree field = (VariableTree) cls.members().get(0);
    ParameterizedTypeTree type = (ParameterizedTypeTree) field.type();
    return type.typeArguments().get(0);
  }

}
