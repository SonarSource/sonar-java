/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.fest.assertions.ObjectAssert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeAndReferenceSolverTest {

  private final ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
  private final BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")), parametrizedTypeCache);
  private final Symbols symbols = new Symbols(bytecodeCompleter);

  private Resolve.Env env;

  private JavaSymbol.TypeJavaSymbol classSymbol;
  private JavaType.ClassJavaType classType;

  private JavaSymbol variableSymbol;
  private JavaSymbol methodSymbol;
  private JavaSymbol argMethodSymbol;

  /**
   * Simulates creation of symbols and types.
   */
  @Before
  public void setUp() {
    JavaSymbol.PackageJavaSymbol p = symbols.defaultPackage;
    p.members = new Scope(p);
    // class MyClass
    classSymbol = new JavaSymbol.TypeJavaSymbol(0, "MyClass", p);
    classType = (JavaType.ClassJavaType) classSymbol.type;
    classType.supertype = symbols.objectType;
    classType.interfaces = ImmutableList.of();
    classSymbol.members = new Scope(classSymbol);
    p.members.enter(classSymbol);
    // int[][] var;
    variableSymbol = new JavaSymbol.VariableJavaSymbol(0, "var", classSymbol);
    variableSymbol.type = new JavaType.ArrayJavaType(new JavaType.ArrayJavaType(symbols.intType, symbols.arrayClass), symbols.arrayClass);
    classSymbol.members.enter(variableSymbol);

    // MyClass var2;
    classSymbol.members.enter(new JavaSymbol.VariableJavaSymbol(0, "var2", classType, classSymbol));

    // int method()
    methodSymbol = new JavaSymbol.MethodJavaSymbol(0, "method", classSymbol);
    ((JavaSymbol.MethodJavaSymbol)methodSymbol).setMethodType(new JavaType.MethodJavaType(ImmutableList.<JavaType>of(), symbols.intType, ImmutableList.<JavaType>of(), classSymbol));
    classSymbol.members.enter(methodSymbol);

    // int method()
    argMethodSymbol = new JavaSymbol.MethodJavaSymbol(0, "argMethod", classSymbol);
    ((JavaSymbol.MethodJavaSymbol)argMethodSymbol).setMethodType(new JavaType.MethodJavaType(ImmutableList.of(symbols.intType), symbols.intType, ImmutableList.<JavaType>of(), classSymbol));
    classSymbol.members.enter(argMethodSymbol);

    classSymbol.members.enter(new JavaSymbol.VariableJavaSymbol(0, "this", classType, classSymbol));
    classSymbol.members.enter(new JavaSymbol.VariableJavaSymbol(0, "super", classType.supertype, classSymbol));

    // FIXME figure out why top is mandatory
    Resolve.Env top = new Resolve.Env();
    top.scope = new Scope((JavaSymbol) null);

    Resolve.Env compilationUnitEnv = new Resolve.Env();
    compilationUnitEnv.outer = top;
    compilationUnitEnv.packge = p;
    compilationUnitEnv.scope = p.members;
    compilationUnitEnv.enclosingClass = symbols.predefClass;
    compilationUnitEnv.namedImports = new Scope(p);
    compilationUnitEnv.starImports = new Scope(p);
    compilationUnitEnv.staticStarImports = new Scope(p);

    env = compilationUnitEnv.dup();
    env.outer = compilationUnitEnv;
    env.enclosingClass = classSymbol;
    env.scope = classSymbol.members;
  }

  @Test
  public void annotation_on_method() {
    CompilationUnitTree compilationUnit = treeOf("@interface MyAnnotation { } class Class { @MyAnnotation void method() { } }");
    ClassTreeImpl annotation = (ClassTreeImpl) compilationUnit.types().get(0);
    ClassTreeImpl clazz = (ClassTreeImpl) compilationUnit.types().get(1);
    MethodTreeImpl method = (MethodTreeImpl) clazz.members().get(0);
    List<AnnotationInstance> annotations = ((JavaSymbol.MethodJavaSymbol) method.symbol()).metadata().annotations();
    assertThat(annotations.size()).isEqualTo(1);
    assertThat(annotations.get(0).symbol().type().is(annotation.symbol().name())).isTrue();
  }

  @Test
  public void annotation_on_method_parameter() {
    CompilationUnitTree compilationUnit = treeOf("@interface MyAnnotation { } class Class { void method(@MyAnnotation int a) { } }");
    ClassTreeImpl annotation = (ClassTreeImpl) compilationUnit.types().get(0);
    ClassTreeImpl clazz = (ClassTreeImpl) compilationUnit.types().get(1);
    MethodTreeImpl method = (MethodTreeImpl) clazz.members().get(0);
    VariableTreeImpl  parameter = (VariableTreeImpl)method.parameters().get(0);
    List<AnnotationInstance> annotations = parameter.getSymbol().metadata().annotations();
    assertThat(annotations.size()).isEqualTo(1);
    assertThat(annotations.get(0).symbol().type().is(annotation.symbol().name())).isTrue();
  }

  @Test
  public void annotation_on_type() {
    CompilationUnitTree compilationUnit = treeOf("@interface MyAnnotation { } @MyAnnotation class Class { }");
    ClassTreeImpl annotation = (ClassTreeImpl) compilationUnit.types().get(0);
    ClassTreeImpl clazz = (ClassTreeImpl) compilationUnit.types().get(1);
    List<AnnotationInstance> annotations = ((JavaSymbol.TypeJavaSymbol) clazz.symbol()).metadata().annotations();
    assertThat(annotations.size()).isEqualTo(1);
    assertThat(annotations.get(0).symbol().type().is(annotation.symbol().name())).isTrue();
  }

  @Test
  public void annotation_on_variable() {
    CompilationUnitTree compilationUnit = treeOf("@interface MyAnnotation { } class Class { @MyAnnotation Object field; }");
    ClassTreeImpl annotation = (ClassTreeImpl) compilationUnit.types().get(0);
    ClassTreeImpl clazz = (ClassTreeImpl) compilationUnit.types().get(1);
    VariableTreeImpl variable = (VariableTreeImpl) clazz.members().get(0);
    List<AnnotationInstance> annotations = variable.getSymbol().metadata().annotations();
    assertThat(annotations.size()).isEqualTo(1);
    assertThat(annotations.get(0).symbol().type().is(annotation.symbol().name())).isTrue();
  }

  @Test
  public void annotation_completion() {
    AnnotationInstance annotation1 = extractFirstAnnotationInstance("@interface MyAnnotation { } @MyAnnotation() class Class { }");
    assertThat(annotation1.values()).isEmpty();

    AnnotationInstance annotation2 = extractFirstAnnotationInstance("@interface MyAnnotation { } @MyAnnotation(expr) class Class { }");
    assertThat(annotation2.values().size()).isEqualTo(1);
    assertThat(annotation2.values().get(0).name()).isEqualTo("");

    AnnotationInstance annotation3 = extractFirstAnnotationInstance("@interface MyAnnotation { public static final String field; String value(); } @MyAnnotation(expr) class Class { }");
    assertThat(annotation3.values().size()).isEqualTo(1);
    assertThat(annotation3.values().get(0).name()).isEqualTo("value");

    AnnotationInstance annotation4 = extractFirstAnnotationInstance("@interface MyAnnotation { } @MyAnnotation(expr = val) class Class { }");
    assertThat(annotation4.values().size()).isEqualTo(1);
    assertThat(annotation4.values().get(0).name()).isEqualTo("expr");

    AnnotationInstance annotation5 = extractFirstAnnotationInstance("@interface MyAnnotation { } @MyAnnotation(expr1 = val1, expr2 = val2) class Class { }");
    assertThat(annotation5.values().size()).isEqualTo(2);
    assertThat(annotation5.values().get(0).name()).isEqualTo("expr1");
    assertThat(annotation5.values().get(1).name()).isEqualTo("expr2");
  }

  private AnnotationInstance extractFirstAnnotationInstance(String source) {
    ClassTree tree = (ClassTree) treeOf(source).types().get(1);
    return ((JavaSymbol.TypeJavaSymbol) tree.symbol()).metadata().annotations().get(0);
  }

  @Test
  public void primary_literal() {
    assertThat(typeOf("false")).isSameAs(symbols.booleanType);
    assertThat(typeOf("true")).isSameAs(symbols.booleanType);
    assertThat(typeOf("null")).isSameAs(symbols.nullType);
    assertThat(typeOf("'a'")).isSameAs(symbols.charType);
    assertThat(typeOf("\"foo\"")).isSameAs(symbols.stringType);
    assertThat(typeOf("42F")).isSameAs(symbols.floatType);
    assertThat(typeOf("42D")).isSameAs(symbols.doubleType);
    assertThat(typeOf("42L")).isSameAs(symbols.longType);
    assertThat(typeOf("42")).isSameAs(symbols.intType);
  }

  @Test
  public void primary_this() {
    assertThat(typeOfExpression("this")).isSameAs(classType);

    // constructor call
    assertThat(typeOf("this(arguments)")).isSameAs(symbols.unknownType);
  }

  @Test
  public void primary_super() {
    // constructor call
    assertThat(typeOf("super(arguments)")).isSameAs(symbols.unknownType);

    // method call
    assertThat(typeOf("super.method(1)")).isSameAs(symbols.unknownType);

    // field access
    assertThat(typeOfExpression("super.clone()")).isSameAs(classType.supertype);
  }

  @Test
  public void primary_par_expression() {
    // (expression)
    assertThat(typeOf("((int) 42L)")).isSameAs(symbols.intType);
  }

  @Test
  public void primary_new() {
    assertThat(typeOf("new MyClass()")).isSameAs(classType);
    assertThat(typeOf("new MyClass() {}")).isSameAs(symbols.unknownType);

    // TODO proper implementation of this test requires definition of equality for types
    assertThat(typeOf("new MyClass[]{}")).isInstanceOf(JavaType.ArrayJavaType.class);
    assertThat(typeOf("new int[]{}")).isInstanceOf(JavaType.ArrayJavaType.class);
    assertThat(typeOf("new int[][]{}")).isInstanceOf(JavaType.ArrayJavaType.class);
  }

  @Test
  public void primary_qualified_identifier() {

    // qualified_identifier
    assertThat(typeOfExpression("var")).isSameAs(variableSymbol.type);
    assertThat(typeOfExpression("var.length")).isSameAs(symbols.intType);
    assertThat(typeOfExpression("MyClass.var")).isSameAs(variableSymbol.type);

    // qualified_identifier[expression]
    assertThat(typeOf("var[42] = 12")).isSameAs(((JavaType.ArrayJavaType) variableSymbol.type).elementType);
    assertThat(typeOfExpression("var[42]")).isSameAs(((JavaType.ArrayJavaType) variableSymbol.type).elementType);

    // qualified_identifier[].class
    assertThat(typeOfExpression("id[].class")).isSameAs(symbols.classType);
    assertThat(typeOfExpression("id[][].class")).isSameAs(symbols.classType);

    // qualified_identifier(arguments)
    assertThat(typeOf("argMethod(1)")).isSameAs(symbols.intType);
    assertThat(typeOf("var2.method()")).isSameAs(symbols.intType);
    assertThat(typeOf("MyClass.var2.method()")).isSameAs(symbols.intType);

    // qualified_identifier.class
    assertThat(typeOfExpression("id.class")).isSameAs(symbols.classType);

    // TODO id.<...>...
    assertThat(typeOfExpression("MyClass.this")).isSameAs(classSymbol.type);
    assertThat(typeOf("id.super(arguments)")).isSameAs(symbols.unknownType);
    // TODO id.new...
  }

  @Test
  public void primary_basic_type() {
    assertThat(typeOfExpression("int.class")).isSameAs(symbols.classType);
    assertThat(typeOfExpression("int[].class")).isSameAs(symbols.classType);
    assertThat(typeOfExpression("int[][].class")).isSameAs(symbols.classType);
  }

  @Test
  public void primary_void() {
    assertThat(typeOfExpression("void.class")).isSameAs(symbols.classType);
  }

  @Test
  public void type_cast() {
    // (basic_type) expression
    assertThat(typeOf("(byte) 42L")).isSameAs(symbols.byteType);
    assertThat(typeOf("(char) 42")).isSameAs(symbols.charType);
    assertThat(typeOf("(short) 42L")).isSameAs(symbols.shortType);
    assertThat(typeOf("(int) 42")).isSameAs(symbols.intType);
    assertThat(typeOf("(long) 42")).isSameAs(symbols.longType);
    assertThat(typeOf("(float) 42")).isSameAs(symbols.floatType);
    assertThat(typeOf("(double) 42")).isSameAs(symbols.doubleType);
    assertThat(typeOf("(boolean) true")).isSameAs(symbols.booleanType);

    // (class_type) expression
    assertThat(typeOf("(MyClass) 42")).isSameAs(classSymbol.type);
  }

  @Test
  public void prefix_op() {
    for (String op : Arrays.asList("++", "--", "!", "~", "+", "-")) {
      assertThat(typeOf(op + INT)).as(op + INT).isSameAs(symbols.intType);
    }
  }

  @Test
  public void postfix_op() {
    for (String op : Arrays.asList("++", "--")) {
      assertThat(typeOf(INT + op)).as(INT + op).isSameAs(symbols.intType);
    }
  }

  @Test
  public void selector() {
    // method call
    assertThat(typeOf("this.method()").isTagged(JavaType.INT)).isTrue();
    assertThat(typeOf("this.argMethod(12)").isTagged(JavaType.INT)).isTrue();
    assertThat(typeOf("var[42].clone()")).isSameAs(symbols.objectType);

    // field access
    assertThat(typeOfExpression("this.var")).isSameAs(variableSymbol.type);
    assertThat(typeOfExpression("var[42].length")).isSameAs(symbols.intType);

    // array access
    assertThat(typeOfExpression("var[42][42]")).isSameAs(((JavaType.ArrayJavaType) ((JavaType.ArrayJavaType) variableSymbol.type).elementType).elementType);
  }

  @Test
  public void multiplicative_and_additive_expression() {
    for (String op : Arrays.asList("*", "/", "%", "+", "-")) {
      for (String o1 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
        for (String o2 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
          assertThatTypeOf(o1, op, o2).isSameAs(symbols.intType);
        }
      }
      for (String other : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG)) {
        assertThatTypeOf(LONG, op, other).isSameAs(symbols.longType);
        assertThatTypeOf(other, op, LONG).isSameAs(symbols.longType);
      }
      for (String other : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG, FLOAT)) {
        assertThatTypeOf(FLOAT, op, other).isSameAs(symbols.floatType);
        assertThatTypeOf(other, op, FLOAT).isSameAs(symbols.floatType);
      }
      for (String other : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)) {
        assertThatTypeOf(DOUBLE, op, other).isSameAs(symbols.doubleType);
        assertThatTypeOf(other, op, DOUBLE).isSameAs(symbols.doubleType);
      }
    }

    // TODO
    // string, object = string
    // object, string = string
    for (String other : Arrays.asList(STRING, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL)) {
      assertThatTypeOf(STRING, "+", other).isSameAs(symbols.stringType);
      assertThatTypeOf(other, "+", STRING).isSameAs(symbols.stringType);
    }
    // TODO check that null + null won't produce string - see Javac
  }

  @Test
  public void shift_expression() {
    for (String op : Arrays.asList("<<", ">>", ">>>")) {
      assertThatTypeOf(INT, op, INT).isSameAs(symbols.intType);
      assertThatTypeOf(INT, op, LONG).isSameAs(symbols.intType);
      assertThatTypeOf(LONG, op, LONG).isSameAs(symbols.longType);
      assertThatTypeOf(LONG, op, INT).isSameAs(symbols.longType);
    }
  }

  @Test
  public void relational_expression() {
    for (String op : Arrays.asList("<", ">", ">=", "<=")) {
      for (String o1 : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)) {
        for (String o2 : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)) {
          assertThatTypeOf(o1, op, o2).isSameAs(symbols.booleanType);
        }
      }
    }

    assertThat(typeOf("var instanceof Object")).isSameAs(symbols.booleanType);
  }

  @Test
  public void equality_expression() {
    // TODO object, object = boolean
    for (String op : Arrays.asList("==", "!=")) {
      for (String o1 : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)) {
        for (String o2 : Arrays.asList(CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)) {
          assertThatTypeOf(o1, op, o2).isSameAs(symbols.booleanType);
        }
      }
      assertThatTypeOf(BOOLEAN, op, BOOLEAN).isSameAs(symbols.booleanType);
    }
  }

  @Test
  public void and_expression() {
    assertThatTypeOf(BOOLEAN, "&", BOOLEAN).isSameAs(symbols.booleanType);
    for (String o1 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
      for (String o2 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
        assertThatTypeOf(o1, "&", o2).isSameAs(symbols.intType);
      }
    }
    assertThatTypeOf(LONG, "&", LONG).isSameAs(symbols.longType);
  }

  @Test
  public void exclusive_or_expression() {
    assertThatTypeOf(BOOLEAN, "^", BOOLEAN).isSameAs(symbols.booleanType);
    for (String o1 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
      for (String o2 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
        assertThatTypeOf(o1, "^", o2).isSameAs(symbols.intType);
      }
    }
    assertThatTypeOf(LONG, "^", LONG).isSameAs(symbols.longType);
  }

  @Test
  public void inclusive_or_expression() {
    assertThatTypeOf(BOOLEAN, "|", BOOLEAN).isSameAs(symbols.booleanType);
    for (String o1 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
      for (String o2 : Arrays.asList(CHAR, BYTE, SHORT, INT)) {
        assertThatTypeOf(o1, "|", o2).isSameAs(symbols.intType);
      }
    }
    assertThatTypeOf(LONG, "|", LONG).isSameAs(symbols.longType);
  }

  @Test
  public void conditional_and_expression() {
    assertThatTypeOf(BOOLEAN, "&&", BOOLEAN).isSameAs(symbols.booleanType);
  }

  @Test
  public void conditional_or_expression() {
    assertThatTypeOf(BOOLEAN, "||", BOOLEAN).isSameAs(symbols.booleanType);
  }

  private ObjectAssert assertThatTypeOf(String o1, String op, String o2) {
    return assertThat(typeOf(o1 + op + o2)).as(o1 + op + o2);
  }

  @Test
  public void conditional_expression() {

    // FIXME implement
    assertThat(typeOf("42 ? 42 : 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void lambda_expression() {
    // FIXME implement
    assertThat(typeOf("a -> a+1")).isSameAs(symbols.unknownType);
  }

  @Test
  public void assignment_expression() {
    assertThat(typeOf("var = 1")).isSameAs(variableSymbol.type);
  }

  private static final String CHAR = "(char) 42";
  private static final String BYTE = "(byte) 42";
  private static final String SHORT = "(short) 42";
  private static final String INT = "(int) 42";
  private static final String LONG = "(long) 42";
  private static final String FLOAT = "(float) 42";
  private static final String DOUBLE = "(double) 42";
  private static final String BOOLEAN = "true";
  private static final String NULL = "null";
  private static final String STRING = "\"string\"";

  private CompilationUnitTree treeOf(String input) {
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(input);
    SemanticModel.createFor(tree, ImmutableList.<File>of());
    return tree;
  }

  private JavaType typeOf(String input) {
    SemanticModel semanticModel = mock(SemanticModel.class);
    when(semanticModel.getEnv(any(Tree.class))).thenReturn(env);
    TypeAndReferenceSolver visitor = new TypeAndReferenceSolver(semanticModel, symbols, new Resolve(symbols, bytecodeCompleter, parametrizedTypeCache), parametrizedTypeCache);

    String p = "class Test { void wrapperMethod() { " + input + "; } }";
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(p);
    tree.accept(visitor);

    TestedNodeExtractor testedNodeExtractor = new TestedNodeExtractor(false);
    testedNodeExtractor.visitCompilationUnit(tree);
    return visitor.getType(testedNodeExtractor.testedNode);
  }
  private JavaType typeOfExpression(String input) {
    SemanticModel semanticModel = mock(SemanticModel.class);
    when(semanticModel.getEnv(any(Tree.class))).thenReturn(env);
    TypeAndReferenceSolver visitor = new TypeAndReferenceSolver(semanticModel, symbols, new Resolve(symbols, bytecodeCompleter, parametrizedTypeCache), parametrizedTypeCache);

    String p = "class Test { void wrapperMethod() { Object o = " + input + "; } }";
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(p);
    tree.accept(visitor);

    TestedNodeExtractor testedNodeExtractor = new TestedNodeExtractor(true);
    testedNodeExtractor.visitCompilationUnit(tree);
    return visitor.getType(testedNodeExtractor.testedNode);
  }
  private static class TestedNodeExtractor extends BaseTreeVisitor {
    private final boolean extractExpression;
    private Tree testedNode;

    public TestedNodeExtractor(boolean extractExpression) {

      this.extractExpression = extractExpression;
    }

    @Override
    public void visitMethod(MethodTree tree) {
      super.visitMethod(tree);
      if ("wrapperMethod".equals(tree.simpleName().name())) {
        testedNode = tree.block().body().get(0);
        if(extractExpression && testedNode.is(Tree.Kind.VARIABLE)) {
          testedNode = ((VariableTree)testedNode).initializer();
        }
      }
    }
  }
}
