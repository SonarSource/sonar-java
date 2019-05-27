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

import com.google.common.collect.Lists;
import java.io.File;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.resolve.TypeAssertions.assertThat;


public class Java10SemanticTest {

  private static final SquidClassLoader CLASS_LOADER = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));

  /**
   * JLS10 §14.4.1-1.a
   */
  @Test
  public void var_basic_type() {
    VariableTree var = getLocalVariable("var a = 42;");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("int");
  }

  /**
   * JLS10 §14.4.1-1.b
   */
  @Test
  public void var_parametrized_type() {
    VariableTree var = getLocalVariable("var b = java.util.Arrays.asList(\"hello\", \"world\");");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.util.List");

    assertThat(((JavaType) type).isParameterized()).isTrue();
    ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) type;
    JavaType substitution = ptjt.substitution(ptjt.typeParameters().get(0));
    assertThat(substitution).is("java.lang.String");
  }

  /**
   * JLS10 §14.4.1-1.c
   */
  @Test
  public void var_parametrized_type_with_wildcard() {
    VariableTree var = getLocalVariable("var c = \"x\".getClass();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.lang.Class");

    assertThat(((JavaType) type).isParameterized()).isTrue();
    ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) type;
    JavaType substitution = ptjt.substitution(ptjt.typeParameters().get(0));
    assertThat(substitution.isTagged(JavaType.WILDCARD)).isTrue();
    WildCardType wildCardType = (WildCardType) substitution;
    assertThat(wildCardType.boundType).isEqualTo(WildCardType.BoundType.EXTENDS);
    assertThat(wildCardType.bound).is("java.lang.String");
  }

  @Test
  public void var_parametrized_type_with_diamond() {
    VariableTree var = getLocalVariable("var c = new java.util.HashSet<>();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.util.HashSet");
    ParametrizedTypeJavaType parametrizedType = (ParametrizedTypeJavaType) type;
    assertThat(parametrizedType.substitution(parametrizedType.typeParameters().get(0))).is("java.lang.Object");
  }

  /**
   * JLS10 §14.4.1-1.d
   */
  @Test
  public void var_type_anonymous_class() {
    VariableTree var = getLocalVariable(
      "var d = new A() { int myField; void myMethod() { } };\n"
        + "d.myField = 42;\n"
        + "d.myMethod();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).isNot("java.lang.Object");
    assertThat(type).isSubtypeOf("java.lang.Object");

    JavaSymbol.TypeJavaSymbol symbol = (JavaSymbol.TypeJavaSymbol) type.symbol();
    assertThat(symbol.memberSymbols()).hasSize(4);

    JavaSymbol thisIdentifier = symbol.members().lookup("this").get(0);
    assertThat(thisIdentifier.declaration()).isNull();
    JavaSymbol superIdentifier = symbol.members().lookup("super").get(0);
    assertThat(superIdentifier.declaration()).isNull();

    JavaSymbol myField = symbol.members().lookup("myField").get(0);
    assertThat(myField.isVariableSymbol()).isTrue();
    assertThat(myField.declaration()).isNotNull();
    assertThat(myField.usages()).hasSize(1);

    JavaSymbol myMethod = symbol.members().lookup("myMethod").get(0);
    assertThat(myMethod.isMethodSymbol()).isTrue();
    assertThat(myMethod.declaration()).isNotNull();
    assertThat(myMethod.usages()).hasSize(1);
  }

  /**
   * JLS10 §14.4.1-1.e
   */
  @Test
  public void var_cast_intersection_type() {
    VariableTree var = getLocalVariable("var e = (CharSequence & Comparable<String>) \"x\";");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());

    // instead of the intersection type (JLS10 - §4.9).
    assertThat(type).isNot("java.lang.CharSequence");
    assertThat(type).isSubtypeOf("java.lang.CharSequence");
    assertThat(type).isNot("java.lang.Comparable");
    assertThat(type).isSubtypeOf("java.lang.Comparable");
  }

  @Test
  public void var_foreach() {
    VariableTree var = ((ForEachStatement) getStatement("for(var item: items) { /* do nothing */ }")).variable();
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.lang.String");
  }

  @Test
  public void var_foreach_unspecified_raw_type() {
    VariableTree var = ((ForEachStatement) getStatement("for(var item: itemsRawType) { /* do nothing */ }")).variable();
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.lang.Object");
  }

  @Test
  public void var_foreach_iterable_raw_type() {
    VariableTree var = ((ForEachStatement) getStatement("for(var item: itemsIterableRawType) { /* do nothing */ }")).variable();
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.lang.Object");
  }

  @Test
  public void var_foreach_unknown_type() {
    VariableTree var = ((ForEachStatement) getStatement("for(var item: unknownCollection) { /* do nothing */ }")).variable();
    Type type = var.symbol().type();
    assertThat(type).isUnknown();
    assertThat(type).isEqualTo(var.type().symbolType());
  }

  @Test
  public void var_try_with_resource() {
    VariableTree var = (VariableTree) ((TryStatementTree) getStatement("try(var r = new java.io.FileInputStream(\"file\")) { /* do nothing */ }")).resourceList().get(0);
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.io.FileInputStream");
  }

  @Test
  public void var_upward_projection_wildcard() {
    VariableTree var = getLocalVariable("var a = itemsWildcard.bar();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.lang.Object");
  }

  @Test
  public void var_upward_projection_wildcard_extends() {
    VariableTree var = getLocalVariable("var a = itemsExtends.bar();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("org.foo.A");
  }

  @Test
  public void var_upward_projection_wildcard_super() {
    VariableTree var = getLocalVariable("var a = itemsSuper.bar();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("java.lang.Object");
  }

  @Test
  public void var_upward_projection_wildcard_nested() {
    VariableTree var = getLocalVariable("var a = itemsNested.bar();");
    Type type = var.symbol().type();
    assertThat(type).isEqualTo(var.type().symbolType());
    assertThat(type).is("org.foo.C");
    assertThat(((JavaType) type).isParameterized()).isTrue();

    ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) type;
    JavaType substitution = ptjt.substitution(ptjt.typeParameters().get(0));
    assertThat(substitution.isTagged(JavaType.WILDCARD)).isTrue();
    WildCardType wildCardType = (WildCardType) substitution;
    assertThat(wildCardType.boundType).isEqualTo(WildCardType.BoundType.EXTENDS);
    assertThat(wildCardType.bound).is("org.foo.A");
  }


  private VariableTree getLocalVariable(String statement) {
    return (VariableTree) getStatement(statement);
  }

  private StatementTree getStatement(String statement) {
    String code = "package org.foo;\n"
      + "class A {\n"
      + "  void tst(java.util.List<String> items,\n"
      + "           java.util.Collection itemsRawType,\n"
      + "           java.lang.Iterable itemsIterableRawType,\n"
      + "           C<?> itemsWildcard,\n"
      + "           C<? extends A> itemsExtends,\n"
      + "           C<? super B> itemsSuper,\n"
      + "           C<C<? extends A>> itemsNested) {\n"
      + "    " + statement + "\n"
      + "  }\n"
      + "}\n"
      + "class B extends A {}\n"
      + "abstract class C<T> {\n"
      + "  abstract T bar();\n"
      + "}";

    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(code);
    SemanticModel.createFor(cut, CLASS_LOADER);

    ClassTree classTree = (ClassTree) (cut.types().get(0));
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    return methodTree.block().body().get(0);
  }
}
