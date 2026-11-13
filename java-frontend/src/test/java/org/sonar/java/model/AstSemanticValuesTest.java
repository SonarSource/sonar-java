/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AstSemanticValuesTest {

  private final InternalSyntaxToken token = new InternalSyntaxToken(1, 0, "name", List.of(), false);

  @Test
  void stores_api_types_without_a_semantic_context_or_parents() {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(token);
    assertThat(identifier.symbolType()).isSameAs(Type.UNKNOWN);
    Type type = mock(Type.class);
    identifier.setSymbolType(type);
    assertThat(identifier.symbolType()).isSameAs(type);

    VariableTreeImpl variable = new VariableTreeImpl(identifier);
    TypePatternTreeImpl pattern = new TypePatternTreeImpl(variable, type);
    assertThat(pattern.symbolType()).isSameAs(type);
    assertThat(new GuardedPatternTreeImpl(pattern, token, identifier, type).symbolType()).isSameAs(type);
    assertThat(new DefaultPatternTreeImpl(token, type).symbolType()).isSameAs(type);
  }

  @Test
  void stores_api_declaration_symbols_without_a_semantic_context_or_parents() {
    Symbol.TypeSymbol typeSymbol = mock(Symbol.TypeSymbol.class);
    Symbol.MethodSymbol methodSymbol = mock(Symbol.MethodSymbol.class);
    Symbol.VariableSymbol variableSymbol = mock(Symbol.VariableSymbol.class);
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(token);

    ClassTreeImpl declaration = new ClassTreeImpl(Tree.Kind.CLASS, token, List.of(), token);
    assertThat(declaration.symbol()).isSameAs(Symbol.TypeSymbol.UNKNOWN_TYPE);
    declaration.setSymbol(typeSymbol);
    assertThat(declaration.symbol()).isSameAs(typeSymbol);

    MethodTreeImpl method = new MethodTreeImpl(new FormalParametersListTreeImpl(token, token), null, null);
    assertThat(method.symbol()).isSameAs(Symbol.MethodSymbol.UNKNOWN_METHOD);
    method.setSymbol(methodSymbol);
    assertThat(method.symbol()).isSameAs(methodSymbol);

    VariableTreeImpl variable = new VariableTreeImpl(identifier);
    assertThat(variable.symbol()).isSameAs(Symbol.UNKNOWN_SYMBOL);
    variable.setSymbol(variableSymbol);
    assertThat(variable.symbol()).isSameAs(variableSymbol);

    TypeParameterTreeImpl parameter = new TypeParameterTreeImpl(identifier);
    assertThat(parameter.symbol()).isSameAs(Symbol.UNKNOWN_SYMBOL);
    parameter.setSymbol(typeSymbol);
    assertThat(parameter.symbol()).isSameAs(typeSymbol);
  }

  @Test
  void stores_api_expression_symbols_without_a_semantic_context_or_parents() {
    Symbol.MethodSymbol methodSymbol = mock(Symbol.MethodSymbol.class);
    Symbol.VariableSymbol variableSymbol = mock(Symbol.VariableSymbol.class);
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(token);
    assertThat(identifier.symbol()).isSameAs(Symbol.UNKNOWN_SYMBOL);
    identifier.setSymbol(variableSymbol);
    assertThat(identifier.symbol()).isSameAs(variableSymbol);

    MethodInvocationTreeImpl invocation = new MethodInvocationTreeImpl(identifier, null, ArgumentListTreeImpl.emptyList());
    assertThat(invocation.methodSymbol()).isSameAs(Symbol.MethodSymbol.UNKNOWN_METHOD);
    invocation.setMethodSymbol(methodSymbol);
    assertThat(invocation.methodSymbol()).isSameAs(methodSymbol);

    LambdaExpressionTreeImpl lambda = new LambdaExpressionTreeImpl(null, List.of(), null, token, identifier);
    assertThat(lambda.symbol()).isSameAs(Symbol.MethodSymbol.UNKNOWN_METHOD);
    lambda.setSymbol(methodSymbol);
    assertThat(lambda.symbol()).isSameAs(methodSymbol);
  }

  @Test
  void preserves_nullable_import_symbols_and_unnamed_identifiers() {
    JavaTree.ImportTreeImpl importTree = new JavaTree.ImportTreeImpl(token, null, null, new IdentifierTreeImpl(token), token);
    assertThat(importTree.symbol()).isNull();
    Symbol symbol = mock(Symbol.class);
    importTree.setSymbol(symbol);
    assertThat(importTree.symbol()).isSameAs(symbol);
    importTree.setSymbol(null);
    assertThat(importTree.symbol()).isNull();

    IdentifierTreeImpl unnamed = new IdentifierTreeImpl(token, true);
    unnamed.setSymbol(symbol);
    assertThat(unnamed.symbol()).isSameAs(Symbol.UNKNOWN_SYMBOL);
  }

  @Test
  void preserves_keyword_symbols_and_super_type_without_ecj() {
    Symbol symbol = mock(Symbol.class);
    Type type = mock(Type.class);
    when(symbol.type()).thenReturn(type);
    KeywordThis keywordThis = new KeywordThis(token, symbol);
    keywordThis.setSymbolType(type);
    assertThat(keywordThis.symbol()).isSameAs(symbol);
    assertThat(keywordThis.symbolType()).isSameAs(type);
    KeywordSuper keywordSuper = new KeywordSuper(token, symbol);
    assertThat(keywordSuper.symbol()).isSameAs(symbol);
    assertThat(keywordSuper.symbolType()).isSameAs(type);
  }

  @Test
  void ast_bytecode_does_not_depend_on_ecj_or_its_semantic_context() throws IOException {
    List<Class<?>> trees = ClassPath.from(JavaTree.class.getClassLoader()).getAllClasses().stream()
      .filter(info -> info.getName().startsWith("org.sonar.java."))
      .map(ClassPath.ClassInfo::load)
      .filter(JavaTree.class::isAssignableFrom)
      .filter(type -> type.getProtectionDomain().getCodeSource().getLocation().equals(JavaTree.class.getProtectionDomain().getCodeSource().getLocation()))
      .toList();
    assertThat(trees).contains(JavaTree.class, ClassTreeImpl.class, IdentifierTreeImpl.class, TypePatternTreeImpl.class);
    for (Class<?> tree : trees) {
      try (var bytecode = tree.getResourceAsStream("/" + tree.getName().replace('.', '/') + ".class")) {
        assertThat(new String(bytecode.readAllBytes(), ISO_8859_1))
          .as(tree.getName())
          .doesNotContain("org/eclipse/jdt/", "org.eclipse.jdt.", "org/sonar/java/jdt/");
      }
    }
  }
}
