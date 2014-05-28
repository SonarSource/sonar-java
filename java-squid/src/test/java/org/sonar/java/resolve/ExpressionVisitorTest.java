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
package org.sonar.java.resolve;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import org.fest.assertions.ObjectAssert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTreeMaker;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpressionVisitorTest {

  private LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

  private BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
  private Symbols symbols = new Symbols(bytecodeCompleter);

  private Resolve.Env env;

  private Symbol.TypeSymbol classSymbol;
  private Type.ClassType classType;

  private Symbol variableSymbol;
  private Symbol methodSymbol;

  /**
   * Simulates creation of symbols and types.
   */
  @Before
  public void setUp() {
    Symbol.PackageSymbol p = symbols.defaultPackage;
    p.members = new Scope(p);

    // class MyClass
    classSymbol = new Symbol.TypeSymbol(0, "MyClass", p);
    classType = ((Type.ClassType) classSymbol.type);
    classType.supertype = symbols.unknownType; // TODO extend some superclass
    classType.interfaces = ImmutableList.of();
    classSymbol.members = new Scope(classSymbol);
    p.members.enter(classSymbol);
    // int[][] var;
    variableSymbol = new Symbol.VariableSymbol(0, "var", classSymbol);
    variableSymbol.type = new Type.ArrayType(new Type.ArrayType(symbols.intType, symbols.arrayClass), symbols.arrayClass);
    classSymbol.members.enter(variableSymbol);

    // MyClass var2;
    classSymbol.members.enter(new Symbol.VariableSymbol(0, "var2", classType, classSymbol));

    // int method()
    methodSymbol = new Symbol.MethodSymbol(0, "method", classSymbol);
    methodSymbol.type = new Type.MethodType(ImmutableList.<Type>of(), symbols.intType, ImmutableList.<Type>of(), /* TODO defining class? */ null);
    classSymbol.members.enter(methodSymbol);

    classSymbol.members.enter(new Symbol.VariableSymbol(0, "this", classType, classSymbol));
    classSymbol.members.enter(new Symbol.VariableSymbol(0, "super", classType.supertype, classSymbol));

    // FIXME figure out why top is mandatory
    Resolve.Env top = new Resolve.Env();
    top.scope = new Scope((Symbol) null);

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
    assertThat(typeOf("this")).isSameAs(classType);

    // constructor call
    assertThat(typeOf("this(arguments)")).isSameAs(symbols.unknownType);
  }

  @Test
  public void primary_super() {
    // constructor call
    assertThat(typeOf("super(arguments)")).isSameAs(symbols.unknownType);

    // method call
    assertThat(typeOf("super.method(arguments)")).isSameAs(symbols.unknownType);

    // field access
    assertThat(typeOf("super.field")).isSameAs(classType.supertype);
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
    assertThat(typeOf("new MyClass[]{}")).isInstanceOf(Type.ArrayType.class);
    assertThat(typeOf("new int[]{}")).isInstanceOf(Type.ArrayType.class);
    assertThat(typeOf("new int[][]{}")).isInstanceOf(Type.ArrayType.class);
  }

  @Test
  public void primary_qualified_identifier() {


    // qualified_identifier
    assertThat(typeOf("var")).isSameAs(variableSymbol.type);
    assertThat(typeOf("var.length")).isSameAs(symbols.intType);
    assertThat(typeOf("MyClass.var")).isSameAs(variableSymbol.type);

    // qualified_identifier[expression]
    assertThat(typeOf("var[42]")).isSameAs(((Type.ArrayType) variableSymbol.type).elementType);

    // qualified_identifier[].class
    assertThat(typeOf("id[].class")).isSameAs(symbols.classType);
    assertThat(typeOf("id[][].class")).isSameAs(symbols.classType);

    // qualified_identifier(arguments)
    assertThat(typeOf("method(arguments)")).isSameAs(methodSymbol.type);
    assertThat(typeOf("var2.method()")).isSameAs(methodSymbol.type);
    assertThat(typeOf("MyClass.var2.method()")).isSameAs(methodSymbol.type);

    // qualified_identifier.class
    assertThat(typeOf("id.class")).isSameAs(symbols.classType);

    // TODO id.<...>...
    assertThat(typeOf("MyClass.this")).isSameAs(classSymbol.type);
    assertThat(typeOf("id.super(arguments)")).isSameAs(symbols.unknownType);
    // TODO id.new...
  }

  @Test
  public void primary_basic_type() {
    assertThat(typeOf("int.class")).isSameAs(symbols.classType);
    assertThat(typeOf("int[].class")).isSameAs(symbols.classType);
    assertThat(typeOf("int[][].class")).isSameAs(symbols.classType);
  }

  @Test
  public void primary_void() {
    assertThat(typeOf("void.class")).isSameAs(symbols.classType);
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
    assertThat(typeOf("this.method(arguments)")).isInstanceOf(Type.MethodType.class);//isSameAs(symbols.unknownType);
    assertThat(typeOf("var[42].clone()")).isSameAs(symbols.unknownType);

    // field access
    assertThat(typeOf("this.var")).isSameAs(variableSymbol.type);
    assertThat(typeOf("var[42].length")).isSameAs(symbols.intType);

    // array access
    assertThat(typeOf("var[42][42]")).isSameAs(((Type.ArrayType) ((Type.ArrayType) variableSymbol.type).elementType).elementType);
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

  private Type typeOf(String input) {
    SemanticModel semanticModel = mock(SemanticModel.class);
    when(semanticModel.getEnv(any(Tree.class))).thenReturn(env);
    ExpressionVisitor visitor = new ExpressionVisitor(semanticModel, symbols, new Resolve(symbols, bytecodeCompleter));

    b.setRootRule(JavaGrammar.COMPILATION_UNIT);
    String p = "class Test { void wrapperMethod() { " + input + "; } }";
    AstNode node = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, b.build()).parse(p);
    CompilationUnitTree tree = new JavaTreeMaker().compilationUnit(node);
    tree.accept(visitor);

    TestedNodeExtractor testedNodeExtractor = new TestedNodeExtractor();
    testedNodeExtractor.visitCompilationUnit(tree);
    return visitor.getType(testedNodeExtractor.testedNode);
  }

  private static class TestedNodeExtractor extends BaseTreeVisitor {
    private Tree testedNode;

    @Override
    public void visitMethod(MethodTree tree) {
      super.visitMethod(tree);
      if ("wrapperMethod".equals(tree.simpleName().name())) {
        testedNode = tree.block().body().get(0);
      }
    }
  }
}
