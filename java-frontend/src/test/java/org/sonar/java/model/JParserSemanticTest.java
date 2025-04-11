/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.model.assertions.TreeAssert.assertThat;
import static org.sonar.java.model.assertions.TypeAssert.assertThat;

class JParserSemanticTest {

  @Test
  void should_not_throw_RecognitionException_in_case_of_non_syntax_errors() {
    test("import nonexisting;");
  }

  @Test
  void expression_null_literal() {
    AbstractTypedTree e = (AbstractTypedTree) expression("null");
    assertThat(e.typeBinding).isNotNull();
  }

  @Test
  void expression_literal() {
    InternalPrefixUnaryExpression e = (InternalPrefixUnaryExpression) expression("-2147483648"); // Integer.MIN_VALUE
    assertThat(e.typeBinding).isNotNull();
    AbstractTypedTree t = (AbstractTypedTree) e.expression();
    assertThat(t.typeBinding)
      .isNotNull()
      .isSameAs(e.typeBinding);
  }

  /**
   * @see org.eclipse.jdt.core.dom.InfixExpression#extendedOperands()
   */
  @Test
  void extended_operands() {
    BinaryExpressionTreeImpl e = (BinaryExpressionTreeImpl) expression("1 - 2 - 3");
    BinaryExpressionTreeImpl leftOperand = (BinaryExpressionTreeImpl) e.leftOperand();
    assertThat(e.typeBinding).isNotNull();
    assertThat(leftOperand.typeBinding).isSameAs(e.typeBinding);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SimpleName
   */
  @Test
  void expression_simple_name() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; Object m() { return f; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTree s = (ReturnStatementTree) m.block().body().get(0);
    IdentifierTreeImpl i = (IdentifierTreeImpl) s.expression();
    assertThat(i.binding).isNotNull();
    assertThat(cu.sema.usages.get(i.binding)).containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.QualifiedName
   */
  @Test
  void expression_qualified_name() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return java.lang.System.out; } }");

    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) m.block().body().get(0);
    MemberSelectExpressionTreeImpl javaLangSystemOut = (MemberSelectExpressionTreeImpl) s.expression();

    MemberSelectExpressionTreeImpl javaLangSystem = (MemberSelectExpressionTreeImpl) javaLangSystemOut.expression();
    assertThat(javaLangSystem.typeBinding).isNotNull();
    assertThat(cu.sema.type(javaLangSystem.typeBinding).is("java.lang.System"))
      .isEqualTo(javaLangSystem.symbolType().is("java.lang.System"))
      .isTrue();

    MemberSelectExpressionTreeImpl javaLang = (MemberSelectExpressionTreeImpl) javaLangSystem.expression();
    assertThat(javaLang.typeBinding).isNull();

    IdentifierTreeImpl java = (IdentifierTreeImpl) javaLang.expression();
    assertThat(java.typeBinding).isNull();
    assertThat(java.binding).isNotNull();
    assertThat(java.binding.getKind()).isSameAs(IBinding.PACKAGE);
    assertThat(java.symbol().isPackageSymbol()).isTrue();

    IdentifierTreeImpl lang = (IdentifierTreeImpl) javaLang.identifier();
    assertThat(lang.typeBinding).isNull();
    assertThat(lang.binding).isNotNull();
    assertThat(lang.binding.getKind()).isSameAs(IBinding.PACKAGE);
    assertThat(lang.symbol().isPackageSymbol()).isTrue();

    IdentifierTreeImpl system = (IdentifierTreeImpl) javaLangSystem.identifier();
    assertThat(system.binding)
      .isNotNull()
      .isEqualTo(system.typeBinding);
    assertThat(system.binding.getKind()).isSameAs(IBinding.TYPE);
    assertThat(cu.sema.type(system.typeBinding).is("java.lang.System"))
      .isEqualTo(system.symbolType().is("java.lang.System"))
      .isTrue();
  }

  /**
   * @see org.eclipse.jdt.core.dom.ThisExpression
   */
  @Test
  void expression_this() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return this; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    KeywordThis keywordThis = Objects.requireNonNull((KeywordThis) s.expression());
    assertThat(keywordThis.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) keywordThis.symbolType().symbol().declaration()).typeBinding)
      .isSameAs(c.typeBinding);
    assertThat(keywordThis.symbol())
      .isSameAs(cu.sema.typeSymbol(c.typeBinding).thisSymbol);
    assertThat(keywordThis.symbol().isVariableSymbol())
      .isTrue();
  }

  /**
   * @see org.eclipse.jdt.core.dom.ThisExpression
   */
  @Test
  void expression_this_qualified() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { class C2 { Object m() { return C1.this; } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c2.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    MemberSelectExpressionTreeImpl e = Objects.requireNonNull((MemberSelectExpressionTreeImpl) s.expression());
    assertThat(e.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) e.symbolType().symbol().declaration()).typeBinding)
      .isSameAs(c1.typeBinding);
    KeywordThis keywordThis = (KeywordThis) e.identifier();
    assertThat(keywordThis.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) keywordThis.symbolType().symbol().declaration()).typeBinding)
      .isSameAs(c1.typeBinding);
    assertThat(keywordThis.symbol())
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding).thisSymbol);
    assertThat(keywordThis.symbol().isVariableSymbol())
      .isTrue();
  }

  /**
   * @see org.eclipse.jdt.core.dom.FieldAccess
   */
  @Test
  void expression_field_access() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; Object m() { return this.f; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl field = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    MemberSelectExpressionTreeImpl expression = Objects.requireNonNull((MemberSelectExpressionTreeImpl) s.expression());
    assertThat(expression.typeBinding)
      .isNotNull();
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) expression.identifier();
    assertThat(identifier.typeBinding)
      .isSameAs(expression.typeBinding);
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((VariableTreeImpl) identifier.symbol().declaration()).variableBinding)
      .isSameAs(field.variableBinding);
    assertThat(cu.sema.usages.get(identifier.binding))
      .containsExactlyElementsOf(identifier.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperFieldAccess
   */
  @Test
  void expression_super_field_access() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C extends S { Object m() { return super.f; } } class S { int f; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    VariableTreeImpl superField = (VariableTreeImpl) superClass.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    MemberSelectExpressionTreeImpl expression = Objects.requireNonNull((MemberSelectExpressionTreeImpl) s.expression());

    KeywordSuper keywordSuper = (KeywordSuper) expression.expression();
    assertThat(keywordSuper.symbolType().symbol().declaration())
      .isSameAs(superClass.symbol().declaration());
    assertThat(keywordSuper.symbol())
      .isSameAs(cu.sema.typeSymbol(c.typeBinding).superSymbol);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) expression.identifier();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((VariableTreeImpl) identifier.symbol().declaration()).variableBinding)
      .isSameAs(superField.variableBinding);
    assertThat(cu.sema.usages.get(identifier.binding))
      .containsExactlyElementsOf(identifier.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperFieldAccess
   */
  @Test
  void expression_super_field_access_qualified() {
    JavaTree.CompilationUnitTreeImpl cu = test("class T extends S { class C { Object m() { return T.super.f; } } } class S { int f; }");
    ClassTreeImpl t = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    VariableTreeImpl superField = (VariableTreeImpl) superClass.members().get(0);
    ClassTreeImpl c = (ClassTreeImpl) t.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    MemberSelectExpressionTreeImpl expression = Objects.requireNonNull((MemberSelectExpressionTreeImpl) s.expression());

    MemberSelectExpressionTreeImpl qualifiedSuper = (MemberSelectExpressionTreeImpl) expression.expression();
    assertThat(qualifiedSuper.typeBinding)
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) qualifiedSuper.symbolType().symbol().declaration()).typeBinding)
      .isSameAs(superClass.typeBinding);

    KeywordSuper keywordSuper = (KeywordSuper) qualifiedSuper.identifier();
    assertThat(keywordSuper.symbolType().symbol().declaration())
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) keywordSuper.symbolType().symbol().declaration()))
      .isSameAs(superClass);
    assertThat(keywordSuper.symbol())
      .isSameAs(cu.sema.typeSymbol(t.typeBinding).superSymbol);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) expression.identifier();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((VariableTreeImpl) identifier.symbol().declaration()).variableBinding)
      .isSameAs(superField.variableBinding);
    assertThat(cu.sema.usages.get(identifier.binding))
      .containsExactlyElementsOf(identifier.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.TypeLiteral
   */
  @Test
  void expression_type_literal() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return C.class; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    MemberSelectExpressionTreeImpl e = Objects.requireNonNull((MemberSelectExpressionTreeImpl) s.expression());
    assertThat(e.typeBinding)
      .isNotNull();
    IdentifierTreeImpl i = (IdentifierTreeImpl) e.identifier();
    assertThat(i.typeBinding)
      .isNull();
    assertThat(i.symbolType().isUnknown())
      .isTrue();
    assertThat(i.binding)
      .isNull();
    assertThat(i.symbol().isUnknown())
      .isTrue();
  }

  /**
   * @see org.eclipse.jdt.core.dom.ClassInstanceCreation
   */
  @Test
  void expression_class_instance_creation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return new C(); } C(){} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    NewClassTreeImpl e = Objects.requireNonNull((NewClassTreeImpl) s.expression());
    assertThat(e.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) e.symbolType().symbol().declaration()).typeBinding)
      .isSameAs(c.typeBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) e.getConstructorIdentifier();
    assertThat(i.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(constructor.methodBinding);
    assertThat(cu.sema.usages.get(constructor.methodBinding))
      .containsExactlyElementsOf(constructor.symbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ClassInstanceCreation
   */
  @Test
  void expression_anonymous_class_instance_creation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return new C() { }; } protected C(){} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    NewClassTreeImpl e = (NewClassTreeImpl) s.expression();
    ClassTreeImpl b = (ClassTreeImpl) e.classBody();
    assertThat(b.typeBinding).isNotNull();
    assertThat(cu.sema.declarations.get(b.typeBinding))
      .isSameAs(b.symbol().declaration())
      .isSameAs(b);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) e.getConstructorIdentifier();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) identifier.symbol().declaration()).methodBinding)
      .isSameAs(constructor.methodBinding);
    assertThat(cu.sema.usages.get(constructor.methodBinding))
      .containsExactlyElementsOf(constructor.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.CreationReference
   */
  @Test
  void expression_creation_reference() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C() { } java.util.function.Supplier m() { return C::new; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) m.block().body().get(0);
    MethodReferenceTreeImpl creationReference = (MethodReferenceTreeImpl) s.expression();
    IdentifierTreeImpl keywordNew = (IdentifierTreeImpl) creationReference.method();
    assertThat(keywordNew.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) creationReference.method().symbol().declaration()).methodBinding)
      .isSameAs(constructor.methodBinding);
    assertThat(cu.sema.usages.get(constructor.methodBinding))
      .containsExactlyElementsOf(constructor.symbol().usages())
      .containsOnly(keywordNew);
  }

  /**
   * @see org.eclipse.jdt.core.dom.MethodReference
   */
  @Test
  void expression_method_reference() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { java.util.function.Supplier m() { return this::m; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) method.block().body().get(0);
    MethodReferenceTreeImpl creationReference = (MethodReferenceTreeImpl) s.expression();
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) creationReference.method();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) creationReference.method().symbol().declaration()).methodBinding)
      .isSameAs(method.methodBinding);
    assertThat(cu.sema.usages.get(method.methodBinding))
      .containsExactlyElementsOf(method.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.TypeMethodReference
   */
  @Test
  void expression_type_method_reference() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { static java.util.function.Supplier m() { return C::m; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) method.block().body().get(0);
    MethodReferenceTreeImpl creationReference = (MethodReferenceTreeImpl) s.expression();
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) creationReference.method();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) creationReference.method().symbol().declaration()).methodBinding)
      .isSameAs(method.methodBinding);
    assertThat(cu.sema.usages.get(method.methodBinding))
      .containsExactlyElementsOf(method.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperMethodReference
   */
  @Test
  void expression_super_method_reference() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      class C extends S {
        java.util.function.Supplier m() { return super::m; }
      }
      class S {
        Object m() { return null; }
      }
      """);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassMethod = (MethodTreeImpl) superClass.members().get(0);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) method.block().body().get(0);
    MethodReferenceTreeImpl creationReference = Objects.requireNonNull((MethodReferenceTreeImpl) s.expression());

    KeywordSuper keywordSuper = (KeywordSuper) creationReference.expression();
    assertThat(keywordSuper.symbolType().symbol().declaration())
      .isSameAs(superClass.symbol().declaration());
    assertThat(keywordSuper.symbol())
      .isSameAs(cu.sema.typeSymbol(c.typeBinding).superSymbol);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) creationReference.method();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) creationReference.method().symbol().declaration()).methodBinding)
      .isSameAs(superClassMethod.methodBinding);
    assertThat(cu.sema.usages.get(superClassMethod.methodBinding))
      .containsExactlyElementsOf(superClassMethod.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperMethodReference
   */
  @Test
  void expression_super_method_reference_qualified() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      class C extends S {
        java.util.function.Supplier m() { return C.super::m; }
      }
      class S {
        Object m() { return null; }
      }
      """);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassMethod = (MethodTreeImpl) superClass.members().get(0);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) method.block().body().get(0);
    MethodReferenceTreeImpl creationReference = Objects.requireNonNull((MethodReferenceTreeImpl) s.expression());

    MemberSelectExpressionTreeImpl mseti = (MemberSelectExpressionTreeImpl) creationReference.expression();

    KeywordSuper keywordSuper = (KeywordSuper) mseti.identifier();
    assertThat(keywordSuper.symbolType().symbol().declaration())
      .isSameAs(superClass.symbol().declaration());
    assertThat(keywordSuper.symbol())
      .isSameAs(cu.sema.typeSymbol(c.typeBinding).superSymbol);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) creationReference.method();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) creationReference.method().symbol().declaration()).methodBinding)
      .isSameAs(superClassMethod.methodBinding);
    assertThat(cu.sema.usages.get(superClassMethod.methodBinding))
      .containsExactlyElementsOf(superClassMethod.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SwitchExpression
   * @see org.eclipse.jdt.core.dom.YieldStatement
   */
  @Test
  void expression_switch() {
    {
      SwitchExpressionTreeImpl switchExpression = (SwitchExpressionTreeImpl) expression("switch (0) { default: yield 0; }");
      assertThat(switchExpression.symbolType().isUnknown()).isFalse();

      YieldStatementTreeImpl statement = (YieldStatementTreeImpl) switchExpression.cases().get(0).body().get(0);
      assertThat(statement.yieldKeyword().text()).isEqualTo("yield");
      assertThat(statement.expression()).isNotNull();
    }
    {
      SwitchExpressionTreeImpl switchExpression = (SwitchExpressionTreeImpl) expression("switch (0) { default -> 0; case 0, 1 -> 0; }");
      assertThat(switchExpression).isInstanceOf(AbstractTypedTree.class);
      assertThat(switchExpression.symbolType().isUnknown()).isFalse();

      YieldStatementTreeImpl statement = (YieldStatementTreeImpl) switchExpression.cases().get(0).body().get(0);
      assertThat(statement.yieldKeyword()).as("implicit yield-statement").isNull();

      CaseLabelTree caseLabel = switchExpression.cases().get(1).labels().get(0);
      assertThat(caseLabel.colonOrArrowToken().text()).isEqualTo("->");
      assertThat(caseLabel.expressions()).hasSize(2);
    }
  }

  @Test
  void switch_expression_with_yield_of_unknown_identifier_without_default_clause() {
    SwitchExpressionTreeImpl switchExpression = (SwitchExpressionTreeImpl) expression("switch (unknownIdentifier) { case A -> 0; case B -> 1; }");
    assertThat(switchExpression.expression().symbolType().isUnknown()).isTrue();
    // the expression type of the full switch expression the should have been int or unknown
    // instead of java.lang.Object, it is probably a limitation in the JDT parser when it face a missing default clause error
    assertThat(switchExpression.symbolType().isUnknown()).isFalse();
    assertThat(switchExpression.symbolType().fullyQualifiedName()).isEqualTo("java.lang.Object");
    assertThat(switchExpression.cases().get(0).body().get(0)).isInstanceOf(YieldStatementTree.class);
  }

  @Test
  void switch_expression_of_unknown_identifier_without_default_clause() {
    SwitchExpressionTreeImpl switchExpression = (SwitchExpressionTreeImpl) expression("switch (unknownIdentifier) { case A: return 0; case B: return 1; }");
    assertThat(switchExpression.expression().symbolType().isUnknown()).isTrue();
    assertThat(switchExpression.symbolType().isUnknown()).isTrue();
    assertThat(switchExpression.cases().get(0).body().get(0)).isInstanceOf(ReturnStatementTree.class);
  }

  @Test
  void switch_expression_of_enum_without_default_clause() {
    CompilationUnitTree cu = test("class C { Object m(java.time.DayOfWeek x) { return switch (x) { case MONDAY: return 0; case TUESDAY: return 1; } ; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    SwitchExpressionTreeImpl switchExpression = (SwitchExpressionTreeImpl) s.expression();
    assertThat(switchExpression.expression().symbolType().isUnknown()).isFalse();
    assertThat(switchExpression.symbolType().isUnknown()).isTrue();
  }

  /**
   * Pattern Matching for instanceof
   * (Preview in Java 14) https://openjdk.java.net/jeps/305
   * (Second Preview in Java 15) https://openjdk.java.net/jeps/375
   * (Final in Java 16) https://openjdk.java.net/jeps/394
   *
   * @see org.eclipse.jdt.core.dom.InstanceofExpression
   */
  @Test
  void expression_pattern_instanceof() {
    PatternInstanceOfTree e = (PatternInstanceOfTree) expression("o instanceof String s");
    VariableTreeImpl v = (VariableTreeImpl) e.variable();
    IdentifierTreeImpl i = (IdentifierTreeImpl) v.simpleName();

    assertThat(v).isNotNull();
    assertThat(v.symbol().type())
      .is("java.lang.String")
      .is(i.symbolType());

    assertThat(i.binding).isNotNull();
    assertThat(i.symbol().declaration()).isEqualTo(v);
  }

  /**
   * @see org.eclipse.jdt.core.dom.MethodInvocation
   */
  @Test
  void expression_method_invocation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return m(); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(method.block()).body().get(0);

    MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) s.expression();
    assertThat(methodInvocation.methodBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) methodInvocation.methodSymbol().declaration()).methodBinding)
      .isSameAs(method.methodBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) methodInvocation.methodSelect();
    assertThat(i.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(methodInvocation.methodBinding);
    assertThat(cu.sema.usages.get(i.binding))
      .containsExactlyElementsOf(methodInvocation.methodSymbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.MethodInvocation
   */
  @Test
  void expression_method_invocation_recovery() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return m(42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(method.block()).body().get(0);
    MethodInvocationTreeImpl methodInvocation = Objects.requireNonNull((MethodInvocationTreeImpl) s.expression());
    assertThat(methodInvocation.methodBinding)
      .isNull();
    IdentifierTreeImpl i = (IdentifierTreeImpl) methodInvocation.methodSelect();
    assertThat(i.binding)
      .isNull();
    assertThat(cu.sema.usages.get(Objects.requireNonNull(method.methodBinding)))
      .isNull();
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperMethodInvocation
   */
  @Test
  void expression_super_method_invocation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C extends S { Object m() { return super.m(); } } class S { Object m() { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassMethod = (MethodTreeImpl) superClass.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);

    MethodInvocationTreeImpl superMethodInvocation = (MethodInvocationTreeImpl) Objects.requireNonNull(s.expression());
    assertThat(superMethodInvocation.methodBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) superMethodInvocation.methodSymbol().declaration()).methodBinding)
      .isSameAs(superClassMethod.methodBinding);
    MemberSelectExpressionTreeImpl e2 = (MemberSelectExpressionTreeImpl) superMethodInvocation.methodSelect();

    KeywordSuper keywordSuper = (KeywordSuper) e2.expression();
    assertThat(keywordSuper.symbolType().symbol().declaration())
      .isSameAs(superClass.symbol().declaration());
    assertThat(keywordSuper.symbol())
      .isSameAs(cu.sema.typeSymbol(c.typeBinding).superSymbol);

    IdentifierTreeImpl i = (IdentifierTreeImpl) e2.identifier();
    assertThat(i.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(superMethodInvocation.methodBinding);
    assertThat(cu.sema.usages.get(i.binding))
      .containsExactlyElementsOf(superMethodInvocation.methodSymbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperMethodInvocation
   */
  @Test
  void expression_super_method_invocation_recovery() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C extends S { Object m() { return super.m(42); } } class S { Object m() { return null; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassMethod = (MethodTreeImpl) superClass.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);

    MethodInvocationTreeImpl superMethodInvocation = (MethodInvocationTreeImpl) Objects.requireNonNull(s.expression());
    assertThat(superMethodInvocation.methodBinding)
      .isNull();
    MemberSelectExpressionTreeImpl e = (MemberSelectExpressionTreeImpl) superMethodInvocation.methodSelect();
    IdentifierTreeImpl i = (IdentifierTreeImpl) e.identifier();
    assertThat(i.binding)
      .isNull();
    assertThat(cu.sema.usages.get(Objects.requireNonNull(superClassMethod.methodBinding)))
      .isNull();
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperMethodInvocation
   */
  @Test
  void expression_super_method_invocation_qualified() {
    JavaTree.CompilationUnitTreeImpl cu = test("class T extends S { class C { Object m() { return T.super.m(); } } } class S { Object m() { return null; } }");
    ClassTreeImpl t = (ClassTreeImpl) cu.types().get(0);
    ClassTreeImpl c = (ClassTreeImpl) t.members().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassMethod = (MethodTreeImpl) superClass.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);

    MethodInvocationTreeImpl superMethodInvocation = (MethodInvocationTreeImpl) Objects.requireNonNull(s.expression());
    assertThat(superMethodInvocation.methodBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) superMethodInvocation.methodSymbol().declaration()).methodBinding)
      .isSameAs(superClassMethod.methodBinding);

    MemberSelectExpressionTreeImpl qualifiedMethodName = (MemberSelectExpressionTreeImpl) superMethodInvocation.methodSelect();
    assertThat(qualifiedMethodName.symbolType().isUnknown())
      .isTrue();
    assertThat(qualifiedMethodName.typeBinding)
      .isNull();

    MemberSelectExpressionTreeImpl qualifiedSuper = (MemberSelectExpressionTreeImpl) qualifiedMethodName.expression();
    assertThat(qualifiedSuper.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((ClassTreeImpl) qualifiedSuper.symbolType().symbol().declaration()).typeBinding)
      .isSameAs(superClass.typeBinding);

    KeywordSuper keywordSuper = (KeywordSuper) qualifiedSuper.identifier();
    assertThat(keywordSuper.symbolType().symbol().declaration())
      .isSameAs(superClass);
    assertThat(keywordSuper.symbol())
      .isSameAs(cu.sema.typeSymbol(t.typeBinding).superSymbol);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) qualifiedMethodName.identifier();
    assertThat(identifier.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) identifier.symbol().declaration()).methodBinding)
      .isSameAs(superMethodInvocation.methodBinding);
    assertThat(cu.sema.usages.get(identifier.binding))
      .containsExactlyElementsOf(superMethodInvocation.methodSymbol().usages())
      .containsOnly(identifier);
  }

  @Test
  void expression_super_method_invocation_interface() {
    String source = """
      interface MyIterable<T> extends Iterable<T> {
        @Override
        default void forEach(java.util.function.Consumer<? super T> action) {
          Iterable.super.forEach(action);
        }
      }
      """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl forEach = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl e = (ExpressionStatementTreeImpl) forEach.block().body().get(0);
    MethodInvocationTreeImpl m = (MethodInvocationTreeImpl) e.expression();
    MemberSelectExpressionTreeImpl iterableSuperForeach = (MemberSelectExpressionTreeImpl) m.methodSelect();
    MemberSelectExpressionTreeImpl iterableSuper = (MemberSelectExpressionTreeImpl) iterableSuperForeach.expression();
    IdentifierTreeImpl superKeyword = (IdentifierTreeImpl) iterableSuper.identifier();

    assertThat(superKeyword.typeBinding).isNotNull();
    assertThat(superKeyword.symbolType().fullyQualifiedName()).isEqualTo("java.lang.Iterable");
  }

  /**
   * Check that the owner of a parameter of a lambda declared outside any method is the class in which it is declared.
   */
  @Test
  void expression_lambda() {
    String source = """
      package org.foo;
      class A {
        java.util.function.Consumer<String> f = p -> { };
      }
      """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    var c = (ClassTree) cu.types().get(0);
    var f = (VariableTree) c.members().get(0);
    var lambda = (LambdaExpressionTree) f.initializer();
    var p = (VariableTreeImpl) lambda.parameters().get(0);

    Symbol newSymbol = cu.sema.variableSymbol(p.variableBinding);
    assertThat(newSymbol.declaration().firstToken().range().start().line()).isEqualTo(3);
    Symbol newOwner = newSymbol.owner();
    assertTrue(newOwner instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda());
  }

  /**
   * Check that the owner of lambda parameter is the method inside which the lambda is declared.
   */
  @Test
  void expression_lambda_variable_owner() {
    String source = """
      package org.foo;
      class A {
        void foo() {
          java.util.function.Consumer<String> v = p -> { };
        }
      }
      """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl foo = (MethodTreeImpl) c.members().get(0);
    Optional<BlockTreeImpl> firstBlock = foo.getChildren().stream()
      .filter(BlockTreeImpl.class::isInstance)
      .map(BlockTreeImpl.class::cast)
      .findFirst();
    VariableTree v = (VariableTree) firstBlock.get().body().get(0);
    LambdaExpressionTreeImpl lambda = (LambdaExpressionTreeImpl) v.initializer();
    VariableTreeImpl p = (VariableTreeImpl) lambda.parameters().get(0);

    // owner of lambda parameter is the field or variable to which it is assigned
    Symbol newSymbol = cu.sema.variableSymbol(p.variableBinding);
    Symbol newOwner = newSymbol.owner();
    assertTrue(newOwner instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda());
  }

  @Test
  void expression_nested_lambda() {
    String source = """
      class A {
        void m() {
          java.util.function.Function<Integer, java.util.function.Supplier<Integer>> v =
            p1 -> {
              return () -> 42;
            };
        }
      }
      """;

    JavaTree.CompilationUnitTreeImpl cu = test(source);

    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl v = (VariableTreeImpl) m.block().body().get(0);
    LambdaExpressionTreeImpl l1 = (LambdaExpressionTreeImpl) v.initializer();

    BlockTreeImpl body = (BlockTreeImpl) l1.body();
    ReturnStatementTreeImpl r = (ReturnStatementTreeImpl) body.body().get(0);
    LambdaExpressionTreeImpl l2 = (LambdaExpressionTreeImpl) r.expression();

    assertThat(l2.typeBinding).isNotNull();
    assertThat(cu.sema.type(l2.typeBinding).is("java.util.function.Supplier")).isTrue();
  }

  /**
   * @see org.eclipse.jdt.core.dom.ConditionalExpression
   */
  @Test
  void conditional_expression() {
    String source = """
      class A {
        void foo(Class<? extends java.io.Serializable>[] arr1, Class<?>[] arr2, boolean b) {
          Class<?>[] t = b ? arr1 : arr2;
        }
      }
      """;

    JavaTree.CompilationUnitTreeImpl cu = test(source);

    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl a = (VariableTreeImpl) m.block().body().get(0);
    ConditionalExpressionTreeImpl cond = (ConditionalExpressionTreeImpl) a.initializer();
    IdentifierTreeImpl c1 = (IdentifierTreeImpl) cond.trueExpression();
    IdentifierTreeImpl c2 = (IdentifierTreeImpl) cond.falseExpression();

    assertThat(cond.typeBinding).isNotNull();
    JType tcond = cu.sema.type(cond.typeBinding);
    assertThat(tcond.is("java.lang.Class[]")).isTrue();

    assertThat(c1.typeBinding).isNotNull();
    JType tc1 = cu.sema.type(c1.typeBinding);
    assertThat(tc1.is("java.lang.Class[]")).isTrue();

    assertThat(c2.typeBinding).isNotNull();
    JType tc2 = cu.sema.type(c2.typeBinding);
    assertThat(tc2.is("java.lang.Class[]")).isTrue();

    assertThat(tc2)
      .isNotEqualTo(tc1)
      .isEqualTo(tcond);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ConstructorInvocation
   */
  @Test
  void statement_constructor_invocation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C() { this(null); } C(Object p) { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl constructor2 = (MethodTreeImpl) c.members().get(1);
    ExpressionStatementTree s = (ExpressionStatementTree) m.block().body().get(0);

    MethodInvocationTreeImpl constructorInvocation = (MethodInvocationTreeImpl) s.expression();
    assertThat(constructorInvocation.methodBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) constructorInvocation.methodSymbol().declaration()).methodBinding)
      .isSameAs(constructor2.methodBinding);
    assertThat(constructorInvocation.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull(((ClassTreeImpl) constructorInvocation.symbolType().symbol().declaration())).typeBinding)
      .isSameAs(c.typeBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) constructorInvocation.methodSelect();
    assertThat(i.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(constructorInvocation.methodBinding);
    assertThat(cu.sema.usages.get(i.binding))
      .containsExactlyElementsOf(constructorInvocation.methodSymbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ConstructorInvocation
   */
  @Test
  void statement_constructor_invocation_recovery() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C() { this(42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) Objects.requireNonNull(m.block()).body().get(0);

    MethodInvocationTreeImpl constructorInvocation = (MethodInvocationTreeImpl) s.expression();
    assertThat(constructorInvocation.methodBinding)
      .isNull();
    assertThat(constructorInvocation.typeBinding)
      .isNotNull()
      .isSameAs(c.typeBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) constructorInvocation.methodSelect();
    assertThat(i.binding)
      .isNull();
    assertThat(cu.sema.usages.get(Objects.requireNonNull(m.methodBinding)))
      .isNull();
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperConstructorInvocation
   */
  @Test
  void statement_super_constructor_invocation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C extends S { C() { super(); } } class S { S() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassConstructor = (MethodTreeImpl) superClass.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) m.block().body().get(0);

    MethodInvocationTreeImpl superConstructorInvocation = (MethodInvocationTreeImpl) s.expression();
    assertThat(superConstructorInvocation.methodBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) superConstructorInvocation.methodSymbol().declaration()).methodBinding)
      .isSameAs(superClassConstructor.methodBinding);
    assertThat(superConstructorInvocation.typeBinding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull(((ClassTreeImpl) superConstructorInvocation.symbolType().symbol().declaration())).typeBinding)
      .isSameAs(superClass.typeBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) superConstructorInvocation.methodSelect();
    assertThat(i.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(superConstructorInvocation.methodBinding);
    assertThat(cu.sema.usages.get(i.binding))
      .containsExactlyElementsOf(superClassConstructor.symbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperConstructorInvocation
   */
  @Test
  void statement_super_constructor_invocation_recovery() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C extends S { C() { super(42); } } class S { S() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassConstructor = (MethodTreeImpl) superClass.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) Objects.requireNonNull(m.block()).body().get(0);

    MethodInvocationTreeImpl superConstructorInvocation = (MethodInvocationTreeImpl) s.expression();
    assertThat(superConstructorInvocation.methodBinding)
      .isNull();
    assertThat(superConstructorInvocation.typeBinding)
      .isNotNull()
      .isSameAs(superClass.typeBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) superConstructorInvocation.methodSelect();
    assertThat(i.binding)
      .isNull();
    assertThat(cu.sema.usages.get(Objects.requireNonNull(superClassConstructor.methodBinding)))
      .isNull();
  }

  @Test
  void statement_variable_declaration() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { int v; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl s = (VariableTreeImpl) m.block().body().get(0);
    assertThat(s.variableBinding).isNotNull();
    assertThat(cu.sema.declarations.get(s.variableBinding)).isSameAs(s);
  }

  @Test
  void statement_for() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { for (int v;;) ; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ForStatementTreeImpl s = (ForStatementTreeImpl) m.block().body().get(0);
    VariableTreeImpl v = (VariableTreeImpl) s.initializer().get(0);
    assertThat(v.variableBinding).isNotNull();
    assertThat(cu.sema.declarations.get(v.variableBinding)).isSameAs(v);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ImportDeclaration
   */
  @Test
  void declaration_import() {
    JavaTree.CompilationUnitTreeImpl cu = test("import java.util.List;");
    JavaTree.ImportTreeImpl i = (JavaTree.ImportTreeImpl) cu.imports().get(0);
    MemberSelectExpressionTreeImpl qualifiedName = (MemberSelectExpressionTreeImpl) i.qualifiedIdentifier();
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) qualifiedName.identifier();
    assertThat(identifier.binding)
      .isNull();
    assertThat(i.binding)
      .isNotNull();
    assertThat(cu.sema.typeSymbol((ITypeBinding) i.binding).usages())
      .isEmpty();
  }

  @Test
  void declaration_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C<T> { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.typeBinding).isNotNull();
    assertThat(cu.sema.declarations.get(c.typeBinding)).isSameAs(c);

    TypeParameterTreeImpl typeParameter = (TypeParameterTreeImpl) c.typeParameters().get(0);
    assertThat(typeParameter.typeBinding)
      .isNotNull();
  }

  /**
   * Records
   * (Preview in Java 14) https://openjdk.java.net/jeps/359
   * (Second Preview in Java 15) https://openjdk.java.net/jeps/384
   * (Final in Java 16) https://openjdk.java.net/jeps/395
   *
   * @see org.eclipse.jdt.core.dom.RecordDeclaration
   */
  @Test
  void declaration_record() {
    JavaTree.CompilationUnitTreeImpl cu = test("record R<T>(int component1, int component2) implements java.io.Serializable { public R { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.kind()).isEqualTo(Tree.Kind.RECORD);
    assertThat(c.declarationKeyword().text()).isEqualTo("record");
    assertThat(c.typeParameters()).hasSize(1);
    assertThat(c.recordOpenParenToken()).isNotNull();
    assertThat(c.recordComponents()).hasSize(2);
    assertThat(c.recordCloseParenToken()).isNotNull();
    assertThat(c.recordComponents().get(0).endToken()).isNotNull();
    assertThat(c.recordComponents().get(1).endToken()).isNull();
    assertThat(c.superInterfaces()).hasSize(1);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    assertThat(m.openParenToken()).isNull();
    assertThat(m.parameters()).isEmpty();
    assertThat(m.closeParenToken()).isNull();
  }

  /**
   * Sealed Classes
   * (Preview in Java 15) https://openjdk.java.net/jeps/360
   * (Second Preview in Java 16) https://openjdk.java.net/jeps/397
   * (Final in Java 17) https://openjdk.java.net/jeps/409
   *
   * @see org.eclipse.jdt.core.dom.TypeDeclaration
   */
  @Test
  void declaration_sealed_class() {
    JavaTree.CompilationUnitTreeImpl cu = test("sealed class Shape permits Circle, Package.Rectangle { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.modifiers()).hasSize(1);
    ModifierKeywordTree m = (ModifierKeywordTree) c.modifiers().get(0);
    assertThat(m.modifier()).isEqualTo(Modifier.SEALED);
    assertThat(m.firstToken()).is("sealed");

    assertThat(c.permitsKeyword()).is("permits");
    ListTree<TypeTree> permittedTypes = c.permittedTypes();
    assertThat(permittedTypes).hasSize(2);
    TypeTree circle = permittedTypes.get(0);
    TypeTree rectangle = permittedTypes.get(1);
    assertThat(circle).isInstanceOf(IdentifierTree.class);
    assertThat(((IdentifierTree) circle).name()).isEqualTo("Circle");
    assertThat(rectangle).isInstanceOf(MemberSelectExpressionTree.class);
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) rectangle;
    assertThat(memberSelect.identifier().name()).isEqualTo("Rectangle");
    assertThat(((IdentifierTree) memberSelect.expression()).name()).isEqualTo("Package");

    cu = test("non-sealed class Square extends Shape { }");
    c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.modifiers()).hasSize(1);
    m = (ModifierKeywordTree) c.modifiers().get(0);
    assertThat(m.modifier()).isEqualTo(Modifier.NON_SEALED);
    assertThat(c.modifiers().get(0).firstToken()).is("non-sealed");
  }

  /**
   * {@link org.eclipse.jdt.core.dom.ITypeBinding#getTypeDeclaration()} should be used
   * for {@link JSymbol#usages()} and {@link JSymbol#declaration()}
   */
  @Test
  void declaration_type_parameterized() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C<T> { C<String> field; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    AbstractTypedTree fieldType = (AbstractTypedTree) ((VariableTreeImpl) c.members().get(0)).type();
    IdentifierTreeImpl i = (IdentifierTreeImpl) ((ParameterizedTypeTree) fieldType).type();

    assertThat(cu.sema.usages.get(c.typeBinding))
      .containsOnly(i);
    assertThat(fieldType.typeBinding)
      .isNotNull();
    assertThat(cu.sema.typeSymbol(fieldType.typeBinding).declaration())
      .isSameAs(fieldType.symbolType().symbol().declaration())
      .isSameAs(c);
    assertThat(cu.sema.typeSymbol(fieldType.typeBinding).usages())
      .containsExactlyElementsOf(fieldType.symbolType().symbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.EnumConstantDeclaration
   */
  @Test
  void declaration_enum_constant() {
    JavaTree.CompilationUnitTreeImpl cu = test("enum E { C ; E() { } }");
    ClassTree e = (ClassTree) cu.types().get(0);
    VariableTreeImpl c = (VariableTreeImpl) e.members().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) e.members().get(1);
    assertThat(c.variableBinding).isNotNull();
    assertThat(cu.sema.declarations.get(c.variableBinding))
      .isSameAs(c.symbol().declaration())
      .isSameAs(c);
    NewClassTreeImpl initializer = Objects.requireNonNull((NewClassTreeImpl) c.initializer());
    IdentifierTreeImpl i = (IdentifierTreeImpl) initializer.getConstructorIdentifier();
    assertThat(i.binding)
      .isNotNull()
      .isSameAs(((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(constructor.methodBinding);
    assertThat(cu.sema.usages.get(constructor.methodBinding))
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.EnumConstantDeclaration
   */
  @Test
  void declaration_enum_constant_recovered_constructor() {
    JavaTree.CompilationUnitTreeImpl cu = test("@GenerateConstructor enum E { A(0), B(1) ; @GenerateGetter Integer value;  }");
    ClassTree e = (ClassTree) cu.types().get(0);
    VariableTreeImpl c = (VariableTreeImpl) e.members().get(0);
    NewClassTreeImpl initializer = Objects.requireNonNull((NewClassTreeImpl) c.initializer());
    IdentifierTreeImpl i = (IdentifierTreeImpl) initializer.getConstructorIdentifier();
    assertThat(i.binding).isNull();
    assertThat(i.symbol().isUnknown()).isTrue();
  }

  /**
   * @see org.eclipse.jdt.core.dom.EnumConstantDeclaration
   */
  @Test
  void declaration_enum_constant_anonymous() {
    JavaTree.CompilationUnitTreeImpl cu = test("enum E { C { }; E(){} }");
    ClassTree e = (ClassTree) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) e.members().get(1);
    VariableTreeImpl c = (VariableTreeImpl) e.members().get(0);
    assertThat(c.variableBinding).isNotNull();
    assertThat(cu.sema.declarations.get(c.variableBinding))
      .isSameAs(c.symbol().declaration())
      .isSameAs(c);

    NewClassTreeImpl initializer = (NewClassTreeImpl) c.initializer();
    ClassTreeImpl enumConstantBody = (ClassTreeImpl) initializer.classBody();
    assertThat(enumConstantBody.typeBinding).isNotNull();
    assertThat(cu.sema.declarations.get(enumConstantBody.typeBinding))
      .isSameAs(enumConstantBody.symbol().declaration())
      .isSameAs(enumConstantBody);

    IdentifierTreeImpl identifier = (IdentifierTreeImpl) initializer.getConstructorIdentifier();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) identifier.symbol().declaration()).methodBinding)
      .isSameAs(constructor.methodBinding);
    assertThat(cu.sema.usages.get(constructor.methodBinding))
      .containsExactlyElementsOf(constructor.symbol().usages())
      .containsOnly(identifier);
  }

  @Test
  void declaration_method() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    assertThat(m.methodBinding).isNotNull();
    assertThat(cu.sema.declarations.get(m.methodBinding)).isSameAs(m);
  }

  /**
   * {@link org.eclipse.jdt.core.dom.IMethodBinding#getMethodDeclaration()} should be used
   * for {@link JSymbol#usages()} and {@link JSymbol#declaration()}
   */
  @Test
  void declaration_method_parameterized() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { <T> void m(T t) { m(42); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) s.expression();
    IdentifierTreeImpl i = (IdentifierTreeImpl) methodInvocation.methodSelect();

    assertThat(cu.sema.usages.get(m.methodBinding))
      .containsOnly(i);
    assertThat(methodInvocation.methodBinding)
      .isNotNull();
    assertThat(cu.sema.methodSymbol(methodInvocation.methodBinding).declaration())
      .isSameAs(methodInvocation.methodSymbol().declaration())
      .isSameAs(m);
    assertThat(cu.sema.methodSymbol(methodInvocation.methodBinding).usages())
      .containsExactlyElementsOf(methodInvocation.methodSymbol().usages())
      .containsOnly(i);
  }

  @Test
  void declaration_method_parameterized_recovered() {
    String source = """
      class D<E> {
        private java.util.Collection<UnknownClass.Entry<E>> samples() {
          return null;
        }
      }
      """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);

    assertThat(m.methodBinding).isNull();
    MethodSymbol recovered = m.symbol();
    assertThat(recovered.isUnknown()).isTrue();

    assertThat(recovered.isTypeSymbol()).isFalse();
    assertThat(recovered.isVariableSymbol()).isFalse();
    assertThat(recovered.isMethodSymbol()).isFalse();
    assertThat(recovered.isPackageSymbol()).isFalse();

    assertThat(recovered.isAbstract()).isFalse();
    assertThat(recovered.isDeprecated()).isFalse();
    assertThat(recovered.isEnum()).isFalse();
    assertThat(recovered.isFinal()).isFalse();
    assertThat(recovered.isInterface()).isFalse();
    assertThat(recovered.isPackageVisibility()).isFalse();
    assertThat(recovered.isPrivate()).isFalse();
    assertThat(recovered.isProtected()).isFalse();
    assertThat(recovered.isPublic()).isFalse();
    assertThat(recovered.isStatic()).isFalse();
    assertThat(recovered.isVolatile()).isFalse();

    assertThat(recovered.owner()).isNotNull();
    assertThat(recovered.owner().isUnknown()).isTrue();
    assertThat(recovered.enclosingClass()).isNotNull();
    assertThat(recovered.enclosingClass().isUnknown()).isTrue();
    assertThat(recovered.returnType()).isNotNull();
    assertThat(recovered.returnType().isUnknown()).isTrue();

    assertThat(recovered.type().isUnknown()).isTrue();
    assertThat(recovered.declaration()).isNull();
    assertThat(recovered.overriddenSymbols()).isEmpty();
    assertThat(recovered.parameterTypes()).isEmpty();
    assertThat(recovered.thrownTypes()).isEmpty();

    SymbolMetadata metadata = recovered.metadata();
    assertThat(metadata).isNotNull();
    assertThat(metadata.annotations()).isEmpty();
  }

  @Test
  void no_metadata_annotations_on_noncompiling_annotation_code() {
    String source = "" +
      " public class C {\n" +
      "  interface I1 {}\n" +
      "  interface I2 {\n" +
      // Does not compile, I1 can not be used as an annotation
      "    @I1(\"\")\n" +
      "    String m();\n" +
      "  }\n" +
      "  void foo(I2 i2) {\n" +
      // ECJ throws a NPE when trying to get the metadata of the method m().
      "    i2.m();\n" +
      "  }\n" +
      "}";

    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(2);
    ExpressionStatementTree expression = (ExpressionStatementTree) m.block().body().get(0);

    SymbolMetadata metadata = assertDoesNotThrow(() -> ((MethodInvocationTreeImpl) expression.expression()).methodSymbol().metadata());
    assertThat(metadata.annotations()).isEmpty();
  }

  @Test
  void declaration_parameter() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m(int p) {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    assertThat(p.variableBinding).isNotNull();
    assertThat(cu.sema.declarations.get(p.variableBinding)).isSameAs(p);
  }

  @Test
  void declaration_field() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    assertThat(f.variableBinding).isNotNull();
    assertThat(cu.sema.declarations.get(f.variableBinding)).isSameAs(f);
  }

  @Test
  void declaration_lambda_parameter() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { lambda(v -> {}); } void lambda(java.util.function.Consumer x) { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) m.block().body().get(0);
    MethodInvocationTreeImpl i = (MethodInvocationTreeImpl) s.expression();
    LambdaExpressionTree e = (LambdaExpressionTree) i.arguments().get(0);
    VariableTreeImpl v = (VariableTreeImpl) e.parameters().get(0);
    assertThat(v.variableBinding).isNotNull();
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(cu.sema.declarations.get(v.variableBinding)).isSameAs(v);
  }

  @Test
  void declaration_annotation_member() {
    JavaTree.CompilationUnitTreeImpl cu = test("@interface A { String m(); }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    assertThat(m.methodBinding).isNotNull();
    assertThat(cu.sema.declarations.get(m.methodBinding)).isSameAs(m);
  }

  @ParameterizedTest(name="[{index}] Type bindings of variable v should not be null in \"{0}\"")
  @ValueSource(strings = {
    "interface I { int v; }", // primitive
    "interface I<T> { I<String> v; }", // parameterized
    "interface I { I1.I2 v; interface I2 {} }", // simple
    "interface I1<T> { I1<String>. @Annotation I2 v; interface I2 {} }", // qualified
    "interface I1 { I1. @Annotation I2 v; interface I2 {} }", // name qualified
  })
  void test_abstract_type_tree_is_not_null(String source) {
    CompilationUnitTree cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_array() {
    CompilationUnitTree cu = test("interface I { int[][] v; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    JavaTree.ArrayTypeTreeImpl t = (JavaTree.ArrayTypeTreeImpl) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(2);
    t = (JavaTree.ArrayTypeTreeImpl) t.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(1);
  }

  @Test
  void type_wildcard() {
    CompilationUnitTree cu = test("interface I<T> { I<? extends Object> v; }");
    ClassTree c = (ClassTree) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    ParameterizedTypeTree p = (ParameterizedTypeTree) v.type();
    AbstractTypedTree t = ((AbstractTypedTree) p.typeArguments().get(0));
    assertThat(t.typeBinding).isNotNull();
  }

  /**
   * Tests a limitation of ECJ engine in marking unknown types as such when using union types
   * Here, 'e' is recognized as being of type 'Object'
   */
  @Test
  void union_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A { void m() { try { } catch (Unknown1 | Unknown2 e) { e.toString(); } } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    CatchTree catchTree = ((TryStatementTree) m.block().body().get(0)).catches().get(0);

    Symbol exceptionVariable = catchTree.parameter().symbol();
    // obviously wrong - recovered type
    assertThat(exceptionVariable.isUnknown()).isFalse();
    assertThat(exceptionVariable.type()).is("java.lang.Object");

    MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) ((ExpressionStatementTree) catchTree.block().body().get(0)).expression();
    IdentifierTreeImpl e = (IdentifierTreeImpl) ((MemberSelectExpressionTreeImpl) mit.methodSelect()).expression();
    // obviously wrong - recovered type
    assertThat(e.symbol().isUnknown()).isFalse();
    assertThat(e.symbolType()).is("java.lang.Object");

    // resolution still works
    assertThat(exceptionVariable.usages())
      .hasSize(1)
      .containsOnly(e);
    assertThat(exceptionVariable).isEqualTo(e.symbol());
  }

  @Test
  void type_try_with_resource() {
    {
      // closeable is unknown but recognized as closeable
      CompilationUnitTree cu = test("class C { void m(UnknownCloseable p) { try (UnknownCloseable f = p) { } } }");
      ClassTree c = (ClassTree) cu.types().get(0);
      MethodTree m = (MethodTree) c.members().get(0);
      TryStatementTree s = (TryStatementTree) m.block().body().get(0);
      VariableTreeImpl v = (VariableTreeImpl) s.resourceList().get(0);
      AbstractTypedTree t = (AbstractTypedTree) v.type();
      assertThat(t.typeBinding).isNotNull();
      JType t2 = (JType) v.symbol().type();
      assertThat(t2).isNotNull();
      assertThat(t2.isUnknown()).isTrue();
      assertThat(t2.typeBinding).isNotNull();
    }
    {
      // C not recognized as closeable
      CompilationUnitTree cu = test("class C { void m(C p) { try (C f = p) { } } }");
      ClassTree c = (ClassTree) cu.types().get(0);
      MethodTree m = (MethodTree) c.members().get(0);
      TryStatementTree s = (TryStatementTree) m.block().body().get(0);
      VariableTreeImpl v = (VariableTreeImpl) s.resourceList().get(0);
      AbstractTypedTree t = (AbstractTypedTree) v.type();
      assertThat(t.typeBinding).isNotNull();
      Type type = v.symbol().type();
      assertThat(type).isNotNull();
      assertThat(type.isUnknown()).isTrue();
    }
  }

  @Test
  void type_union() {
    CompilationUnitTree cu = test("class C { void m() { try { } catch (E1 | E2 v) { } } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    TryStatementTree s = (TryStatementTree) m.block().body().get(0);
    VariableTreeImpl v = (VariableTreeImpl) s.catches().get(0).parameter();
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  /**
   * @see Tree.Kind#VAR_TYPE
   */
  @Test
  void type_var() {
    CompilationUnitTree cu = test("class C { void m() { var v = 42; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    VariableTreeImpl v = (VariableTreeImpl) m.block().body().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_extra_dimensions() {
    CompilationUnitTree cu = test("interface I { I v[][]; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    JavaTree.ArrayTypeTreeImpl t = (JavaTree.ArrayTypeTreeImpl) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(2);
    t = (JavaTree.ArrayTypeTreeImpl) t.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(1);
  }

  @Test
  void type_vararg() {
    CompilationUnitTree cu = test("interface I { void m(int[]... v); }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    VariableTreeImpl v = (VariableTreeImpl) m.parameters().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(2);
  }

  @Test
  void symbol_unknown() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A implements UnknownInterface { }");
    ClassTree c = (ClassTree) cu.types().get(0);
    IdentifierTreeImpl i = (IdentifierTreeImpl) c.superInterfaces().get(0);
    Symbol s = cu.sema.typeSymbol(Objects.requireNonNull((ITypeBinding) i.binding));
    assertThat(s.isUnknown())
      .isEqualTo(i.symbol().isUnknown())
      .isTrue();
    assertThat(s.isTypeSymbol())
      .isEqualTo(i.symbol().isTypeSymbol())
      .isFalse();
  }

  @Test
  void annotation_on_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("interface I { void m(@Annotation Object p); }" +
      " @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE}) @interface Annotation { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl parameter = (VariableTreeImpl) m.parameters().get(0);
    assertThat(cu.sema.variableSymbol(parameter.variableBinding).metadata().annotations().size())
      .isEqualTo(parameter.symbol().metadata().annotations().size())
      .isEqualTo(1);
    assertThat(JUtils.parameterAnnotations(cu.sema.methodSymbol(Objects.requireNonNull(m.methodBinding)), 0).annotations().size())
      .isEqualTo(JUtils.parameterAnnotations(m.symbol(), 0).annotations().size())
      .isEqualTo(1);
  }

  @Test
  void annotation_on_var_type_local_variable() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m() { @Annotation var v = 42; } }" +
      " @java.lang.annotation.Target({java.lang.annotation.ElementType.LOCAL_VARIABLE}) @interface Annotation { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl variable = (VariableTreeImpl) m.block().body().get(0);

    List<SymbolMetadata.AnnotationInstance> annotations = variable.symbol().metadata().annotations();
    assertThat(annotations).hasSize(1);
    assertThat(annotations.get(0).symbol().name()).isEqualTo("Annotation");
  }

  /**
   * @see org.eclipse.jdt.core.dom.LabeledStatement
   */
  @Nested
  class Labels {
    /**
     * @see org.eclipse.jdt.core.dom.BreakStatement
     */
    @Test
    void statement_break() {
      CompilationUnitTree cu = test("class C { void m() { i: break i; } }");
      ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
      MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
      LabeledStatementTreeImpl l = (LabeledStatementTreeImpl) m.block().body().get(0);
      BreakStatementTreeImpl b = (BreakStatementTreeImpl) l.statement();
      IdentifierTreeImpl i = (IdentifierTreeImpl) b.label();

      assertThat(l.labelSymbol)
        .isInstanceOf(Symbol.LabelSymbol.class);
      assertThat(i.labelSymbol)
        .isNotNull()
        .isInstanceOf(Symbol.LabelSymbol.class)
        .isInstanceOf(Symbol.class)
        .isSameAs(l.labelSymbol);
      assertThat(i.binding)
        .isNull();

      assertThat(i.symbol())
        .isInstanceOf(Symbol.LabelSymbol.class)
        .isInstanceOf(Symbol.class)
        .isSameAs(l.symbol());

      assertThat(i.labelSymbol.declaration())
        .isSameAs(i.symbol().declaration());
      assertThat(i.labelSymbol.usages())
        .containsExactlyElementsOf(l.symbol().usages())
        .containsOnly(i);
    }

    /**
     * @see org.eclipse.jdt.core.dom.ContinueStatement
     */
    @Test
    void statement_continue() {
      CompilationUnitTree cu = test("class C { void m() { i: for(;;) continue i; } }");
      ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
      MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
      LabeledStatementTreeImpl l = (LabeledStatementTreeImpl) m.block().body().get(0);
      ForStatementTreeImpl f = (ForStatementTreeImpl) l.statement();
      ContinueStatementTreeImpl co = (ContinueStatementTreeImpl) f.statement();
      IdentifierTreeImpl i = (IdentifierTreeImpl) co.label();

      assertThat(l.labelSymbol)
        .isNotNull()
        .isSameAs(i.labelSymbol);
      assertThat(i.binding)
        .isNull();
      assertThat(l.labelSymbol.declaration)
        .isSameAs(i.labelSymbol.declaration)
        .isSameAs(l);
      assertThat(l.labelSymbol.usages())
        .containsExactlyElementsOf(l.symbol().usages())
        .containsOnly(i);
    }

    @Test
    void nested() {
      CompilationUnitTree cu = test("class C { void m1() { i: { new C() { void m2() { i: break i; } }; break i; } } }");
      MethodTreeImpl m1 = (MethodTreeImpl) ((ClassTreeImpl) cu.types().get(0)).members().get(0);
      LabeledStatementTreeImpl l1 = (LabeledStatementTreeImpl) m1.block().body().get(0);
      BlockTreeImpl block = (BlockTreeImpl) l1.statement();
      BreakStatementTreeImpl b1 = (BreakStatementTreeImpl) block.body().get(1);
      IdentifierTreeImpl i1 = (IdentifierTreeImpl) b1.label();

      assertThat(l1.labelSymbol)
        .isNotNull()
        .isSameAs(i1.labelSymbol);
      assertThat(i1.binding)
        .isNull();
      assertThat(l1.labelSymbol.declaration)
        .isSameAs(i1.labelSymbol.declaration)
        .isSameAs(l1);
      assertThat(l1.labelSymbol.usages())
        .containsOnly(i1);

      ExpressionStatementTreeImpl e = (ExpressionStatementTreeImpl) block.body().get(0);
      NewClassTreeImpl n = (NewClassTreeImpl) e.expression();
      MethodTreeImpl m2 = (MethodTreeImpl) n.classBody().members().get(0);
      LabeledStatementTreeImpl l2 = (LabeledStatementTreeImpl) m2.block().body().get(0);
      BreakStatementTreeImpl b2 = (BreakStatementTreeImpl) l2.statement();
      IdentifierTreeImpl i2 = (IdentifierTreeImpl) b2.label();

      assertThat(l2.labelSymbol)
        .isNotNull()
        .isSameAs(i2.labelSymbol);
      assertThat(i2.binding)
        .isNull();
      assertThat(l2.labelSymbol.declaration)
        .isSameAs(i2.labelSymbol.declaration)
        .isSameAs(l2);
      assertThat(l2.labelSymbol.usages())
        .containsOnly(i2);
    }
  }

  @Test
  void constructor_with_type_arguments() {
    String source = """
      class MyClass {
        <T extends I> MyClass(T t) {}
        <T extends J & I> MyClass(T t) {}
        void foo(B b, C c) {
          new<B>MyClass((I) b);
          new<C>MyClass(c);
        }
      }
      interface I {}
      interface J {}
      class B implements I {}
      class C implements I, J {}
      """;

    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree firstConstructor = (MethodTree) c.members().get(0);
    MethodTree secondConstructor = (MethodTree) c.members().get(1);

    assertThat(firstConstructor.symbol().usages()).hasSize(1);
    assertThat(secondConstructor.symbol().usages()).hasSize(1);
  }

  private static ExpressionTree expression(String expression) {
    CompilationUnitTree cu = test("class C { Object m() { return " + expression + " ; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    return Objects.requireNonNull(s.expression());
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

  /**
   * Previously ECJ was adding an implicit break statement for empty switch cases blocks.
   * This is not the case anymore, and we do not need to test this.
   */
  @Test
  void not_need_to_skip_implicit_break_statement() {
    final String source = "class C { void m() { switch (0) { case 0 -> { } } } }";
    CompilationUnit cu = createAST(source);
    TypeDeclaration c = (TypeDeclaration) cu.types().get(0);
    MethodDeclaration m = c.getMethods()[0];
    SwitchStatement s = (SwitchStatement) m.getBody().statements().get(0);
    Block block = (Block) s.statements().get(1);
    assertThat(block.statements()).isEmpty();

    CompilationUnitTree compilationUnit = test(source);
    ClassTree cls = (ClassTree) compilationUnit.types().get(0);
    MethodTree method = (MethodTree) cls.members().get(0);
    SwitchStatementTree switchStatement = (SwitchStatementTree) Objects.requireNonNull(method.block()).body().get(0);
    BlockTree blockStatement = (BlockTree) switchStatement.cases().get(0).body().get(0);
    assertThat(blockStatement.body()).isEmpty();
  }

  private CompilationUnit createAST(String source) {
    JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    Map<String, String> options =  new HashMap<>(JavaCore.getOptions());
    JavaCore.setComplianceOptions(version.effectiveJavaVersionAsString(), options);
    options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    astParser.setCompilerOptions(options);
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName("File.java");
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);
    astParser.setSource(source.toCharArray());
    return (CompilationUnit) astParser.createAST(null);
  }

  @Test
  void super_constructor_external_inner_class() {
    final String source = """
      class C {
          class InnerC {
              InnerC(int i) {}
          }
      }
      
      class B {
          class InnerB extends C.InnerC {
              InnerB(C c, int i) {
                  c.super(i);
              }
          }
      }
      """;

    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTree c = (ClassTree) cu.types().get(0);
    ClassTree innerC = (ClassTree) c.members().get(0);
    MethodTree innerConstructor = (MethodTree) innerC.members().get(0);
    assertThat(innerConstructor.symbol().usages()).hasSize(1);
  }

  @Test
  void inner_class_depending_on_outer_class_parametrized_type() {
    final String source = """
        class X<T> {
          InnerClass innerClass;
          class InnerClass {
            T method() {
              return null;
            }
          }
          static void test() {
            new X<Y>().innerClass.method().method1();
          }
        }
        class Y {
          void method1() {
          }
        }
        """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTree classX = (ClassTree) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) ((ClassTree) classX.members().get(1)).members().get(0);
    MethodTreeImpl method1 = ((MethodTreeImpl) ((ClassTree) cu.types().get(1)).members().get(0));
    assertThat(method1.symbol().usages()).hasSize(1);
    assertThat(method.symbol().usages()).hasSize(1);
  }

  @Test
  void warnings_are_detected() {
    String source = "package test;\n"
      + "import java.util.List;\n"   // useless import
      + "import test.C;\n"           // not detected by ECJ
      + "import java.lang.Object;\n" // useless import
      + "class C {\n"
      + "  void foo(String s) {\n"
      + "    String o = (String) ((String) s);\n" // 2x redundant cast
      + "  }\n"
      + "}\n";

    JavaTree.CompilationUnitTreeImpl cu = test(source);
    List<JWarning> importsWarnings = cu.warnings(JWarning.Type.UNUSED_IMPORT);
    assertThat(importsWarnings).hasSize(2);

    JWarning listWarning = importsWarnings.get(0);
    assertThat(listWarning.message()).isEqualTo("The import java.util.List is never used");
    assertThat(listWarning.syntaxTree()).isEqualTo(cu.imports().get(0));

    JWarning objectWarning = importsWarnings.get(1);
    assertThat(objectWarning.message()).isEqualTo("The import java.lang.Object is never used");
    assertThat(objectWarning.syntaxTree()).isEqualTo(cu.imports().get(2));

    List<JWarning> castWarnings = cu.warnings(JWarning.Type.REDUNDANT_CAST);
    assertThat(castWarnings).hasSize(2);

    TypeCastTree typeCast = (TypeCastTree) ((VariableTree) ((MethodTree) (((ClassTree) cu.types().get(0)).members().get(0))).block().body().get(0)).initializer();
    JWarning parentCastWarning = castWarnings.get(0);
    assertThat(parentCastWarning.message()).isEqualTo("Unnecessary cast from String to String");
    assertThat(parentCastWarning.syntaxTree()).isEqualTo(typeCast);

    ParenthesizedTree parenthesizedTree = (ParenthesizedTree) typeCast.expression();
    JWarning nestedCastWarning = castWarnings.get(1);
    assertThat(nestedCastWarning.message()).isEqualTo("Unnecessary cast from String to String");
    assertThat(nestedCastWarning.syntaxTree()).isEqualTo(parenthesizedTree);
  }

  @Test
  void test_varialbe_equals_inNestedLambdas() {
    String source = """
          package org.foo;
          class A {
            void foo() {
              consume(p2 -> {
                 consume(p1 -> {
                      usage(p1);
                 });
              });
            }
            interface C extends java.util.function.Consumer<String> {}
            int consume(C c) {
              return 0;
            }
          }
      """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    var c = (ClassTreeImpl) cu.types().get(0);
    var foo = (MethodTree) c.members().get(0);
    var consumeInvocation = (MethodInvocationTree) ((ExpressionStatementTree) foo.block().body().get(0)).expression();
    var lambdaA = (LambdaExpressionTree) consumeInvocation.arguments().get(0);
    var p2 = lambdaA.parameters().get(0);

    var consumeInvocation2 = (MethodInvocationTree) ((ExpressionStatementTree) ((BlockTree) lambdaA.body()).body().get(0)).expression();
    var lambda2 = (LambdaExpressionTree) consumeInvocation2.arguments().get(0);
    var p1 = lambda2.parameters().get(0);

    Symbol symbol1 = p1.symbol();
    Symbol symbol2 = p2.symbol();
    assertNotEquals(symbol1, p2.symbol());

    Symbol owner1 = symbol1.owner();
    assertTrue(owner1 instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda());
    Symbol owner2 = symbol2.owner();
    assertTrue(owner2 instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda());
    assertNotEquals(owner1, owner2);
  }

  @Test
  void testOwner_inInitializers() {
    CompilationUnitTree cu = test("""
      class C { 
        static { int j = 1; } 
        { java.util.function.IntConsumer k = i -> {}; }
      }
      """);
    ClassTree classTree = (ClassTree) cu.types().get(0);
    var block1 = (StaticInitializerTreeImpl) classTree.members().get(0);
    var j = (VariableTree) block1.body().get(0);

    var block2 = (BlockTree) classTree.members().get(1);
    var k = (VariableTree) block2.body().get(0);

    var lambda = (LambdaExpressionTree) k.initializer();
    var i = lambda.parameters().get(0).symbol();
    Symbol iOwner = i.owner();
    Symbol jOwner = j.symbol().owner();
    Symbol kOwner = k.symbol().owner();
    assertNotEquals(jOwner, kOwner);
    assertTrue(iOwner instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda());
  }

  @Test
  void testOwner_inInitializersLambdas() {
    String source = """
          package org.foo;
          class A {
            static { consume(p -> {}); }
            { consume(p -> {}); }
            interface C extends java.util.function.Consumer<String> {}
            int consume(C c) { return 0; }
          }
      """;
    CompilationUnitTree cu = test(source);
    var classTree = (ClassTree) cu.types().get(0);

    var staticInit = (StaticInitializerTree) classTree.members().get(0);
    var invocation1 = (MethodInvocationTree) ((ExpressionStatementTree) staticInit.body().get(0)).expression();
    var lambda1 = (LambdaExpressionTree) invocation1.arguments().get(0);
    var p1 = lambda1.parameters().get(0);
    var owner1 = p1.symbol().owner();

    var init = (BlockTree) classTree.members().get(1);
    var invocation2 = (MethodInvocationTree) ((ExpressionStatementTree) init.body().get(0)).expression();
    var lambda2 = (LambdaExpressionTree) invocation2.arguments().get(0);
    var p2 = lambda2.parameters().get(0);
    var owner2 = p2.symbol().owner();

    assertNotEquals(owner1, owner2);
  }

  @Test
  void testOwner_inFieldsInLambda() {
    CompilationUnitTree cu = test("""
      class C {
        F k = s -> {};
        F j = s -> {};
        interface F extends java.util.function.Consumer<String> {}
      }
      """);
    ClassTree classTree = (ClassTree) cu.types().get(0);
    var k = (VariableTree) classTree.members().get(0);
    Symbol s1 = ((LambdaExpressionTree) k.initializer()).parameters().get(0).symbol();
    Symbol s1Owner = s1.owner();
    assertThat(s1Owner instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda()).isTrue();

    var j = (VariableTree) classTree.members().get(1);
    Symbol s2 = ((LambdaExpressionTree) j.initializer()).parameters().get(0).symbol();
    Symbol s2Owner = s2.owner();
    assertThat(s2Owner instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda()).isTrue();
    assertNotEquals(s1Owner, s2Owner);

    // For sanity, check that the symbols are not equal
    assertNotEquals(s1, s2);
  }

  @Test
  void testVariableOwner_inLambdasInMethods() {
    String source = """
      package org.foo;
      class A {
        void f1() {
          java.util.function.Consumer<String> a = p -> { System.out.println(p); };
          java.util.function.Consumer<String> c = p -> { System.out.println(p); };
        }
        void f2() {
          java.util.function.Consumer<String> b = p -> { System.out.println(p); };
        }
      }
      """;
    JavaTree.CompilationUnitTreeImpl cu = test(source);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);

    var f1 = (MethodTree) c.members().get(0);
    var variableTreeA = (VariableTree) f1.block().body().get(0);
    var lambdaA = (LambdaExpressionTreeImpl) variableTreeA.initializer();
    var pOfA = lambdaA.parameters().get(0);

    var variableTreeC = (VariableTree) f1.block().body().get(1);
    var lambdaC = (LambdaExpressionTree) variableTreeC.initializer();
    var pOfC = lambdaC.parameters().get(0);

    var f2 = (MethodTree) c.members().get(1);
    var variableTreeB = (VariableTree) f2.block().body().get(0);
    var lambdaB = (LambdaExpressionTree) variableTreeB.initializer();
    var pOfB = lambdaB.parameters().get(0);

    Symbol ownerA = pOfA.symbol().owner();
    Symbol ownerC = pOfC.symbol().owner();
    Symbol ownerB = pOfB.symbol().owner();

    assertThat(ownerA instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda()).isTrue();
    assertThat(ownerB instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda()).isTrue();
    assertThat(ownerC instanceof JMethodSymbol methodSymbol && methodSymbol.isLambda()).isTrue();
    assertNotEquals(ownerA, ownerC);
    assertNotEquals(ownerA, ownerB);
    assertNotEquals(ownerC, ownerB);

    assertNotEquals(pOfA.symbol(), pOfB.symbol());
    assertNotEquals(pOfA.symbol(), pOfC.symbol());
    assertNotEquals(pOfB.symbol(), pOfC.symbol());

  }
}
