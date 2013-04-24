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

public class ExpressionVisitorTest {

  private LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

  private Symbols symbols = new Symbols();

  private Resolve.Env env;

  private Symbol.TypeSymbol classSymbol;
  private Type.ClassType classType;

  private Symbol.VariableSymbol variableSymbol;

  /**
   * Simulates creation of symbols and types.
   */
  @Before
  public void setUp() {
    Symbol.PackageSymbol p = new Symbol.PackageSymbol(null, null);
    p.members = new Scope(p);

    classSymbol = new Symbol.TypeSymbol(0, "MyClass", p);
    classType = ((Type.ClassType) classSymbol.type);
    classType.supertype = symbols.unknownType;
    classType.interfaces = ImmutableList.of();
    classSymbol.members = new Scope(classSymbol);
    p.members.enter(classSymbol);

    variableSymbol = new Symbol.VariableSymbol(/* FIXME figure out why it fails with default visibility */Flags.PUBLIC, "var", classSymbol);
    variableSymbol.type = new Type.ArrayType(new Type.ArrayType(symbols.intType, symbols.arrayClass), symbols.arrayClass);
    classSymbol.members.enter(variableSymbol);

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
    assertThat(typeOf("this(arguments)")).isSameAs(symbols.unknownType);
  }

  @Test
  public void primary_super() {
    b.setRootRule(JavaGrammar.PRIMARY);

    assertThat(typeOf("super(arguments)")).isSameAs(symbols.unknownType);
    assertThat(typeOf("super.method(arguments)")).isSameAs(symbols.unknownType);

    assertThat(typeOf("super.field")).isSameAs(classType.supertype);
  }

  @Test
  public void primary_par_expression() {
    b.setRootRule(JavaGrammar.PRIMARY);

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

    assertThat(typeOf("var")).isSameAs(variableSymbol.type);
    assertThat(typeOf("var[expression]")).isSameAs(((Type.ArrayType) variableSymbol.type).elementType);
    assertThat(typeOf("id[].class")).isSameAs(symbols.classType);
    assertThat(typeOf("id(arguments)")).isSameAs(symbols.unknownType);
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

    assertThat(typeOf("(byte) 42L")).isSameAs(symbols.byteType);
    assertThat(typeOf("(short) 42L")).isSameAs(symbols.shortType);
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

    assertThat(typeOf("this.var")).isSameAs(variableSymbol.type);
    assertThat(typeOf("this.method(arguments)")).isSameAs(symbols.unknownType);

    assertThat(typeOf("var[42].length")).isSameAs(symbols.intType);
    assertThat(typeOf("var[42][42]")).isSameAs(((Type.ArrayType) ((Type.ArrayType) variableSymbol.type).elementType).elementType);
  }

  @Test
  public void multiplicative_expression() {
    b.setRootRule(JavaGrammar.MULTIPLICATIVE_EXPRESSION);

    assertThat(typeOf("42 * 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 / 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 % 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void additive_expression() {
    b.setRootRule(JavaGrammar.ADDITIVE_EXPRESSION);

    assertThat(typeOf("42 + 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 - 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void shift_expression() {
    b.setRootRule(JavaGrammar.SHIFT_EXPRESSION);

    assertThat(typeOf("42 << 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 >> 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 >>> 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void relational_expression() {
    b.setRootRule(JavaGrammar.RELATIONAL_EXPRESSION);

    assertThat(typeOf("42 >= 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 > 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 <= 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 < 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("var instanceof Object")).isSameAs(symbols.unknownType);
  }

  @Test
  public void equality_expression() {
    b.setRootRule(JavaGrammar.EQUALITY_EXPRESSION);

    assertThat(typeOf("42 == 42")).isSameAs(symbols.unknownType);
    assertThat(typeOf("42 != 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void and_expression() {
    b.setRootRule(JavaGrammar.AND_EXPRESSION);

    assertThat(typeOf("42 & 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void exclusive_or_expression() {
    b.setRootRule(JavaGrammar.EXCLUSIVE_OR_EXPRESSION);

    assertThat(typeOf("42 ^ 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void inclusive_or_expression() {
    b.setRootRule(JavaGrammar.INCLUSIVE_OR_EXPRESSION);

    assertThat(typeOf("42 | 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void conditional_and_expression() {
    b.setRootRule(JavaGrammar.CONDITIONAL_AND_EXPRESSION);

    assertThat(typeOf("42 && 42")).isSameAs(symbols.unknownType);
  }

  @Test
  public void conditional_or_expression() {
    b.setRootRule(JavaGrammar.CONDITIONAL_OR_EXPRESSION);

    assertThat(typeOf("42 || 42")).isSameAs(symbols.unknownType);
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
    ExpressionVisitor visitor = new ExpressionVisitor(symbols, new Resolve(), env);
    visitor.init();
    astWalker.addVisitor(visitor);
    astWalker.walkAndVisit(astNode);

    return visitor.getType(astNode);
  }

}
