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
package org.sonar.java.resolve;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.test.ImportsResolutionCases.ImportInnerClass;
import org.sonar.plugins.java.api.semantic.Symbol;

import static org.assertj.core.api.Assertions.assertThat;

// FIXME(Godin): must not depend on real files
public class ImportResolutionTest {


  private static Result result;
  //test inner imports
  private ImportInnerClass importInnerClass;

  @BeforeClass
  public static void setUp() throws Exception {
    result = Result.createFor("ImportResolution");
  }

  @Test
  public void extends_should_point_to_correct_symbol() {
    assertThat(result.symbol("Class2").kind == JavaSymbol.TYP).isTrue();
    JavaSymbol.TypeJavaSymbol class1 = (JavaSymbol.TypeJavaSymbol) result.symbol("Class1");
    JavaSymbol.TypeJavaSymbol class2 = (JavaSymbol.TypeJavaSymbol) result.symbol("Class2");
    assertThat(class2.getSuperclass().symbol).isEqualTo(class1);
    assertThat(class1.getSuperclass()).isNotNull();
    assertThat(class1.getSuperclass().symbol.name).isEqualTo("Collection");
    JavaSymbol.TypeJavaSymbol interface1 = (JavaSymbol.TypeJavaSymbol) result.symbol("Interface1");
    assertThat(interface1.getInterfaces()).isNotEmpty();
    assertThat(interface1.getInterfaces().get(0).symbol.name).isEqualTo("List");
  }

  @Test
  public void import_on_inner_type_should_be_resolved() throws Exception {
    assertThat(result.symbol("annotationTree").type.symbol.name).isEqualTo("NotImplementedTreeImpl");
    assertThat(result.symbol("annotationTree").type.symbol.owner().name).isEqualTo("JavaTree");
  }

  @Test
  public void import_on_inner_generic_type_member_should_not_fail() throws Exception {
    assertThat(result.symbol("BAR", 4).type.symbol.name).isEqualTo("String");
  }

  @Test
  public void import_static_var_should_be_resolved() throws Exception {
    JavaSymbol http_ok = result.symbol("HTTP_OK");
    assertThat(http_ok.owner().name).isEqualTo("HttpURLConnection");
    assertThat(http_ok.owner().type.symbol.name).isEqualTo("HttpURLConnection");
    assertThat(http_ok.kind).isEqualTo(JavaSymbol.VAR);
    assertThat(http_ok.type.tag).isEqualTo(JavaType.INT);
  }

  @Test
  public void import_static_method_should_be_resolved() throws Exception {
    JavaSymbol reverse = result.symbol("reverse");
    assertThat(reverse.owner().name).isEqualTo("Collections");
    assertThat(reverse.owner().type.symbol.name).isEqualTo("Collections");
    assertThat(reverse.kind).isEqualTo(JavaSymbol.MTH);
  }

  @Test
  public void import_static_method_should_be_resolved_when_refering_to_multiple_symbols() throws Exception {
    JavaSymbol sort = result.symbol("sort");
    assertThat(sort.owner().name).isEqualTo("Collections");
    assertThat(sort.owner().type.symbol.name).isEqualTo("Collections");
    assertThat(sort.kind).isEqualTo(JavaSymbol.MTH);
    assertThat(sort.type.tag).isEqualTo(JavaType.METHOD);
    assertThat(result.reference(46, 7).name).isEqualTo("sort");
    assertThat(result.reference(46, 7)).isEqualTo(sort);
    JavaSymbol sortMethod = result.reference(47, 7);
    assertThat(sortMethod.name).isEqualTo("sort");
    assertThat(sortMethod).isNotEqualTo(sort);
    assertThat(sortMethod.isMethodSymbol()).isTrue();
    assertThat(((Symbol.MethodSymbol) sortMethod).parameterTypes()).hasSize(2);

    JavaSymbol nCopiesSymbol = result.symbol("nCopies");
    assertThat(result.reference(67, 5)).isSameAs(nCopiesSymbol);
    assertThat(result.reference(68, 5)).isSameAs(nCopiesSymbol);
  }

  @Test
  public void package_should_be_resolved() {
    assertThat(result.symbol("sym")).isNotNull();
    assertThat(result.symbol("sym").kind).isEqualTo(JavaSymbol.PCK);
    assertThat(result.symbol("sym").owner().kind).isEqualTo(JavaSymbol.PCK);
    //default package name is empty
    assertThat(result.symbol("sym").owner().name).isEmpty();
  }

  @Test
  public void types_from_same_package_should_be_resolved() {
    Result result1 = Result.createForJavaFile("src/test/java/org/sonar/java/resolve/BytecodeCompleterTest");
    JavaSymbol.TypeJavaSymbol thisTest = (JavaSymbol.TypeJavaSymbol) result1.symbol("BytecodeCompleterTest");
    List<JavaSymbol> symbols = thisTest.members().lookup("bytecodeCompleterPackageVisibility");
    assertThat(symbols).hasSize(1);
    JavaSymbol.VariableJavaSymbol symbol = (JavaSymbol.VariableJavaSymbol) symbols.get(0);
    assertThat(symbol.type.symbol.name).isEqualTo("BytecodeCompleterPackageVisibility");
    assertThat(symbol.type.symbol.owner().name).isEqualTo(thisTest.owner().name);
  }

  @Test
  public void star_imports_should_be_resolved() {
    JavaSymbol sort = result.symbol("file");
    assertThat(sort.type.symbol.name).isEqualTo("File");
    assertThat(sort.type.symbol.owner().name).isEqualTo("java.io");
  }

  @Test
  public void star_imports_on_type_should_be_resolved() {
    Result result1 = Result.createForJavaFile("src/test/java/org/sonar/java/resolve/ImportResolutionTest");
    JavaSymbol importInnerClassSymbol = result1.symbol("importInnerClass");
    assertThat(importInnerClassSymbol.type.symbol.name).isEqualTo("ImportInnerClass");
    assertThat(importInnerClassSymbol.type.symbol.owner().name).isEqualTo("ImportsResolutionCases");
    assertThat(importInnerClassSymbol.type.symbol.owner().owner().name).isEqualTo("org.sonar.java.test");
  }

  @Test
  public void import_static_on_demand_should_be_resolved() throws Exception {
    JavaSymbol http_accepted = result.reference(42, 10);
    assertThat(http_accepted.name).isEqualTo("HTTP_ACCEPTED");
    assertThat(http_accepted.owner().name).isEqualTo("HttpURLConnection");
    assertThat(http_accepted.owner().type.symbol.name).isEqualTo("HttpURLConnection");
    assertThat(http_accepted.kind).isEqualTo(JavaSymbol.VAR);
    assertThat(http_accepted.type.tag).isEqualTo(JavaType.INT);
  }

  @Test
  public void imports_from_java_lang() {
    JavaSymbol iterable = result.symbol("iterable");
    assertThat(iterable.type.symbol.name).isEqualTo("Iterable");
    assertThat(iterable.type.symbol.owner().name).isEqualTo("java.lang");
  }

  @Test
  public void only_one_symbol_per_class_should_be_created() {
    Result result1 = Result.createForJavaFile("src/test/java/org/sonar/java/resolve/BytecodeCompleterTest");
    JavaSymbol.TypeJavaSymbol thisTest = (JavaSymbol.TypeJavaSymbol) result1.symbol("BytecodeCompleterTest");
    List<JavaSymbol> symbols = thisTest.members().lookup("bytecodeCompleterPackageVisibility");
    assertThat(symbols).hasSize(1);
    JavaSymbol.TypeJavaSymbol symbol = ((JavaSymbol.VariableJavaSymbol) symbols.get(0)).type.symbol;
    symbols = symbol.members().lookup("bytecodeCompleterTest");
    assertThat(symbols).hasSize(1);
    JavaSymbol.TypeJavaSymbol test = ((JavaSymbol.VariableJavaSymbol) symbols.get(0)).type.symbol;
    assertThat(test).isEqualTo(thisTest);
  }

}
