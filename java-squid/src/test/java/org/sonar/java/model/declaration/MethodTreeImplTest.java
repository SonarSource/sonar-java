/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.declaration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class MethodTreeImplTest {

  private final Parser p = JavaParser.createParser(Charsets.UTF_8, true);
  private final JavaTreeMaker maker = new JavaTreeMaker();

  @Test
  public void override_without_annotation_should_be_detected() {
    CompilationUnitTree cut = createTree("interface T { int m(); } class A implements T { int m(){return 0;}}");
    ClassTree interfaze = (ClassTree) cut.types().get(0);
    MethodTreeImpl methodInterface = (MethodTreeImpl) interfaze.members().get(0);
    ClassTree clazz = (ClassTree) cut.types().get(1);
    MethodTreeImpl methodClazz = (MethodTreeImpl) clazz.members().get(0);
    assertThat(methodInterface.isOverriding()).isFalse();
    assertThat(methodClazz.isOverriding()).isTrue();
  }

  @Test
  public void override_from_object_should_be_detected() {
    MethodTreeImpl method = getUniqueMethod("class A { String toString(){return \"\";}}");
    assertThat(method.isOverriding()).isTrue();
  }

  @Test
  public void static_method_cannot_be_overriden() {
    assertThat(getUniqueMethod("class A{ static void m(){}}").isOverriding()).isFalse();
  }

  @Test
  public void private_method_cannot_be_overriden() {
    assertThat(getUniqueMethod("class A{ private void m(){}}").isOverriding()).isFalse();
  }

  @Test
  public void override_annotated_method_should_be_overriden() {
    assertThat(getUniqueMethod("class A{ @Override void m(){}}").isOverriding()).isTrue();
    assertThat(getUniqueMethod("class A{ @cutom.namespace.Override void m(){}}").isOverriding()).isFalse();
    assertThat(getUniqueMethod("class A{ @Foo void m(){}}").isOverriding()).isFalse();
  }

  @Test
  public void symbol_not_set_should_lead_to_null_result() throws Exception {
    AstNode astNode = p.parse("class A { String toString(){return \"\";}}");
    CompilationUnitTree cut = maker.compilationUnit(astNode);
    MethodTreeImpl methodTree = (MethodTreeImpl) ((ClassTree) cut.types().get(0)).members().get(0);
    assertThat(methodTree.isOverriding()).isNull();
  }

  private MethodTreeImpl getUniqueMethod(String code) {
    CompilationUnitTree cut = createTree(code);
    return (MethodTreeImpl) ((ClassTree)cut.types().get(0)).members().get(0);
  }

  private CompilationUnitTree createTree(String code) {
    AstNode astNode = p.parse(code);
    CompilationUnitTree compilationUnitTree = maker.compilationUnit(astNode);
    SemanticModel.createFor(compilationUnitTree, Lists.<File>newArrayList());
    return compilationUnitTree;
  }

}
