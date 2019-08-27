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
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JWorkaroundsTest {

  private AST ast;

  @Before
  public void setup() {
    ASTParser astParser = ASTParser.newParser(AST.JLS12);
    astParser.setEnvironment(
      new String[]{"target/classes"},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);
    astParser.setUnitName("Example.java");
    astParser.setSource("".toCharArray());
    ast = astParser.createAST(null).getAST();
  }

  @Test
  public void resolveType() {
    ITypeBinding typeBinding = JWorkarounds.resolveType(ast, "java.lang.Object");
    assertNotNull(typeBinding);
    assertEquals("java.lang.Object", typeBinding.getQualifiedName());
  }

  @Test
  public void resolvePackageAnnotations_should_return_annotations_from_package_info() {
    IAnnotationBinding[] annotationBindings = JWorkarounds.resolvePackageAnnotations(ast, "org.sonar.java.model");
    assertEquals(2, annotationBindings.length);
    assertEquals("ParametersAreNonnullByDefault", annotationBindings[0].getName());
    assertEquals("MethodsAreNonnullByDefault", annotationBindings[1].getName());
  }

  @Test
  public void resolvePackageAnnotations_should_return_empty_array_when_no_package_info() {
    IAnnotationBinding[] annotationBindings = JWorkarounds.resolvePackageAnnotations(ast, "without.package.info");
    assertEquals(0, annotationBindings.length);
  }

}
