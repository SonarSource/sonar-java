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
package org.sonar.java.model;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTreeMakerTest {

  private final Parser p = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, JavaGrammar.createGrammar());
  private final JavaTreeMaker maker = new JavaTreeMaker();

  @Test
  public void integration_test() {
    Iterable<File> files = Iterables.concat(
      FileUtils.listFiles(new File("src/main/java/"), new String[]{"java"}, true),
      FileUtils.listFiles(new File("src/test/java/"), new String[]{"java"}, true),
      FileUtils.listFiles(new File("src/test/files/"), new String[]{"java"}, true)
    );
    TreeVisitorsDispatcher visitors = new TreeVisitorsDispatcher(ImmutableList.of(new CompleteVisitor()));
    for (File file : files) {
      Tree tree = maker.compilationUnit(p.parse(file));
      JavaTree.scan(tree, visitors);
    }
  }

  @Test
  public void basic_type() {
    AstNode astNode = p.parse("class T { int m() { return null; } }").getFirstDescendant(JavaGrammar.BASIC_TYPE);
    PrimitiveTypeTree tree = maker.basicType(astNode);
    assertThat(tree).isNotNull();

    astNode = p.parse("class T { void m() { return null; } }").getFirstDescendant(JavaKeyword.VOID);
    tree = maker.basicType(astNode);
    assertThat(tree).isNotNull();
  }

  @Test
  public void type() {
    AstNode astNode = p.parse("class T { int[] m() { return null; } }").getFirstDescendant(JavaGrammar.TYPE);
    ArrayTypeTree tree = (ArrayTypeTree) maker.referenceType(astNode);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
  }

  @Test
  public void literal() {
    AstNode astNode = p.parse("class T { int m() { return 1; } }").getFirstDescendant(JavaGrammar.LITERAL);
    LiteralTree tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.INT_LITERAL)).isTrue();

    astNode = p.parse("class T { long m() { return 1L; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.LONG_LITERAL)).isTrue();

    astNode = p.parse("class T { float m() { return 1F; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.FLOAT_LITERAL)).isTrue();

    astNode = p.parse("class T { double m() { return 1d; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.DOUBLE_LITERAL)).isTrue();

    astNode = p.parse("class T { boolean m() { return true; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.BOOLEAN_LITERAL)).isTrue();

    astNode = p.parse("class T { boolean m() { return false; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.BOOLEAN_LITERAL)).isTrue();

    astNode = p.parse("class T { char m() { return 'c'; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.CHAR_LITERAL)).isTrue();

    astNode = p.parse("class T { String m() { return \"s\"; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.STRING_LITERAL)).isTrue();

    astNode = p.parse("class T { Object m() { return null; } }").getFirstDescendant(JavaGrammar.LITERAL);
    tree = maker.literal(astNode);
    assertThat(tree.is(Tree.Kind.NULL_LITERAL)).isTrue();
  }

  @Test
  public void compilation_unit() {
    AstNode astNode = p.parse("import foo; import bar; class Foo {} class Bar {}");
    CompilationUnitTree tree = maker.compilationUnit(astNode);
    assertThat(tree.packageName()).isNull();
    assertThat(tree.imports()).hasSize(2);
    assertThat(tree.types()).hasSize(2);

    astNode = p.parse("package pkg; import foo; import bar; class Foo {} class Bar {}");
    tree = maker.compilationUnit(astNode);
    assertThat(tree.packageName()).isNotNull();
    assertThat(tree.imports()).hasSize(2);
    assertThat(tree.types()).hasSize(2);
  }

  @Test
  public void import_declaration() {
    AstNode astNode = p.parse("import foo.Bar;");
    ImportTree tree = maker.compilationUnit(astNode).imports().get(0);
    assertThat(tree.isStatic()).isFalse();
    assertThat(tree.qualifiedIdentifier()).isNotNull();

    astNode = p.parse("import foo.bar.*;");
    tree = maker.compilationUnit(astNode).imports().get(0);
    assertThat(tree.isStatic()).isFalse();
    assertThat(tree.qualifiedIdentifier()).isNotNull();

    astNode = p.parse("import static foo.Bar.method;");
    tree = maker.compilationUnit(astNode).imports().get(0);
    assertThat(tree.isStatic()).isTrue();
    assertThat(tree.qualifiedIdentifier()).isNotNull();

    astNode = p.parse("import static foo.Bar.*;");
    tree = maker.compilationUnit(astNode).imports().get(0);
    assertThat(tree.isStatic()).isTrue();
    assertThat(tree.qualifiedIdentifier()).isNotNull();
  }

  /*
   * 8. Classes
   */

  @Test
  public void class_declaration() {
    AstNode astNode = p.parse("public class T extends C implements I1, I2 { }");
    ClassTree tree = (ClassTree) maker.compilationUnit(astNode).types().get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
  }

  @Test
  public void class_init_declaration() {
    AstNode astNode = p.parse("class T { { ; ; } }");
    InitializerTree tree = (InitializerTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.INITIALIZER)).isTrue();
    assertThat(tree.body()).hasSize(2);

    astNode = p.parse("class T { static { ; ; } }");
    tree = (InitializerTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.STATIC_INITIALIZER)).isTrue();
    assertThat(tree.body()).hasSize(2);
  }

  @Test
  public void class_constructor() {
    AstNode astNode = p.parse("class T { T(int p1, int... p2) throws Exception1, Exception2 {} }");
    MethodTree tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();
  }

  @Test
  public void class_field() {
    AstNode astNode = p.parse("class T { public int f1 = 42, f2[]; }");
    List<? extends Tree> declarations = ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNull();
  }

  @Test
  public void class_method() {
    // TODO test "int m(int p)[]"
    // TODO test "int m(int p[])"

    AstNode astNode = p.parse("class T { public int m(int p1, int... p2) throws Exception1, Exception2 {} }");
    MethodTree tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();

    // void method
    astNode = p.parse("class T { public void m(int p) throws Exception1, Exception2 {} }");
    tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();
  }

  /*
   * 8.9. Enums
   */

  @Test
  public void enum_constant() {
    AstNode astNode = p.parse("enum T { C1(1) { }, C2(2) { }; }");
    List<? extends Tree> declarations = ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    NewClassTree tree = (NewClassTree) declarations.get(0);
    assertThat(tree.arguments()).hasSize(1);
    assertThat(tree.classBody()).isNotNull();

    tree = (NewClassTree) declarations.get(1);
    assertThat(tree.arguments()).hasSize(1);
    assertThat(tree.classBody()).isNotNull();
  }

  @Test
  public void enum_field() {
    AstNode astNode = p.parse("enum T { ; public int f1 = 42, f2[]; }");
    List<? extends Tree> declarations = ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNull();
  }

  @Test
  public void enum_constructor() {
    AstNode astNode = p.parse("enum T { ; T(int p1, int... p2) throws Exception1, Exception2 {} }");
    MethodTree tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();
  }

  @Test
  public void enum_method() {
    AstNode astNode = p.parse("enum T { ; int m(int p1, int... p2) throws Exception1, Exception2 {} }");
    MethodTree tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();

    // void method
    astNode = p.parse("enum T { ; void m(int p) throws Exception1, Exception2; }");
    tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNull();
  }

  /*
   * 9. Interfaces
   */

  @Test
  public void interface_field() {
    AstNode astNode = p.parse("interface T { public int f1 = 42, f2[] = { 13 }; }");
    List<? extends Tree> declarations = ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();
  }

  @Test
  public void interface_method() {
    AstNode astNode = p.parse("interface T { int m(int p1, int... p2) throws Exception1, Exception2; }");
    MethodTree tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNull();

    // void method
    astNode = p.parse("interface T { void m(int p) throws Exception1, Exception2; }");
    tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNull();
  }

  /*
   * 9.6. Annotation Types
   */

  @Test
  public void annotation_method() {
    AstNode astNode = p.parse("@interface T { int m() default 0; }");
    MethodTree tree = (MethodTree) ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members().get(0);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.name()).isNotNull();
    assertThat(tree.parameters()).isEmpty();
    assertThat(tree.throwsClauses()).isEmpty();
    assertThat(tree.block()).isNull();
    // FIXME
    // assertThat(tree.defaultValue()).isNotNull();
  }

  @Test
  public void annotation_constant() {
    AstNode astNode = p.parse("@interface T { int c1 = 1, c2[] = { 2 }; }");
    List<? extends Tree> members = ((ClassTree) maker.compilationUnit(astNode).types().get(0)).members();
    assertThat(members).hasSize(2);

    VariableTree tree = (VariableTree) members.get(0);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) members.get(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();
  }

  /*
   * 14. Blocks and Statements
   */

  /**
   * 14.2. Blocks
   */
  @Test
  public void blocks() {
    AstNode astNode = p.parse("class T { void m() { ; ; } }").getFirstDescendant(JavaGrammar.BLOCK);
    BlockTree tree = maker.block(astNode);
    assertThat(tree.body()).hasSize(2);
  }

  /**
   * 14.3. Local Class Declarations
   */
  @Test
  public void local_class_declaration() {
    AstNode astNode = p.parse("class T { void m() { class Local { } } }").getFirstDescendant(JavaGrammar.BLOCK);
    ClassTree tree = (ClassTree) maker.block(astNode).body().get(0);
    assertThat(tree).isNotNull();

    astNode = p.parse("class T { void m() { enum Local { ; } } }").getFirstDescendant(JavaGrammar.BLOCK);
    tree = (ClassTree) maker.block(astNode).body().get(0);
    assertThat(tree).isNotNull();
  }

  /**
   * 14.4. Local Variable Declaration Statements
   */
  @Test
  public void local_variable_declaration() {
    AstNode astNode = p.parse("class T { void m() { int a = 42, b[]; } }").getFirstDescendant(JavaGrammar.BLOCK);
    List<? extends StatementTree> declarations = maker.block(astNode).body();
    assertThat(declarations).hasSize(2);

    // TODO test modifiers

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.name()).isNotNull();
    assertThat(tree.initializer()).isNull();
  }

  /**
   * 14.6. The Empty Statement
   */
  @Test
  public void empty_statement() {
    AstNode astNode = p.parse("class T { void m() { ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    EmptyStatementTree tree = (EmptyStatementTree) maker.statement(astNode);
    assertThat(tree).isNotNull();
  }

  /**
   * 14.7. Labeled Statements
   */
  @Test
  public void labeled_statement() {
    AstNode astNode = p.parse("class T { void m() { label: ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    LabeledStatementTree tree = (LabeledStatementTree) maker.statement(astNode);
    assertThat(tree.label()).isEqualTo("label");
    assertThat(tree.statement()).isNotNull();
  }

  /**
   * 14.8. Expression Statements
   */
  @Test
  public void expression_statement() {
    AstNode astNode = p.parse("class T { void m() { i++; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    ExpressionStatementTree tree = (ExpressionStatementTree) maker.statement(astNode);
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 14.9. The if Statement
   */
  @Test
  public void if_statement() {
    AstNode astNode = p.parse("class T { void m() { if (true) { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    IfStatementTree tree = (IfStatementTree) maker.statement(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.thenStatement()).isNotNull();
    assertThat(tree.elseStatement()).isNull();

    astNode = p.parse("class T { void m() { if (true) { } else { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (IfStatementTree) maker.statement(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.thenStatement()).isNotNull();
    assertThat(tree.elseStatement()).isNotNull();
  }

  /**
   * 14.10. The assert Statement
   */
  @Test
  public void assert_statement() {
    AstNode astNode = p.parse("class T { void m() { assert true; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    AssertStatementTree tree = (AssertStatementTree) maker.statement(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.detail()).isNull();

    astNode = p.parse("class T { void m() { assert true : \"detail\"; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (AssertStatementTree) maker.statement(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.detail()).isNotNull();
  }

  /**
   * 14.11. The switch Statement
   */
  @Test
  public void switch_statement() {
    AstNode astNode = p.parse("class T { void m() { switch (e) { case 1: case 2: ; default: ; } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    SwitchStatementTree tree = (SwitchStatementTree) maker.statement(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.cases()).hasSize(2);
    CaseGroupTree c = tree.cases().get(0);
    assertThat(c.labels()).hasSize(2);
    assertThat(c.labels().get(0).expression()).isNotNull();
    assertThat(c.labels().get(1).expression()).isNotNull();
    assertThat(c.body()).hasSize(1);
    c = tree.cases().get(1);
    assertThat(c.labels()).hasSize(1);
    assertThat(c.labels().get(0).expression()).isNull();
    assertThat(c.body()).hasSize(1);

    astNode = p.parse("class T { void m() { switch (e) { default: } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (SwitchStatementTree) maker.statement(astNode);
    assertThat(tree.cases()).hasSize(1);
    assertThat(tree.cases().get(0).body()).isEmpty();
  }

  /**
   * 14.12. The while Statement
   */
  @Test
  public void while_statement() {
    AstNode astNode = p.parse("class T { void m() { while (true) ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    WhileStatementTree tree = (WhileStatementTree) maker.statement(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.statement()).isNotNull();
  }

  /**
   * 14.13. The do Statement
   */
  @Test
  public void do_statement() {
    AstNode astNode = p.parse("class T { void m() { do ; while (true); } }").getFirstDescendant(JavaGrammar.STATEMENT);
    DoWhileStatementTree tree = (DoWhileStatementTree) maker.statement(astNode);
    assertThat(tree.statement()).isNotNull();
    assertThat(tree.condition()).isNotNull();
  }

  /**
   * 14.14. The for Statement
   */
  @Test
  public void for_statement() {
    AstNode astNode = p.parse("class T { void m() { for (int i = 0; i < 42; i ++) ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    ForStatementTree tree = (ForStatementTree) maker.statement(astNode);
    assertThat(tree.initializer()).hasSize(1);
    assertThat(tree.initializer().get(0)).isInstanceOf(VariableTree.class);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.update()).isNotNull();
    assertThat(tree.statement()).isNotNull();

    astNode = p.parse("class T { void m() { for (i = 0; i < 42; i ++) ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (ForStatementTree) maker.statement(astNode);
    assertThat(tree.initializer()).hasSize(1);
    assertThat(tree.initializer().get(0)).isInstanceOf(ExpressionStatementTree.class);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.update()).isNotNull();
    assertThat(tree.statement()).isNotNull();

    astNode = p.parse("class T { void m() { for ( ; ; ) ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (ForStatementTree) maker.statement(astNode);
    assertThat(tree.initializer()).isEmpty();
    assertThat(tree.condition()).isNull();
    assertThat(tree.update()).isEmpty();
    assertThat(tree.statement()).isNotNull();
  }

  @Test
  public void enhanced_for_statement() {
    AstNode astNode = p.parse("class T { void m() { for (Object o : objects) ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    EnhancedForStatementTree tree = (EnhancedForStatementTree) maker.statement(astNode);
    assertThat(tree.variable()).isNotNull();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.statement()).isNotNull();
  }

  /**
   * 14.15. The break Statement
   */
  @Test
  public void break_statement() {
    AstNode astNode = p.parse("class T { void m() { break ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    BreakStatementTree tree = (BreakStatementTree) maker.statement(astNode);
    assertThat(tree.label()).isNull();

    astNode = p.parse("class T { void m() { break label ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (BreakStatementTree) maker.statement(astNode);
    assertThat(tree.label()).isEqualTo("label");
  }

  /**
   * 14.16. The continue Statement
   */
  @Test
  public void continue_statement() {
    AstNode astNode = p.parse("class T { void m() { continue ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    ContinueStatementTree tree = (ContinueStatementTree) maker.statement(astNode);
    assertThat(tree.label()).isNull();

    astNode = p.parse("class T { void m() { continue label ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (ContinueStatementTree) maker.statement(astNode);
    assertThat(tree.label()).isEqualTo("label");
  }

  /**
   * 14.17. The return Statement
   */
  @Test
  public void return_statement() {
    AstNode astNode = p.parse("class T { boolean m() { return ; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    ReturnStatementTree tree = (ReturnStatementTree) maker.statement(astNode);
    assertThat(tree.expression()).isNull();

    astNode = p.parse("class T { boolean m() { return true; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (ReturnStatementTree) maker.statement(astNode);
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 14.18. The throw Statement
   */
  @Test
  public void throw_statement() {
    AstNode astNode = p.parse("class T { void m() { throw e; } }").getFirstDescendant(JavaGrammar.STATEMENT);
    ThrowStatementTree tree = (ThrowStatementTree) maker.statement(astNode);
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 14.19. The synchronized Statement
   */
  @Test
  public void synchronized_statement() {
    AstNode astNode = p.parse("class T { void m() { synchronized(e) { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    SynchronizedStatementTree tree = (SynchronizedStatementTree) maker.statement(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.block()).isNotNull();
  }

  /**
   * 14.20. The try statement
   */
  @Test
  public void try_statement() {
    AstNode astNode = p.parse("class T { void m() { try { } finally { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    TryStatementTree tree = (TryStatementTree) maker.statement(astNode);
    assertThat(tree.resources()).isEmpty();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).isEmpty();
    assertThat(tree.finallyBlock()).isNotNull();

    astNode = p.parse("class T { void m() { try { } catch (RuntimeException e) { } catch (Exception e) { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (TryStatementTree) maker.statement(astNode);
    assertThat(tree.resources()).isEmpty();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).hasSize(2);
    assertThat(tree.catches().get(0).parameter()).isNotNull();
    assertThat(tree.catches().get(0).block()).isNotNull();
    assertThat(tree.catches().get(1).parameter()).isNotNull();
    assertThat(tree.catches().get(1).block()).isNotNull();
    assertThat(tree.finallyBlock()).isNull();

    astNode = p.parse("class T { void m() { try { } catch (Exception e) { } finally { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (TryStatementTree) maker.statement(astNode);
    assertThat(tree.resources()).isEmpty();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).hasSize(1);
    assertThat(tree.finallyBlock()).isNotNull();

    astNode = p.parse("class T { void m() { try (Resource r = open()) { } catch (Exception e) { } finally { } } }").getFirstDescendant(JavaGrammar.STATEMENT);
    tree = (TryStatementTree) maker.statement(astNode);
    // FIXME try with resources
//    assertThat(tree.resources()).isEmpty();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).hasSize(1);
    assertThat(tree.finallyBlock()).isNotNull();

    // FIXME multi-catch
  }

  /*
   * 15. Expressions
   */

  /*
   * 15.8. Primary Expressions
   */

  /**
   * 15.8.2. Class Literals
   */
  @Test
  public void class_literal() {
    AstNode astNode = p.parse("class T { m() { return void.class; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    MemberSelectExpressionTree tree = (MemberSelectExpressionTree) maker.primary(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier().name()).isEqualTo("class");

    astNode = p.parse("class T { m() { return int.class; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    tree = (MemberSelectExpressionTree) maker.primary(astNode);
    assertThat(tree.expression()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.identifier().name()).isEqualTo("class");

    astNode = p.parse("class T { m() { return int[].class; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    tree = (MemberSelectExpressionTree) maker.primary(astNode);
    assertThat(tree.expression()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.identifier().name()).isEqualTo("class");

    astNode = p.parse("class T { m() { return T.class; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    tree = (MemberSelectExpressionTree) maker.primary(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier().name()).isEqualTo("class");

    astNode = p.parse("class T { m() { return T[].class; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    tree = (MemberSelectExpressionTree) maker.primary(astNode);
    assertThat(tree.expression()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.identifier().name()).isEqualTo("class");
  }

  /**
   * 15.8.3. this
   */
  @Test
  public void this_expression() {
    AstNode astNode = p.parse("class T { Object m() { return this; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    IdentifierTree tree = (IdentifierTree) maker.primary(astNode);
    assertThat(tree).isNotNull();
    assertThat(tree.name()).isEqualTo("this");
  }

  /**
   * 15.8.4. Qualified this
   */
  @Test
  public void qualified_this() {
    AstNode astNode = p.parse("class T { Object m() { return ClassName.this; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    MemberSelectExpressionTree tree = (MemberSelectExpressionTree) maker.primary(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier().name()).isEqualTo("this");
  }

  /**
   * 15.8.5. Parenthesized Expressions
   */
  @Test
  public void parenthesized_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return (true); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    ParenthesizedTree tree = (ParenthesizedTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 15.9. Class Instance Creation Expressions
   */
  @Test
  public void class_instance_creation_expression() {
    AstNode astNode = p.parse("class T { T m() { return new T(true, false) {}; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    NewClassTree tree = (NewClassTree) maker.primary(astNode);
    assertThat(tree.enclosingExpression()).isNull();
    assertThat(tree.arguments()).hasSize(2);
    // FIXME
//    assertThat(tree.identifier()).isNotNull();
    assertThat(tree.classBody()).isNotNull();
//    assertThat(tree.typeArguments()).isEmpty();

    astNode = p.parse("class T { T m() { return Enclosing.new T(true, false) {}; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    tree = (NewClassTree) maker.primary(astNode);
    assertThat(tree.enclosingExpression()).isNotNull();
//    assertThat(tree.identifier()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.classBody()).isNotNull();
//    assertThat(tree.typeArguments()).isEmpty();
  }

  /**
   * 15.10. Array Creation Expressions
   */
  @Test
  public void array_creation_expression() {
    AstNode astNode = p.parse("class T { int[][] m() { return new int[][]{{1}, {2, 3}}; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    NewArrayTree tree = (NewArrayTree) maker.primary(astNode);
    assertThat(tree.type()).isNotNull();
    assertThat(tree.dimensions()).isEmpty();
    assertThat(tree.initializers()).hasSize(2);
    assertThat(((NewArrayTree) tree.initializers().get(0)).initializers()).hasSize(1);
    assertThat(((NewArrayTree) tree.initializers().get(1)).initializers()).hasSize(2);

    astNode = p.parse("class T { int[] m() { return new int[2][2]; } }").getFirstDescendant(JavaGrammar.PRIMARY);
    tree = (NewArrayTree) maker.primary(astNode);
    assertThat(tree.type()).isNotNull();
    assertThat(tree.dimensions()).hasSize(2);
    assertThat(tree.initializers()).isEmpty();
  }

  /**
   * 15.11. Field Access Expressions
   */
  @Test
  public void field_access_expression() {
    AstNode astNode;
    MemberSelectExpressionTree tree;

    // TODO greedily consumed by QUALIFIED_IDENTIFIER?:
//    AstNode astNode = p.parse("class T { int m() { return primary.identifier; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
//    MemberSelectExpressionTree tree = (MemberSelectExpressionTree) maker.makeFrom(astNode);
//    assertThat(tree.expression()).isNotNull();
//    assertThat(tree.identifier()).isNotNull();

    astNode = p.parse("class T { int m() { return super.identifier; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MemberSelectExpressionTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier()).isNotNull();

    astNode = p.parse("class T { int m() { return ClassName.super.identifier; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MemberSelectExpressionTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier()).isNotNull();
  }

  /**
   * 15.12. Method Invocation Expressions
   */
  @Test
  public void method_invocation_expression() {
    // TODO test NonWildTypeArguments

    AstNode astNode = p.parse("class T { void m() { identifier(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    MethodInvocationTree tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { super.identifier(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { TypeName.super.identifier(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { TypeName.identifier(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { TypeName.<T>identifier(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { TypeName.<T>super(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
  }

  /**
   * 8.8.7.1. Explicit Constructor Invocations
   */
  @Test
  public void explicit_constructor_invocation() {
    // TODO test NonWildTypeArguments

    AstNode astNode = p.parse("class T { T() { this(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    MethodInvocationTree tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { super(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);

    astNode = p.parse("class T { T() { ClassName.super(true, false); } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (MethodInvocationTree) maker.expression(astNode);
    assertThat(tree.methodSelect()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
  }

  /**
   * 15.13. Array Access Expressions
   */
  @Test
  public void array_access_expression() {
    AstNode astNode = p.parse("class T { T() { return a[42]; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    ArrayAccessExpressionTree tree = (ArrayAccessExpressionTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.index()).isNotNull();
  }

  /**
   * 15.14. Postfix Expressions
   */
  @Test
  public void postfix_expression() {
    AstNode astNode = p.parse("class T { void m() { i++; } }").getFirstDescendant(JavaGrammar.UNARY_EXPRESSION);
    UnaryExpressionTree tree = (UnaryExpressionTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.is(Tree.Kind.POSTFIX_INCREMENT)).isTrue();

    astNode = p.parse("class T { void m() { i--; } }").getFirstDescendant(JavaGrammar.UNARY_EXPRESSION);
    tree = (UnaryExpressionTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.is(Tree.Kind.POSTFIX_DECREMENT)).isTrue();
  }

  /**
   * 15.15. Unary Operators
   */
  @Test
  public void unary_operators() {
    AstNode astNode = p.parse("class T { void m() { ++i; } }").getFirstDescendant(JavaGrammar.UNARY_EXPRESSION);
    UnaryExpressionTree tree = (UnaryExpressionTree) maker.expression(astNode);
    assertThat(tree.is(Tree.Kind.PREFIX_INCREMENT)).isTrue();
    assertThat(tree.expression()).isNotNull();

    astNode = p.parse("class T { void m() { --i; } }").getFirstDescendant(JavaGrammar.UNARY_EXPRESSION);
    tree = (UnaryExpressionTree) maker.expression(astNode);
    assertThat(tree.is(Tree.Kind.PREFIX_DECREMENT)).isTrue();
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 15.16. Cast Expressions
   */
  @Test
  public void type_cast() {
    AstNode astNode = p.parse("class T { boolean m() { return (Boolean) true; } }").getFirstDescendant(JavaGrammar.UNARY_EXPRESSION);
    TypeCastTree tree = (TypeCastTree) maker.expression(astNode);
    assertThat(tree.type()).isNotNull();
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 15.17. Multiplicative Operators
   */
  @Test
  public void multiplicative_expression() {
    AstNode astNode = p.parse("class T { int m() { return 1 * 2 / 3 % 4; } }").getFirstDescendant(JavaGrammar.MULTIPLICATIVE_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.MULTIPLY)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.DIVIDE)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.REMAINDER)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.18. Additive Operators
   */
  @Test
  public void additive_expression() {
    AstNode astNode = p.parse("class T { int m() { return 1 + 2 - 3; } }").getFirstDescendant(JavaGrammar.ADDITIVE_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.PLUS)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.MINUS)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.19. Shift Operators
   */
  @Test
  public void shift_expression() {
    AstNode astNode = p.parse("class T { int m() { return 1 >> 2 << 3 >>> 4; } }").getFirstDescendant(JavaGrammar.SHIFT_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.RIGHT_SHIFT)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.LEFT_SHIFT)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.UNSIGNED_RIGHT_SHIFT)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.20. Relational Operators
   */
  @Test
  public void relational_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return 1 < 2 > 3; } }").getFirstDescendant(JavaGrammar.RELATIONAL_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.LESS_THAN)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.GREATER_THAN)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.20.2. Type Comparison Operator instanceof
   */
  @Test
  public void instanceof_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return null instanceof Object; } }").getFirstDescendant(JavaGrammar.RELATIONAL_EXPRESSION);
    InstanceOfTree tree = (InstanceOfTree) maker.expression(astNode);
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.type()).isNotNull();
  }

  /**
   * 15.21. Equality Operators
   */
  @Test
  public void equality_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return false == false != true; } }").getFirstDescendant(JavaGrammar.EQUALITY_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.EQUAL_TO)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.NOT_EQUAL_TO)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.22. Bitwise and Logical Operators
   */
  @Test
  public void bitwise_and_logical_operators() {
    AstNode astNode = p.parse("class T { int m() { return 1 & 2 & 3; } }").getFirstDescendant(JavaGrammar.AND_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.AND)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.AND)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();

    astNode = p.parse("class T { int m() { return 1 ^ 2 ^ 3; } }").getFirstDescendant(JavaGrammar.EXCLUSIVE_OR_EXPRESSION);
    tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.XOR)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.XOR)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();

    astNode = p.parse("class T { int m() { return 1 | 2 | 3; } }").getFirstDescendant(JavaGrammar.INCLUSIVE_OR_EXPRESSION);
    tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.OR)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.OR)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.23. Conditional-And Operator &&
   */
  @Test
  public void conditional_and_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return false && false && true; } }").getFirstDescendant(JavaGrammar.CONDITIONAL_AND_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.24. Conditional-Or Operator ||
   */
  @Test
  public void conditional_or_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return false || false || true; } }").getFirstDescendant(JavaGrammar.CONDITIONAL_OR_EXPRESSION);
    BinaryExpressionTree tree = (BinaryExpressionTree) maker.expression(astNode);
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_OR)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.rightOperand();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_OR)).isTrue();
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.25. Conditional Operator ? :
   */
  @Test
  public void conditional_expression() {
    AstNode astNode = p.parse("class T { boolean m() { return true ? true : false; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    ConditionalExpressionTree tree = (ConditionalExpressionTree) maker.expression(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.trueExpression()).isNotNull();
    assertThat(tree.falseExpression()).isNotNull();

    astNode = p.parse("class T { boolean m() { return true ? true : false ? true : false; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    tree = (ConditionalExpressionTree) maker.expression(astNode);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.trueExpression()).isNotNull();
    assertThat(tree.falseExpression()).isNotNull();
    tree = (ConditionalExpressionTree) tree.falseExpression();
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.trueExpression()).isNotNull();
    assertThat(tree.falseExpression()).isNotNull();
  }

  /**
   * 15.26. Assignment Operators
   */
  @Test
  public void assignment_expression() {
    AstNode astNode = p.parse("class T { void m() { a += 42; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    AssignmentExpressionTree tree = (AssignmentExpressionTree) maker.expression(astNode);
    assertThat(tree.variable()).isNotNull();
    assertThat(tree.is(Tree.Kind.PLUS_ASSIGNMENT)).isTrue();
    assertThat(tree.expression()).isNotNull();
  }

}
