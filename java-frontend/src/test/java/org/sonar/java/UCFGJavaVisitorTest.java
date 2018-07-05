/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.ucfg.BasicBlock;
import org.sonar.ucfg.Expression;
import org.sonar.ucfg.LocationInFile;
import org.sonar.ucfg.UCFG;
import org.sonar.ucfg.UCFGBuilder;
import org.sonar.ucfg.UCFGElement.Instruction;
import org.sonar.ucfg.UCFGtoProtobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.ucfg.UCFGBuilder.call;
import static org.sonar.ucfg.UCFGBuilder.constant;
import static org.sonar.ucfg.UCFGBuilder.newBasicBlock;
import static org.sonar.ucfg.UCFGBuilder.variableWithId;

public class UCFGJavaVisitorTest {

  private static final String FILE_KEY = "someRandomFileKey.java";
  private static SquidClassLoader squidClassLoader;

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @BeforeClass
  public static void setUp() {
    File testJarsDir = new File("target/test-jars/");
    squidClassLoader = new SquidClassLoader(Arrays.asList(testJarsDir.listFiles()));
  }

  @Test
  public void visit_java_file() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1").ret(arg, new LocationInFile(FILE_KEY, 1,37,1,48)))
      .build();
    assertCodeToUCfg("class A { String method(String arg) {return arg;}}", expectedUCFG);
  }

  @Test
  public void assign_to_field() {
    // fields are ignored
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1").ret(arg, new LocationInFile(FILE_KEY, 1,69,1,80)))
      .build();
    assertCodeToUCfg("class A { String field; String method(String arg) {this.field = arg; return arg;}}", expectedUCFG);
  }

  @Test
  public void void_method_with_flow() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable var1 = UCFGBuilder.variableWithId("%1");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)V").addMethodParam(arg)
      .addStartingBlock(newBasicBlock("1").assignTo(var1, call("java.lang.String#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 1,72,1,86))
        .jumpTo(UCFGBuilder.createLabel("0")))
      .addStartingBlock(newBasicBlock("2").assignTo(var0, call("java.lang.String#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 1,54,1,68))
        .jumpTo(UCFGBuilder.createLabel("1")))
      .addBasicBlock(newBasicBlock("0").ret(constant("implicit return"), new LocationInFile(FILE_KEY, 1,87,1,88)))
      .build();
    assertCodeToUCfg("class A { public void method(String arg) { if(cond) { arg.toString(); } arg.toString();}}", expectedUCFG);
  }

  @Test
  public void create_assign_call_for_method_invocation() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/Object;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1").assignTo(var, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 1,44,1,58))
        .ret(var, new LocationInFile(FILE_KEY, 1,37,1,59)))
      .build();
    assertCodeToUCfg("class A { String method(Object arg) {return arg.toString();}}", expectedUCFG);
  }

  @Test
  public void invocation_on_string() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable temp = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(temp, call("java.lang.String#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 1, 45, 1, 59))
            .ret(temp, new LocationInFile(FILE_KEY, 1, 38, 1, 60)))
        .build();
    assertCodeToUCfg("class A { String method(String arg) { return arg.toString();}}", expectedUCFG);
  }

  @Test
  public void invocation_on_object() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable temp = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/Object;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1")
          .assignTo(temp, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 1,62,1,76))
          .ret(temp, new LocationInFile(FILE_KEY, 1, 55, 1, 77)))
      .build();
    assertCodeToUCfg("class A { String method(Object arg) {int var; var = 2; return arg.toString(); }}", expectedUCFG);
  }

  @Test
  public void build_concatenate_elements() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1").assignTo(var, call("__concat").withArgs(constant("Myconst"), arg), new LocationInFile(FILE_KEY, 1,43,1,56))
        .ret(var,new LocationInFile(FILE_KEY, 1,36,1,57)))
      .build();
    assertCodeToUCfg("class A {String method(String arg) {return \"Myconst\"+arg;}}", expectedUCFG);

    expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1").assignTo(arg, call("__concat").withArgs(arg, constant("someConst")), new LocationInFile(FILE_KEY, 1,43,1,59))
        .ret(arg, new LocationInFile(FILE_KEY, 1,36,1,60)))
      .build();
    assertCodeToUCfg("class A {String method(String arg) {return arg+=\"someConst\";}}", expectedUCFG);
  }

  @Test
  public void build_concatenate_heterogenous_elements() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(aux0, call("__concat").withArgs(constant("\"\""), arg), new LocationInFile(FILE_KEY, 1,43,1,53))
            .assignTo(aux1, call("__concat").withArgs(aux0, constant("\"\"")), new LocationInFile(FILE_KEY, 1,43,1,58))
            .ret(aux1,new LocationInFile(FILE_KEY, 1,36,1,59)))
        .build();
    assertCodeToUCfg("class A {String method(String arg) {return true + arg + 42;}}", expectedUCFG);

    expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1").assignTo(arg, call("__concat").withArgs(arg, constant("\"\"")), new LocationInFile(FILE_KEY, 1,43,1,50))
            .ret(arg, new LocationInFile(FILE_KEY, 1,36,1,51)))
        .build();
    assertCodeToUCfg("class A {String method(String arg) {return arg+=42;}}", expectedUCFG);
  }

  @Test
  public void build_parameter_annotations() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    Expression.Variable aux2 = UCFGBuilder.variableWithId("%2");

    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("paramAnnotations")
        .assignTo(aux0, call("javax.annotation.Nullable").withArgs(arg), new LocationInFile(FILE_KEY, 1, 24, 1, 50))
        .assignTo(aux1, call("org.springframework.web.bind.annotation.RequestParam").withArgs(arg), new LocationInFile(FILE_KEY, 1, 51, 1, 106))
        .assignTo(aux2, call("org.springframework.format.annotation.DateTimeFormat").withArgs(arg), new LocationInFile(FILE_KEY, 1, 107, 1, 160))
        .assignTo(arg, call("__annotation").withArgs(aux0, aux1, aux2), new LocationInFile(FILE_KEY, 1, 168, 1, 171))
        .jumpTo(UCFGBuilder.createLabel("1")))
      .addBasicBlock(
        newBasicBlock("1")
          .ret(constant("foo"), new LocationInFile(FILE_KEY, 1, 175, 1, 188)))
      .build();
    assertCodeToUCfg("class A { String method(@javax.annotation.Nullable @org.springframework.web.bind.annotation.RequestParam() @org.springframework.format.annotation.DateTimeFormat String arg) { return \"foo\";}}", expectedUCFG);
  }

  @Test
  public void unknown_method() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    Expression.Variable aux2 = UCFGBuilder.variableWithId("%2");

    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/util/Set;)V").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1")
        .assignTo(aux0, call("java.util.Collection#stream()Ljava/util/stream/Stream;").withArgs(arg), new LocationInFile(FILE_KEY, 6, 4, 6, 16))
        .assignTo(aux1, call("java.util.stream.Stream#flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;").withArgs(aux0, constant("\"\"")), new LocationInFile(FILE_KEY, 6, 4, 7, 34))
        .assignTo(aux2,
            call("java.util.stream.Collectors#toCollection(Ljava/util/function/Supplier;)Ljava/util/stream/Collector;").withArgs(new Expression.ClassName("java.util.stream.Collectors"), constant("\"\"")),
            new LocationInFile(FILE_KEY, 8, 15, 8, 58))
        .jumpTo(UCFGBuilder.createLabel("0")))
      .addBasicBlock(newBasicBlock("0")
        .ret(constant("implicit return"), new LocationInFile(FILE_KEY, 9, 2, 9, 3)))
      .build();

    String source = "import java.util.Set;\n" +
      "import java.util.Collection;\n" +
      "import java.util.stream.Collectors;\n" +
      "public class A {\n" +
      "  void method(Set<String> arg) { \n" +
      "    arg.stream()\n" +
      "      .flatMap(Collection::stream)\n" +
      "      .collect(Collectors.toCollection(LinkedHashSet::new)); \n" +
      "  }\n" +
      "}";

    // Semantic model creates 2 kinds of "unknown" symbols: "Symbols.unknownSymbol" and "JavaSymbolNotFound"
    // We need to test first case
    // To make sure that code contains "Symbols.unknownSymbol" this assertion is there
    // Note that if Semantic model is improved somehow that "Symbols.unknownSymbol" is not anymore generated in this case
    // this test might be removed
    assertUnknownMethodCalled(source);
    assertCodeToUCfg(source, expectedUCFG);
  }

  @Test
  public void explicit_usage_of_this() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#foo()Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(aux0, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(Expression.THIS), new LocationInFile(FILE_KEY, 3, 11, 3, 26))
            .ret(aux0, new LocationInFile(FILE_KEY, 3, 4, 3, 27)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  private String foo() { \n" +
        "    return this.toString();\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void ignore_usage_of_super() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#foo()Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(aux0, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(constant("\"\"")), new LocationInFile(FILE_KEY, 3, 11, 3, 27))
            .ret(aux0, new LocationInFile(FILE_KEY, 3, 4, 3, 28)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  private String foo() { \n" +
        "    return super.toString();\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void create_multiple_auxiliaries_for_same_expression() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    Expression.Variable aux2 = UCFGBuilder.variableWithId("%2");
    Expression.Variable aux3 = UCFGBuilder.variableWithId("%3");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#foo(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(aux0, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(constant("\"\"")), new LocationInFile(FILE_KEY, 3, 4, 3, 20))
            .assignTo(aux1, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(constant("\"\"")), new LocationInFile(FILE_KEY, 4, 4, 4, 20))
            .assignTo(aux2, call("__concat").withArgs(constant("\"\""), arg), new LocationInFile(FILE_KEY, 5,15,5,25))
            .assignTo(aux3, call("__concat").withArgs(constant("\"\""), arg), new LocationInFile(FILE_KEY, 6,15,6,25))
            .ret(constant(""), new LocationInFile(FILE_KEY, 7, 4, 7, 14)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  private String foo(String arg) { \n" +
        "    super.toString();\n" +
        "    super.toString();\n" +
        "    String x = true + arg;\n" +
        "    String y = true + arg;\n" +
        "    return \"\";\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void implicit_usage_of_this() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#foo()Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(aux0, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(Expression.THIS), new LocationInFile(FILE_KEY, 3, 11, 3, 21))
            .ret(aux0, new LocationInFile(FILE_KEY, 3, 4, 3, 22)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  private String foo() { \n" +
        "    return toString();\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void explicit_static_method_call() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/Integer;)Ljava/lang/String;")
        .addStartingBlock(
            newBasicBlock("1")
                .assignTo(aux0, call("java.util.Objects#toString(Ljava/lang/Object;)Ljava/lang/String;").withArgs(new Expression.ClassName("java.util.Objects"), arg),
                    new LocationInFile(FILE_KEY, 3,11,3,42))
                .ret(aux0, new LocationInFile(FILE_KEY, 3,4,3,43)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  String method(Integer arg) {\n" +
        "    return java.util.Objects.toString(arg);\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void ignore_primitives_and_interfaces() {
    Expression.Variable arg = UCFGBuilder.variableWithId("foo");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#foo(Ljava/lang/String;)V").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("0")
            .ret(constant("implicit return"), new LocationInFile(FILE_KEY, 7, 2, 7, 3)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  private void foo(String foo) { \n" +
        "    int x = 1 + 1;\n" +
        "    x += 1;\n" +
        "    Consumer<String> c = s -> System.out.println(s);\n" +
        "    c.accept(\"foo\");\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void build_two_parameters_annotations() {
    Expression.Variable arg0 = UCFGBuilder.variableWithId("arg0");
    Expression.Variable arg1 = UCFGBuilder.variableWithId("arg1");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");

    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;").addMethodParam(arg0).addMethodParam(arg1)
      .addBasicBlock(newBasicBlock("paramAnnotations")
        .assignTo(aux0, call("javax.annotation.Nullable").withArgs(arg0), new LocationInFile(FILE_KEY, 1, 24, 1, 50))
        .assignTo(arg0, call("__annotation").withArgs(aux0), new LocationInFile(FILE_KEY, 1, 58, 1, 62))
        .assignTo(aux1, call("javax.annotation.Nullable").withArgs(arg1), new LocationInFile(FILE_KEY, 1, 64, 1, 90))
        .assignTo(arg1, call("__annotation").withArgs(aux1), new LocationInFile(FILE_KEY, 1, 99, 1, 103))
        .jumpTo(UCFGBuilder.createLabel("1")))
      .addBasicBlock(
        newBasicBlock("1")
          .ret(constant("foo"), new LocationInFile(FILE_KEY, 1, 107, 1, 120)))
      .build();
    assertCodeToUCfg("class A { String method(@javax.annotation.Nullable String arg0, @javax.annotation.Nullable Integer arg1) { return \"foo\";}}", expectedUCFG);
  }

  @Test
  public void build_assignment_for_object() {
    Expression.Variable arg1 = UCFGBuilder.variableWithId("arg1");
    Expression.Variable arg2 = UCFGBuilder.variableWithId("arg2");
    Expression.Variable var = UCFGBuilder.variableWithId("var");
    Expression.Variable aux = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/String;").addMethodParam(arg1)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(var, call("__id").withArgs(arg1), new LocationInFile(FILE_KEY, 1,50,1,68))
            .assignTo(var, call("__id").withArgs(arg2), new LocationInFile(FILE_KEY, 1,87,1,97))
            .assignTo(aux, call("java.lang.Object#toString()Ljava/lang/String;").withArgs(var), new LocationInFile(FILE_KEY, 1,117,1,131))
            .ret(aux, new LocationInFile(FILE_KEY, 1,110,1,132)))
        .build();

    assertCodeToUCfg("class A {String method(Object arg1, Object arg2) {Object var = arg1; int foo; int bar; var = arg2; foo = bar; return var.toString();}}", expectedUCFG);
  }

  @Test
  public void build_assignment_for_string() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable arg2 = UCFGBuilder.variableWithId("arg2");
    Expression.Variable var1 = UCFGBuilder.variableWithId("var1");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg).addMethodParam(arg2)
      .addBasicBlock(newBasicBlock("1")
        .assignTo(var1, call("__id").withArgs(arg), new LocationInFile(FILE_KEY, 1,49,1,67))
        .assignTo(var1, call("__id").withArgs(arg2), new LocationInFile(FILE_KEY, 1,88,1,99))
        .ret(var1, new LocationInFile(FILE_KEY, 1,114,1,126)))
      .build();
    assertCodeToUCfg("class A {String method(String arg, String arg2) {String var1 = arg; int var2; int var3; var1 = arg2; var2 = var3; return var1;}}", expectedUCFG);
  }

  @Test
  public void variable_declaration_without_initializer() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var1 = UCFGBuilder.variableWithId("var1");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
        .addBasicBlock(newBasicBlock("1")
            .assignTo(var1, call("__id").withArgs(variableWithId("\"\"")), new LocationInFile(FILE_KEY, 1,36,1,48))
            .assignTo(var1, call("__id").withArgs(arg), new LocationInFile(FILE_KEY, 1,49,1,59))
            .ret(var1, new LocationInFile(FILE_KEY, 1,61,1,73)))
        .build();
    assertCodeToUCfg("class A {String method(String arg) {String var1; var1 = arg; return var1;}}", expectedUCFG);
  }

  @Test
  public void constructor_with_return() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#<init>(Ljava/lang/String;)V").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1")
        .ret(constant("\"\""), new LocationInFile(FILE_KEY, 3,4,3,11)))
      .build();
    assertCodeToUCfg("class A { \n" +
      "  A(String arg) { \n" +
      "    return;\n" +
      "  }\n" +
      "}", expectedUCFG);
  }

  @Test
  public void location_in_source_file_should_be_preserved() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1")
        .assignTo(var, call("java.lang.String#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 2,0,2,14))
        .ret(arg, new LocationInFile(FILE_KEY, 3, 2, 3, 13)))
      .build();
    assertCodeToUCfgAndLocations("class A { String method(String arg) {\narg.toString();\n  return arg;\n}}", expectedUCFG);
  }

  @Test
  public void basic_block_location() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/String;").addMethodParam(arg)
      .addBasicBlock(newBasicBlock("1").ret(arg, new LocationInFile(FILE_KEY, 3,5,3,16)))
      .build();
    UCFG ucfg = assertCodeToUCfg("class A {\n  String method(String arg) {\n     return arg;\n}}", expectedUCFG);
    assertThat(ucfg.entryBlocks()).hasSize(1);
    assertThat(ucfg.entryBlocks().iterator().next().locationInFile()).isEqualTo(new LocationInFile(FILE_KEY, 3, 12, 3, 15));
  }

  @Test
  public void string_length_invocation() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)I").addMethodParam(arg)
      .addStartingBlock(newBasicBlock("1").assignTo(var0, call("java.lang.String#length()I").withArgs(arg), new LocationInFile(FILE_KEY, 1,41,1,53))
        .ret(constant("\"\""), new LocationInFile(FILE_KEY, 1,34,1,54)))
      .build();
    assertCodeToUCfg("class A { int method(String arg) {return arg.length();}}", expectedUCFG);
  }

  @Test
  public void object_to_string_invocation() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/Integer;)Ljava/lang/String;").addMethodParam(arg)
      .addStartingBlock(newBasicBlock("1").assignTo(var0, call("java.lang.Integer#toString()Ljava/lang/String;").withArgs(arg), new LocationInFile(FILE_KEY, 1, 45, 1, 59))
        .ret(var0, new LocationInFile(FILE_KEY, 1, 38, 1, 60)))
      .build();
    assertCodeToUCfg("class A { String method(Integer arg) {return arg.toString();}}", expectedUCFG);
  }

  @Test
  public void static_method_call_without_object() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable var0 = UCFGBuilder.variableWithId("%0");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/Integer;)I").addMethodParam(arg)
      .addStartingBlock(
        newBasicBlock("1")
          .assignTo(var0, call("java.lang.String#valueOf(Ljava/lang/Object;)Ljava/lang/String;").withArgs(new Expression.ClassName("java.lang.String"), arg),
            new LocationInFile(FILE_KEY, 4,11,4,23))
        .ret(constant("\"\""), new LocationInFile(FILE_KEY, 4,4,4,24)))
      .build();
    assertCodeToUCfg("import static java.lang.String.valueOf; \n" +
      "class A { \n" +
      "  int method(Integer arg) {\n" +
      "    return valueOf(arg);\n" +
      "  }\n" +
      "}", expectedUCFG);
  }

  @Test
  public void constructor_call_as_initializer() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable instance = UCFGBuilder.variableWithId("instance");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");

    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/Integer;").addMethodParam(arg)
      .addStartingBlock(newBasicBlock("1")
        .newObject(aux0, "java.lang.Integer", new LocationInFile(FILE_KEY, 3, 27, 3, 34))
        .assignTo(aux1, call("java.lang.Integer#<init>(Ljava/lang/String;)V").withArgs(aux0, arg), new LocationInFile(FILE_KEY, 3,23,3,39))
        .assignTo(instance, call("__id").withArgs(aux0), new LocationInFile(FILE_KEY, 3,4,3,40))
        .ret(instance, new LocationInFile(FILE_KEY, 4,4,4,20)))
      .build();
    assertCodeToUCfg("class A { \n" +
      "  Integer method(String arg) {\n" +
      "    Integer instance = new Integer(arg);\n" +
      "    return instance;\n" +
      "  }\n" +
      "}", expectedUCFG);
  }

  @Test
  public void constructor_calls_assigned_to_same_local_variable() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable instance = UCFGBuilder.variableWithId("instance");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    Expression.Variable aux2 = UCFGBuilder.variableWithId("%2");
    Expression.Variable aux3 = UCFGBuilder.variableWithId("%3");

    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/Integer;").addMethodParam(arg)
        .addStartingBlock(newBasicBlock("1")
            .assignTo(instance, call("__id").withArgs(variableWithId("\"\"")), new LocationInFile(FILE_KEY, 3,4,3,21))
            .newObject(aux0, "java.lang.Integer", new LocationInFile(FILE_KEY, 4, 19, 4, 26))
            .assignTo(aux1, call("java.lang.Integer#<init>(Ljava/lang/String;)V").withArgs(aux0, arg), new LocationInFile(FILE_KEY, 4,15,4,31))
            .assignTo(instance, call("__id").withArgs(aux0), new LocationInFile(FILE_KEY, 4,4,4,31))
            .newObject(aux2, "java.lang.Integer", new LocationInFile(FILE_KEY, 5, 19, 5, 26))
            .assignTo(aux3, call("java.lang.Integer#<init>(Ljava/lang/String;)V").withArgs(aux2, arg), new LocationInFile(FILE_KEY, 5,15,5,31))
            .assignTo(instance, call("__id").withArgs(aux2), new LocationInFile(FILE_KEY, 5,4,5,31))
            .ret(instance, new LocationInFile(FILE_KEY, 6,4,6,20)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  Integer method(String arg) {\n" +
        "    Integer instance;\n" +
        "    instance = new Integer(arg);\n" +
        "    instance = new Integer(arg);\n" +
        "    return instance;\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void constructor_call_inside_constructor_call() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable instance = UCFGBuilder.variableWithId("instance");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    Expression.Variable aux2 = UCFGBuilder.variableWithId("%2");
    Expression.Variable aux3 = UCFGBuilder.variableWithId("%3");

    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/Integer;").addMethodParam(arg)
        .addStartingBlock(newBasicBlock("1")
            .newObject(aux0, "java.lang.Integer", new LocationInFile(FILE_KEY, 3, 39, 3, 46))
            .assignTo(aux1, call("java.lang.Integer#<init>(Ljava/lang/String;)V").withArgs(aux0, arg), new LocationInFile(FILE_KEY, 3,35,3,51))
            .newObject(aux2, "java.lang.Integer", new LocationInFile(FILE_KEY, 3, 27, 3, 34))
            .assignTo(aux3, call("java.lang.Integer#<init>(I)V").withArgs(aux2, aux0), new LocationInFile(FILE_KEY, 3,23,3,52))
            .assignTo(instance, call("__id").withArgs(aux2), new LocationInFile(FILE_KEY, 3,4,3,53))
            .ret(instance, new LocationInFile(FILE_KEY, 4,4,4,20)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  Integer method(String arg) {\n" +
        "    Integer instance = new Integer(new Integer(arg));\n" +
        "    return instance;\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  @Test
  public void constructor_call_returned() {
    Expression.Variable arg = UCFGBuilder.variableWithId("arg");
    Expression.Variable aux0 = UCFGBuilder.variableWithId("%0");
    Expression.Variable aux1 = UCFGBuilder.variableWithId("%1");
    UCFG expectedUCFG = UCFGBuilder.createUCFGForMethod("A#method(Ljava/lang/String;)Ljava/lang/Integer;").addMethodParam(arg)
        .addStartingBlock(newBasicBlock("1")
            .newObject(aux0, "java.lang.Integer", new LocationInFile(FILE_KEY, 3, 15, 3, 22))
            .assignTo(aux1, call("java.lang.Integer#<init>(Ljava/lang/String;)V").withArgs(aux0, arg), new LocationInFile(FILE_KEY, 3,11,3,27))
            .ret(aux0, new LocationInFile(FILE_KEY, 3,4,3,28)))
        .build();
    assertCodeToUCfg("class A { \n" +
        "  Integer method(String arg) {\n" +
        "    return new Integer(arg);\n" +
        "  }\n" +
        "}", expectedUCFG);
  }

  private void assertUnknownMethodCalled(String source) {
    CompilationUnitTree cut = getCompilationUnitTreeWithSemantics(source);
    UnknownMethodVisitor unknownMethodVisitor = new UnknownMethodVisitor();
    unknownMethodVisitor.visitCompilationUnit(cut);
    assertThat(unknownMethodVisitor.unknownMethodCount).isGreaterThan(0);
  }

  private void assertCodeToUCfgAndLocations(String source, UCFG expectedUCFG) {
    assertCodeToUCfg(source, expectedUCFG, true);
  }

  private UCFG assertCodeToUCfg(String source, UCFG expectedUCFG) {
    return assertCodeToUCfg(source, expectedUCFG, false);
  }

  private UCFG assertCodeToUCfg(String source, UCFG expectedUCFG, boolean testLocations) {
    UCFG actualUCFG = createUCFG(source);
    assertThat(actualUCFG.methodId()).isEqualTo(expectedUCFG.methodId());
    assertThat(actualUCFG.basicBlocks()).isEqualTo(expectedUCFG.basicBlocks());
    assertThat(actualUCFG.basicBlocks().values().stream().flatMap(b->b.instructions().stream()))
        .containsExactlyElementsOf(expectedUCFG.basicBlocks().values().stream().flatMap(b->b.instructions().stream()).collect(Collectors.toList()));
    assertThat(actualUCFG.basicBlocks().values().stream().map(BasicBlock::terminator))
        .containsExactlyElementsOf(expectedUCFG.basicBlocks().values().stream().map(BasicBlock::terminator).collect(Collectors.toList()));
    assertThat(actualUCFG.entryBlocks()).isEqualTo(expectedUCFG.entryBlocks());
    assertThat(toLocationStream(actualUCFG).noneMatch(l->l == UCFGBuilder.LOC)).isTrue();
    if(testLocations) {
      Stream<LocationInFile> locStream = toLocationStream(actualUCFG);
      assertThat(locStream).containsExactlyElementsOf(toLocationStream(expectedUCFG).collect(Collectors.toList()));
    }
    return actualUCFG;
  }

  private UCFG createUCFG(String source) {
    File java_ucfg_dir = new File(new File(tmp.getRoot(), "ucfg"), "java");
    if(java_ucfg_dir.isDirectory()) {
      for (File file : java_ucfg_dir.listFiles()) {
        file.delete();
      }
    }
    CompilationUnitTree cut = getCompilationUnitTreeWithSemantics(source);
    UCFGJavaVisitor UCFGJavaVisitor = new UCFGJavaVisitor(tmp.getRoot());
    UCFGJavaVisitor.javaFileKey = FILE_KEY;
    UCFGJavaVisitor.visitCompilationUnit(cut);

    UCFG actualUCFG = null;
    try {
      File ucfg = new File(java_ucfg_dir, "ucfg_0.proto");
      actualUCFG = UCFGtoProtobuf.fromProtobufFile(ucfg);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return actualUCFG;
  }

  @Test
  public void test_index_initialization() {
    File java_ucfg_dir = new File(new File(tmp.getRoot(), "ucfg"), "java");
    if(java_ucfg_dir.isDirectory()) {
      for (File file : java_ucfg_dir.listFiles()) {
        file.delete();
      }
    }
    CompilationUnitTree cut = getCompilationUnitTreeWithSemantics("class A {String fun() {return \"\";}} ");
    UCFGJavaVisitor UCFGJavaVisitor = new UCFGJavaVisitor(tmp.getRoot());
    UCFGJavaVisitor.javaFileKey = FILE_KEY;
    UCFGJavaVisitor.visitCompilationUnit(cut);

    UCFGJavaVisitor = new UCFGJavaVisitor(tmp.getRoot());
    UCFGJavaVisitor.javaFileKey = FILE_KEY;
    UCFGJavaVisitor.visitCompilationUnit(cut);

    String[] list = java_ucfg_dir.list();
    assertThat(list).hasSize(2).containsExactlyInAnyOrder("ucfg_0.proto", "ucfg_1.proto");

  }

  @Test
  public void method_with_unknown_symbol_should_not_produce_ucfg() {
    UCFGJavaVisitor UCFGJavaVisitor = new UCFGJavaVisitor(tmp.getRoot()) {
      @Override
      protected void serializeUCFG(MethodTree tree, CFG cfg) {
        fail("should not serialize a UCFG of a method with unknown parameters");
      }
    };
    UCFGJavaVisitor.javaFileKey = FILE_KEY;
    // method with unknown arg
    CompilationUnitTree cut = getCompilationUnitTreeWithSemantics("class A {void foo(UnkownType t) {return t;}}");
    UCFGJavaVisitor.visitCompilationUnit(cut);

    // method with unkown return type
    cut = getCompilationUnitTreeWithSemantics("class A {UnkownType foo(String t) {return t;}}");
    UCFGJavaVisitor.visitCompilationUnit(cut);
  }

  @Test
  public void ucfg_requires_semantic() {
    UCFGJavaVisitor UCFGJavaVisitor = Mockito.spy(new UCFGJavaVisitor(tmp.getRoot()) {
      @Override
      protected void serializeUCFG(MethodTree tree, CFG cfg) {
        // do nothing
      }
    });
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse("class A {void foo(String t) {return t;}}");
    SemanticModel semanticModel = SemanticModel.createFor(cut, squidClassLoader);

    JavaFileScannerContext context = Mockito.mock(JavaFileScannerContext.class);
    Mockito.when(context.getTree()).thenReturn(cut);
    Mockito.when(context.getSemanticModel()).thenReturn(semanticModel);

    UCFGJavaVisitor.scanFile(context);

    Mockito.verify(UCFGJavaVisitor, Mockito.times(1)).serializeUCFG(Mockito.any(), Mockito.any());
  }

  @Test
  public void no_ucfg_without_semantic() {
    UCFGJavaVisitor UCFGJavaVisitor = new UCFGJavaVisitor(tmp.getRoot()) {
      @Override
      protected void serializeUCFG(MethodTree tree, CFG cfg) {
        fail("should not serialize a UCFG whout semantic");
      }
    };
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse("class A {void foo(String t) {return t;}}");

    JavaFileScannerContext context = Mockito.mock(JavaFileScannerContext.class);
    Mockito.when(context.getTree()).thenReturn(cut);
    Mockito.when(context.getSemanticModel()).thenReturn(null);

    UCFGJavaVisitor.scanFile(context);
  }

  @Test
  public void null_literal_should_produce_a_constant_expression() {
    UCFG ucfg = createUCFG("class A {String foo(String s) {return foo(null);}}");
    BasicBlock basicBlock = ucfg.entryBlocks().iterator().next();
    // first argument is "this"
    Expression argExpression = ((Instruction.AssignCall)basicBlock.instructions().get(0)).getArgExpressions().get(1);
    assertThat(argExpression.isConstant()).isTrue();
  }

  @Test
  public void string_literal_should_produce_a_constant_expression() {
    UCFG ucfg = createUCFG("class A {String foo(String s) {return foo(\"plop\");}}");
    BasicBlock basicBlock = ucfg.entryBlocks().iterator().next();
    // first argument is "this"
    Expression argExpression = ((Instruction.AssignCall)basicBlock.instructions().get(0)).getArgExpressions().get(1);
    assertThat(argExpression.isConstant()).isTrue();
  }

  @Test
  public void constructors_should_have_a_ucfg() {
    UCFG ucfg = createUCFG("class A { Object foo(String s) {new A(s); new Object(); new Unknown(\"\"); return new String();} A(String s) {} }");
    assertThat(ucfg.methodId()).isEqualTo("A#foo(Ljava/lang/String;)Ljava/lang/Object;");
    List<Instruction> calls = ucfg.entryBlocks().iterator().next().instructions();
    assertThat(calls).hasSize(6);
    Instruction.NewObject newInstance0 = (Instruction.NewObject)calls.get(0);
    Instruction.AssignCall newInstanceAssignCall0 = (Instruction.AssignCall)calls.get(1);
    assertThat(newInstance0.instanceType()).isEqualTo("A");
    assertThat(newInstanceAssignCall0.getMethodId()).isEqualTo("A#<init>(Ljava/lang/String;)V");
    Instruction.NewObject newInstance1 = (Instruction.NewObject)calls.get(2);
    Instruction.AssignCall newInstanceAssignCall1 = (Instruction.AssignCall)calls.get(3);
    assertThat(newInstance1.instanceType()).isEqualTo("java.lang.Object");
    assertThat(newInstanceAssignCall1.getMethodId()).isEqualTo("java.lang.Object#<init>()V");
    Instruction.NewObject newInstance2 = (Instruction.NewObject)calls.get(4);
    Instruction.AssignCall newInstanceAssignCall2 = (Instruction.AssignCall)calls.get(5);
    assertThat(newInstance2.instanceType()).isEqualTo("java.lang.String");
    assertThat(newInstanceAssignCall2.getMethodId()).isEqualTo("java.lang.String#<init>()V");
  }

  private CompilationUnitTree getCompilationUnitTreeWithSemantics(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(source);
    SemanticModel.createFor(cut, squidClassLoader);
    return cut;
  }

  private Stream<LocationInFile> toLocationStream(UCFG UCFG) {
    return UCFG.basicBlocks().values().stream().flatMap(b -> Stream.concat(b.instructions().stream().map(Instruction::location), Stream.of(b.terminator().location())));
  }

  private static class UnknownMethodVisitor extends BaseTreeVisitor {

    int unknownMethodCount = 0;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.symbol().equals(Symbols.unknownSymbol)) {
        unknownMethodCount++;
      }
      super.visitMethodInvocation(tree);
    }
  }
}
