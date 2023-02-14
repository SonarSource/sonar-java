/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
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
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.WildcardTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.assertions.TreeAssert.assertThat;

class JavaTreeModelTest {

  @Test
  void line_of_tree() {
    CompilationUnitTree empty = compilationUnit("");
    assertThat(((JavaTree) empty).getLine()).isEqualTo(1);

    ClassTree classTree = firstType("class A {}");
    assertThat(((JavaTree) classTree).getLine()).isEqualTo(1);
    assertThat(((JavaTree) classTree.modifiers()).getLine()).isEqualTo(-1);
  }

  @Test
  void primitive_type() {
    PrimitiveTypeTree tree = (PrimitiveTypeTree) ((MethodTree) firstTypeMember("class T { int m() { return null; } }")).returnType();
    assertThat(tree)
      .is(Tree.Kind.PRIMITIVE_TYPE)
      .hasChildrenSize(1);
    assertThat(tree.keyword()).is("int");
  }

  @Test
  void void_type() {
    PrimitiveTypeTree tree = (PrimitiveTypeTree) ((MethodTree) firstTypeMember("class T { void m() { return null; } }")).returnType();
    assertThat(tree)
      .is(Tree.Kind.PRIMITIVE_TYPE)
      .hasChildrenSize(1);
    assertThat(tree.keyword()).is("void");
  }

  @Test
  void type() {
    ArrayTypeTree tree = (ArrayTypeTree) ((MethodTree) firstTypeMember("class T { int[] m() { return null; } }")).returnType();
    assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
  }

  @Nested
  class Literals {
    @Test
    void int_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { int m() { return 1; } }");
      assertThat(tree)
        .is(Tree.Kind.INT_LITERAL)
        .hasChildrenSize(1)
        .hasValue("1");
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(28);
    }

    @Test
    void long_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { long m() { return 1L; } }");
      assertThat(tree)
        .is(Tree.Kind.LONG_LITERAL)
        .hasChildrenSize(1)
        .hasValue("1L");
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(29);
    }

    @Test
    void float_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { float m() { return 1F; } }");
      assertThat(tree)
        .is(Tree.Kind.FLOAT_LITERAL)
        .hasValue("1F")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(30);
    }

    @Test
    void double_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { double m() { return 1d; } }");
      assertThat(tree)
        .is(Tree.Kind.DOUBLE_LITERAL)
        .hasValue("1d")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(31);
    }

    @Test
    void boolean_true_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { boolean m() { return true; } }");
      assertThat(tree)
        .is(Tree.Kind.BOOLEAN_LITERAL)
        .hasValue("true")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(32);
    }

    @Test
    void boolean_false_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { boolean m() { return false; } }");
      assertThat(tree)
        .is(Tree.Kind.BOOLEAN_LITERAL)
        .hasValue("false")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(32);
    }

    @Test
    void char_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { char m() { return 'c'; } }");
      assertThat(tree)
        .is(Tree.Kind.CHAR_LITERAL)
        .hasValue("'c'")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(29);
    }

    @Test
    void string_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { String m() { return \"s\"; } }");
      assertThat(tree)
        .is(Tree.Kind.STRING_LITERAL)
        .hasValue("\"s\"")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(31);
    }

    @Test
    void null_literal() {
      LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { Object m() { return null; } }");
      assertThat(tree)
        .is(Tree.Kind.NULL_LITERAL)
        .hasValue("null")
        .hasChildrenSize(1);
      assertThat(tree.token())
        .isAtLine(1)
        .startsAtColumn(31);
    }
  }

  /**
   * Text Blocks
   * (Preview in Java 13) http://openjdk.java.net/jeps/355
   * (Second Preview in Java 14) https://openjdk.java.net/jeps/368
   * https://openjdk.java.net/jeps/378
   *
   * @see org.eclipse.jdt.core.dom.TextBlock
   */
  @Test
  void expression_text_block() {
    LiteralTree tree = (LiteralTree) expressionOfReturnStatement("class T { Object m() { return \"\"\"\ntext block\"\"\"; } }");
    assertThat(tree)
      .is(Tree.Kind.TEXT_BLOCK)
      .hasValue("\"\"\"\ntext block\"\"\"");
    assertThat(tree.token())
      .isAtLine(1)
      .startsAtColumn(31);
  }

  /**
   * Switch Expressions http://openjdk.java.net/jeps/361
   *
   * @see org.eclipse.jdt.core.dom.SwitchExpression
   * @see org.eclipse.jdt.core.dom.YieldStatement
   */
  @Test
  void expression_switch() {
    SwitchExpressionTree tree = (SwitchExpressionTree) expressionOfReturnStatement("class T { Object m() { return switch (0) { default -> 0; case 0 -> 0; }; } }");
    assertThat(tree).is(Tree.Kind.SWITCH_EXPRESSION);
    assertThat(tree.cases()).hasSize(2);

    YieldStatementTree statement = (YieldStatementTree) tree.cases().get(0).body().get(0);
    assertThat(statement).is(Tree.Kind.YIELD_STATEMENT);
    assertThat(statement.yieldKeyword()).as("implicit yield-statement").isNull();
    assertThat(statement.expression()).is(Tree.Kind.INT_LITERAL);
  }

  @Test
  void expression_switch_explicit_yield() {
    SwitchExpressionTree tree = (SwitchExpressionTree) expressionOfReturnStatement("class T { Object m() { return switch (0) { default: yield 0; case 0: yield 0; }; } }");
    assertThat(tree).is(Tree.Kind.SWITCH_EXPRESSION);
    assertThat(tree.cases()).hasSize(2);

    YieldStatementTree statement = (YieldStatementTree) tree.cases().get(0).body().get(0);
    assertThat(statement).is(Tree.Kind.YIELD_STATEMENT);
    assertThat(statement.yieldKeyword()).is("yield");
    assertThat(statement.expression()).is(Tree.Kind.INT_LITERAL);
  }

  @Test
  void compilation_unit_in_default_package() {
    CompilationUnitTree tree = compilationUnit("import foo; import bar; class Foo {} class Bar {}");
    assertThat(tree)
      .is(Tree.Kind.COMPILATION_UNIT)
      .hasChildrenSize(5);
    assertThat(tree.packageDeclaration()).isNull();
    assertThat(tree.imports()).hasSize(2);
    assertThat(tree.types()).hasSize(2);
  }

  @Test
  void compilation_unit_in_named_package() {
    CompilationUnitTree tree = compilationUnit("package pkg; import foo; import bar; class Foo {} class Bar {}");
    assertThat(tree)
      .is(Tree.Kind.COMPILATION_UNIT)
      .hasChildrenSize(6);
    assertThat(tree.packageDeclaration()).is(Tree.Kind.PACKAGE);
    assertThat(tree.imports()).hasSize(2);
    assertThat(tree.types()).hasSize(2);
  }

  @Test
  void empty_statement_after_imports() {
    CompilationUnitTree tree = compilationUnit("import foo; import bar; ; class Foo {} class Bar {}");
    assertThat(tree)
      .is(Tree.Kind.COMPILATION_UNIT)
      .hasChildrenSize(6);
    assertThat(tree.packageDeclaration()).isNull();
    assertThat(tree.imports()).hasSize(3);
    assertThat(tree.imports().get(2)).is(Tree.Kind.EMPTY_STATEMENT);
    assertThat(tree.types()).hasSize(2);
  }

  @Test
  void package_declaration() {
    PackageDeclarationTree tree = JParserTestUtils.parsePackage("package myPackage;").packageDeclaration();
    assertThat(tree)
      .is(Tree.Kind.PACKAGE)
      .hasChildrenSize(3);
    assertThat(tree.annotations()).isEmpty();
    assertThat(tree.packageKeyword()).is("package");
    assertThat(tree.packageName()).is(Tree.Kind.IDENTIFIER);
    assertThat(tree.semicolonToken()).is(";");
  }

  @Test
  void annotated_package_declaration() {
    PackageDeclarationTree tree = JParserTestUtils.parsePackage("@Foo @Bar package org.myPackage;").packageDeclaration();
    assertThat(tree)
      .is(Tree.Kind.PACKAGE)
      .hasChildrenSize(5);
    assertThat(tree.annotations()).hasSize(2);
    assertThat(tree.packageKeyword()).is("package");
    assertThat(tree.packageName()).is(Tree.Kind.MEMBER_SELECT);
    assertThat(tree.semicolonToken()).is(";");
  }

  @Nested
  class ImportStatements {
    @Test
    void import_declaration() {
      ImportClauseTree tree = compilationUnit(";").imports().get(0);
      assertThat(tree)
        .is(Tree.Kind.EMPTY_STATEMENT)
        .isNot(Tree.Kind.IMPORT);
    }

    @Test
    void import_qualified() {
      ImportTree importTree = (ImportTree) compilationUnit("import foo.Bar;").imports().get(0);
      assertThat(importTree)
        .is(Tree.Kind.IMPORT)
        .hasChildrenSize(3);
      assertThat(importTree.isStatic()).isFalse();
      assertThat(importTree.qualifiedIdentifier()).is(Tree.Kind.MEMBER_SELECT);
    }

    @Test
    void import_star() {
      ImportTree importTree = (ImportTree) compilationUnit("import foo.bar.*;").imports().get(0);
      assertThat(importTree)
        .is(Tree.Kind.IMPORT)
        .hasChildrenSize(3);
      assertThat(importTree.isStatic()).isFalse();
      assertThat(importTree.qualifiedIdentifier()).is(Tree.Kind.MEMBER_SELECT);
    }

    @Test
    void import_static() {
      ImportTree importTree = (ImportTree) compilationUnit("import static foo.Bar.method;").imports().get(0);
      assertThat(importTree)
        .is(Tree.Kind.IMPORT)
        .hasChildrenSize(4);
      assertThat(importTree.isStatic()).isTrue();
      assertThat(importTree.qualifiedIdentifier()).is(Tree.Kind.MEMBER_SELECT);
    }

    @Test
    void import_static_star() {
      ImportTree importTree = (ImportTree) compilationUnit("import static foo.Bar.*;").imports().get(0);
      assertThat(importTree)
        .is(Tree.Kind.IMPORT)
        .hasChildrenSize(4);
      assertThat(importTree.isStatic()).isTrue();
      assertThat(importTree.qualifiedIdentifier()).is(Tree.Kind.MEMBER_SELECT);
    }
  }

  /**
   * 4.5.1. Type Arguments and Wildcards
   */
  @Nested
  class TypeArgumentsAndWildcards {

    final VariableTree variableTree = (VariableTree) firstMethodFirstStatement("public class T { void m() { ClassType<? extends A, ? super B, ?, C> var; } }");
    final ParameterizedTypeTree parameterizedTypeTree = (ParameterizedTypeTree) variableTree.type();
    final TypeArguments typeArguments = parameterizedTypeTree.typeArguments();

    @Test
    void type_arguments() {
      assertThat(variableTree)
        .hasChildrenSize(4);
      assertThat(parameterizedTypeTree)
        .hasChildrenSize(2);
      assertThat(typeArguments)
        .hasSize(4)
        .hasSeparatorsSize(3)
        .hasChildrenSize(9);
      assertThat(typeArguments.get(3))
        .is(Tree.Kind.IDENTIFIER);
    }

    @Test
    void extends_wildcard() {
      WildcardTree wildcard = (WildcardTree) typeArguments.get(0);

      assertThat(wildcard)
        .is(Tree.Kind.EXTENDS_WILDCARD)
        .hasChildrenSize(3);
      assertThat(wildcard.bound()).is(Tree.Kind.IDENTIFIER);
      assertThat(wildcard.queryToken()).is("?");
      assertThat(wildcard.extendsOrSuperToken()).is("extends");
    }

    @Test
    void super_wildcard() {
      WildcardTree wildcard = (WildcardTree) typeArguments.get(1);

      assertThat(wildcard)
        .is(Tree.Kind.SUPER_WILDCARD)
        .hasChildrenSize(3);
      assertThat(wildcard.bound()).is(Tree.Kind.IDENTIFIER);
      assertThat(wildcard.queryToken()).is("?");
      assertThat(wildcard.extendsOrSuperToken()).is("super");
    }

    @Test
    void unbounded_wildcard() {
      WildcardTree wildcard = (WildcardTree) typeArguments.get(2);

      assertThat(wildcard)
        .is(Tree.Kind.UNBOUNDED_WILDCARD)
        .hasChildrenSize(1);
      assertThat(wildcard.bound()).isNull();
      assertThat(wildcard.queryToken()).is("?");
      assertThat(wildcard.extendsOrSuperToken()).isNull();
    }

    @Test
    void annotated_extends_wildcard() {
      VariableTree variableTree = (VariableTree) firstMethodFirstStatement("public class T { void m() { ClassType<@Foo ? extends A> var; } }");
      ParameterizedTypeTree parameterizedTypeTree = (ParameterizedTypeTree) variableTree.type();
      assertThat(parameterizedTypeTree).hasChildrenSize(2);

      TypeArguments typeArguments = parameterizedTypeTree.typeArguments();
      assertThat(typeArguments).hasChildrenSize(3);

      WildcardTree wildcard = (WildcardTree) typeArguments.get(0);
      assertThat(wildcard)
        .is(Tree.Kind.EXTENDS_WILDCARD)
        .hasChildrenSize(4);
      assertThat(wildcard.bound()).is(Tree.Kind.IDENTIFIER);
      assertThat(wildcard.queryToken()).is("?");
      assertThat(wildcard.annotations()).hasSize(1);
      assertThat(wildcard.extendsOrSuperToken()).is("extends");

    }

    @Test
    void annotated_extends_wildcard_bound() {
      VariableTree variableTree = (VariableTree) firstMethodFirstStatement("public class T { void m() { ClassType<? extends @Foo @Bar A> var; } }");
      ParameterizedTypeTree parameterizedTypeTree = (ParameterizedTypeTree) variableTree.type();
      assertThat(parameterizedTypeTree).hasChildrenSize(2);

      TypeArguments typeArguments = parameterizedTypeTree.typeArguments();
      assertThat(typeArguments).hasChildrenSize(3);

      WildcardTree wildcard = (WildcardTree) typeArguments.get(0);
      assertThat(wildcard)
        .is(Tree.Kind.EXTENDS_WILDCARD)
        .hasChildrenSize(3);
      assertThat(wildcard.bound()).is(Tree.Kind.IDENTIFIER);
      assertThat(wildcard.annotations()).isEmpty();
      assertThat(wildcard.queryToken()).is("?");
      assertThat(wildcard.extendsOrSuperToken()).is("extends");
    }
  }

  /*
   * 8. Classes
   */
  @Nested
  class Classes {
    @Test
    void extended() {
      ClassTree tree = firstType("public class T<U> extends C implements I1, I2 { }");
      assertThat(tree).is(Tree.Kind.CLASS);
      assertThat(tree.modifiers())
        .hasSize(1)
        .hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      TypeParameters typeParameters = tree.typeParameters();
      assertThat(typeParameters)
        .isNotEmpty()
        .hasEmptySeparators()
        .hasChildrenSize(3);
      assertThat(tree.openBraceToken()).is("{");
      assertThat(tree.superClass()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.superInterfaces())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.superInterfaces().separators().get(0)).is(",");
      assertThat(tree.closeBraceToken()).is("}");
      assertThat(tree.declarationKeyword()).is("class");
    }

    @Test
    void simple() {
      ClassTree tree = firstType("public class T { }");
      assertThat(tree.modifiers())
        .hasSize(1)
        .hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.typeParameters()).isEmpty();
      assertThat(tree.superClass()).isNull();
      assertThat(tree.superInterfaces()).isEmpty();
      assertThat(tree.declarationKeyword()).is("class");
    }

    @Test
    void parametrized() {
      ClassTree tree = firstType("class T<U,V> { }");
      assertThat(tree.modifiers()).isEmpty();
      assertThat(tree.simpleName()).hasName("T");
      TypeParameters typeParameters = tree.typeParameters();
      assertThat(typeParameters)
        .hasSize(2)
        .hasSeparatorsSize(1)
        .hasChildrenSize(5);
      assertThat(tree.superClass()).isNull();
      assertThat(tree.superInterfaces()).isEmpty();
      assertThat(tree.declarationKeyword()).is("class");
    }

    @Test
    void annotated() {
      ClassTree tree = firstType("@Deprecated class T { }");
      assertThat(tree).is(Tree.Kind.CLASS);
      assertThat(tree.modifiers()).hasAnnotations("@Deprecated");
      assertThat(tree.declarationKeyword()).is("class");
    }

    @Test
    void constructor() {
      MethodTree tree = (MethodTree) firstTypeMember("class T { T(int p1, int... p2) throws Exception1, Exception2 {} }");
      assertThat(tree)
        .is(Tree.Kind.CONSTRUCTOR)
        .hasChildrenSize(10);
      assertThat(tree.returnType()).isNull();
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.parameters()).hasSize(2);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.parameters().get(1).type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void fields() {
      List<Tree> declarations = firstType("class T { public int f1 = 42, f2[]; }").members();
      assertThat(declarations).hasSize(2);

      VariableTree f1 = (VariableTree) declarations.get(0);
      assertThat(f1)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(f1.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(f1.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(f1.simpleName()).hasName("f1");
      assertThat(f1.initializer()).is(Tree.Kind.INT_LITERAL);
      assertThat(f1.endToken()).is(",");

      VariableTree f2 = (VariableTree) declarations.get(1);
      assertThat(f2)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(4);
      assertThat(f2.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(f2.simpleName()).hasName("f2");
      assertThat(f2.initializer()).isNull();
      assertThat(f2.endToken()).is(";");
      assertThat(f2.type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat((ArrayTypeTree) f2.type()).isDeclaredArrayDimension();
    }

    @Test
    void initializer() {
      BlockTree tree = (BlockTree) firstTypeMember("class T { { ; ; } }");
      assertThat(tree)
        .is(Tree.Kind.INITIALIZER)
        .hasChildrenSize(4);
      assertThat(tree.body()).hasSize(2);
      assertThat(tree.openBraceToken()).is("{");
      assertThat(tree.closeBraceToken()).is("}");
    }

    @Test
    void static_initializer() {
      BlockTree tree = (BlockTree) firstTypeMember("class T { static { ; ; } }");
      assertThat(tree)
        .is(Tree.Kind.STATIC_INITIALIZER)
        .hasChildrenSize(5);
      StaticInitializerTree staticInitializerTree = (StaticInitializerTree) tree;
      assertThat(staticInitializerTree.body()).hasSize(2);
      assertThat(staticInitializerTree.staticKeyword()).is("static");
      assertThat(staticInitializerTree.openBraceToken()).is("{");
      assertThat(staticInitializerTree.closeBraceToken()).is("}");
    }

    @Test
    void method_declaration() {
      MethodTree tree = (MethodTree) firstTypeMember("class T { public int m(int p[][]){} }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(8);
      assertThat(tree.parameters()).hasSize(1);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(((ArrayTypeTree) tree.parameters().get(0).type()).type()).is(Tree.Kind.ARRAY_TYPE);
    }

    @Test
    void method_parametrized() {
      MethodTree tree = (MethodTree) firstTypeMember("class T { public <T> int m(@Annotate int p1, int... p2) throws Exception1, Exception2 {} }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(11);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.typeParameters()).isNotEmpty();
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(2);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.parameters().get(0).modifiers()).hasAnnotations("@Annotate");
      assertThat(tree.parameters().get(1).type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(tree.parameters().get(1).endToken()).isNull();
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void method_void() {
      MethodTree tree = (MethodTree) firstTypeMember("class T { public void m(int p) throws Exception1, Exception2 {} }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(10);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.typeParameters()).isEmpty();
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(1);
      assertThat(tree.parameters().get(0).endToken()).isNull();
      assertThat(tree.throwsClauses()).hasSize(2);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void method_varArg() {
      MethodTree tree = (MethodTree) firstTypeMember("class T { public int[] m(int p1, int... p2)[] throws Exception1, Exception2 {} }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(11);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.returnType()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(((ArrayTypeTree) tree.returnType()).type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(((ArrayTypeTree) ((ArrayTypeTree) tree.returnType()).type()).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(2);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.parameters().get(1).type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void method_array_brackets() {
      MethodTree tree = (MethodTree) firstTypeMember("class T { public int m()[] { return null; } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(7);
      assertThat(tree.parameters()).isEmpty();
      assertThat(tree.returnType())
        .is(Tree.Kind.ARRAY_TYPE)
        .isNot(Tree.Kind.PRIMITIVE_TYPE);
    }
  }

  @Nested
  class Annotations {
    private List<AnnotationTree> annotations(String code) {
      return firstType(code).modifiers().annotations();
    }

    @Test
    void with_parameter() {
      List<AnnotationTree> annotations = annotations("@SuppressWarnings(\"unchecked\") class T { }");
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation).hasChildrenSize(3);
      assertThat(annotation.annotationType()).is(Tree.Kind.IDENTIFIER);
      assertThat(annotation.atToken()).is("@");

      Arguments arguments = annotation.arguments();
      assertThat(arguments)
        .hasEmptySeparators()
        .hasSize(1);
      assertThat(arguments.get(0)).is(Tree.Kind.STRING_LITERAL);
      assertThat(arguments.openParenToken()).is("(");
      assertThat(arguments.closeParenToken()).is(")");
    }

    @Test
    void without_parameter_but_parenthesis() {
      List<AnnotationTree> annotations = annotations("@Target( ) class U {}");
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation).hasChildrenSize(3);
      assertThat(annotation.atToken()).is("@");

      Arguments arguments = annotation.arguments();
      assertThat(arguments)
        .hasEmptySeparators()
        .isEmpty();
      assertThat(arguments.openParenToken()).is("(");
      assertThat(arguments.closeParenToken()).is(")");
    }

    @Test
    void with_array_parameter() {
      List<AnnotationTree> annotations = annotations("@Target({ElementType.METHOD}) class U {}");
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation).hasChildrenSize(3);
      assertThat(annotation.atToken()).is("@");

      Arguments arguments = annotation.arguments();
      assertThat(arguments)
        .hasEmptySeparators()
        .hasSize(1);
      assertThat(arguments.get(0)).is(Tree.Kind.NEW_ARRAY);
      assertThat(arguments.openParenToken()).is("(");
      assertThat(arguments.closeParenToken()).is(")");
    }

    @Test
    void with_array_parameter_and_extra_comma() {
      List<AnnotationTree> annotations = annotations("@SuppressWarnings({\"hello\",}) class U {}");
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation).hasChildrenSize(3);
      assertThat(annotation.atToken()).is("@");

      Arguments arguments = annotation.arguments();
      assertThat(arguments).hasSize(1);
      assertThat(arguments.get(0)).is(Tree.Kind.NEW_ARRAY);
      assertThat(arguments.openParenToken()).is("(");
      assertThat(arguments.closeParenToken()).is(")");

      ListTree<ExpressionTree> initializers = ((NewArrayTree) arguments.get(0)).initializers();
      assertThat(initializers)
        .hasSeparatorsSize(1)
        .hasSize(1);
      assertThat(initializers.get(0)).is(Tree.Kind.STRING_LITERAL);
    }

    @Test
    void with_multiple_parameters() {
      List<AnnotationTree> annotations = annotations("@Target(value={ElementType.METHOD}, value2=\"toto\") class T { }");
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation).hasChildrenSize(3);
      assertThat(annotation.atToken()).is("@");
      assertThat(annotation.annotationType()).is(Tree.Kind.IDENTIFIER);

      Arguments arguments = annotation.arguments();
      assertThat(arguments)
        .hasSeparatorsSize(1)
        .hasSize(2);
      assertThat(arguments.get(0)).is(Tree.Kind.ASSIGNMENT);
      assertThat(arguments.openParenToken()).is("(");
      assertThat(arguments.closeParenToken()).is(")");
    }

    @Test
    void on_package() {
      List<AnnotationTree> annotations = JParserTestUtils.parsePackage("@PackageLevelAnnotation package blammy;").packageDeclaration().annotations();
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation.atToken()).is("@");
      assertThat(annotation.arguments()).isEmpty();
      assertThat(annotation).hasChildrenSize(3);
    }

    @Test
    void on_variable_type() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("class T { private void meth() { @Foo String str; } }");
      assertThat(variable).hasChildrenSize(4);

      List<AnnotationTree> annotations = variable.modifiers().annotations();
      assertThat(annotations).hasSize(1);

      AnnotationTree annotation = annotations.get(0);
      assertThat(annotation).hasChildrenSize(3);
      assertThat(annotation.annotationType()).is(Tree.Kind.IDENTIFIER);
      assertThat(annotation.atToken()).is("@");
      assertThat(annotation.arguments()).isEmpty();
    }

    @Test
    void on_variable_fully_qualifed_type() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("class T { private void m() { @Foo java.lang.Integer foo; } }");
      assertThat(variable).hasChildrenSize(4);
      assertThat(variable.modifiers().annotations()).hasSize(1);

      TypeTree type = variable.type();
      assertThat(type).is(Tree.Kind.MEMBER_SELECT);
      assertThat(type.annotations()).isEmpty();
    }

    @Test
    void inside_fully_qualified_name() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("class T { private void m() { java.lang.@Foo Integer foo; } }");
      assertThat(variable).hasChildrenSize(4);
      assertThat(variable.modifiers()).isEmpty();

      TypeTree type = variable.type();
      assertThat(type).is(Tree.Kind.MEMBER_SELECT);
      assertThat(type.annotations()).isEmpty();
      assertThat(((MemberSelectExpressionTree) type).identifier().annotations()).hasSize(1);
    }

    @Test
    void on_local_class_constructor_call() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("class T { private void m() { a.B.C foo = a.B.new @Foo C(); } }");
      assertThat(variable).hasChildrenSize(6);
      assertThat(variable.modifiers()).isEmpty();

      TypeTree type = ((NewClassTree) variable.initializer()).identifier();
      assertThat(type)
        .is(Tree.Kind.IDENTIFIER)
        .hasChildrenSize(2);
      assertThat(type.annotations()).hasSize(1);
    }

    @Test
    void on_new_array_dimension() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("class T { private void m() { int[] foo = new @Foo int[42]; } }");
      assertThat(variable).hasChildrenSize(6);
      assertThat(variable.modifiers()).isEmpty();

      TypeTree type = ((NewArrayTree) variable.initializer()).type();
      assertThat(type)
        .is(Tree.Kind.PRIMITIVE_TYPE)
        .hasChildrenSize(2);
      assertThat(type.annotations()).hasSize(1);
    }

    @Test
    void within_catch_clause() {
      VariableTree variable = ((TryStatementTree) firstMethodFirstStatement("class T { private void m() { try{ } catch (@Foo E1 | E2 e) {}; } }")).catches().get(0).parameter();
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.modifiers()).hasSize(1);

      TypeTree type = variable.type();
      assertThat(type)
        .is(Tree.Kind.UNION_TYPE)
        .hasChildrenSize(1);
      assertThat(type.annotations()).isEmpty();
      assertThat(((UnionTypeTree) type).typeAlternatives()).hasSeparatorsSize(1);
    }

    @Test
    void on_new_inner_class() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("package a; class B { private void m() { a.B.C foo = new a.B. @Foo C(); } class C {}}");
      assertThat(variable).hasChildrenSize(6);
      assertThat(variable.modifiers()).isEmpty();

      TypeTree type = ((NewClassTree) variable.initializer()).identifier();
      assertThat(type)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(type.annotations()).isEmpty();

      TypeTree abc = ((MemberSelectExpressionTree) type).identifier();
      assertThat(abc).hasChildrenSize(2);
      assertThat(abc.annotations()).hasSize(1);
    }

    @Test
    void on_new_parametrized_inner_class() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("class T { private void m() { a.B.C<Integer> foo = a.B.new @Foo C<Integer>(); }}");
      assertThat(variable).hasChildrenSize(6);
      assertThat(variable.modifiers()).isEmpty();

      TypeTree type = ((NewClassTree) variable.initializer()).identifier();
      assertThat(type)
        .is(Tree.Kind.PARAMETERIZED_TYPE)
        .hasChildrenSize(2);
      assertThat(type.annotations()).isEmpty();

      TypeTree abc = ((ParameterizedTypeTree) type).type();
      assertThat(abc)
        .is(Tree.Kind.IDENTIFIER)
        .hasChildrenSize(2);
      assertThat(abc.annotations()).hasSize(1);
    }

    @Test
    void within_parametrized_type_fully_qualified_name() {
      VariableTree variable = (VariableTree) firstMethodFirstStatement("package a.b; class C<T> { private void m() { a.b.C<Integer> foo = new a.b. @Foo C<Integer>(); } }");
      assertThat(variable).hasChildrenSize(6);
      assertThat(variable.modifiers()).isEmpty();

      TypeTree type = ((NewClassTree) variable.initializer()).identifier();
      assertThat(type)
        .is(Tree.Kind.PARAMETERIZED_TYPE)
        .hasChildrenSize(2);
      assertThat(type.annotations()).isEmpty();

      TypeTree abC = ((ParameterizedTypeTree) type).type();
      assertThat(abC)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(abC.annotations()).isEmpty();

      TypeTree fooC = ((MemberSelectExpressionTree) abC).identifier();
      assertThat(fooC).hasChildrenSize(2);
      assertThat(fooC.annotations()).hasSize(1);
    }

    @Test
    void within_extended_interface_fully_qualified_name() {
      ClassTree classTree = firstType("class T extends a.b.@Foo C {}");
      assertThat(classTree).hasChildrenSize(10);
      assertThat(classTree.modifiers()).isEmpty();

      TypeTree type = classTree.superClass();
      assertThat(type)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(type.annotations()).isEmpty();

      TypeTree abC = ((MemberSelectExpressionTree) type).identifier();
      assertThat(abC).hasChildrenSize(2);
      assertThat(abC.annotations()).hasSize(1);
    }

    @Test
    void within_extended_parametrized_interface_fully_qualified_name() {
      ClassTree classTree = firstType("class T extends a.b. @Foo C<Integer> {}");
      assertThat(classTree).hasChildrenSize(10);
      assertThat(classTree.modifiers()).isEmpty();

      TypeTree superClass = classTree.superClass();
      assertThat(superClass)
        .is(Tree.Kind.PARAMETERIZED_TYPE)
        .hasChildrenSize(2);
      assertThat(superClass.annotations()).isEmpty();

      TypeTree abC = ((ParameterizedTypeTree) superClass).type();
      assertThat(abC)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(abC.annotations()).isEmpty();

      TypeTree fooC = ((MemberSelectExpressionTree) abC).identifier();
      assertThat(fooC).hasChildrenSize(2);
      assertThat(fooC.annotations()).hasSize(1);
    }

    @Test
    void on_extended_interface() {
      ClassTree classTree = (ClassTree) firstMethodFirstStatement("class MyClass<A, B, C> { void foo() { class MyOtherClass extends @Foo MyClass<A, B, C>.MyInnerClass {} } class MyInnerClass {}}");
      assertThat(classTree).hasChildrenSize(10);
      assertThat(classTree.modifiers()).isEmpty();

      TypeTree type = classTree.superClass();
      assertThat(type).is(Tree.Kind.MEMBER_SELECT);
      assertThat(type.annotations()).isEmpty();

      TypeTree myInnerClass = (ParameterizedTypeTree) ((MemberSelectExpressionTree) type).expression();
      assertThat(myInnerClass)
        .is(Tree.Kind.PARAMETERIZED_TYPE)
        .hasChildrenSize(2);
      assertThat(myInnerClass.annotations()).isEmpty();

      TypeTree myClass = ((ParameterizedTypeTree) myInnerClass).type();
      assertThat(myClass)
        .is(Tree.Kind.IDENTIFIER)
        .hasChildrenSize(2);
      assertThat(myClass.annotations()).hasSize(1);
    }

    @Test
    void on_cast() {
      TypeCastTree typeCast = (TypeCastTree) ((ReturnStatementTree) firstMethodFirstStatement("class T { private long m(int a) { return (@Foo long) a; } }")).expression();
      assertThat(typeCast).hasChildrenSize(5);

      TypeTree type = typeCast.type();
      assertThat(type)
        .is(Tree.Kind.PRIMITIVE_TYPE)
        .hasChildrenSize(2);
      assertThat(type.annotations()).hasSize(1);
    }

    @Test
    void annotations_in_for_each_statements() {
      ForEachStatement tree = (ForEachStatement) firstMethodFirstStatement("class C { void foo(Object[] values) { for(@Nullable Object value : values) { } } }");
      assertThat(tree.variable().modifiers()).hasAnnotations("@Nullable");
    }
  }

  /*
   * 8.9. Enums
   */
  @Nested
  class Enums {
    @Test
    void enum_declaration() {
      ClassTree tree = firstType("public enum T implements I1, I2 { }");
      assertThat(tree)
        .is(Tree.Kind.ENUM)
        .hasChildrenSize(9);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.superClass()).isNull();
      assertThat(tree.superInterfaces()).hasSize(2);
      assertThat(tree.declarationKeyword()).is("enum");
    }

    @Test
    void simple_enum() {
      ClassTree tree = firstType("public enum T { }");
      assertThat(tree)
        .is(Tree.Kind.ENUM)
        .hasChildrenSize(8);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.superClass()).isNull();
      assertThat(tree.openBraceToken()).is("{");
      assertThat(tree.closeBraceToken()).is("}");
      assertThat(tree.superInterfaces()).isEmpty();
      assertThat(tree.declarationKeyword()).is("enum");
    }

    @Test
    void enum_constant() {
      List<Tree> declarations = firstType("enum T { C1, C2(2) { }; }").members();
      assertThat(declarations).hasSize(2);

      EnumConstantTree c1 = (EnumConstantTree) declarations.get(0);
      assertThat(c1)
        .is(Tree.Kind.ENUM_CONSTANT)
        .hasChildrenSize(3);
      assertThat(c1.simpleName()).hasName("C1");
      assertThat(c1.separatorToken()).is(",");
      NewClassTree c1Initializer = c1.initializer();
      assertThat(c1Initializer.arguments()).isEmpty();
      assertThat(c1Initializer.classBody()).isNull();
      assertThat(c1Initializer.newKeyword()).isNull();
      assertThat(c1Initializer).hasChildrenSize(2);

      EnumConstantTree c2 = (EnumConstantTree) declarations.get(1);
      assertThat(c2)
        .is(Tree.Kind.ENUM_CONSTANT)
        .hasChildrenSize(3);
      assertThat(c2.simpleName()).hasName("C2");
      assertThat(c2.separatorToken()).is(";");
      NewClassTree c2Initializer = c2.initializer();
      assertThat(c2Initializer.arguments().openParenToken()).is("(");
      assertThat(c2Initializer.arguments()).hasSize(1);
      assertThat(c2Initializer.arguments().closeParenToken()).is(")");
      assertThat(c2Initializer.classBody()).is(Tree.Kind.CLASS);
      assertThat(c2Initializer.classBody().openBraceToken()).is("{");
      assertThat(c2Initializer.newKeyword()).isNull();
      assertThat(c2Initializer).hasChildrenSize(3);
    }

    @Test
    void enum_field() {
      List<Tree> declarations = firstType("enum T { ; public int f1 = 42, f2[]; }").members();
      assertThat(declarations).hasSize(3);

      assertThat(declarations.get(0)).is(Tree.Kind.EMPTY_STATEMENT);

      VariableTree f1 = (VariableTree) declarations.get(1);
      assertThat(f1)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(f1.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(f1.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(f1.simpleName()).hasName("f1");
      assertThat(f1.initializer()).is(Tree.Kind.INT_LITERAL);

      VariableTree f2 = (VariableTree) declarations.get(2);
      assertThat(f2)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(4);
      assertThat(f2.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(f2.simpleName()).hasName("f2");
      assertThat(f2.initializer()).isNull();
      assertThat((ArrayTypeTree) f2.type()).isDeclaredArrayDimension();
    }

    @Test
    void enum_constructor() {
      MethodTree tree = (MethodTree) firstType("enum T { ; T(int p1, int... p2) throws Exception1, Exception2 {} }").members().get(1);
      assertThat(tree)
        .is(Tree.Kind.CONSTRUCTOR)
        .hasChildrenSize(10);
      assertThat(tree.returnType()).isNull();
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.parameters()).hasSize(2);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat((ArrayTypeTree) tree.parameters().get(1).type()).isVarArg();
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void enum_method() {
      MethodTree tree = (MethodTree) firstType("enum T { ; int m(int p1, int... p2) throws Exception1, Exception2 {} }").members().get(1);
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(11);
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(2);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat((ArrayTypeTree) tree.parameters().get(1).type()).isVarArg();
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void enum_void_method() {
      MethodTree tree = (MethodTree) firstType("enum T { ; void m(int p) throws Exception1, Exception2; }").members().get(1);
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(10);
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(1);
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).isNull();
      assertThat(tree.defaultValue()).isNull();
    }
  }

  /*
   * 9. Interfaces
   */
  @Nested
  class Interfaces {
    @Test
    void interface_declaration_with_extensions() {
      ClassTree tree = firstType("public interface T<U> extends I1, I2 { }");
      assertThat(tree)
        .is(Tree.Kind.INTERFACE)
        .hasChildrenSize(9);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.superClass()).isNull();
      assertThat(tree.superInterfaces())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.declarationKeyword()).is("interface");
      TypeParameters typeParameters = tree.typeParameters();
      assertThat(typeParameters)
        .hasChildrenSize(3)
        .isNotEmpty();
    }

    @Test
    void interface_declaration() {
      ClassTree tree = firstType("public interface T { }");
      assertThat(tree)
        .is(Tree.Kind.INTERFACE)
        .hasChildrenSize(8);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.typeParameters()).isEmpty();
      assertThat(tree.superClass()).isNull();
      assertThat(tree.openBraceToken()).is("{");
      assertThat(tree.closeBraceToken()).is("}");
      assertThat(tree.superInterfaces()).isEmpty();
      assertThat(tree.declarationKeyword()).is("interface");
    }

    @Test
    void interface_field() {
      List<Tree> declarations = firstType("interface T { public int f1 = 42, f2[] = { 13 }; }").members();
      assertThat(declarations).hasSize(2);

      VariableTree f1 = (VariableTree) declarations.get(0);
      assertThat(f1)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(f1.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(f1.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(f1.simpleName()).hasName("f1");
      assertThat(f1.initializer()).is(Tree.Kind.INT_LITERAL);

      VariableTree f2 = (VariableTree) declarations.get(1);
      assertThat(f2)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(f2.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(f2.simpleName()).hasName("f2");
      assertThat(f2.initializer()).is(Tree.Kind.NEW_ARRAY);
      assertThat(f2.type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat((ArrayTypeTree) f2.type()).isDeclaredArrayDimension();
    }

    @Test
    void interface_method() {
      MethodTree tree = (MethodTree) firstTypeMember("interface T { <T> int m(int p1, int... p2) throws Exception1, Exception2; }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(11);
      assertThat(tree.typeParameters()).isNotEmpty();
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(2);
      assertThat(tree.parameters().get(0).type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.parameters().get(1).type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).isNull();
      assertThat(tree.defaultValue()).isNull();
    }

    @Test
    void interface_void_method() {
      MethodTree tree = (MethodTree) firstTypeMember("interface T { void m(int p) throws Exception1, Exception2; }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(10);
      assertThat(tree.typeParameters()).isEmpty();
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).hasSize(1);
      assertThat(tree.throwsClauses())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.block()).isNull();
      assertThat(tree.defaultValue()).isNull();
    }
  }

  /*
   * 9.6. Annotation Types
   */
  @Nested
  class AnnotationTypes {
    @Test
    void annotation_declaration() {
      ClassTree tree = firstType("public @interface T { }");
      assertThat(tree)
        .is(Tree.Kind.ANNOTATION_TYPE)
        .hasChildrenSize(9);
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree.simpleName()).hasName("T");
      assertThat(tree.superClass()).isNull();
      assertThat(tree.openBraceToken()).is("{");
      assertThat(tree.closeBraceToken()).is("}");
      assertThat(tree.superInterfaces()).isEmpty();
      assertThat(tree.declarationKeyword()).is("interface");
    }

    @Test
    void annotation_method() {
      MethodTree tree = (MethodTree) firstTypeMember("@interface T { int m() default 0; }");
      assertThat(tree)
        .is(Tree.Kind.METHOD)
        .hasChildrenSize(9);
      assertThat(tree.returnType()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("m");
      assertThat(tree.parameters()).isEmpty();
      assertThat(tree.throwsClauses()).isEmpty();
      assertThat(tree.block()).isNull();
      assertThat(tree.defaultValue()).is(Tree.Kind.INT_LITERAL);
    }

    @Test
    void annotation_public_method() {
      MethodTree tree = (MethodTree) firstTypeMember("@interface T { public String method(); }");
      assertThat(tree.modifiers()).hasModifiers(Modifier.PUBLIC);
      assertThat(tree).hasChildrenSize(7);
    }

    @Test
    void annotation_constant() {
      List<Tree> members = firstType("@interface T { int c1 = 1, c2[] = { 2 }; }").members();
      assertThat(members).hasSize(2);

      VariableTree c1 = (VariableTree) members.get(0);
      assertThat(c1)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6); // 1+5, as empty modifiers are always included
      assertThat(c1.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(c1.simpleName()).hasName("c1");
      assertThat(c1.initializer()).is(Tree.Kind.INT_LITERAL);

      VariableTree c2 = (VariableTree) members.get(1);
      assertThat(c2)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(c2.type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat((ArrayTypeTree) c2.type()).isDeclaredArrayDimension();
      assertThat(c2.simpleName()).hasName("c2");
      assertThat(c2.initializer()).is(Tree.Kind.NEW_ARRAY);
    }
  }

  /*
   * 14. Blocks and Statements
   */

  /**
   * 14.2. Blocks
   */
  @Test
  void blocks() {
    BlockTree tree = ((MethodTree) firstTypeMember("class T { void m() { ; ; } }")).block();
    assertThat(tree).is(Tree.Kind.BLOCK);
    assertThat(tree.openBraceToken()).is("{");
    assertThat(tree.body()).hasSize(2);
    assertThat(tree.closeBraceToken()).is("}");
    assertThat(tree).hasChildrenSize(4);
  }

  /**
   * 14.3. Local Class Declarations
   */
  @Test
  void local_class_declaration() {
    BlockTree block = ((MethodTree) firstTypeMember("class T { void m() { abstract class Local { } } }")).block();
    ClassTree tree = (ClassTree) block.body().get(0);
    assertThat(tree)
      .is(Tree.Kind.CLASS)
      .hasChildrenSize(8);
    assertThat(tree.simpleName().identifierToken()).is("Local");
    assertThat(tree.modifiers()).hasModifiers(Modifier.ABSTRACT);
  }

  /**
   * 14.4. Local Variable Declaration Statements
   */
  @Nested
  class LocalVariables {

    final List<StatementTree> declarations = ((MethodTree) firstTypeMember("class T { void m() { int a = 42, b[]; final @Nullable int c = 42; } }"))
      .block()
      .body();

    @Test
    void with_initializer() {
      VariableTree tree = (VariableTree) declarations.get(0);
      assertThat(tree)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(tree.modifiers().modifiers()).isEmpty();
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("a");
      assertThat(tree.initializer()).is(Tree.Kind.INT_LITERAL);
      assertThat(tree.endToken()).is(",");
    }

    @Test
    void without_initialize() {
      VariableTree tree = (VariableTree) declarations.get(1);
      assertThat(tree)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(4);
      assertThat(tree.modifiers().modifiers()).isEmpty();
      assertThat(tree.type()).is(Tree.Kind.ARRAY_TYPE);
      assertThat((ArrayTypeTree) tree.type()).isDeclaredArrayDimension();
      assertThat(tree.simpleName()).hasName("b");
      assertThat(tree.initializer()).isNull();
      assertThat(tree.endToken()).is(";");
    }

    @Test
    void with_annotation() {
      VariableTree tree = (VariableTree) declarations.get(2);
      assertThat(tree)
        .is(Tree.Kind.VARIABLE)
        .hasChildrenSize(6);
      assertThat(tree.modifiers())
        .hasModifiers(Modifier.FINAL)
        .hasAnnotations("@Nullable")
        .hasSize(2);
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.simpleName()).hasName("c");
      assertThat(tree.initializer()).is(Tree.Kind.INT_LITERAL);
      assertThat(tree.endToken()).is(";");
    }
  }

  /**
   * 14.6. The Empty Statement
   */
  @Test
  void empty_statement() {
    EmptyStatementTree tree = (EmptyStatementTree) firstMethodFirstStatement("class T { void m() { ; } }");
    assertThat(tree)
      .is(Tree.Kind.EMPTY_STATEMENT)
      .hasChildrenSize(1);
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.7. Labeled Statements
   */
  @Test
  void labeled_statement() {
    LabeledStatementTree tree = (LabeledStatementTree) firstMethodFirstStatement("class T { void m() { label: ; } }");
    assertThat(tree)
      .is(Tree.Kind.LABELED_STATEMENT)
      .hasChildrenSize(3);
    assertThat(tree.label()).hasName("label");
    assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    assertThat(tree.colonToken()).is(":");
  }

  /**
   * 14.8. Expression Statements
   */
  @Test
  void expression_statement() {
    ExpressionStatementTree tree = (ExpressionStatementTree) firstMethodFirstStatement("class T { void m() { i++; } }");
    assertThat(tree)
      .is(Tree.Kind.EXPRESSION_STATEMENT)
      .hasChildrenSize(2);
    assertThat(tree.expression()).is(Tree.Kind.POSTFIX_INCREMENT);
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.9. The if Statement
   */
  @Test
  void if_statement_without_else() {
    IfStatementTree tree = (IfStatementTree) firstMethodFirstStatement("class T { void m() { if (true) { } } }");
    assertThat(tree)
      .is(Tree.Kind.IF_STATEMENT)
      .hasChildrenSize(5);
    assertThat(tree.ifKeyword()).is("if");
    assertThat(tree.openParenToken()).is("(");
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.closeParenToken()).is(")");
    assertThat(tree.thenStatement()).is(Tree.Kind.BLOCK);
    assertThat(tree.elseKeyword()).isNull();
    assertThat(tree.elseStatement()).isNull();
  }

  @Test
  void if_statement_with_else() {
    IfStatementTree tree = (IfStatementTree) firstMethodFirstStatement("class T { void m() { if (true) { } else { } } }");
    assertThat(tree)
      .is(Tree.Kind.IF_STATEMENT)
      .hasChildrenSize(7);
    assertThat(tree.ifKeyword()).is("if");
    assertThat(tree.openParenToken()).is("(");
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.closeParenToken()).is(")");
    assertThat(tree.thenStatement()).is(Tree.Kind.BLOCK);
    assertThat(tree.elseKeyword()).is("else");
    assertThat(tree.elseStatement()).is(Tree.Kind.BLOCK);
  }

  /**
   * 14.10. The assert Statement
   */
  @Test
  void assert_statement() {
    AssertStatementTree tree = (AssertStatementTree) firstMethodFirstStatement("class T { void m() { assert true; } }");
    assertThat(tree)
      .is(Tree.Kind.ASSERT_STATEMENT)
      .hasChildrenSize(3);
    assertThat(tree.assertKeyword()).is("assert");
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.colonToken()).isNull();
    assertThat(tree.detail()).isNull();
    assertThat(tree.semicolonToken()).is(";");
  }

  @Test
  void assert_statement_with_detail() {
    AssertStatementTree tree = (AssertStatementTree) firstMethodFirstStatement("class T { void m() { assert true : \"detail\"; } }");
    assertThat(tree)
      .is(Tree.Kind.ASSERT_STATEMENT)
      .hasChildrenSize(5);
    assertThat(tree.assertKeyword()).is("assert");
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.colonToken()).is(":");
    assertThat(tree.detail()).is(Tree.Kind.STRING_LITERAL);
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.11. The switch Statement
   */
  @Nested
  class Switches {
    final SwitchStatementTree switchStatement = (SwitchStatementTree) firstMethodFirstStatement("class T {"
      + "  void m(int e) {"
      + "    switch (e) {"
      + "      case 1: "
      + "      case 2, 3:"
      + "        ;"
      + "      default:"
      + "        ;"
      + "    }"
      + "  }"
      + "}");
    final CaseGroupTree firstCaseGroup = switchStatement.cases().get(0);
    final CaseGroupTree secondCaseGroup = switchStatement.cases().get(1);

    @Test
    void mixed_cases() {
      assertThat(firstCaseGroup).is(Tree.Kind.CASE_GROUP);
      assertThat(firstCaseGroup.labels()).hasSize(2);
      assertThat(firstCaseGroup.body()).hasSize(1);
    }

    @Test
    void single_case() {
      assertThat(secondCaseGroup).is(Tree.Kind.CASE_GROUP);
      assertThat(secondCaseGroup.labels()).hasSize(1);
      assertThat(secondCaseGroup.body()).hasSize(1);
    }

    @Test
    void simple_case() {
      CaseLabelTree c1 = firstCaseGroup.labels().get(0);
      assertThat(c1).hasChildrenSize(3);
      assertThat(c1.isFallThrough()).isTrue();
      assertThat(c1.caseOrDefaultKeyword()).is("case");
      assertThat(c1.expressions()).hasSize(1);
      assertThat(((LiteralTree) c1.expressions().get(0))).hasValue("1");
      assertThat(c1.colonOrArrowToken()).is(":");
    }

    @Test
    void combined_case() {
      CaseLabelTree c23 = firstCaseGroup.labels().get(1);
      assertThat(c23).hasChildrenSize(4);
      assertThat(c23.isFallThrough()).isTrue();
      assertThat(c23.caseOrDefaultKeyword()).is("case");
      assertThat(c23.expressions()).hasSize(2);
      assertThat(((LiteralTree) c23.expressions().get(0))).hasValue("2");
      assertThat(((LiteralTree) c23.expressions().get(1))).hasValue("3");
      assertThat(c23.colonOrArrowToken()).is(":");
    }

    @Test
    void default_case() {
      CaseLabelTree cDefault = secondCaseGroup.labels().get(0);
      assertThat(cDefault).hasChildrenSize(2);
      assertThat(cDefault.isFallThrough()).isTrue();
      assertThat(cDefault.caseOrDefaultKeyword()).is("default");
      assertThat(cDefault.colonOrArrowToken()).is(":");
    }

    @Test
    void switch_statement() {
      SwitchStatementTree tree = (SwitchStatementTree) firstMethodFirstStatement("class T { void m(int e) { switch (e) { case 1: case 2, 3: ; default: ; } } }");
      assertThat(tree).is(Tree.Kind.SWITCH_STATEMENT);
      assertThat(tree.switchKeyword()).is("switch");
      assertThat(tree.openBraceToken()).is("{");
      assertThat(tree.closeBraceToken()).is("}");
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.cases()).hasSize(2);
      assertThat(tree)
        .isInstanceOf(ExpressionTree.class)
        .isInstanceOf(SwitchExpressionTree.class);
    }

    @Test
    void switch_expression() {
      ReturnStatementTree rst = (ReturnStatementTree) firstMethodFirstStatement("class T {\n" +
      "  int m(int e) {\n" +
      "    return switch (e) {\n" +
      "      case 1 -> 0;\n" +
      "      case 2, 3 -> 1;\n" +
      "      default -> 42;\n" +
      "    };\n" +
      "  }\n" +
      "}");
      SwitchExpressionTree switchExpression = (SwitchExpressionTree) rst.expression();
      assertThat(switchExpression.cases()).hasSize(3);
      CaseGroupTree c = switchExpression.cases().get(1);
      assertThat(c.labels()).hasSize(1); // "case 2, 3" should have labels size == 2, ECJ does not count number of labels correctly
      CaseLabelTree caseLabelTree = c.labels().get(0);
      assertThat(caseLabelTree.isFallThrough()).isFalse();
      assertThat(caseLabelTree.caseOrDefaultKeyword()).is("case");
      assertThat(caseLabelTree.colonOrArrowToken()).is("->");
      assertThat(c.body()).hasSize(1);
      assertThat(switchExpression).isNotInstanceOf(SwitchStatementTree.class);
      assertThat(switchExpression.symbolType().name()).isEqualTo("int");
    }

    @Test
    void switch_with_only_default() {
      SwitchStatementTree tree = (SwitchStatementTree) firstMethodFirstStatement("class T { void m() { switch (e) { default: } } }");
      assertThat(tree.cases()).hasSize(1);
      assertThat(tree.cases().get(0).body()).isEmpty();
      assertThat(tree).hasChildrenSize(7);
    }
  }

  /**
   * 14.12. The while Statement
   */
  @Test
  void while_statement() {
    WhileStatementTree tree = (WhileStatementTree) firstMethodFirstStatement("class T { void m() { while (true) ; } }");
    assertThat(tree)
      .is(Tree.Kind.WHILE_STATEMENT)
      .hasChildrenSize(5);
    assertThat(tree.whileKeyword()).is("while");
    assertThat(tree.openParenToken()).is("(");
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.closeParenToken()).is(")");
    assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
  }

  /**
   * 14.13. The do Statement
   */
  @Test
  void do_statement() {
    DoWhileStatementTree tree = (DoWhileStatementTree) firstMethodFirstStatement("class T { void m() { do ; while (true); } }");
    assertThat(tree)
      .is(Tree.Kind.DO_STATEMENT)
      .hasChildrenSize(7);
    assertThat(tree.doKeyword()).is("do");
    assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    assertThat(tree.whileKeyword()).is("while");
    assertThat(tree.openParenToken()).is("(");
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.closeParenToken()).is(")");
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.14. The for Statement
   */
  @Nested
  class ForStatements {
    @Test
    void with_declared_variable() {
      ForStatementTree tree = (ForStatementTree) firstMethodFirstStatement("class T { void m() { for (int i = 0; i < 42; i ++) ; } }");
      assertThat(tree)
        .is(Tree.Kind.FOR_STATEMENT)
        .hasChildrenSize(9);
      assertThat(tree.forKeyword()).is("for");
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.initializer()).hasSize(1);
      assertThat(tree.initializer().get(0)).is(Tree.Kind.VARIABLE);
      assertThat(tree.initializer().get(0)).hasChildrenSize(5);
      assertThat(tree.firstSemicolonToken()).is(";");
      assertThat(tree.condition()).is(Tree.Kind.LESS_THAN);
      assertThat(tree.secondSemicolonToken()).is(";");
      assertThat(tree.update()).is(Tree.Kind.LIST);
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    }

    @Test
    void with_variable() {
      ForStatementTree tree = (ForStatementTree) firstMethodFirstStatement("class T { void m() { for (i = 0; i < 42; i ++) ; } }");
      assertThat(tree)
        .is(Tree.Kind.FOR_STATEMENT)
        .hasChildrenSize(9);
      assertThat(tree.initializer()).hasSize(1);
      assertThat(tree.initializer().get(0)).is(Tree.Kind.EXPRESSION_STATEMENT);
      assertThat(tree.condition()).is(Tree.Kind.LESS_THAN);
      assertThat(tree.update()).is(Tree.Kind.LIST);
      assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    }

    @Test
    void empty() {
      ForStatementTree tree = (ForStatementTree) firstMethodFirstStatement("class T { void m() { for ( ; ; ) ; } }");
      assertThat(tree)
        .is(Tree.Kind.FOR_STATEMENT)
        .hasChildrenSize(8);
      assertThat(tree.initializer()).isEmpty();
      assertThat(tree.condition()).isNull();
      assertThat(tree.update()).isEmpty();
      assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    }

    @Test
    void multiple_variables() {
      ForStatementTree tree = (ForStatementTree) firstMethodFirstStatement("class T { void m() { for (i = 0, j = 1; i < 42; i++, j--) ; } }");
      assertThat(tree)
        .is(Tree.Kind.FOR_STATEMENT)
        .hasChildrenSize(9);
      assertThat(tree.initializer())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.condition()).is(Tree.Kind.LESS_THAN);
      assertThat(tree.update())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    }

    @Test
    void multiple_variables_some_delcared() {
      ForStatementTree tree = (ForStatementTree) firstMethodFirstStatement("class T { void m() { for (int i = 0, j = 1; i < 42; i++, j--) ; } }");
      assertThat(tree)
        .is(Tree.Kind.FOR_STATEMENT)
        .hasChildrenSize(9);
      assertThat(tree.initializer())
        .hasSize(2)
        .hasEmptySeparators();
      assertThat(tree.condition()).is(Tree.Kind.LESS_THAN);
      assertThat(tree.update())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    }

    @Test
    void foreach_statement() {
      ForEachStatement tree = (ForEachStatement) firstMethodFirstStatement("class T { void m() { for (Object o : objects) ; } }");
      assertThat(tree)
        .is(Tree.Kind.FOR_EACH_STATEMENT)
        .hasChildrenSize(7);
      assertThat(tree.forKeyword()).is("for");
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.variable()).is(Tree.Kind.VARIABLE);
      assertThat(tree.colonToken()).is(":");
      assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.statement()).is(Tree.Kind.EMPTY_STATEMENT);
    }
  }

  /**
   * 14.15. The break Statement
   */
  @Test
  void break_statement() {
    BreakStatementTree tree = (BreakStatementTree) firstMethodFirstStatement("class T { void m() { break ; } }");
    assertThat(tree)
      .is(Tree.Kind.BREAK_STATEMENT)
      .hasChildrenSize(2);
    assertThat(tree.breakKeyword()).is("break");
    assertThat(tree.label()).isNull();
    assertThat(tree.semicolonToken()).is(";");

  }

  @Test
  void break_statement_With_label() {
    BreakStatementTree tree = (BreakStatementTree) firstMethodFirstStatement("class T { void m() { break label ; } }");
    assertThat(tree)
      .is(Tree.Kind.BREAK_STATEMENT)
      .hasChildrenSize(3);
    assertThat(tree.breakKeyword()).is("break");
    assertThat(tree.label()).hasName("label");
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.16. The continue Statement
   */
  @Test
  void continue_statement() {
    ContinueStatementTree tree = (ContinueStatementTree) firstMethodFirstStatement("class T { void m() { continue ; } }");
    assertThat(tree)
      .is(Tree.Kind.CONTINUE_STATEMENT)
      .hasChildrenSize(2);
    assertThat(tree.continueKeyword()).is("continue");
    assertThat(tree.label()).isNull();
    assertThat(tree.semicolonToken()).is(";");
  }

  @Test
  void continue_statement_with_label() {
    ContinueStatementTree tree = (ContinueStatementTree) firstMethodFirstStatement("class T { void m() { continue label ; } }");
    assertThat(tree)
      .is(Tree.Kind.CONTINUE_STATEMENT)
      .hasChildrenSize(3);
    assertThat(tree.continueKeyword()).is("continue");
    assertThat(tree.label()).hasName("label");
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.17. The return Statement
   */
  @Test
  void return_statement_without_expression() {
    ReturnStatementTree tree = (ReturnStatementTree) firstMethodFirstStatement("class T { boolean m() { return ; } }");
    assertThat(tree)
      .is(Tree.Kind.RETURN_STATEMENT)
      .hasChildrenSize(2);
    assertThat(tree.returnKeyword()).is("return");
    assertThat(tree.expression()).isNull();
    assertThat(tree.semicolonToken()).is(";");
  }

  @Test
  void return_statement() {
    ReturnStatementTree tree = (ReturnStatementTree) firstMethodFirstStatement("class T { boolean m() { return true; } }");
    assertThat(tree)
      .is(Tree.Kind.RETURN_STATEMENT)
      .hasChildrenSize(3);
    assertThat(tree.returnKeyword()).is("return");
    assertThat(tree.expression()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.18. The throw Statement
   */
  @Test
  void throw_statement() {
    ThrowStatementTree tree = (ThrowStatementTree) firstMethodFirstStatement("class T { void m() { throw e; } }");
    assertThat(tree)
      .is(Tree.Kind.THROW_STATEMENT)
      .hasChildrenSize(3);
    assertThat(tree.throwKeyword()).is("throw");
    assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
    assertThat(tree.semicolonToken()).is(";");
  }

  /**
   * 14.19. The synchronized Statement
   */
  @Test
  void synchronized_statement() {
    SynchronizedStatementTree tree = (SynchronizedStatementTree) firstMethodFirstStatement("class T { void m() { synchronized(e) { } } }");
    assertThat(tree)
      .is(Tree.Kind.SYNCHRONIZED_STATEMENT)
      .hasChildrenSize(5);
    assertThat(tree.synchronizedKeyword()).is("synchronized");
    assertThat(tree.openParenToken()).is("(");
    assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
    assertThat(tree.closeParenToken()).is(")");
    assertThat(tree.block()).is(Tree.Kind.BLOCK);
  }

  /**
   * 14.20. The try statement
   */
  @Nested
  class TryStatements {
    @Test
    void try_finally() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try { } finally { } } }");
      assertThat(tree)
        .is(Tree.Kind.TRY_STATEMENT)
        .hasChildrenSize(4);
      assertThat(tree.resourceList()).isEmpty();
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.catches()).isEmpty();
      assertThat(tree.finallyKeyword()).is("finally");
      assertThat(tree.finallyBlock()).is(Tree.Kind.BLOCK);
    }

    @Test
    void try_catches() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try { } catch (RuntimeException e1) { } catch (Exception e2) { } } }");
      assertThat(tree)
        .is(Tree.Kind.TRY_STATEMENT)
        .hasChildrenSize(4);
      assertThat(tree.tryKeyword()).is("try");
      assertThat(tree.openParenToken()).isNull();
      assertThat(tree.resourceList()).isEmpty();
      assertThat(tree.closeParenToken()).isNull();
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.finallyKeyword()).isNull();
      assertThat(tree.finallyBlock()).isNull();
      assertThat(tree.catches()).hasSize(2);

      CatchTree catchTreeRuntimeException = tree.catches().get(0);
      assertThat(catchTreeRuntimeException.catchKeyword()).is("catch");
      assertThat(catchTreeRuntimeException.block()).is(Tree.Kind.BLOCK);
      assertThat(catchTreeRuntimeException.openParenToken()).is("(");
      assertThat(catchTreeRuntimeException.closeParenToken()).is(")");
      assertThat(catchTreeRuntimeException).hasChildrenSize(5);

      VariableTree parameterTreeRuntimeException = catchTreeRuntimeException.parameter();
      assertThat(parameterTreeRuntimeException.type()).is(Tree.Kind.IDENTIFIER);
      assertThat(parameterTreeRuntimeException.simpleName()).hasName("e1");
      assertThat(parameterTreeRuntimeException.initializer()).isNull();
      assertThat(parameterTreeRuntimeException).hasChildrenSize(3);

      CatchTree catchTreeException = tree.catches().get(1);
      assertThat(catchTreeException).hasChildrenSize(5);

      VariableTree parameterTreeException = catchTreeException.parameter();
      assertThat(parameterTreeException.type()).is(Tree.Kind.IDENTIFIER);
      assertThat(parameterTreeException.simpleName()).hasName("e2");
      assertThat(parameterTreeException.initializer()).isNull();
      assertThat(parameterTreeException).hasChildrenSize(3);
    }

    @Test
    void try_catch_finally() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try { } catch (Exception e) { } finally { } } }");
      assertThat(tree)
        .is(Tree.Kind.TRY_STATEMENT)
        .hasChildrenSize(5);
      assertThat(tree.resourceList()).isEmpty();
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.catches()).hasSize(1);
      assertThat(tree.finallyKeyword()).is("finally");
      assertThat(tree.catches().get(0)).hasChildrenSize(5);
      assertThat(tree.finallyBlock()).is(Tree.Kind.BLOCK);
    }

    @Test
    void try_catch_annotated() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try { } catch (final @Foo Exception e) { } } }");
      assertThat(tree)
        .is(Tree.Kind.TRY_STATEMENT)
        .hasChildrenSize(3);
      assertThat(tree.resourceList()).isEmpty();
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.catches()).hasSize(1);
      assertThat(tree.finallyKeyword()).isNull();
      assertThat(tree.finallyBlock()).isNull();

      CatchTree catchTreeException = tree.catches().get(0);
      assertThat(catchTreeException).hasChildrenSize(5);

      VariableTree parameterTreeException = catchTreeException.parameter();
      assertThat(parameterTreeException.modifiers()).hasSize(2);
      assertThat(parameterTreeException.simpleName()).hasName("e");
      assertThat(parameterTreeException.type()).is(Tree.Kind.IDENTIFIER);
      assertThat(parameterTreeException.endToken()).isNull();
      assertThat(parameterTreeException.initializer()).isNull();
      assertThat(parameterTreeException).hasChildrenSize(3);
    }

    @Test
    void try_with_resource() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (final @Foo Resource r = open()) { } } }");
      assertThat(tree).is(Tree.Kind.TRY_STATEMENT).hasChildrenSize(5);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.catches()).isEmpty();
      assertThat(tree.finallyKeyword()).isNull();
      assertThat(tree.finallyBlock()).isNull();
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.resourceList()).hasSize(1);
      assertThat(tree.closeParenToken()).is(")");

      VariableTree resource = (VariableTree) tree.resourceList().get(0);
      assertThat(resource.simpleName()).hasName("r");
      assertThat(resource.initializer()).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(resource.modifiers()).hasSize(2);
    }

    @Test
    void try_with_multiple_resources() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (Resource r1 = open(); Resource r2 = open()) { } catch (Exception e) { } finally { } } }");
      assertThat(tree).is(Tree.Kind.TRY_STATEMENT);
      assertThat(tree.block()).is(Tree.Kind.BLOCK);
      assertThat(tree.catches()).hasSize(1);
      assertThat(tree.finallyKeyword()).is("finally");
      assertThat(tree.catches().get(0)).hasChildrenSize(5);
      assertThat(tree.finallyBlock()).is(Tree.Kind.BLOCK);
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.resourceList()).hasSize(2).hasSeparatorsSize(1);
      assertThat(tree.closeParenToken()).is(")");

      VariableTree r1 = (VariableTree) tree.resourceList().get(0);
      assertThat(r1.simpleName()).hasName("r1");
      assertThat(r1.initializer()).is(Tree.Kind.METHOD_INVOCATION);

      VariableTree r2 = (VariableTree) tree.resourceList().get(1);
      assertThat(r2.simpleName()).hasName("r2");
      assertThat(r2.initializer()).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(tree).hasChildrenSize(8);
    }

    @Test
    void try_catch_with_union_type() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try { } catch (Exception1 | Exception2 e) { } } }");
      assertThat(tree).hasChildrenSize(3);

      VariableTree parameterTree = tree.catches().get(0).parameter();
      assertThat(parameterTree).hasChildrenSize(3);

      UnionTypeTree type = (UnionTypeTree) parameterTree.type();
      assertThat(type).hasChildrenSize(1);
      assertThat(type.typeAlternatives())
        .hasSize(2)
        .hasSeparatorsSize(1);
    }

    @Test
    void try_with_reused_resource() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (r1) { } } }");
      assertThat(tree.resourceList())
        .hasSize(1)
        .hasEmptySeparators();
    }

    @Test
    void try_with_reused_resource_and_field() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (r1; super.field; new A().f) { } } }");
      assertThat(tree.resourceList())
        .hasSize(3)
        .hasSeparatorsSize(2);
    }

    @Test
    void try_with_reused_resource_and_declaration_and_extra_semicolon() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (r1; Resource r2 = open();) { } } }");
      assertThat(tree.resourceList())
        .hasSize(2)
        .hasSeparatorsSize(2);
    }

    @Test
    void try_with_reused_resource_and_declaration() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (r1; Resource r2 = open()) { } } }");
      assertThat(tree.resourceList())
        .hasSize(2)
        .hasSeparatorsSize(1);
    }

    @Test
    void try_with_reused_resource_and_declaration_and_extra_semicolon_inverted_order() {
      TryStatementTree tree = (TryStatementTree) firstMethodFirstStatement("class T { void m() { try (Resource r2 = open(); r1;) { } } }");
      assertThat(tree.resourceList())
        .hasSize(2)
        .hasSeparatorsSize(2);
    }
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
  @Nested
  class ClassLiterals {
    @Test
    void void_class() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { m() { return void.class; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.identifier()).hasName("class");
      assertThat(tree.operatorToken()).is(".");
    }

    @Test
    void int_class() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { m() { return int.class; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.identifier()).hasName("class");
      assertThat(tree.operatorToken()).is(".");
    }

    @Test
    void array_class() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { m() { return int[].class; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.ARRAY_TYPE);
      assertThat((ArrayTypeTree) tree.expression()).isDeclaredArrayDimension();
      assertThat(tree.identifier()).hasName("class");
      assertThat(tree.operatorToken()).is(".");
    }

    @Test
    void type_class() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { m() { return T.class; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.identifier()).hasName("class");
      assertThat(tree.operatorToken()).is(".");
    }

    @Test
    void type_array_class() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { m() { return T[].class; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.ARRAY_TYPE);
      assertThat((ArrayTypeTree) tree.expression()).isDeclaredArrayDimension();
      assertThat(tree.identifier()).hasName("class");
      assertThat(tree.operatorToken()).is(".");
    }
  }

  /**
   * 15.8.3. this
   */
  @Test
  void this_expression() {
    IdentifierTree tree = (IdentifierTree) expressionOfReturnStatement("class T { Object m() { return this; } }");
    assertThat(tree)
      .is(Tree.Kind.IDENTIFIER)
      .hasName("this")
      .hasChildrenSize(1);
    assertThat(tree.identifierToken()).is("this");
  }

  /**
   * 15.8.4. Qualified this
   */
  @Test
  void qualified_this() {
    MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { Object m() { return ClassName.this; } }");
    assertThat(tree)
      .is(Tree.Kind.MEMBER_SELECT)
      .hasChildrenSize(3);
    assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
    assertThat(tree.identifier()).hasName("this");
    assertThat(tree.identifier().identifierToken()).is("this");
  }

  /**
   * 15.8.5. Parenthesized Expressions
   */
  @Test
  void parenthesized_expression() {
    ParenthesizedTree tree = (ParenthesizedTree) expressionOfReturnStatement("class T { boolean m() { return (true); } }");
    assertThat(tree)
      .is(Tree.Kind.PARENTHESIZED_EXPRESSION)
      .hasChildrenSize(3);
    assertThat(tree.openParenToken()).is("(");
    assertThat(tree.expression()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.closeParenToken()).is(")");
  }

  /**
   * 15.9. Class Instance Creation Expressions
   */
  @Nested
  class ClassInstanceCreationExpressions {
    @Test
    void constructor() {
      NewClassTree tree = (NewClassTree) expressionOfReturnStatement("class T { T m() { return new T(true, false) {}; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_CLASS)
        .hasChildrenSize(4);
      assertThat(tree.enclosingExpression()).isNull();
      assertThat(tree.dotToken()).isNull();
      assertThat(tree.arguments().openParenToken()).is("(");
      assertThat(tree.arguments())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.arguments().closeParenToken()).is(")");
      assertThat(tree.identifier()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.classBody()).is(Tree.Kind.CLASS);
      assertThat(tree.newKeyword()).is("new");
      assertThat(tree.typeArguments()).isNull();
    }

    @Test
    void enclosingClass() {
      NewClassTree tree = (NewClassTree) expressionOfReturnStatement("class T { T m() { return Enclosing.new T(true, false) {}; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_CLASS)
        .hasChildrenSize(6);
      assertThat(tree.enclosingExpression()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.dotToken()).is(".");
      assertThat(tree.identifier()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.arguments().openParenToken()).is("(");
      assertThat(tree.arguments())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.arguments().closeParenToken()).is(")");
      assertThat(tree.classBody()).is(Tree.Kind.CLASS);
      assertThat(tree.newKeyword()).is("new");
      assertThat(tree.typeArguments()).isNull();
    }

    @Test
    void enclosingThis() {
      NewClassTree tree = (NewClassTree) expressionOfReturnStatement("class T { T m() { return this.new T(true, false) {}; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_CLASS)
        .hasChildrenSize(6);
      assertThat(tree.enclosingExpression()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.dotToken()).is(".");
      assertThat(tree.identifier()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.arguments().openParenToken()).is("(");
      assertThat(tree.arguments())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.arguments().closeParenToken()).is(")");
      assertThat(tree.classBody()).is(Tree.Kind.CLASS);
      assertThat(tree.newKeyword()).is("new");
      assertThat(tree.typeArguments()).isNull();
    }

    @Test
    void parametrizedType() {
      NewClassTree tree = (NewClassTree) ((VariableTree) firstMethodFirstStatement("class T { void m() { Foo myInt = new<Integer>Foo(42); } }")).initializer();
      assertThat(tree)
        .is(Tree.Kind.NEW_CLASS)
        .hasChildrenSize(4);
      assertThat(tree.enclosingExpression()).isNull();
      assertThat(tree.dotToken()).isNull();
      assertThat(tree.identifier()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.typeArguments())
        .is(Tree.Kind.TYPE_ARGUMENTS)
        .hasSize(1);
      assertThat(tree.arguments().openParenToken()).is("(");
      assertThat(tree.arguments())
        .hasSize(1)
        .hasEmptySeparators();
      assertThat(tree.arguments().closeParenToken()).is(")");
    }
  }

  /**
   * 15.10. Array Creation Expressions
   */
  @Nested
  class ArrayCreationExpression {
    @Test
    void with_initializers() {
      NewArrayTree tree = (NewArrayTree) expressionOfReturnStatement("class T { int[][] m() { return new int[][]{{1}, {2, 3}}; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_ARRAY)
        .hasChildrenSize(7);
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.dimensions()).hasSize(2);
      assertThat(tree.initializers())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(tree.newKeyword()).is("new");

      ArrayDimensionTree dim1 = tree.dimensions().get(0);
      assertThat(dim1)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(2);
      assertThat(dim1.annotations()).isEmpty();
      assertThat(dim1.openBracketToken()).is("[");
      assertThat(dim1.expression()).isNull();
      assertThat(dim1.closeBracketToken()).is("]");

      ArrayDimensionTree dim2 = tree.dimensions().get(1);
      assertThat(dim2)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(2);
      assertThat(dim2.annotations()).isEmpty();
      assertThat(dim2.openBracketToken()).is("[");
      assertThat(dim2.expression()).isNull();
      assertThat(dim2.closeBracketToken()).is("]");

      NewArrayTree firstDim = (NewArrayTree) tree.initializers().get(0);
      assertThat(firstDim.initializers())
        .hasSize(1)
        .hasEmptySeparators();
      assertThat(firstDim).hasChildrenSize(3);

      NewArrayTree secondDim = (NewArrayTree) tree.initializers().get(1);
      assertThat(secondDim.initializers())
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(secondDim).hasChildrenSize(3);
    }

    @Test
    void with_dimensions() {
      NewArrayTree tree = (NewArrayTree) expressionOfReturnStatement("class T { int[][] m() { return new int[2][2]; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_ARRAY)
        .hasChildrenSize(5);
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.dimensions()).hasSize(2);
      assertThat(tree.initializers()).isEmpty();
      assertThat(tree.newKeyword()).is("new");

      ArrayDimensionTree dimension = tree.dimensions().get(0);
      assertThat(dimension)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(3);
      assertThat(dimension.annotations()).isEmpty();
      assertThat(dimension.openBracketToken()).is("[");
      assertThat(dimension.expression()).is(Tree.Kind.INT_LITERAL);
      assertThat(dimension.closeBracketToken()).is("]");
    }

    @Test
    void with_anotations_and_initializers() {
      NewArrayTree tree = (NewArrayTree) expressionOfReturnStatement("class T { int[][] m() { return new int[] @Bar [] {{}, {}}; } }");
      assertThat(tree).is(Tree.Kind.NEW_ARRAY);
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.dimensions()).hasSize(2);
      assertThat(tree.initializers()).hasSize(2);
      assertThat(tree.newKeyword()).is("new");
      assertThat(tree).hasChildrenSize(7);

      ArrayDimensionTree dim1 = tree.dimensions().get(0);
      assertThat(dim1)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(2);
      assertThat(dim1.annotations()).isEmpty();
      assertThat(dim1.openBracketToken()).is("[");
      assertThat(dim1.expression()).isNull();
      assertThat(dim1.closeBracketToken()).is("]");

      ArrayDimensionTree dim2 = tree.dimensions().get(1);
      assertThat(dim2)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(3);
      assertThat(dim2.annotations()).hasSize(1);
      assertThat(dim2.openBracketToken()).is("[");
      assertThat(dim2.expression()).isNull();
      assertThat(dim2.closeBracketToken()).is("]");
    }

    @Test
    void with_anotations_and_dimensions() {
      NewArrayTree tree = (NewArrayTree) expressionOfReturnStatement("class T { int[][] m() { return new int[2] @Bar [3]; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_ARRAY)
        .hasChildrenSize(5);
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.dimensions()).hasSize(2);
      assertThat(tree.initializers()).isEmpty();
      assertThat(tree.newKeyword()).is("new");

      ArrayDimensionTree dim1 = tree.dimensions().get(0);
      assertThat(dim1)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(3);
      assertThat(dim1.annotations()).isEmpty();
      assertThat(dim1.openBracketToken()).is("[");
      assertThat(dim1.expression()).is(Tree.Kind.INT_LITERAL);
      assertThat(dim1.closeBracketToken()).is("]");

      ArrayDimensionTree dim2 = tree.dimensions().get(1);
      assertThat(dim2)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(4);
      assertThat(dim2.annotations()).hasSize(1);
      assertThat(dim2.openBracketToken()).is("[");
      assertThat(dim2.expression()).is(Tree.Kind.INT_LITERAL);
      assertThat(dim2.closeBracketToken()).is("]");
    }

    @Test
    void with_anotations() {
      NewArrayTree tree = (NewArrayTree) expressionOfReturnStatement("class T { int[] m() { return new int @Foo [2]; } }");
      assertThat(tree)
        .is(Tree.Kind.NEW_ARRAY)
        .hasChildrenSize(4);
      assertThat(tree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(tree.dimensions()).hasSize(1);
      assertThat(tree.initializers()).isEmpty();
      assertThat(tree.newKeyword()).is("new");

      ArrayDimensionTree dimension = tree.dimensions().get(0);
      assertThat(dimension)
        .is(Tree.Kind.ARRAY_DIMENSION)
        .hasChildrenSize(4);
      assertThat(dimension.annotations()).hasSize(1);
      assertThat(dimension.openBracketToken()).is("[");
      assertThat(dimension.expression()).is(Tree.Kind.INT_LITERAL);
      assertThat(dimension.closeBracketToken()).is("]");
    }

    @Test
    void with_empty_initializers() {
      NewArrayTree tree = (NewArrayTree) ((VariableTree) firstMethodFirstStatement("class T { void m() { int[] a = {,}; }}")).initializer();
      assertThat(tree).is(Tree.Kind.NEW_ARRAY);
      assertThat(tree.type()).isNull();
      assertThat(tree.dimensions()).isEmpty();
      assertThat(tree.initializers())
        // FIXME missing separator
        .hasEmptySeparators()
        .isEmpty();
    }
  }

  /**
   * 15.11. Field Access Expressions
   */
  @Nested
  class FieldAccesExpressions {
    @Test
    void field_access_expression() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { int m() { return super.identifier; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.identifier()).hasName("identifier");
      assertThat(tree.operatorToken()).is(".");
    }

    @Test
    void field_access_expression_in_nested_classes() {
      MemberSelectExpressionTree tree = (MemberSelectExpressionTree) expressionOfReturnStatement("class T { int m() { return ClassName.super.identifier; } }");
      assertThat(tree)
        .is(Tree.Kind.MEMBER_SELECT)
        .hasChildrenSize(3);
      assertThat(tree.expression()).is(Tree.Kind.MEMBER_SELECT);
      assertThat(tree.identifier()).hasName("identifier");
      assertThat(tree.operatorToken()).is(".");
    }
  }

  /**
   * 15.12. Method Invocation Expressions
   */
  // TODO test NonWildTypeArguments
  @Nested
  class MethodInvocationExpressions {

    private MethodInvocationTree firstMethodInvocation(String code) {
      return (MethodInvocationTree) ((ExpressionStatementTree) firstMethodFirstStatement(code)).expression();
    }

    @Test
    void simple() {
      MethodInvocationTree tree = firstMethodInvocation("class T { void m() { identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(2);
      assertThat(((IdentifierTree) tree.methodSelect())).hasName("identifier");
      Arguments arguments = tree.arguments();
      assertThat(arguments.openParenToken()).is("(");
      assertThat(arguments)
        .hasSize(2)
        .hasSeparatorsSize(1);
      assertThat(arguments.closeParenToken()).is(")");
    }

    @Test
    void parametrized() {
      MethodInvocationTree tree = firstMethodInvocation("class T { void m() { this.<T>identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(3);
      MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(memberSelectExpression.identifier()).hasName("identifier");
      assertThat(tree.arguments()).hasSize(2);
    }

    @Test
    void enclosed_super_invocation() {
      MethodInvocationTree tree = firstMethodInvocation("class T { T() { TypeName.super.identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(2);
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).isNull();

      MemberSelectExpressionTree firstMemberSelect = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(firstMemberSelect.identifier()).hasName("identifier");
      assertThat(firstMemberSelect.operatorToken()).is(".");

      MemberSelectExpressionTree secondMemberSelect = (MemberSelectExpressionTree) firstMemberSelect.expression();
      assertThat(secondMemberSelect).hasChildrenSize(3);
      assertThat(secondMemberSelect.identifier()).hasName("super");
      assertThat(secondMemberSelect.operatorToken()).is(".");
      assertThat(((IdentifierTree) secondMemberSelect.expression())).hasName("TypeName");
    }

    @Test
    void super_invocation() {
      MethodInvocationTree tree = firstMethodInvocation("class T { T() { super.identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(2);
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).isNull();

      MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(memberSelectExpression).hasChildrenSize(3);
      assertThat(memberSelectExpression.identifier()).hasName("identifier");
      assertThat(memberSelectExpression.operatorToken()).is(".");
      assertThat(((IdentifierTree) memberSelectExpression.expression())).hasName("super");
    }

    @Test
    void enclosed_invocation() {
      MethodInvocationTree tree = firstMethodInvocation("class T { T() { TypeName.identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(2);
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).isNull();

      MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(memberSelectExpression).hasChildrenSize(3);
      assertThat(memberSelectExpression.identifier()).hasName("identifier");
      assertThat(memberSelectExpression.operatorToken()).is(".");
      assertThat(((IdentifierTree) memberSelectExpression.expression())).hasName("TypeName");
    }

    @Test
    void enclosed_parametrized_invocation() {
      MethodInvocationTree tree = firstMethodInvocation("class T { T() { TypeName.<T>identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(3);
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).hasSize(1);

      MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(memberSelectExpression).hasChildrenSize(3);
      assertThat(memberSelectExpression.identifier()).hasName("identifier");
      assertThat(memberSelectExpression.operatorToken()).is(".");
      assertThat(((IdentifierTree) memberSelectExpression.expression())).hasName("TypeName");
    }

    @Test
    void method_invocation_expression() {
      MethodInvocationTree tree = firstMethodInvocation("class T { T() { primary().<T>identifier(true, false); } }");
      assertThat(tree)
        .is(Tree.Kind.METHOD_INVOCATION)
        .hasChildrenSize(3);
      assertThat(tree.arguments()).hasSize(2);

      MemberSelectExpressionTree memberSelectExpression = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(memberSelectExpression).hasChildrenSize(3);
      assertThat(memberSelectExpression.identifier()).hasName("identifier");
      assertThat(memberSelectExpression.expression()).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(memberSelectExpression.operatorToken()).is(".");
    }
  }

  /**
   * 8.8.7.1. Explicit Constructor Invocations
   */
  // TODO test NonWildTypeArguments
  @Nested
  class ExplicitConstructorInvocations {

    private MethodInvocationTree firstConstructorInvocation(String code) {
      return (MethodInvocationTree) ((ExpressionStatementTree) firstMethodFirstStatement(code)).expression();
    }

    @Test
    void this_invocation() {
      MethodInvocationTree tree = firstConstructorInvocation("class T { T() { this(true, false); } }");
      assertThat(tree).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(((IdentifierTree) tree.methodSelect())).hasName("this");
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).isNull();
    }

    @Test
    void parametrized_this() {
      MethodInvocationTree tree = firstConstructorInvocation("class T { T() { <T>this(true, false); } }");
      assertThat(tree).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(((IdentifierTree) tree.methodSelect())).hasName("this");
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).hasSize(1);
    }

    @Test
    void super_invocation() {
      MethodInvocationTree tree = firstConstructorInvocation("class T { T() { super(true, false); } }");
      assertThat(tree).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(((IdentifierTree) tree.methodSelect())).hasName("super");
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).isNull();
    }

    @Test
    void parametrized_super() {
      MethodInvocationTree tree = firstConstructorInvocation("class T { T() { <T>super(true, false); } }");
      assertThat(tree).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(((IdentifierTree) tree.methodSelect())).hasName("super");
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).hasSize(1);
    }

    @Test
    void qualified_super() {
      MethodInvocationTree tree = firstConstructorInvocation("class T { T() { ClassName.super(true, false); } }");
      assertThat(tree).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).isNull();

      MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(methodSelect).hasChildrenSize(3);
      assertThat(methodSelect.identifier()).hasName("super");
      assertThat(methodSelect.operatorToken()).is(".");
      assertThat(((IdentifierTree) methodSelect.expression())).hasName("ClassName");
    }

    @Test
    void qualified_parametrized_super() {
      MethodInvocationTree tree = firstConstructorInvocation("class T { T() { ClassName.<T>super(true, false); } }");
      assertThat(tree).is(Tree.Kind.METHOD_INVOCATION);
      assertThat(tree.arguments()).hasSize(2);
      assertThat(tree.typeArguments()).hasSize(1);

      MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) tree.methodSelect();
      assertThat(methodSelect).hasChildrenSize(3);
      assertThat(methodSelect.identifier()).hasName("super");
      assertThat(methodSelect.operatorToken()).is(".");
      assertThat(((IdentifierTree) methodSelect.expression())).hasName("ClassName");
    }
  }

  @Nested
  class ArrayFields {
    @Test
    void single_dimension_array() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int[] a; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void two_dimension_array() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int[][] a; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void annotated_dimension() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int @Foo [] a; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void double_annotated_dimension() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int @Foo @bar [] a; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(2)
        .isDeclaredArrayDimension()
        .hasChildrenSize(5);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void two_dimension_but_one_annotated() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int[] @Foo [] a; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void different_dimension_position() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int[] a[]; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree  arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void different_dimension_position_with_annotation() {
      VariableTree field = (VariableTree) firstTypeMember("class T { int[] a @Foo []; }");
      assertThat(field).hasChildrenSize(4);
      assertThat(field.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) field.type();
      assertThat(arrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }
  }

  @Nested
  class ArrayAsMethodReturnType {
    @Test
    void simple() {
      MethodTree method = (MethodTree) firstTypeMember("class T { int[] m(); }");
      assertThat(method.returnType()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) method.returnType();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void annotated_dimenion() {
      MethodTree method = (MethodTree) firstTypeMember("class T { int @Foo [] m(); }");
      assertThat(method.returnType()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) method.returnType();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void double_annotated_dimensions() {
      MethodTree method = (MethodTree) firstTypeMember("class T { int @Foo @bar [] m(); }");
      assertThat(method.returnType()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) method.returnType();
      assertThat(arrayTypeTree)
        .hasAnnotations(2)
        .isDeclaredArrayDimension()
        .hasChildrenSize(5);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void annotation_between_dimensions() {
      MethodTree method = (MethodTree) firstTypeMember("class T { int[] @Foo [] m(); }");
      assertThat(method.returnType()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) method.returnType();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void dimension_after_method_arguments() {
      MethodTree method = (MethodTree) firstTypeMember("class T { int[] m()[]; }");
      assertThat(method.returnType()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) method.returnType();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void dimension_after_method_arguments_with_annotations() {
      MethodTree method = (MethodTree) firstTypeMember("class T { int[] m() @Foo []; }");
      assertThat(method.returnType()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) method.returnType();
      assertThat(arrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }
  }

  @Nested
  class ArrayAsFormalParameters {

    private VariableTree firstMethodParameter(String expression) {
      MethodTree method = (MethodTree) firstTypeMember(expression);
      return method.parameters().get(0);
    }

    @Test
    void array_formal_parameter() {
      VariableTree variable = firstMethodParameter("interface T { void m(int[] a); }");
      assertThat(variable).hasChildrenSize(3); // 1+2, as empty modifiers are always included
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
    }

    @Test
    void vararg_parameter() {
      VariableTree variable = firstMethodParameter("interface T { void m(int... a); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .isVarArg()
        .hasChildrenSize(2);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void vararg_annnotated() {
      VariableTree variable = firstMethodParameter("interface T { void m(int @Foo ... a); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isVarArg()
        .hasChildrenSize(3);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void combined_vararg_annotated() {
      VariableTree variable = firstMethodParameter("interface T { void m(int[] @Foo ... a); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree varArgType = (ArrayTypeTree) variable.type();

      assertThat(varArgType)
        .is(Tree.Kind.ARRAY_TYPE)
        .hasAnnotations(1)
        .isVarArg()
        .hasChildrenSize(3);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) varArgType.type();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(varArgType.ellipsisToken().range().start())
        .isGreaterThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void anntoated_array() {
      VariableTree variable = firstMethodParameter("interface T { void m(int @Foo [] a); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void double_annotated_array() {
      VariableTree variable = firstMethodParameter("interface T { void m(int @Foo @bar [] a); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(2)
        .isDeclaredArrayDimension()
        .hasChildrenSize(5);
      assertThat(arrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
    }

    @Test
    void two_dimention_with_one_annotated() {
      VariableTree variable = firstMethodParameter("interface T { void m(int[] @Foo [] a); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void split_dimension_style() {
      VariableTree variable = firstMethodParameter("interface T { void m(int[] a[]); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }

    @Test
    void split_dimension_style_with_annotation() {
      VariableTree variable = firstMethodParameter("interface T { void m(int[] a @Foo []); }");
      assertThat(variable).hasChildrenSize(3);
      assertThat(variable.type()).is(Tree.Kind.ARRAY_TYPE);

      ArrayTypeTree arrayTypeTree = (ArrayTypeTree) variable.type();
      assertThat(arrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .hasAnnotations(1)
        .isDeclaredArrayDimension()
        .hasChildrenSize(4);

      ArrayTypeTree childArrayTypeTree = (ArrayTypeTree) arrayTypeTree.type();
      assertThat(childArrayTypeTree)
        .is(Tree.Kind.ARRAY_TYPE)
        .isDeclaredArrayDimension()
        .hasChildrenSize(3);
      assertThat(childArrayTypeTree.type()).is(Tree.Kind.PRIMITIVE_TYPE);
      assertThat(childArrayTypeTree.openBracketToken().range().start())
        .isLessThan(arrayTypeTree.openBracketToken().range().start());
    }
  }

  /**
   * 15.13. Array Access Expressions
   */
  @Test
  void array_access_expression() {
    String code = "class T { T() { return a[42]; } }";
    ArrayAccessExpressionTree tree = (ArrayAccessExpressionTree) expressionOfReturnStatement(code);
    assertThat(tree)
      .is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)
      .hasChildrenSize(2);
    assertThat(tree.expression()).is(Tree.Kind.IDENTIFIER);

    ArrayDimensionTree dimension = tree.dimension();
    assertThat(dimension)
      .is(Tree.Kind.ARRAY_DIMENSION)
      .hasChildrenSize(3);
    assertThat(dimension.openBracketToken()).is("[");
    assertThat(dimension.expression()).is(Tree.Kind.INT_LITERAL);
    assertThat(dimension.closeBracketToken()).is("]");
  }

  /**
   * 15.14. Postfix Expressions
   */
  @Test
  void postfix_expression() {
    UnaryExpressionTree postfixIncrement = (UnaryExpressionTree) ((ExpressionStatementTree) firstMethodFirstStatement(("class T { void m() { i++; } }"))).expression();
    assertThat(postfixIncrement)
      .is(Tree.Kind.POSTFIX_INCREMENT)
      .hasChildrenSize(2);
    assertThat(postfixIncrement.expression()).is(Tree.Kind.IDENTIFIER);
    assertThat(postfixIncrement.operatorToken()).is("++");

    UnaryExpressionTree postfixDecrement = (UnaryExpressionTree) ((ExpressionStatementTree) firstMethodFirstStatement(("class T { void m() { i--; } }"))).expression();
    assertThat(postfixDecrement)
      .is(Tree.Kind.POSTFIX_DECREMENT)
      .hasChildrenSize(2);
    assertThat(postfixDecrement.expression()).is(Tree.Kind.IDENTIFIER);
    assertThat(postfixDecrement.operatorToken()).is("--");
  }

  /**
   * 15.15. Unary Operators
   */
  @Test
  void unary_operators() {
    UnaryExpressionTree prefixIncrement = (UnaryExpressionTree) ((ExpressionStatementTree) firstMethodFirstStatement(("class T { void m() { ++i; } }"))).expression();
    assertThat(prefixIncrement)
      .is(Tree.Kind.PREFIX_INCREMENT)
      .hasChildrenSize(2);
    assertThat(prefixIncrement.operatorToken()).is("++");
    assertThat(prefixIncrement.expression()).is(Tree.Kind.IDENTIFIER);

    UnaryExpressionTree prefixDecrement = (UnaryExpressionTree) ((ExpressionStatementTree) firstMethodFirstStatement(("class T { void m() { --i; } }"))).expression();
    assertThat(prefixDecrement)
      .is(Tree.Kind.PREFIX_DECREMENT)
      .hasChildrenSize(2);
    assertThat(prefixDecrement.operatorToken()).is("--");
    assertThat(prefixDecrement.expression()).is(Tree.Kind.IDENTIFIER);
  }

  /**
   * 15.16. Cast Expressions
   */
  @Nested
  class CastExpressions {
    @Test
    void simpe_type() {
      TypeCastTree tree = (TypeCastTree) expressionOfReturnStatement("class T { boolean m() { return (Boolean) true; } }");
      assertThat(tree)
        .is(Tree.Kind.TYPE_CAST)
        .hasChildrenSize(5);
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.type()).is(Tree.Kind.IDENTIFIER);
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.expression()).is(Tree.Kind.BOOLEAN_LITERAL);
    }

    @Test
    void intersection_type() {
      TypeCastTree tree = (TypeCastTree) expressionOfReturnStatement("class T { boolean m() { return (Foo<T> & Bar) true; } }");
      assertThat(tree)
        .is(Tree.Kind.TYPE_CAST)
        .hasChildrenSize(6);
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.type()).is(Tree.Kind.PARAMETERIZED_TYPE);
      assertThat(tree.andToken()).is("&");
      assertThat(tree.bounds()).hasSize(1);
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.expression()).is(Tree.Kind.BOOLEAN_LITERAL);
    }

    @Test
    void annotated_intersection_type() {
      TypeCastTree tree = (TypeCastTree) expressionOfReturnStatement("class T { boolean m() { return (Foo<T> & @Gul Bar) true; } }");
      assertThat(tree)
        .is(Tree.Kind.TYPE_CAST)
        .hasChildrenSize(6);
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.type()).is(Tree.Kind.PARAMETERIZED_TYPE);
      assertThat(tree.andToken()).is("&");
      assertThat(tree.bounds())
        .hasSize(1)
        .hasEmptySeparators();
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.expression()).is(Tree.Kind.BOOLEAN_LITERAL);
    }

    @Test
    void annotated_intersection_type_with_multiple_bounds() {
      TypeCastTree tree = (TypeCastTree) expressionOfReturnStatement("class T { boolean m() { return (Foo<T> & @Gul Bar & Qix & Plop) true; } }");
      assertThat(tree)
        .is(Tree.Kind.TYPE_CAST)
        .hasChildrenSize(6);
      assertThat(tree.openParenToken()).is("(");
      assertThat(tree.type()).is(Tree.Kind.PARAMETERIZED_TYPE);
      assertThat(tree.andToken()).is("&");
      assertThat(tree.bounds())
        .hasSize(3)
        .hasSeparatorsSize(2);
      assertThat(tree.closeParenToken()).is(")");
      assertThat(tree.expression()).is(Tree.Kind.BOOLEAN_LITERAL);
    }
  }

  /**
   * 15.17. Multiplicative Operators
   */
  @Test
  void multiplicative_expression() {
    String code = "class T { int m() { return 1 * 2 / 3 % 4; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.REMAINDER)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.DIVIDE);
    assertThat(parent.operatorToken()).is("%");
    assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.DIVIDE)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.MULTIPLY);
    assertThat(leftChild.operatorToken()).is("/");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);

    BinaryExpressionTree secondLeftChild = (BinaryExpressionTree) leftChild.leftOperand();
    assertThat(secondLeftChild)
      .is(Tree.Kind.MULTIPLY)
      .hasChildrenSize(3);
    assertThat(secondLeftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
    assertThat(secondLeftChild.operatorToken()).is("*");
    assertThat(secondLeftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
  }

  /**
   * 15.18. Additive Operators
   */
  @Test
  void additive_expression() {
    String code = "class T { int m() { return 1 + 2 - 3; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.MINUS)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.PLUS);
    assertThat(parent.operatorToken()).is("-");
    assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.PLUS)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
    assertThat(leftChild.operatorToken()).is("+");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
  }

  /**
   * 15.19. Shift Operators
   */
  @Test
  void shift_expression() {
    String code = "class T { int m() { return 1 >> 2 << 3 >>> 4; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.UNSIGNED_RIGHT_SHIFT)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.LEFT_SHIFT);
    assertThat(parent.operatorToken()).is(">>>");
    assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.LEFT_SHIFT)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.RIGHT_SHIFT);
    assertThat(leftChild.operatorToken()).is("<<");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);

    BinaryExpressionTree secondLeftChild = (BinaryExpressionTree) leftChild.leftOperand();
    assertThat(secondLeftChild)
      .is(Tree.Kind.RIGHT_SHIFT)
      .hasChildrenSize(3);
    assertThat(secondLeftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
    assertThat(secondLeftChild.operatorToken()).is(">>");
    assertThat(secondLeftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
  }

  /**
   * 15.20. Relational Operators
   */
  @Test
  void relational_expression() {
    String code = "class T { boolean m() { return 1 < 2 > 3; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.GREATER_THAN)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.LESS_THAN);
    assertThat(parent.operatorToken()).is(">");
    assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.LESS_THAN)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
    assertThat(leftChild.operatorToken()).is("<");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
  }

  /**
   * 15.20.2. Type Comparison Operator instanceof
   */
  @Test
  void instanceof_expression() {
    InstanceOfTree tree = (InstanceOfTree) expressionOfReturnStatement("class T { boolean m() { return null instanceof Object; } }");
    assertThat(tree)
      .is(Tree.Kind.INSTANCE_OF)
      .hasChildrenSize(3);
    assertThat(tree.expression()).is(Tree.Kind.NULL_LITERAL);
    assertThat(tree.instanceofKeyword()).is("instanceof");
    assertThat(tree.type()).is(Tree.Kind.IDENTIFIER);
  }

  /**
   * 15.21. Equality Operators
   */
  @Test
  void equality_expression() {
    String code = "class T { boolean m() { return false == false != true; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.NOT_EQUAL_TO)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.EQUAL_TO);
    assertThat(parent.operatorToken()).is("!=");
    assertThat(parent.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.EQUAL_TO)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(leftChild.operatorToken()).is("==");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
  }

  /**
   * 15.22. Bitwise and Logical Operators
   */
  @Nested
  class BitwiseAndLogicalOperators {
    @Test
    void and() {
      String code = "class T { int m() { return 1 & 2 & 3; } }";
      BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
      assertThat(parent)
        .is(Tree.Kind.AND)
        .hasChildrenSize(3);
      assertThat(parent.leftOperand()).is(Tree.Kind.AND);
      assertThat(parent.operatorToken()).is("&");
      assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

      BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
      assertThat(leftChild)
        .is(Tree.Kind.AND)
        .hasChildrenSize(3);
      assertThat(leftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
      assertThat(leftChild.operatorToken()).is("&");
      assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
    }

    @Test
    void xor() {
      String code = "class T { int m() { return 1 ^ 2 ^ 3; } }";
      BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
      assertThat(parent)
        .is(Tree.Kind.XOR)
        .hasChildrenSize(3);
      assertThat(parent.leftOperand()).is(Tree.Kind.XOR);
      assertThat(parent.operatorToken()).is("^");
      assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

      BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
      assertThat(leftChild)
        .is(Tree.Kind.XOR)
        .hasChildrenSize(3);
      assertThat(leftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
      assertThat(leftChild.operatorToken()).is("^");
      assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
    }

    @Test
    void or() {
      String code = "class T { int m() { return 1 | 2 | 3; } }";
      BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
      assertThat(parent)
        .is(Tree.Kind.OR)
        .hasChildrenSize(3);
      assertThat(parent.leftOperand()).is(Tree.Kind.OR);
      assertThat(parent.operatorToken()).is("|");
      assertThat(parent.rightOperand()).is(Tree.Kind.INT_LITERAL);

      BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
      assertThat(leftChild)
        .is(Tree.Kind.OR)
        .hasChildrenSize(3);
      assertThat(leftChild.leftOperand()).is(Tree.Kind.INT_LITERAL);
      assertThat(leftChild.operatorToken()).is("|");
      assertThat(leftChild.rightOperand()).is(Tree.Kind.INT_LITERAL);
    }
  }

  /**
   * 15.23. Conditional-And Operator &&
   */
  @Test
  void conditional_and_expression() {
    String code = "class T { boolean m() { return false && false && true; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.CONDITIONAL_AND)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.CONDITIONAL_AND);
    assertThat(parent.operatorToken()).is("&&");
    assertThat(parent.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.CONDITIONAL_AND)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(leftChild.operatorToken()).is("&&");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
  }

  /**
   * 15.24. Conditional-Or Operator ||
   */
  @Test
  void conditional_or_expression() {
    String code = "class T { boolean m() { return false || false || true; } }";
    BinaryExpressionTree parent = (BinaryExpressionTree) expressionOfReturnStatement(code);
    assertThat(parent)
      .is(Tree.Kind.CONDITIONAL_OR)
      .hasChildrenSize(3);
    assertThat(parent.leftOperand()).is(Tree.Kind.CONDITIONAL_OR);
    assertThat(parent.operatorToken()).is("||");
    assertThat(parent.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);

    BinaryExpressionTree leftChild = (BinaryExpressionTree) parent.leftOperand();
    assertThat(leftChild)
      .is(Tree.Kind.CONDITIONAL_OR)
      .hasChildrenSize(3);
    assertThat(leftChild.leftOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(leftChild.operatorToken()).is("||");
    assertThat(leftChild.rightOperand()).is(Tree.Kind.BOOLEAN_LITERAL);
  }

  /**
   * 15.25. Conditional Operator ? :
   */
  @Test
  void conditional_expression() {
    ConditionalExpressionTree tree = (ConditionalExpressionTree) expressionOfReturnStatement("class T { boolean m() { return true ? true : false; } }");
    assertThat(tree)
      .is(Tree.Kind.CONDITIONAL_EXPRESSION)
      .hasChildrenSize(5);
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.questionToken()).is("?");
    assertThat(tree.trueExpression()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.colonToken()).is(":");
    assertThat(tree.falseExpression()).is(Tree.Kind.BOOLEAN_LITERAL);
  }

  @Test
  void conditional_expression_nested() {
    ConditionalExpressionTree tree = (ConditionalExpressionTree) expressionOfReturnStatement("class T { boolean m() { return true ? true : false ? true : false; } }");
    assertThat(tree)
      .is(Tree.Kind.CONDITIONAL_EXPRESSION)
      .hasChildrenSize(5);
    assertThat(tree.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.trueExpression()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(tree.falseExpression()).is(Tree.Kind.CONDITIONAL_EXPRESSION);

    ConditionalExpressionTree nested = (ConditionalExpressionTree) tree.falseExpression();
    assertThat(nested)
      .is(Tree.Kind.CONDITIONAL_EXPRESSION)
      .hasChildrenSize(5);
    assertThat(nested.condition()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(nested.trueExpression()).is(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(nested.falseExpression()).is(Tree.Kind.BOOLEAN_LITERAL);
  }

  /**
   * 15.26. Assignment Operators
   */
  @Test
  void assignment_expression() {
    String code = "class T { void m() { a += 42; } }";
    AssignmentExpressionTree tree = (AssignmentExpressionTree) ((ExpressionStatementTree) firstMethodFirstStatement(code)).expression();
    assertThat(tree)
      .is(Tree.Kind.PLUS_ASSIGNMENT)
      .hasChildrenSize(3);
    assertThat(tree.variable()).is(Tree.Kind.IDENTIFIER);
    assertThat(tree.operatorToken()).is("+=");
    assertThat(tree.expression()).is(Tree.Kind.INT_LITERAL);
  }

  @Test
  void method_reference_expression_should_not_break_AST() throws Exception {
    String code = "class T { public void meth() {"
      + "  IntStream.range(1,12)"
      + "    .map(new MethodReferences()::<String>square)"
      + "    .map(super::myMethod)"
      + "    .map(int[]::new)"
      + "    .forEach(System.out::println);"
      + "  }"
      + "}";
    MethodInvocationTree forEach = (MethodInvocationTree) ((ExpressionStatementTree) firstMethodFirstStatement(code)).expression();

    MethodReferenceTree systemOutPrintln = (MethodReferenceTree) forEach.arguments().get(0);
    assertThat(systemOutPrintln)
      .is(Tree.Kind.METHOD_REFERENCE)
      .hasChildrenSize(3);
    assertThat(systemOutPrintln.expression()).is(Tree.Kind.MEMBER_SELECT);
    assertThat(systemOutPrintln.typeArguments());
    assertThat(systemOutPrintln.doubleColon()).is("::");
    assertThat(systemOutPrintln.method()).hasName("println");

    MethodInvocationTree map3 = (MethodInvocationTree) ((MemberSelectExpressionTree) forEach.methodSelect()).expression();
    MethodReferenceTree intNew = (MethodReferenceTree) map3.arguments().get(0);
    assertThat(intNew)
      .is(Tree.Kind.METHOD_REFERENCE)
      .hasChildrenSize(3);
    assertThat(intNew.expression()).is(Tree.Kind.ARRAY_TYPE);
    assertThat(intNew.typeArguments()).isNull();
    assertThat(intNew.doubleColon()).is("::");
    assertThat(intNew.method()).hasName("new");

    MethodInvocationTree map2 = (MethodInvocationTree) ((MemberSelectExpressionTree) map3.methodSelect()).expression();
    MethodReferenceTree superMyMethod = (MethodReferenceTree) map2.arguments().get(0);
    assertThat(superMyMethod)
      .is(Tree.Kind.METHOD_REFERENCE)
      .hasChildrenSize(3);
    assertThat(superMyMethod.expression()).is(Tree.Kind.IDENTIFIER);
    assertThat(((IdentifierTree) superMyMethod.expression())).hasName("super");
    assertThat(superMyMethod.typeArguments()).isNull();
    assertThat(superMyMethod.doubleColon()).is("::");
    assertThat(superMyMethod.method()).hasName("myMethod");

    MethodInvocationTree map1 = (MethodInvocationTree) ((MemberSelectExpressionTree) map2.methodSelect()).expression();
    MethodReferenceTree methodReferenceSquar = (MethodReferenceTree) map1.arguments().get(0);
    assertThat(methodReferenceSquar)
      .is(Tree.Kind.METHOD_REFERENCE)
      .hasChildrenSize(4);
    assertThat(methodReferenceSquar.expression()).is(Tree.Kind.NEW_CLASS);
    assertThat(methodReferenceSquar.typeArguments()).is(Tree.Kind.TYPE_ARGUMENTS);
    assertThat(methodReferenceSquar.doubleColon()).is("::");
    assertThat(methodReferenceSquar.method()).hasName("square");
  }

  @Test
  void lambda_expressions() {
    String code = "class T { public void meth() {"
      + "  IntStream.range(1,12)"
      + "    .map(x -> x*x)"
      + "    .map((int a) -> { return a*a; });"
      + "  }"
      + "}";
    ExpressionTree expressionTree = ((ExpressionStatementTree) firstMethodFirstStatement(code)).expression();

    // parsing not broken by lambda
    assertThat(expressionTree).is(Tree.Kind.METHOD_INVOCATION);

    MethodInvocationTree mit = (MethodInvocationTree) expressionTree;
    LambdaExpressionTree aa = (LambdaExpressionTree) mit.arguments().get(0);
    assertThat(aa.openParenToken()).is("(");
    assertThat(aa.parameters()).hasSize(1);
    assertThat(aa.parameters().get(0)).is(Tree.Kind.VARIABLE);
    assertThat(aa.closeParenToken()).is(")");
    assertThat(aa.arrowToken()).is("->");
    assertThat(aa.body()).is(Tree.Kind.BLOCK);

    LambdaExpressionTree xx = (LambdaExpressionTree) ((MethodInvocationTree) ((MemberSelectExpressionTree) mit.methodSelect()).expression()).arguments().get(0);
    assertThat(xx.openParenToken()).isNull();
    assertThat(xx.parameters()).hasSize(1);
    assertThat(xx.parameters().get(0)).is(Tree.Kind.VARIABLE);
    assertThat(xx.closeParenToken()).isNull();
    assertThat(xx.arrowToken()).is("->");
    assertThat(xx.body()).is(Tree.Kind.MULTIPLY);
  }

  @Test
  void type_parameters() {
    ParameterizedTypeTree tree = (ParameterizedTypeTree) firstType("class Foo<E> extends List<E> {}").superClass();
    assertThat(tree)
      .is(Tree.Kind.PARAMETERIZED_TYPE)
      .hasChildrenSize(2);
    TypeArguments typeArguments = tree.typeArguments();
    assertThat(typeArguments)
      .is(Tree.Kind.TYPE_ARGUMENTS)
      .hasSize(1)
      .hasChildrenSize(3)
      .hasEmptySeparators();
    assertThat(typeArguments.openBracketToken()).is("<");
    assertThat(typeArguments.closeBracketToken()).is(">");
  }

  @Test
  void type_parameters_multiple() {
    ParameterizedTypeTree tree = (ParameterizedTypeTree) firstType("class Mop<K,V> implements Map<K,V> {}").superInterfaces().get(0);
    assertThat(tree)
      .is(Tree.Kind.PARAMETERIZED_TYPE)
      .hasChildrenSize(2);
    TypeArguments typeArguments = tree.typeArguments();
    assertThat(typeArguments)
      .is(Tree.Kind.TYPE_ARGUMENTS)
      .hasSize(2)
      .hasSeparatorsSize(1)
      .hasChildrenSize(5);
    assertThat(typeArguments.openBracketToken()).is("<");
    assertThat(typeArguments.closeBracketToken()).is(">");
  }

  @Test
  void type_parameters_and_bounds() {
    TypeParameterListTreeImpl tree = (TypeParameterListTreeImpl) firstType("class Foo<T, U extends Object & Number> {}").typeParameters();
    assertThat(tree.openBracketToken()).is("<");
    assertThat(tree.closeBracketToken()).is(">");
    assertThat(tree)
      .hasSize(2)
      .hasSeparatorsSize(1)
      .hasChildrenSize(5);

    TypeParameterTree paramT = tree.get(0);
    assertThat(paramT.identifier()).hasName("T");
    assertThat(paramT.bounds()).isEmpty();
    assertThat(paramT.bounds().separators()).isEmpty();
    assertThat(paramT).hasChildrenSize(1);

    TypeParameterTree paramU = tree.get(1);
    assertThat(paramU.identifier()).hasName("U");
    assertThat(paramU.bounds())
      .hasSize(2)
      .hasSeparatorsSize(1);
    assertThat(((IdentifierTree) paramU.bounds().get(0))).hasName("Object");
    assertThat(((IdentifierTree) paramU.bounds().get(1))).hasName("Number");
    assertThat(paramU).hasChildrenSize(3);
  }

  private static ExpressionTree expressionOfReturnStatement(String code) {
    return ((ReturnStatementTree) firstMethodFirstStatement(code)).expression();
  }

  private static StatementTree firstMethodFirstStatement(String code) {
    return ((MethodTree) firstTypeMember(code)).block().body().get(0);
  }

  private static Tree firstTypeMember(String code) {
    return firstType(code).members().get(0);
  }

  private static ClassTree firstType(String code) {
    return (ClassTree) compilationUnit(code).types().get(0);
  }

  private static CompilationUnitTree compilationUnit(String code) {
    return JParserTestUtils.parse(code);
  }
}
