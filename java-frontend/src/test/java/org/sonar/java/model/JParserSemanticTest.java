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
package org.sonar.java.model;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding).isSameAs(e.typeBinding);
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

  @Test
  void expression_field_access() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; Object m() { return this.f; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(1);
    ReturnStatementTree s = (ReturnStatementTree) m.block().body().get(0);
    MemberSelectExpressionTreeImpl e = (MemberSelectExpressionTreeImpl) s.expression();
    assertThat(e.typeBinding).isNotNull();
    IdentifierTreeImpl i = (IdentifierTreeImpl) e.identifier();
    assertThat(i.typeBinding).isSameAs(e.typeBinding);
    assertThat(i.binding).isNotNull();
    assertThat(cu.sema.usages.get(i.binding)).containsOnly(i);
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
      .containsOnlyElementsOf(constructor.symbol().usages())
      .containsOnly(i);
  }

  /**
   * @see org.eclipse.jdt.core.dom.ClassInstanceCreation
   */
  @Test
  void expression_anonymous_class_instance_creation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m() { return new Object() { }; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    NewClassTreeImpl e = (NewClassTreeImpl) s.expression();
    ClassTreeImpl b = (ClassTreeImpl) e.classBody();
    assertThat(b.typeBinding).isNotNull();
    assertThat(cu.sema.declarations.get(b.typeBinding))
      .isSameAs(b.symbol().declaration())
      .isSameAs(b);
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
      .containsOnlyElementsOf(constructor.symbol().usages())
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
      .containsOnlyElementsOf(method.symbol().usages())
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
      .containsOnlyElementsOf(method.symbol().usages())
      .containsOnly(identifier);
  }

  /**
   * @see org.eclipse.jdt.core.dom.SuperMethodReference
   */
  @Test
  void expression_super_method_reference() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C extends S { java.util.function.Supplier m() { return super::m; } } class S { Object m() { } }");
    ClassTreeImpl superClass = (ClassTreeImpl) cu.types().get(1);
    MethodTreeImpl superClassMethod = (MethodTreeImpl) superClass.members().get(0);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) method.block().body().get(0);
    MethodReferenceTreeImpl creationReference = (MethodReferenceTreeImpl) s.expression();
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) creationReference.method();
    assertThat(identifier.binding)
      .isNotNull()
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) creationReference.method().symbol().declaration()).methodBinding)
      .isSameAs(superClassMethod.methodBinding);
    assertThat(cu.sema.usages.get(superClassMethod.methodBinding))
      .containsOnlyElementsOf(superClassMethod.symbol().usages())
      .containsOnly(identifier);
  }

  @Test
  void expression_switch() {
    assertThat(expression("switch (0) { default -> 0; case 0 -> 0; }"))
      .isNotInstanceOf(AbstractTypedTree.class);
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
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) methodInvocation.symbol().declaration()).methodBinding)
      .isSameAs(method.methodBinding);
    IdentifierTreeImpl i = (IdentifierTreeImpl) methodInvocation.methodSelect();
    assertThat(i.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(methodInvocation.methodBinding);
    assertThat(cu.sema.usages.get(i.binding))
      .containsOnlyElementsOf(methodInvocation.symbol().usages())
      .containsOnly(i);
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
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) superMethodInvocation.symbol().declaration()).methodBinding)
      .isSameAs(superClassMethod.methodBinding);
    MemberSelectExpressionTreeImpl e2 = (MemberSelectExpressionTreeImpl) superMethodInvocation.methodSelect();
    IdentifierTreeImpl i = (IdentifierTreeImpl) e2.identifier();
    assertThat(i.binding)
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) i.symbol().declaration()).methodBinding)
      .isSameAs(superMethodInvocation.methodBinding);
    assertThat(cu.sema.usages.get(i.binding))
      .containsOnlyElementsOf(superMethodInvocation.symbol().usages())
      .containsOnly(i);
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
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) constructorInvocation.symbol().declaration()).methodBinding)
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
      .containsOnlyElementsOf(constructorInvocation.symbol().usages())
      .containsOnly(i);
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
      .isSameAs(Objects.requireNonNull((MethodTreeImpl) superConstructorInvocation.symbol().declaration()).methodBinding)
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
      .containsOnlyElementsOf(superClassConstructor.symbol().usages())
      .containsOnly(i);
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

  @Test
  void declaration_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.typeBinding).isNotNull();
    assertThat(cu.sema.declarations.get(c.typeBinding)).isSameAs(c);
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
  void declaration_enum_constant_anonymous() {
    JavaTree.CompilationUnitTreeImpl cu = test("enum E { C { } }");
    ClassTree e = (ClassTree) cu.types().get(0);
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
      .isSameAs(methodInvocation.symbol().declaration())
      .isSameAs(m);
    assertThat(cu.sema.methodSymbol(methodInvocation.methodBinding).usages())
      .containsExactlyElementsOf(methodInvocation.symbol().usages())
      .containsOnly(i);
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

  @Test
  void type_primitive() {
    CompilationUnitTree cu = test("interface I { int v; }");
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
  void type_parameterized() {
    CompilationUnitTree cu = test("interface I<T> { I<String> v; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_simple() {
    CompilationUnitTree cu = test("interface I { I1.I2 v; interface I2 {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_qualified() {
    CompilationUnitTree cu = test("interface I1<T> { I1<String>. @Annotation I2 v; interface I2 {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_name_qualified() {
    CompilationUnitTree cu = test("interface I1 { I1. @Annotation I2 v; interface I2 {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
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

  private ExpressionTree expression(String expression) {
    CompilationUnitTree cu = test("class C { Object m() { return " + expression + " ; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    return Objects.requireNonNull(s.expression());
  }

  private JavaTree.CompilationUnitTreeImpl test(String source) {
    List<File> classpath = Collections.emptyList();
    JavaTree.CompilationUnitTreeImpl t = (JavaTree.CompilationUnitTreeImpl) JParser.parse("12", "File.java", source, true, classpath);
    SemanticModel.createFor(t, new SquidClassLoader(classpath));
    return t;
  }

  @Test
  void should_skip_implicit_break_statement() {
    final String source = "class C { void m() { switch (0) { case 0 -> { } } } }";
    CompilationUnit cu = createAST(source);
    TypeDeclaration c = (TypeDeclaration) cu.types().get(0);
    MethodDeclaration m = c.getMethods()[0];
    SwitchStatement s = (SwitchStatement) m.getBody().statements().get(0);
    Block block = (Block) s.statements().get(1);
    BreakStatement breakStatement = (BreakStatement) block.statements().get(0);
    assertThat(breakStatement.getLength())
      .isZero();

    test(source);
  }

  private CompilationUnit createAST(String source) {
    String version = "12";
    ASTParser astParser = ASTParser.newParser(AST.JLS12);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, version);
    options.put(JavaCore.COMPILER_SOURCE, version);
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

}
