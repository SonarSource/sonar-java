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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTreeModelTest {

  private final Parser p = JavaParser.createParser(Charsets.UTF_8);

  private static Kind[] getKindsAssociatedTo(Class<? extends Tree> associatedInterface) {
    List<Kind> result = Lists.newArrayList();
    for (Kind kind : Kind.values()) {
      if (associatedInterface.equals(kind.getAssociatedInterface())) {
        result.add(kind);
      }
    }
    return result.toArray(new Kind[result.size()]);
  }

  @Test
  public void integration_test() {
    Iterable<File> files = Iterables.concat(
        FileUtils.listFiles(new File("src/main/java/"), new String[]{"java"}, true),
        FileUtils.listFiles(new File("src/test/java/"), new String[]{"java"}, true),
        FileUtils.listFiles(new File("src/test/files/"), new String[]{"java"}, true)
    );
    BaseTreeVisitor visitor = new BaseTreeVisitor();
    for (File file : files) {
      Tree tree = (CompilationUnitTree) p.parse(file);
      tree.accept(visitor);
    }
  }

  @Test
  public void explicit_generic_invocation() {
    p.parse("class A { void f() { <A>foo(); } }");
  }

  @Test
  public void basic_type() {
    AstNode astNode = p.parse("class T { int m() { return null; } }").getFirstDescendant(Kind.PRIMITIVE_TYPE);
    PrimitiveTypeTree tree = (PrimitiveTypeTree) astNode;
    assertThat(tree.keyword().text()).isEqualTo("int");

    tree = (PrimitiveTypeTree) p.parse("class T { void m() { return null; } }").getFirstDescendant(Kind.PRIMITIVE_TYPE);
    assertThat(tree.keyword().text()).isEqualTo("void");
  }

  @Test
  public void type() {
    ArrayTypeTree tree = (ArrayTypeTree) p.parse("class T { int[] m() { return null; } }").getFirstDescendant(Kind.ARRAY_TYPE);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
  }

  @Test
  public void literal() {
    LiteralTree tree = (LiteralTree) p.parse("class T { int m() { return 1; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.INT_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("1");

    tree = (LiteralTree) p.parse("class T { long m() { return 1L; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.LONG_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("1L");

    tree = (LiteralTree) p.parse("class T { float m() { return 1F; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.FLOAT_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("1F");

    tree = (LiteralTree) p.parse("class T { double m() { return 1d; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.DOUBLE_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("1d");

    tree = (LiteralTree) p.parse("class T { boolean m() { return true; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.BOOLEAN_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("true");

    tree = (LiteralTree) p.parse("class T { boolean m() { return false; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.BOOLEAN_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("false");

    tree = (LiteralTree) p.parse("class T { char m() { return 'c'; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.CHAR_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("'c'");

    tree = (LiteralTree) p.parse("class T { String m() { return \"s\"; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.STRING_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("\"s\"");

    tree = (LiteralTree) p.parse("class T { Object m() { return null; } }").getFirstDescendant(getKindsAssociatedTo(LiteralTree.class));
    assertThat(tree.is(Tree.Kind.NULL_LITERAL)).isTrue();
    assertThat(tree.value()).isEqualTo("null");
  }

  @Test
  public void compilation_unit() {
    CompilationUnitTree tree = (CompilationUnitTree) p.parse("import foo; import bar; class Foo {} class Bar {}");
    assertThat(tree.is(Tree.Kind.COMPILATION_UNIT)).isTrue();
    assertThat(tree.packageName()).isNull();
    assertThat(tree.imports()).hasSize(2);
    assertThat(tree.types()).hasSize(2);

    tree = (CompilationUnitTree) p.parse("package pkg; import foo; import bar; class Foo {} class Bar {}");
    assertThat(tree.is(Tree.Kind.COMPILATION_UNIT)).isTrue();
    assertThat(tree.packageName()).isNotNull();
    assertThat(tree.imports()).hasSize(2);
    assertThat(tree.types()).hasSize(2);

    tree = (CompilationUnitTree) p.parse("import foo; ; import bar; class Foo {} class Bar {}");
    assertThat(tree.is(Tree.Kind.COMPILATION_UNIT)).isTrue();
    assertThat(tree.packageName()).isNull();
    assertThat(tree.imports()).hasSize(3);
    assertThat(tree.imports().get(1).is(Kind.EMPTY_STATEMENT)).isTrue();
    assertThat(tree.types()).hasSize(2);
  }

  @Test
  public void import_declaration() {
    AstNode astNode = p.parse(";");
    ImportClauseTree tree = ((CompilationUnitTree) astNode).imports().get(0);
    assertThat(tree.is(Kind.EMPTY_STATEMENT)).isTrue();
    assertThat(tree.is(Kind.IMPORT)).isFalse();

    astNode = p.parse("import foo.Bar;");
    tree = ((CompilationUnitTree) astNode).imports().get(0);
    assertThat(tree.is(Kind.IMPORT)).isTrue();
    ImportTree importTree = (ImportTree) tree;
    assertThat(importTree.isStatic()).isFalse();
    assertThat(importTree.qualifiedIdentifier()).isNotNull();

    astNode = p.parse("import foo.bar.*;");
    tree = ((CompilationUnitTree) astNode).imports().get(0);
    assertThat(tree.is(Kind.IMPORT)).isTrue();
    importTree = (ImportTree) tree;
    assertThat(importTree.isStatic()).isFalse();
    assertThat(importTree.qualifiedIdentifier()).isNotNull();

    astNode = p.parse("import static foo.Bar.method;");
    tree = ((CompilationUnitTree) astNode).imports().get(0);
    assertThat(tree.is(Kind.IMPORT)).isTrue();
    importTree = (ImportTree) tree;
    assertThat(importTree.isStatic()).isTrue();
    assertThat(importTree.qualifiedIdentifier()).isNotNull();

    astNode = p.parse("import static foo.Bar.*;");
    tree = ((CompilationUnitTree) astNode).imports().get(0);
    assertThat(tree.is(Kind.IMPORT)).isTrue();
    importTree = (ImportTree) tree;
    assertThat(importTree.isStatic()).isTrue();
    assertThat(importTree.qualifiedIdentifier()).isNotNull();
  }

  /**
   * 4.5.1. Type Arguments and Wildcards
   */
  @Test
  public void type_arguments() {
    List<Tree> typeArguments = (TypeArgumentListTreeImpl) p.parse("public class T { void m() { ClassType<? extends A, ? super B, ?, C> var; } }")
        .getFirstDescendant(JavaLexer.TYPE_ARGUMENTS);
    assertThat(typeArguments).hasSize(4);

    WildcardTree wildcard = (WildcardTree) typeArguments.get(0);
    assertThat(wildcard.is(Tree.Kind.EXTENDS_WILDCARD)).isTrue();
    assertThat(wildcard.bound()).isInstanceOf(IdentifierTree.class);

    wildcard = (WildcardTree) typeArguments.get(1);
    assertThat(wildcard.is(Tree.Kind.SUPER_WILDCARD)).isTrue();
    assertThat(wildcard.bound()).isInstanceOf(IdentifierTree.class);

    wildcard = (WildcardTree) typeArguments.get(2);
    assertThat(wildcard.is(Tree.Kind.UNBOUNDED_WILDCARD)).isTrue();
    assertThat(wildcard.bound()).isNull();

    assertThat(typeArguments.get(3)).isInstanceOf(IdentifierTree.class);
  }

  /*
   * 8. Classes
   */

  @Test
  public void class_declaration() {
    AstNode astNode = p.parse("public class T<U> extends C implements I1, I2 { }");
    ClassTree tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.is(Tree.Kind.CLASS)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.typeParameters()).isNotEmpty();
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.superClass()).isNotNull();
    assertThat(tree.superInterfaces()).hasSize(2);
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");

    astNode = p.parse("public class T { }");
    assertThat(tree.is(Tree.Kind.CLASS)).isTrue();
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.typeParameters()).isEmpty();
    assertThat(tree.superClass()).isNull();
    assertThat(tree.superInterfaces()).isEmpty();

    astNode = p.parse("@Deprecated class T { }");
    assertThat(tree.is(Tree.Kind.CLASS)).isTrue();
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.modifiers().annotations()).hasSize(1);
  }

  @Test
  public void annotations() {
    AstNode astNode = p.parse("@SuppressWarnings(\"unchecked\") class T { }");
    ClassTree tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    List<AnnotationTree> annotations = tree.modifiers().annotations();
    assertThat(annotations).hasSize(1);
    AnnotationTree annotation = annotations.get(0);
    assertThat(annotation.annotationType().is(Tree.Kind.IDENTIFIER)).isTrue();
    assertThat(annotation.arguments()).hasSize(1);
    assertThat(annotation.arguments().get(0).is(Tree.Kind.STRING_LITERAL)).isTrue();

    astNode = p.parse("@Target( ) class U {}");
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    annotations = tree.modifiers().annotations();
    assertThat(annotations).hasSize(1);
    annotation = annotations.get(0);
    assertThat(annotation.arguments()).hasSize(0);

    astNode = p.parse("@Target({ElementType.METHOD}) class U {}");
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    annotations = tree.modifiers().annotations();
    assertThat(annotations).hasSize(1);
    annotation = annotations.get(0);
    assertThat(annotation.arguments()).hasSize(1);
    assertThat(annotation.arguments().get(0).is(Tree.Kind.NEW_ARRAY)).isTrue();

    astNode = p.parse("@Target(value={ElementType.METHOD}, value2=\"toto\") class T { }");
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    annotations = tree.modifiers().annotations();
    assertThat(annotations).hasSize(1);
    annotation = annotations.get(0);
    assertThat(annotation.annotationType().is(Tree.Kind.IDENTIFIER)).isTrue();
    assertThat(annotation.arguments()).hasSize(2);
    assertThat(annotation.arguments().get(0).is(Tree.Kind.ASSIGNMENT)).isTrue();

    astNode = p.parse("class T { private void meth() { @NonNullable String str;}}");
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    VariableTree variable = (VariableTree) ((MethodTree) tree.members().get(0)).block().body().get(0);
    annotations = variable.modifiers().annotations();
    assertThat(annotations).hasSize(1);
    annotation = annotations.get(0);
    assertThat(annotation.annotationType().is(Tree.Kind.IDENTIFIER)).isTrue();

    astNode = p.parse("@PackageLevelAnnotation package blammy;");
    annotations = ((CompilationUnitTree) astNode).packageAnnotations();
    assertThat(annotations).hasSize(1);
  }

  @Test
  public void class_init_declaration() {
    AstNode astNode = p.parse("class T { { ; ; } }");
    BlockTree tree = (BlockTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.INITIALIZER)).isTrue();
    assertThat(tree.body()).hasSize(2);
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");

    astNode = p.parse("class T { static { ; ; } }");
    tree = (BlockTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.STATIC_INITIALIZER)).isTrue();
    assertThat(tree.body()).hasSize(2);
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");
  }

  @Test
  public void class_constructor() {
    AstNode astNode = p.parse("class T { T(int p1, int... p2) throws Exception1, Exception2 {} }");
    MethodTree tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.CONSTRUCTOR)).isTrue();
    assertThat(tree.returnType()).isNull();
    assertThat(tree.simpleName().name()).isEqualTo("T");
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
    List<Tree> declarations = ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("f1");
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("f2");
    assertThat(tree.initializer()).isNull();
  }

  @Test
  public void class_method() {
    AstNode astNode = p.parse("class T { public int m(int p[][]){} }");
    MethodTree tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(((ArrayTypeTree) tree.parameters().get(0).type()).type()).isInstanceOf(ArrayTypeTree.class);

    astNode = p.parse("class T { public <T> int m(@Annotate int p1, int... p2) throws Exception1, Exception2 {} }");
    tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.typeParameters()).isNotEmpty();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(0).modifiers().annotations()).hasSize(1);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.parameters().get(1).endToken()).isNull();
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();

    // void method
    astNode = p.parse("class T { public void m(int p) throws Exception1, Exception2 {} }");
    tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.typeParameters()).isEmpty();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.parameters().get(0).endToken()).isNull();
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();

    astNode = p.parse("class T { public int[] m(int p1, int... p2)[] throws Exception1, Exception2 {} }");
    tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.returnType().is(Tree.Kind.ARRAY_TYPE));
    assertThat(((ArrayTypeTree) tree.returnType()).type().is(Tree.Kind.INT_LITERAL));
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();

    astNode = p.parse("class T { public int m()[] { return null; } }");
    tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.parameters()).isEmpty();
    assertThat(tree.returnType().is(Kind.ARRAY_TYPE));
    assertThat(((ArrayTypeTree) tree.returnType()).is(Kind.PRIMITIVE_TYPE));
  }

  /*
   * 8.9. Enums
   */

  @Test
  public void enum_declaration() {
    AstNode astNode = p.parse("public enum T implements I1, I2 { }");
    ClassTree tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.is(Tree.Kind.ENUM)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.superClass()).isNull();
    assertThat(tree.superInterfaces()).hasSize(2);

    astNode = p.parse("public enum T { }");
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.is(Tree.Kind.ENUM)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.superClass()).isNull();
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");
    assertThat(tree.superInterfaces()).isEmpty();
  }

  @Test
  public void enum_constant() {
    AstNode astNode = p.parse("enum T { C1, C2(2) { }; }");
    List<Tree> declarations = ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    EnumConstantTree tree = (EnumConstantTree) declarations.get(0);
    assertThat(tree.is(Tree.Kind.ENUM_CONSTANT)).isTrue();
    assertThat(tree.simpleName().name()).isEqualTo("C1");
    NewClassTree newClassTree = (NewClassTree) tree.initializer();
    assertThat(newClassTree.arguments()).isEmpty();
    assertThat(newClassTree.classBody()).isNull();

    tree = (EnumConstantTree) declarations.get(1);
    assertThat(tree.is(Tree.Kind.ENUM_CONSTANT)).isTrue();
    assertThat(tree.simpleName().name()).isEqualTo("C2");
    newClassTree = (NewClassTree) tree.initializer();
    assertThat(newClassTree.arguments()).hasSize(1);
    assertThat(newClassTree.classBody()).isNotNull();
    assertThat(newClassTree.classBody().openBraceToken().text()).isEqualTo("{");

  }

  @Test
  public void enum_field() {
    AstNode astNode = p.parse("enum T { ; public int f1 = 42, f2[]; }");
    List<Tree> declarations = ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("f1");
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("f2");
    assertThat(tree.initializer()).isNull();
  }

  @Test
  public void enum_constructor() {
    AstNode astNode = p.parse("enum T { ; T(int p1, int... p2) throws Exception1, Exception2 {} }");
    MethodTree tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.CONSTRUCTOR)).isTrue();
    assertThat(tree.returnType()).isNull();
    assertThat(tree.simpleName().name()).isEqualTo("T");
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
    MethodTree tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNotNull();
    assertThat(tree.defaultValue()).isNull();

    // void method
    astNode = p.parse("enum T { ; void m(int p) throws Exception1, Exception2; }");
    tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNull();
  }

  /*
   * 9. Interfaces
   */

  @Test
  public void interface_declaration() {
    AstNode astNode = p.parse("public interface T<U> extends I1, I2 { }");
    ClassTree tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.is(Tree.Kind.INTERFACE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.typeParameters()).isNotEmpty();
    assertThat(tree.superClass()).isNull();
    assertThat(tree.superInterfaces()).hasSize(2);

    astNode = p.parse("public interface T { }");
    tree = (ClassTree) ((CompilationUnitTree) astNode).types().get(0);
    assertThat(tree.is(Tree.Kind.INTERFACE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.typeParameters()).isEmpty();
    assertThat(tree.superClass()).isNull();
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");
    assertThat(tree.superInterfaces()).isEmpty();
  }

  @Test
  public void interface_field() {
    AstNode astNode = p.parse("interface T { public int f1 = 42, f2[] = { 13 }; }");
    List<Tree> declarations = ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members();
    assertThat(declarations).hasSize(2);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("f1");
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("f2");
    assertThat(tree.initializer()).isNotNull();
  }

  @Test
  public void interface_method() {
    AstNode astNode = p.parse("interface T { <T> int m(int p1, int... p2) throws Exception1, Exception2; }");
    MethodTree tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.typeParameters()).isNotEmpty();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(2);
    assertThat(tree.parameters().get(0).type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.parameters().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNull();

    // void method
    astNode = p.parse("interface T { void m(int p) throws Exception1, Exception2; }");
    tree = (MethodTree) ((ClassTree) ((CompilationUnitTree) astNode).types().get(0)).members().get(0);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.typeParameters()).isEmpty();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).hasSize(1);
    assertThat(tree.throwsClauses()).hasSize(2);
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNull();
  }

  /*
   * 9.6. Annotation Types
   */

  @Test
  public void annotation_declaration() {
    ClassTree tree = (ClassTree) ((CompilationUnitTree) p.parse("public @interface T { }")).types().get(0);
    assertThat(tree.is(Tree.Kind.ANNOTATION_TYPE)).isTrue();
    assertThat(tree.modifiers().modifiers()).hasSize(1);
    assertThat(tree.simpleName().name()).isEqualTo("T");
    assertThat(tree.superClass()).isNull();
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");
    assertThat(tree.superInterfaces()).isEmpty();
  }

  @Test
  public void annotation_method() {
    MethodTree tree = (MethodTree) p.parse("@interface T { int m() default 0; }").getFirstDescendant(Kind.METHOD);
    assertThat(tree.is(Tree.Kind.METHOD)).isTrue();
    assertThat(tree.returnType()).isNotNull();
    assertThat(tree.simpleName().name()).isEqualTo("m");
    assertThat(tree.parameters()).isEmpty();
    assertThat(tree.throwsClauses()).isEmpty();
    assertThat(tree.block()).isNull();
    assertThat(tree.defaultValue()).isNotNull();
    tree = (MethodTree) p.parse("@interface plop{ public String method(); }").getFirstDescendant(Kind.METHOD);
    assertThat(tree.modifiers().modifiers()).hasSize(1);

  }

  @Test
  public void annotation_constant() {
    List<Tree> members = ((ClassTree) p.parse("@interface T { int c1 = 1, c2[] = { 2 }; }").getFirstDescendant(Kind.ANNOTATION_TYPE)).members();
    assertThat(members).hasSize(2);

    VariableTree tree = (VariableTree) members.get(0);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("c1");
    assertThat(tree.initializer()).isNotNull();

    tree = (VariableTree) members.get(1);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("c2");
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
    BlockTree tree = (BlockTree) p.parse("class T { void m() { ; ; } }").getFirstDescendant(Kind.BLOCK);
    assertThat(tree.is(Tree.Kind.BLOCK)).isTrue();
    assertThat(tree.openBraceToken().text()).isEqualTo("{");
    assertThat(tree.body()).hasSize(2);
    assertThat(tree.closeBraceToken().text()).isEqualTo("}");
  }

  /**
   * 14.3. Local Class Declarations
   */
  @Test
  public void local_class_declaration() {
    BlockTree block = (BlockTree) p.parse("class T { void m() { abstract class Local { } } }").getFirstDescendant(Kind.BLOCK);
    ClassTree tree = (ClassTree) block.body().get(0);
    assertThat(tree.is(Tree.Kind.CLASS)).isTrue();
    assertThat(tree.simpleName().identifierToken().text()).isEqualTo("Local");
    assertThat(tree.modifiers().modifiers()).containsOnly(Modifier.ABSTRACT);
    assertThat(tree).isNotNull();

    block = (BlockTree) p.parse("class T { void m() { static enum Local { ; } } }").getFirstDescendant(Kind.BLOCK);
    tree = (ClassTree) block.body().get(0);
    assertThat(tree.is(Tree.Kind.ENUM)).isTrue();
    assertThat(tree.modifiers().modifiers()).containsOnly(Modifier.STATIC);
    assertThat(tree).isNotNull();
  }

  /**
   * 14.4. Local Variable Declaration Statements
   */
  @Test
  public void local_variable_declaration() {
    BlockTree block = (BlockTree) p.parse("class T { void m() { int a = 42, b[]; final @Nullable int c = 42; } }").getFirstDescendant(Kind.BLOCK);
    List<StatementTree> declarations = block.body();
    assertThat(declarations).hasSize(3);

    VariableTree tree = (VariableTree) declarations.get(0);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).isEmpty();
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("a");
    assertThat(tree.initializer()).isNotNull();
    assertThat(tree.endToken()).isNotNull();
    assertThat(tree.endToken().text()).isEqualTo(",");

    tree = (VariableTree) declarations.get(1);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).isEmpty();
    assertThat(tree.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("b");
    assertThat(tree.initializer()).isNull();
    assertThat(tree.endToken()).isNotNull();
    assertThat(tree.endToken().text()).isEqualTo(";");

    // TODO Test annotation

    tree = (VariableTree) declarations.get(2);
    assertThat(tree.is(Tree.Kind.VARIABLE)).isTrue();
    assertThat(tree.modifiers().modifiers()).containsOnly(Modifier.FINAL);
    assertThat(tree.type()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.simpleName().name()).isEqualTo("c");
    assertThat(tree.initializer()).isNotNull();
    assertThat(tree.endToken()).isNotNull();
    assertThat(tree.endToken().text()).isEqualTo(";");
  }

  /**
   * 14.6. The Empty Statement
   */
  @Test
  public void empty_statement() {
    EmptyStatementTree tree = (EmptyStatementTree) p.parse("class T { void m() { ; } }").getFirstDescendant(Kind.EMPTY_STATEMENT);
    assertThat(tree.is(Tree.Kind.EMPTY_STATEMENT)).isTrue();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.7. Labeled Statements
   */
  @Test
  public void labeled_statement() {
    LabeledStatementTree tree = (LabeledStatementTree) p.parse("class T { void m() { label: ; } }").getFirstDescendant(Kind.LABELED_STATEMENT);
    assertThat(tree.is(Tree.Kind.LABELED_STATEMENT)).isTrue();
    assertThat(tree.label().name()).isEqualTo("label");
    assertThat(tree.statement()).isNotNull();
    assertThat(tree.colonToken().text()).isEqualTo(":");
  }

  /**
   * 14.8. Expression Statements
   */
  @Test
  public void expression_statement() {
    ExpressionStatementTree tree = (ExpressionStatementTree) p.parse("class T { void m() { i++; } }").getFirstDescendant(Kind.EXPRESSION_STATEMENT);
    assertThat(tree.is(Tree.Kind.EXPRESSION_STATEMENT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.9. The if Statement
   */
  @Test
  public void if_statement() {
    IfStatementTree tree = (IfStatementTree) p.parse("class T { void m() { if (true) { } } }").getFirstDescendant(Kind.IF_STATEMENT);
    assertThat(tree.is(Tree.Kind.IF_STATEMENT)).isTrue();
    assertThat(tree.ifKeyword().text()).isEqualTo("if");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.thenStatement()).isNotNull();
    assertThat(tree.elseKeyword()).isNull();
    assertThat(tree.elseStatement()).isNull();

    tree = (IfStatementTree) p.parse("class T { void m() { if (true) { } else { } } }").getFirstDescendant(Kind.IF_STATEMENT);
    assertThat(tree.is(Tree.Kind.IF_STATEMENT)).isTrue();
    assertThat(tree.ifKeyword().text()).isEqualTo("if");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.thenStatement()).isNotNull();
    assertThat(tree.elseKeyword().text()).isEqualTo("else");
    assertThat(tree.elseStatement()).isNotNull();
  }

  /**
   * 14.10. The assert Statement
   */
  @Test
  public void assert_statement() {
    AssertStatementTree tree = (AssertStatementTree) p.parse("class T { void m() { assert true; } }").getFirstDescendant(Kind.ASSERT_STATEMENT);
    assertThat(tree.is(Tree.Kind.ASSERT_STATEMENT)).isTrue();
    assertThat(tree.assertKeyword().text()).isEqualTo("assert");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.colonToken()).isNull();
    assertThat(tree.detail()).isNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");

    tree = (AssertStatementTree) p.parse("class T { void m() { assert true : \"detail\"; } }").getFirstDescendant(Kind.ASSERT_STATEMENT);
    assertThat(tree.is(Tree.Kind.ASSERT_STATEMENT)).isTrue();
    assertThat(tree.assertKeyword().text()).isEqualTo("assert");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.colonToken().text()).isEqualTo(":");
    assertThat(tree.detail()).isNotNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.11. The switch Statement
   */
  @Test
  public void switch_statement() {
    SwitchStatementTree tree = (SwitchStatementTree) p.parse("class T { void m() { switch (e) { case 1: case 2: ; default: ; } } }").getFirstDescendant(
        Kind.SWITCH_STATEMENT);
    assertThat(tree.is(Tree.Kind.SWITCH_STATEMENT)).isTrue();
    assertThat(tree.switchKeyword().text()).isEqualTo("switch");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.cases()).hasSize(2);

    CaseGroupTree c = tree.cases().get(0);
    assertThat(c.is(Tree.Kind.CASE_GROUP)).isTrue();
    assertThat(c.labels()).hasSize(2);
    assertThat(c.labels().get(0).is(Tree.Kind.CASE_LABEL)).isTrue();
    assertThat(c.labels().get(0).caseOrDefaultKeyword().text()).isEqualTo("case");
    assertThat(c.labels().get(0).expression()).isNotNull();
    assertThat(c.labels().get(0).colonToken().text()).isEqualTo(":");
    assertThat(c.labels().get(1).is(Tree.Kind.CASE_LABEL)).isTrue();
    assertThat(c.labels().get(1).caseOrDefaultKeyword().text()).isEqualTo("case");
    assertThat(c.labels().get(1).expression()).isNotNull();
    assertThat(c.labels().get(1).colonToken().text()).isEqualTo(":");
    assertThat(c.body()).hasSize(1);

    c = tree.cases().get(1);
    assertThat(c.is(Tree.Kind.CASE_GROUP)).isTrue();
    assertThat(c.labels()).hasSize(1);
    assertThat(c.labels().get(0).is(Tree.Kind.CASE_LABEL)).isTrue();
    assertThat(c.labels().get(0).caseOrDefaultKeyword().text()).isEqualTo("default");
    assertThat(c.labels().get(0).expression()).isNull();
    assertThat(c.labels().get(0).colonToken().text()).isEqualTo(":");
    assertThat(c.body()).hasSize(1);

    tree = (SwitchStatementTree) p.parse("class T { void m() { switch (e) { default: } } }").getFirstDescendant(Kind.SWITCH_STATEMENT);
    assertThat(tree.cases()).hasSize(1);
    assertThat(tree.cases().get(0).body()).isEmpty();
  }

  /**
   * 14.12. The while Statement
   */
  @Test
  public void while_statement() {
    WhileStatementTree tree = (WhileStatementTree) p.parse("class T { void m() { while (true) ; } }").getFirstDescendant(Kind.WHILE_STATEMENT);
    assertThat(tree.is(Tree.Kind.WHILE_STATEMENT)).isTrue();
    assertThat(tree.whileKeyword().text()).isEqualTo("while");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.statement()).isNotNull();
  }

  /**
   * 14.13. The do Statement
   */
  @Test
  public void do_statement() {
    DoWhileStatementTree tree = (DoWhileStatementTree) p.parse("class T { void m() { do ; while (true); } }").getFirstDescendant(Kind.DO_STATEMENT);
    assertThat(tree.is(Tree.Kind.DO_STATEMENT)).isTrue();
    assertThat(tree.doKeyword().text()).isEqualTo("do");
    assertThat(tree.statement()).isNotNull();
    assertThat(tree.whileKeyword().text()).isEqualTo("while");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.14. The for Statement
   */
  @Test
  public void for_statement() {
    ForStatementTree tree = (ForStatementTree) p.parse("class T { void m() { for (int i = 0; i < 42; i ++) ; } }").getFirstDescendant(Kind.FOR_STATEMENT);
    assertThat(tree.is(Tree.Kind.FOR_STATEMENT)).isTrue();
    assertThat(tree.forKeyword().text()).isEqualTo("for");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.initializer()).hasSize(1);
    assertThat(tree.initializer().get(0)).isInstanceOf(VariableTree.class);
    assertThat(tree.firstSemicolonToken().text()).isEqualTo(";");
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.secondSemicolonToken().text()).isEqualTo(";");
    assertThat(tree.update()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.statement()).isNotNull();

    tree = (ForStatementTree) p.parse("class T { void m() { for (i = 0; i < 42; i ++) ; } }").getFirstDescendant(Kind.FOR_STATEMENT);
    assertThat(tree.is(Tree.Kind.FOR_STATEMENT)).isTrue();
    assertThat(tree.initializer()).hasSize(1);
    assertThat(tree.initializer().get(0)).isInstanceOf(ExpressionStatementTree.class);
    assertThat(tree.condition()).isNotNull();
    assertThat(tree.update()).isNotNull();
    assertThat(tree.statement()).isNotNull();

    tree = (ForStatementTree) p.parse("class T { void m() { for ( ; ; ) ; } }").getFirstDescendant(Kind.FOR_STATEMENT);
    assertThat(tree.is(Tree.Kind.FOR_STATEMENT)).isTrue();
    assertThat(tree.initializer()).isEmpty();
    assertThat(tree.condition()).isNull();
    assertThat(tree.update()).isEmpty();
    assertThat(tree.statement()).isNotNull();
  }

  @Test
  public void enhanced_for_statement() {
    ForEachStatement tree = (ForEachStatement) p.parse("class T { void m() { for (Object o : objects) ; } }").getFirstDescendant(Kind.FOR_EACH_STATEMENT);
    assertThat(tree.is(Tree.Kind.FOR_EACH_STATEMENT)).isTrue();
    assertThat(tree.forKeyword().text()).isEqualTo("for");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.variable()).isNotNull();
    assertThat(tree.colonToken().text()).isEqualTo(":");
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.statement()).isNotNull();
  }

  /**
   * 14.15. The break Statement
   */
  @Test
  public void break_statement() {
    BreakStatementTree tree = (BreakStatementTree) p.parse("class T { void m() { break ; } }").getFirstDescendant(Kind.BREAK_STATEMENT);
    assertThat(tree.is(Tree.Kind.BREAK_STATEMENT)).isTrue();
    assertThat(tree.breakKeyword().text()).isEqualTo("break");
    assertThat(tree.label()).isNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");

    tree = (BreakStatementTree) p.parse("class T { void m() { break label ; } }").getFirstDescendant(Kind.BREAK_STATEMENT);
    assertThat(tree.is(Tree.Kind.BREAK_STATEMENT)).isTrue();
    assertThat(tree.breakKeyword().text()).isEqualTo("break");
    assertThat(tree.label().name()).isEqualTo("label");
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.16. The continue Statement
   */
  @Test
  public void continue_statement() {
    ContinueStatementTree tree = (ContinueStatementTree) p.parse("class T { void m() { continue ; } }").getFirstDescendant(Kind.CONTINUE_STATEMENT);
    assertThat(tree.is(Tree.Kind.CONTINUE_STATEMENT)).isTrue();
    assertThat(tree.continueKeyword().text()).isEqualTo("continue");
    assertThat(tree.label()).isNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");

    tree = (ContinueStatementTree) p.parse("class T { void m() { continue label ; } }").getFirstDescendant(Kind.CONTINUE_STATEMENT);
    assertThat(tree.is(Tree.Kind.CONTINUE_STATEMENT)).isTrue();
    assertThat(tree.continueKeyword().text()).isEqualTo("continue");
    assertThat(tree.label().name()).isEqualTo("label");
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.17. The return Statement
   */
  @Test
  public void return_statement() {
    ReturnStatementTree tree = (ReturnStatementTree) p.parse("class T { boolean m() { return ; } }").getFirstDescendant(Kind.RETURN_STATEMENT);
    assertThat(tree.is(Tree.Kind.RETURN_STATEMENT)).isTrue();
    assertThat(tree.returnKeyword().text()).isEqualTo("return");
    assertThat(tree.expression()).isNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");

    tree = (ReturnStatementTree) p.parse("class T { boolean m() { return true; } }").getFirstDescendant(Kind.RETURN_STATEMENT);
    assertThat(tree.is(Tree.Kind.RETURN_STATEMENT)).isTrue();
    assertThat(tree.returnKeyword().text()).isEqualTo("return");
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.18. The throw Statement
   */
  @Test
  public void throw_statement() {
    ThrowStatementTree tree = (ThrowStatementTree) p.parse("class T { void m() { throw e; } }").getFirstDescendant(Kind.THROW_STATEMENT);
    assertThat(tree.is(Tree.Kind.THROW_STATEMENT)).isTrue();
    assertThat(tree.throwKeyword().text()).isEqualTo("throw");
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.semicolonToken().text()).isEqualTo(";");
  }

  /**
   * 14.19. The synchronized Statement
   */
  @Test
  public void synchronized_statement() {
    SynchronizedStatementTree tree = (SynchronizedStatementTree) p.parse("class T { void m() { synchronized(e) { } } }").getFirstDescendant(Kind.SYNCHRONIZED_STATEMENT);
    assertThat(tree.is(Tree.Kind.SYNCHRONIZED_STATEMENT)).isTrue();
    assertThat(tree.synchronizedKeyword().text()).isEqualTo("synchronized");
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.block()).isNotNull();
  }

  /**
   * 14.20. The try statement
   */
  @Test
  public void try_statement() {
    TryStatementTree tree = (TryStatementTree) p.parse("class T { void m() { try { } finally { } } }").getFirstDescendant(Kind.TRY_STATEMENT);
    assertThat(tree.is(Tree.Kind.TRY_STATEMENT)).isTrue();
    assertThat(tree.resources()).isEmpty();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).isEmpty();
    assertThat(tree.finallyKeyword().text()).isEqualTo("finally");
    assertThat(tree.finallyBlock()).isNotNull();

    tree = (TryStatementTree) p.parse("class T { void m() { try { } catch (RuntimeException e1) { } catch (Exception e2) { } } }").getFirstDescendant(Kind.TRY_STATEMENT);
    assertThat(tree.is(Tree.Kind.TRY_STATEMENT)).isTrue();
    assertThat(tree.tryKeyword().text()).isEqualTo("try");
    assertThat(tree.openParenToken()).isNull();
    assertThat(tree.resources()).isEmpty();
    assertThat(tree.closeParenToken()).isNull();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.finallyKeyword()).isNull();
    assertThat(tree.finallyBlock()).isNull();
    assertThat(tree.catches()).hasSize(2);
    CatchTree catchTree = tree.catches().get(0);
    assertThat(catchTree.catchKeyword().text()).isEqualTo("catch");
    assertThat(catchTree.block()).isNotNull();
    assertThat(catchTree.openParenToken().text()).isEqualTo("(");
    assertThat(catchTree.closeParenToken().text()).isEqualTo(")");
    VariableTree parameterTree = catchTree.parameter();
    assertThat(parameterTree.type()).isNotNull();
    assertThat(parameterTree.simpleName().name()).isEqualTo("e1");
    assertThat(parameterTree.initializer()).isNull();
    catchTree = tree.catches().get(1);
    parameterTree = catchTree.parameter();
    assertThat(parameterTree.type()).isNotNull();
    assertThat(parameterTree.simpleName().name()).isEqualTo("e2");
    assertThat(parameterTree.initializer()).isNull();

    tree = (TryStatementTree) p.parse("class T { void m() { try { } catch (Exception e) { } finally { } } }").getFirstDescendant(Kind.TRY_STATEMENT);
    assertThat(tree.is(Tree.Kind.TRY_STATEMENT)).isTrue();
    assertThat(tree.resources()).isEmpty();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).hasSize(1);
    assertThat(tree.finallyBlock()).isNotNull();

    tree = (TryStatementTree) p.parse("class T { void m() { try (Resource r1 = open(); Resource r2 = open()) { } catch (Exception e) { } finally { } } }")
        .getFirstDescendant(Kind.TRY_STATEMENT);
    assertThat(tree.is(Tree.Kind.TRY_STATEMENT)).isTrue();
    assertThat(tree.block()).isNotNull();
    assertThat(tree.catches()).hasSize(1);
    assertThat(tree.finallyBlock()).isNotNull();
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.resources()).hasSize(2);
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    VariableTree resource = tree.resources().get(0);
    assertThat(resource.simpleName().name()).isEqualTo("r1");
    assertThat(resource.initializer()).isNotNull();
    resource = tree.resources().get(1);
    assertThat(resource.simpleName().name()).isEqualTo("r2");
    assertThat(resource.initializer()).isNotNull();

    tree = (TryStatementTree) p.parse("class T { void m() { try { } catch (Exception1 | Exception2 e) { } } }").getFirstDescendant(Kind.TRY_STATEMENT);
    parameterTree = tree.catches().get(0).parameter();
    assertThat(((UnionTypeTree) parameterTree.type()).typeAlternatives()).hasSize(2);
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
    MemberSelectExpressionTree tree = (MemberSelectExpressionTree) p.parse("class T { m() { return void.class; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier().identifierToken().text()).isEqualTo("class");
    assertThat(tree.identifier().name()).isEqualTo("class");

    tree = (MemberSelectExpressionTree) p.parse("class T { m() { return int.class; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isInstanceOf(PrimitiveTypeTree.class);
    assertThat(tree.identifier().identifierToken().text()).isEqualTo("class");
    assertThat(tree.identifier().name()).isEqualTo("class");

    tree = (MemberSelectExpressionTree) p.parse("class T { m() { return int[].class; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.identifier().identifierToken().text()).isEqualTo("class");
    assertThat(tree.identifier().name()).isEqualTo("class");

    tree = (MemberSelectExpressionTree) p.parse("class T { m() { return T.class; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier().identifierToken().text()).isEqualTo("class");
    assertThat(tree.identifier().name()).isEqualTo("class");

    tree = (MemberSelectExpressionTree) p.parse("class T { m() { return T[].class; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isInstanceOf(ArrayTypeTree.class);
    assertThat(tree.identifier().identifierToken().text()).isEqualTo("class");
    assertThat(tree.identifier().name()).isEqualTo("class");
  }

  /**
   * 15.8.3. this
   */
  @Test
  public void this_expression() {
    IdentifierTree tree = (IdentifierTree) ((ReturnStatementTree) p.parse("class T { Object m() { return this; } }").getFirstDescendant(Kind.RETURN_STATEMENT)).expression();
    assertThat(tree.is(Tree.Kind.IDENTIFIER)).isTrue();
    assertThat(tree).isNotNull();
    assertThat(tree.identifierToken().text()).isEqualTo("this");
    assertThat(tree.name()).isEqualTo("this");
  }

  /**
   * 15.8.4. Qualified this
   */
  @Test
  public void qualified_this() {
    MemberSelectExpressionTree tree = (MemberSelectExpressionTree) p.parse("class T { Object m() { return ClassName.this; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier().identifierToken().text()).isEqualTo("this");
    assertThat(tree.identifier().name()).isEqualTo("this");
  }

  /**
   * 15.8.5. Parenthesized Expressions
   */
  @Test
  public void parenthesized_expression() {
    ParenthesizedTree tree = (ParenthesizedTree) p.parse("class T { boolean m() { return (true); } }").getFirstDescendant(Kind.PARENTHESIZED_EXPRESSION);
    assertThat(tree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)).isTrue();
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
  }

  /**
   * 15.9. Class Instance Creation Expressions
   */
  @Test
  public void class_instance_creation_expression() {
    NewClassTree tree = (NewClassTree) p.parse("class T { T m() { return new T(true, false) {}; } }").getFirstDescendant(Kind.NEW_CLASS);
    assertThat(tree.is(Tree.Kind.NEW_CLASS)).isTrue();
    assertThat(tree.enclosingExpression()).isNull();
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.identifier()).isNotNull();
    assertThat(tree.classBody()).isNotNull();
    // assertThat(tree.typeArguments()).isEmpty();

    tree = (NewClassTree) p.parse("class T { T m() { return Enclosing.new T(true, false) {}; } }").getFirstDescendant(Kind.NEW_CLASS);
    assertThat(tree.is(Tree.Kind.NEW_CLASS)).isTrue();
    assertThat(tree.enclosingExpression()).isNotNull();
    assertThat(tree.identifier()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.classBody()).isNotNull();
    // assertThat(tree.typeArguments()).isEmpty();

    tree = (NewClassTree) p.parse("class T { T m() { return this.new T(true, false) {}; } }").getFirstDescendant(Kind.NEW_CLASS);
    assertThat(tree.enclosingExpression()).isNotNull();
    assertThat(tree.identifier()).isNotNull();
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.classBody()).isNotNull();
    // assertThat(tree.typeArguments()).isEmpty();
  }

  /**
   * 15.10. Array Creation Expressions
   */
  @Test
  public void array_creation_expression() {
    NewArrayTree tree = (NewArrayTree) p.parse("class T { int[][] m() { return new int[][]{{1}, {2, 3}}; } }").getFirstDescendant(Kind.NEW_ARRAY);
    assertThat(tree.is(Tree.Kind.NEW_ARRAY)).isTrue();
    assertThat(tree.type()).isNotNull();
    assertThat(tree.dimensions()).isEmpty();
    assertThat(tree.initializers()).hasSize(2);
    assertThat(((NewArrayTree) tree.initializers().get(0)).initializers()).hasSize(1);
    assertThat(((NewArrayTree) tree.initializers().get(1)).initializers()).hasSize(2);

    tree = (NewArrayTree) p.parse("class T { int[] m() { return new int[2][2]; } }").getFirstDescendant(Kind.NEW_ARRAY);
    assertThat(tree.is(Tree.Kind.NEW_ARRAY)).isTrue();
    assertThat(tree.type()).isNotNull();
    assertThat(tree.dimensions()).hasSize(2);
    assertThat(tree.initializers()).isEmpty();
  }

  /**
   * 15.11. Field Access Expressions
   */
  @Test
  public void field_access_expression() {
    MemberSelectExpressionTree tree;

    // TODO greedily consumed by QUALIFIED_IDENTIFIER?:
    // AstNode astNode = p.parse("class T { int m() { return primary.identifier; } }").getFirstDescendant(JavaGrammar.EXPRESSION);
    // MemberSelectExpressionTree tree = (MemberSelectExpressionTree) maker.makeFrom(astNode);
    // assertThat(tree.expression()).isNotNull();
    // assertThat(tree.identifier()).isNotNull();

    tree = (MemberSelectExpressionTree) p.parse("class T { int m() { return super.identifier; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier()).isNotNull();

    tree = (MemberSelectExpressionTree) p.parse("class T { int m() { return ClassName.super.identifier; } }").getFirstDescendant(Kind.MEMBER_SELECT);
    assertThat(tree.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.identifier()).isNotNull();
  }

  /**
   * 15.12. Method Invocation Expressions
   */
  @Test
  public void method_invocation_expression() {
    // TODO test NonWildTypeArguments
    MethodInvocationTree tree = (MethodInvocationTree) p.parse("class T { void m() { identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    assertThat(((IdentifierTree) tree.methodSelect()).name()).isEqualTo("identifier");
    assertThat(tree.arguments()).hasSize(2);
    assertThat(tree.closeParenToken()).isNotNull();
    assertThat(tree.openParenToken()).isNotNull();

    tree = (MethodInvocationTree) p.parse("class T { void m() { <T>identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    assertThat(((IdentifierTree) tree.methodSelect()).name()).isEqualTo("identifier");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { super.identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(memberSelectExpression.identifier().name()).isEqualTo("identifier");
    assertThat(((IdentifierTree) memberSelectExpression.expression()).name()).isEqualTo("super");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { TypeName.super.identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(memberSelectExpression.identifier().name()).isEqualTo("identifier");
    memberSelectExpression = (MemberSelectExpressionTree) memberSelectExpression.expression();
    assertThat(memberSelectExpression.identifier().name()).isEqualTo("super");
    assertThat(((IdentifierTree) memberSelectExpression.expression()).name()).isEqualTo("TypeName");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { TypeName.identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(memberSelectExpression.identifier().name()).isEqualTo("identifier");
    assertThat(((IdentifierTree) memberSelectExpression.expression()).name()).isEqualTo("TypeName");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { TypeName.<T>identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(memberSelectExpression.identifier().name()).isEqualTo("identifier");
    assertThat(((IdentifierTree) memberSelectExpression.expression()).name()).isEqualTo("TypeName");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { primary().<T>identifier(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(memberSelectExpression.identifier().name()).isEqualTo("identifier");
    assertThat(memberSelectExpression.expression()).isInstanceOf(MethodInvocationTree.class);
    assertThat(tree.arguments()).hasSize(2);
  }

  /**
   * 8.8.7.1. Explicit Constructor Invocations
   */
  @Test
  public void explicit_constructor_invocation() {
    // TODO test NonWildTypeArguments

    MethodInvocationTree tree = (MethodInvocationTree) p.parse("class T { T() { this(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    assertThat(((IdentifierTree) tree.methodSelect()).name()).isEqualTo("this");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { <T>this(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    assertThat(((IdentifierTree) tree.methodSelect()).name()).isEqualTo("this");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { super(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    assertThat(((IdentifierTree) tree.methodSelect()).name()).isEqualTo("super");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { <T>super(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    assertThat(((IdentifierTree) tree.methodSelect()).name()).isEqualTo("super");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { ClassName.super(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(methodSelect.identifier().name()).isEqualTo("super");
    assertThat(((IdentifierTree) methodSelect.expression()).name()).isEqualTo("ClassName");
    assertThat(tree.arguments()).hasSize(2);

    tree = (MethodInvocationTree) p.parse("class T { T() { ClassName.<T>super(true, false); } }").getFirstDescendant(Kind.METHOD_INVOCATION);
    assertThat(tree.is(Tree.Kind.METHOD_INVOCATION)).isTrue();
    methodSelect = (MemberSelectExpressionTree) tree.methodSelect();
    assertThat(methodSelect.identifier().name()).isEqualTo("super");
    assertThat(((IdentifierTree) methodSelect.expression()).name()).isEqualTo("ClassName");
    assertThat(tree.arguments()).hasSize(2);
  }

  /**
   * 15.13. Array Access Expressions
   */
  @Test
  public void array_access_expression() {
    ArrayAccessExpressionTree tree = (ArrayAccessExpressionTree) p.parse("class T { T() { return a[42]; } }").getFirstDescendant(Kind.ARRAY_ACCESS_EXPRESSION);
    assertThat(tree.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.index()).isNotNull();
  }

  /**
   * 15.14. Postfix Expressions
   */
  @Test
  public void postfix_expression() {
    UnaryExpressionTree tree = (UnaryExpressionTree) p.parse("class T { void m() { i++; } }").getFirstDescendant(Kind.POSTFIX_INCREMENT);
    assertThat(tree.is(Tree.Kind.POSTFIX_INCREMENT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("++");

    tree = (UnaryExpressionTree) p.parse("class T { void m() { i--; } }").getFirstDescendant(Kind.POSTFIX_DECREMENT);
    assertThat(tree.is(Tree.Kind.POSTFIX_DECREMENT)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("--");
  }

  /**
   * 15.15. Unary Operators
   */
  @Test
  public void unary_operators() {
    UnaryExpressionTree tree = (UnaryExpressionTree) p.parse("class T { void m() { ++i; } }").getFirstDescendant(Kind.PREFIX_INCREMENT);
    assertThat(tree.is(Tree.Kind.PREFIX_INCREMENT)).isTrue();
    assertThat(tree.operatorToken().text()).isEqualTo("++");
    assertThat(tree.expression()).isNotNull();

    tree = (UnaryExpressionTree) p.parse("class T { void m() { --i; } }").getFirstDescendant(Kind.PREFIX_DECREMENT);
    assertThat(tree.is(Tree.Kind.PREFIX_DECREMENT)).isTrue();
    assertThat(tree.operatorToken().text()).isEqualTo("--");
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 15.16. Cast Expressions
   */
  @Test
  public void type_cast() {
    TypeCastTree tree = (TypeCastTree) p.parse("class T { boolean m() { return (Boolean) true; } }").getFirstDescendant(Kind.TYPE_CAST);
    assertThat(tree.is(Tree.Kind.TYPE_CAST)).isTrue();
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.type()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.expression()).isNotNull();

    tree = (TypeCastTree) p.parse("class T { boolean m() { return (Foo<T> & Bar) true; } }").getFirstDescendant(Kind.TYPE_CAST);
    assertThat(tree.is(Tree.Kind.TYPE_CAST)).isTrue();
    assertThat(tree.openParenToken().text()).isEqualTo("(");
    assertThat(tree.type()).isNotNull();
    assertThat(tree.closeParenToken().text()).isEqualTo(")");
    assertThat(tree.expression()).isNotNull();
  }

  /**
   * 15.17. Multiplicative Operators
   */
  @Test
  public void multiplicative_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { int m() { return 1 * 2 / 3 % 4; } }").getFirstDescendant(Kind.REMAINDER);
    assertThat(tree.is(Kind.REMAINDER)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("%");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.DIVIDE)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("/");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Kind.MULTIPLY)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("*");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.18. Additive Operators
   */
  @Test
  public void additive_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { int m() { return 1 + 2 - 3; } }").getFirstDescendant(Kind.MINUS);
    assertThat(tree.is(Kind.MINUS)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("-");
    assertThat(tree.rightOperand()).isNotNull();
    assertThat(tree.rightOperand().is(Kind.INT_LITERAL)).isTrue();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Kind.PLUS)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("+");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.19. Shift Operators
   */
  @Test
  public void shift_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { int m() { return 1 >> 2 << 3 >>> 4; } }").getFirstDescendant(Kind.UNSIGNED_RIGHT_SHIFT);
    assertThat(tree.is(Tree.Kind.UNSIGNED_RIGHT_SHIFT)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo(">>>");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.LEFT_SHIFT)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("<<");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Kind.RIGHT_SHIFT)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo(">>");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.20. Relational Operators
   */
  @Test
  public void relational_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { boolean m() { return 1 < 2 > 3; } }").getFirstDescendant(Kind.GREATER_THAN);
    assertThat(tree.is(Tree.Kind.GREATER_THAN)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo(">");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Kind.LESS_THAN)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("<");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.20.2. Type Comparison Operator instanceof
   */
  @Test
  public void instanceof_expression() {
    InstanceOfTree tree = (InstanceOfTree) p.parse("class T { boolean m() { return null instanceof Object; } }").getFirstDescendant(Kind.INSTANCE_OF);
    assertThat(tree.is(Tree.Kind.INSTANCE_OF)).isTrue();
    assertThat(tree.expression()).isNotNull();
    assertThat(tree.instanceofKeyword().text()).isEqualTo("instanceof");
    assertThat(tree.type()).isNotNull();
  }

  /**
   * 15.21. Equality Operators
   */
  @Test
  public void equality_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { boolean m() { return false == false != true; } }").getFirstDescendant(Kind.NOT_EQUAL_TO);
    assertThat(tree.is(Tree.Kind.NOT_EQUAL_TO)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("!=");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.EQUAL_TO)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("==");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.22. Bitwise and Logical Operators
   */
  @Test
  public void bitwise_and_logical_operators() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { int m() { return 1 & 2 & 3; } }").getFirstDescendant(Kind.AND);
    assertThat(tree.is(Tree.Kind.AND)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("&");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.AND)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("&");
    assertThat(tree.rightOperand()).isNotNull();

    tree = (BinaryExpressionTree) p.parse("class T { int m() { return 1 ^ 2 ^ 3; } }").getFirstDescendant(Kind.XOR);
    assertThat(tree.is(Tree.Kind.XOR)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("^");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.XOR)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("^");
    assertThat(tree.rightOperand()).isNotNull();

    tree = (BinaryExpressionTree) p.parse("class T { int m() { return 1 | 2 | 3; } }").getFirstDescendant(Kind.OR);
    assertThat(tree.is(Tree.Kind.OR)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("|");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.OR)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("|");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.23. Conditional-And Operator &&
   */
  @Test
  public void conditional_and_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { boolean m() { return false && false && true; } }").getFirstDescendant(Kind.CONDITIONAL_AND);
    assertThat(tree.is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("&&");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("&&");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.24. Conditional-Or Operator ||
   */
  @Test
  public void conditional_or_expression() {
    BinaryExpressionTree tree = (BinaryExpressionTree) p.parse("class T { boolean m() { return false || false || true; } }").getFirstDescendant(Kind.CONDITIONAL_OR);
    assertThat(tree.is(Tree.Kind.CONDITIONAL_OR)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("||");
    assertThat(tree.rightOperand()).isNotNull();
    tree = (BinaryExpressionTree) tree.leftOperand();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_OR)).isTrue();
    assertThat(tree.leftOperand()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("||");
    assertThat(tree.rightOperand()).isNotNull();
  }

  /**
   * 15.25. Conditional Operator ? :
   */
  @Test
  public void conditional_expression() {
    ConditionalExpressionTree tree = (ConditionalExpressionTree) p.parse("class T { boolean m() { return true ? true : false; } }").getFirstDescendant(Kind.CONDITIONAL_EXPRESSION);
    assertThat(tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)).isTrue();
    assertThat(tree.condition()).isInstanceOf(LiteralTree.class);
    assertThat(tree.questionToken().text()).isEqualTo("?");
    assertThat(tree.trueExpression()).isInstanceOf(LiteralTree.class);
    assertThat(tree.colonToken().text()).isEqualTo(":");
    assertThat(tree.falseExpression()).isInstanceOf(LiteralTree.class);

    tree = (ConditionalExpressionTree) p.parse("class T { boolean m() { return true ? true : false ? true : false; } }").getFirstDescendant(Kind.CONDITIONAL_EXPRESSION);
    assertThat(tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)).isTrue();
    assertThat(tree.condition()).isInstanceOf(LiteralTree.class);
    assertThat(tree.trueExpression()).isInstanceOf(LiteralTree.class);
    assertThat(tree.falseExpression()).isInstanceOf(ConditionalExpressionTree.class);
    tree = (ConditionalExpressionTree) tree.falseExpression();
    assertThat(tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)).isTrue();
    assertThat(tree.condition()).isInstanceOf(LiteralTree.class);
    assertThat(tree.trueExpression()).isInstanceOf(LiteralTree.class);
    assertThat(tree.falseExpression()).isInstanceOf(LiteralTree.class);
  }

  /**
   * 15.26. Assignment Operators
   */
  @Test
  public void assignment_expression() {
    AssignmentExpressionTree tree = (AssignmentExpressionTree) p.parse("class T { void m() { a += 42; } }").getFirstDescendant(Kind.PLUS_ASSIGNMENT);
    assertThat(tree.is(Tree.Kind.PLUS_ASSIGNMENT)).isTrue();
    assertThat(tree.variable()).isNotNull();
    assertThat(tree.operatorToken().text()).isEqualTo("+=");
    assertThat(tree.expression()).isNotNull();
  }

  @Test
  public void method_reference_expression_should_not_break_AST() throws Exception {
    List<AstNode> methodReferences = p.parse(
        "class T { public void meth(){IntStream.range(1,12).map(new MethodReferences()::<String>square).map(super::myMethod).map(int[]::new).forEach(System.out::println);}}")
        .getDescendants(JavaLexer.METHOD_REFERENCE);
    assertThat(methodReferences).hasSize(4);
    MethodReferenceTree mrt = (MethodReferenceTree) methodReferences.get(0);
    assertThat(mrt.expression().is(Kind.NEW_CLASS)).isTrue();
    assertThat(mrt.doubleColon()).isNotNull();
    assertThat(mrt.typeArguments()).isNotNull();
    assertThat(mrt.method().name()).isEqualTo("square");

    mrt = (MethodReferenceTree) methodReferences.get(1);
    assertThat(mrt.expression().is(Kind.IDENTIFIER)).isTrue();
    assertThat(((IdentifierTree) mrt.expression()).name()).isEqualTo("super");
    assertThat(mrt.doubleColon()).isNotNull();

    mrt = (MethodReferenceTree) methodReferences.get(2);
    assertThat(mrt.expression().is(Kind.ARRAY_TYPE)).isTrue();
    assertThat(mrt.doubleColon()).isNotNull();
    assertThat(mrt.method().name()).isEqualTo("new");

    mrt = (MethodReferenceTree) methodReferences.get(3);
    assertThat(mrt.expression().is(Kind.MEMBER_SELECT)).isTrue();
    assertThat(mrt.doubleColon()).isNotNull();

  }

  // TODO Poor test
  @Test
  public void lambda_expressions_should_not_break_AST() {
    ExpressionTree expressionTree = (ExpressionTree) p.parse("class T { public void meth(){IntStream.range(1,12).map(x->x*x).map((int a)-> {return a*a;});}}").getFirstDescendant(
        Kind.METHOD_INVOCATION);
    assertThat(expressionTree).isNotNull();
  }

  @Test
  public void type_parameters_and_bounds() {
    TypeParameterListTreeImpl tree = (TypeParameterListTreeImpl) p.parse("class Foo<T, U extends Object & Number> {}").getFirstDescendant(JavaLexer.TYPE_PARAMETERS);
    assertThat(tree.openBracketToken().text()).isEqualTo("<");
    assertThat(tree.closeBracketToken().text()).isEqualTo(">");

    assertThat(tree).hasSize(2);

    TypeParameterTree param = tree.get(0);
    assertThat(param.identifier().name()).isEqualTo("T");
    assertThat(param.bounds()).isEmpty();

    param = tree.get(1);
    assertThat(param.identifier().name()).isEqualTo("U");
    assertThat(param.bounds()).hasSize(2);
    assertThat(((IdentifierTree) param.bounds().get(0)).name()).isEqualTo("Object");
    assertThat(((IdentifierTree) param.bounds().get(1)).name()).isEqualTo("Number");
  }

}
