/*
 * Sonar Java
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
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.ast.AstWalker;
import com.sonar.sslr.impl.ast.AstXmlPrinter;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpressionVisitorTest {

  private LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

  private Symbols symbols = new Symbols();

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
    Symbol.PackageSymbol p = new Symbol.PackageSymbol(null, null);
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
    methodSymbol.type = symbols.intType;
    classSymbol.members.enter(methodSymbol);

    classSymbol.members.enter(new Symbol.VariableSymbol(0, "this", classType, classSymbol));
    classSymbol.members.enter(new Symbol.VariableSymbol(0, "super", classType.supertype, classSymbol));

    // FIXME figure out why top is mandatory
    Resolve.Env top = new Resolve.Env();

    Resolve.Env compilationUnitEnv = new Resolve.Env();
    compilationUnitEnv.outer = top;
    compilationUnitEnv.packge = p;
    compilationUnitEnv.scope = p.members;
    compilationUnitEnv.enclosingClass = symbols.predefClass;

    env = compilationUnitEnv.dup();
    env.outer = compilationUnitEnv;
    env.enclosingClass = classSymbol;
    env.scope = classSymbol.members;
  }

  @Test
  public void primary_literal() {
    b.setRootRule(JavaGrammar.PRIMARY);

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
    b.setRootRule(JavaGrammar.PRIMARY);

    assertThat(typeOf("this")).isSameAs(classType);

    // constructor call
    assertThat(typeOf("this(arguments)")).isSameAs(symbols.unknownType);
  }

  @Test
  public void primary_super() {
    b.setRootRule(JavaGrammar.PRIMARY);

    // constructor call
    assertThat(typeOf("super(arguments)")).isSameAs(symbols.unknownType);

    // method call
    assertThat(typeOf("super.method(arguments)")).isSameAs(symbols.unknownType);

    // field access
    assertThat(typeOf("super.field")).isSameAs(classType.supertype);
  }

  @Test
  public void primary_par_expression() {
    b.setRootRule(JavaGrammar.PRIMARY);

    // (expression)
    assertThat(typeOf("((int) 42L)")).isSameAs(symbols.intType);
  }

  @Test
  public void primary_new() {
    b.setRootRule(JavaGrammar.PRIMARY);

    assertThat(typeOf("new Object()")).isSameAs(symbols.unknownType);
  }

  @Test
  public void primary_qualified_identifier() {
    b.setRootRule(JavaGrammar.PRIMARY);

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
    b.setRootRule(JavaGrammar.PRIMARY);

    assertThat(typeOf("int.class")).isSameAs(symbols.classType);
    assertThat(typeOf("int[].class")).isSameAs(symbols.classType);
    assertThat(typeOf("int[][].class")).isSameAs(symbols.classType);
  }

  @Test
  public void primary_void() {
    b.setRootRule(JavaGrammar.PRIMARY);

    assertThat(typeOf("void.class")).isSameAs(symbols.classType);
  }

  @Test
  public void type_cast() {
    b.setRootRule(JavaGrammar.UNARY_EXPRESSION);

    // (basic_type) expression
    assertThat(typeOf("(byte) 42L")).isSameAs(symbols.byteType);
    assertThat(typeOf("(char) 42")).isSameAs(symbols.charType);
    assertThat(typeOf("(short) 42L")).isSameAs(symbols.shortType);
    assertThat(typeOf("(int) 42")).isSameAs(symbols.intType);
    assertThat(typeOf("(long) 42")).isSameAs(symbols.longType);
    assertThat(typeOf("(float) 42")).isSameAs(symbols.floatType);
    assertThat(typeOf("(double) 42")).isSameAs(symbols.doubleType);

    // (class_type) expression
    assertThat(typeOf("(MyClass) 42")).isSameAs(classSymbol.type);
  }

  @Test
  public void prefix_op() {
    b.setRootRule(JavaGrammar.UNARY_EXPRESSION);

    assertThat(typeOf("++42")).isSameAs(symbols.intType);
  }

  @Test
  public void postfix_op() {
    b.setRootRule(JavaGrammar.UNARY_EXPRESSION);

    assertThat(typeOf("42++")).isSameAs(symbols.intType);
  }

  @Test
  public void selector() {
    b.setRootRule(JavaGrammar.UNARY_EXPRESSION);

    // method call
    assertThat(typeOf("this.method(arguments)")).isSameAs(symbols.unknownType);
    assertThat(typeOf("var[42].clone()")).isSameAs(symbols.unknownType);

    // field access
    assertThat(typeOf("this.var")).isSameAs(variableSymbol.type);
    assertThat(typeOf("var[42].length")).isSameAs(symbols.intType);

    // array access
    assertThat(typeOf("var[42][42]")).isSameAs(((Type.ArrayType) ((Type.ArrayType) variableSymbol.type).elementType).elementType);
  }

  @Test
  public void multiplicative_expression() {
    b.setRootRule(JavaGrammar.MULTIPLICATIVE_EXPRESSION);

    // double, double = double
    // float, float = float
    // long, long = long
    // int, int = int
    assertThat(typeOf("42 * 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 / 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 % 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void additive_expression() {
    b.setRootRule(JavaGrammar.ADDITIVE_EXPRESSION);

    // double, double = double
    // float, float = float
    // long, long = long
    // int, int = int
    assertThat(typeOf("42 + 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 - 42")).isSameAs(symbols.unknownType);

    // TODO
    // assertThat(typeOf("'a' + 'b'")).isSameAs(symbols.intType);

    // string, object = string
    // object, string = string
    // string, string = string
    // string, int = string
    // string, long = string
    // string, float = string
    // string, double = string
    // string, boolean = string
    // string, bot = string
    // int, string = string
    // long, string = string
    // float, string = string
    // double, string = string
    // boolean, string = string
    // bot, string = string
  }

  @Test
  public void shift_expression() {
    b.setRootRule(JavaGrammar.SHIFT_EXPRESSION);

    // long, long = long
    // int, long = int
    // long, int = long
    // int, int = int
    assertThat(typeOf("42 << 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 >> 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 >>> 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void relational_expression() {
    b.setRootRule(JavaGrammar.RELATIONAL_EXPRESSION);

    // double, double = boolean
    // float, float = boolean
    // long, long = boolean
    // int, int = boolean
    assertThat(typeOf("42 >= 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 > 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 <= 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 < 42")).isSameAs(symbols.unknownType);

    assertThat(typeOf("var instanceof Object")).isSameAs(symbols.unknownType);
  }

  @Test
  public void equality_expression() {
    b.setRootRule(JavaGrammar.EQUALITY_EXPRESSION);

    // object, object = boolean
    // boolean, boolean = boolean
    // double, double = boolean
    // float, float = boolean
    // long, long = boolean
    // int, int = boolean
    assertThat(typeOf("42 == 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 != 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void and_expression() {
    b.setRootRule(JavaGrammar.AND_EXPRESSION);

    // boolean, boolean = boolean
    // int, int = int
    // long, long = long
    assertThat(typeOf("true & false")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 & 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42L & 42L")).isSameAs(symbols.unknownType);
  }

  @Test
  public void exclusive_or_expression() {
    b.setRootRule(JavaGrammar.EXCLUSIVE_OR_EXPRESSION);

    // boolean, boolean = boolean
    // int, int = int
    // long, long = long
    assertThat(typeOf("true ^ false")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 ^ 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42L ^ 42L")).isSameAs(symbols.unknownType);
  }

  @Test
  public void inclusive_or_expression() {
    b.setRootRule(JavaGrammar.INCLUSIVE_OR_EXPRESSION);

    // boolean, boolean = boolean
    // int, int = int
    // long, long = long
    assertThat(typeOf("true | false")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 | 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42L | 42L")).isSameAs(symbols.unknownType);
  }

  @Test
  public void conditional_and_expression() {
    b.setRootRule(JavaGrammar.CONDITIONAL_AND_EXPRESSION);

    // boolean, boolean = boolean
    assertThat(typeOf("true && false")).isSameAs(symbols.unknownType);
  }

  @Test
  public void conditional_or_expression() {
    b.setRootRule(JavaGrammar.CONDITIONAL_OR_EXPRESSION);

    // boolean, boolean = boolean
    assertThat(typeOf("true || false")).isSameAs(symbols.unknownType);
  }

  @Test
  public void conditional_expression() {
    b.setRootRule(JavaGrammar.CONDITIONAL_EXPRESSION);

    assertThat(typeOf("42 ? 42 : 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void assignment_expression() {
    b.setRootRule(JavaGrammar.ASSIGNMENT_EXPRESSION);

    // TODO
  }

  private Type typeOf(String input) {
    AstNode astNode = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, b.build()).parse(input);
    System.out.println(AstXmlPrinter.print(astNode));

    AstWalker astWalker = new AstWalker();
    SemanticModel semanticModel = mock(SemanticModel.class);
    when(semanticModel.getEnv(any(AstNode.class))).thenReturn(env);
    ExpressionVisitor visitor = new ExpressionVisitor(semanticModel, symbols, new Resolve());
    visitor.init();
    astWalker.addVisitor(visitor);
    astWalker.walkAndVisit(astNode);

    return visitor.getType(astNode);
  }

}
