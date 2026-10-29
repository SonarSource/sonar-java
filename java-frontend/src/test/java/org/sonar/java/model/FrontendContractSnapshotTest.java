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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.java.test.TestResourceScope;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

@TestResourceScope.Scoped
class FrontendContractSnapshotTest {

  @Test
  void contract_records_are_repeatable_and_can_be_exported() throws Exception {
    Map<String, Object> first = records();
    Map<String, Object> second = records();
    String json = FrontendContractSnapshot.json(first);
    assertThat(json).isEqualTo(FrontendContractSnapshot.json(second));
    assertThat(json).contains("UNUSED_IMPORT", "UNKNOWN", "quickFixes", "replacement", "equalSymbols", "equalTypes");
    String output = System.getProperty("frontend.contract.output");
    if (output != null) {
      Files.writeString(Path.of(output), json + "\n");
    }
  }

  @Test
  void generic_invocations_preserve_declaration_and_instantiation_identity() {
    JavaTree.CompilationUnitTreeImpl tree = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(
      "class A { <T> T id(T t) { return t; } String a() { return id(\"\"); } Integer b() { return id(1); } }");
    try {
      ClassTree type = (ClassTree) tree.types().get(0);
      MethodInvocationTree first = invocation((MethodTree) type.members().get(1));
      MethodInvocationTree second = invocation((MethodTree) type.members().get(2));
      assertThat(first.methodSymbol()).isSameAs(first.methodSymbol()).isNotEqualTo(second.methodSymbol());
      assertThat(first.methodSymbol().declaration()).isSameAs(second.methodSymbol().declaration());
      assertThat(first.methodSymbol().parameterTypes().get(0).fullyQualifiedName()).isEqualTo("java.lang.String");
      assertThat(second.methodSymbol().parameterTypes().get(0).fullyQualifiedName()).isEqualTo("java.lang.Integer");
      assertThat(first.symbolType()).isSameAs(first.symbolType());
    } finally {
      tree.sema.getEnvironmentCleaner().run();
    }
  }

  private static MethodInvocationTree invocation(MethodTree method) {
    return (MethodInvocationTree) ((ReturnStatementTree) method.block().body().get(0)).expression();
  }

  private static Map<String, Object> records() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("schema", 1);
    result.put("empty", capture("File.java", ""));
    result.put("tokens", capture("A.java", "// header\r\nclass A { String s = \"😀\"; /* tail */ }\r\n// eof"));
    result.put("semantics", capture("A.java", "import java.util.List; class A<T extends Number> { T value; <U> U id(U u) { return u; } String m() { return id(\"\"); } }"));
    result.put("recovery", capture("A.java", "class A { Missing value; void m() { value.call(); } }"));
    result.put("patterns", capture("A.java", "record A(int value) { int m(Object o) { return switch(o) { case A(int x) when x > 0 -> x; default -> 0; }; } }"));
    result.put("module", capture("module-info.java", "module sample { requires java.base; exports sample; }"));
    result.put("malformed", capture("A.java", "class A { void m( }"));
    VisitorsBridgeForTests bridge = new VisitorsBridgeForTests.Builder(new ContractCheck())
      .enableSemanticWithProjectClasspath(JParserTestUtils.DEFAULT_CLASSPATH).build();
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/files/metrics/Classes.java"), bridge);
    result.put("issues", bridge.testContexts().stream().map(FrontendContractSnapshot::issues).toList());
    return result;
  }

  private static Map<String, Object> capture(String name, String source) {
    JavaTree.CompilationUnitTreeImpl tree;
    try {
      tree = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(name, source, JParserTestUtils.DEFAULT_CLASSPATH);
    } catch (RuntimeException failure) {
      return Map.of("failure", failure.getClass().getName(), "message", failure.getMessage());
    }
    try {
      return new FrontendContractSnapshot().capture(tree);
    } finally {
      tree.sema.getEnvironmentCleaner().run();
    }
  }

  private static class ContractCheck extends IssuableSubscriptionVisitor {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return List.of(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      Tree name = ((ClassTree) tree).simpleName();
      ((org.sonar.java.reporting.InternalJavaIssueBuilder) ((org.sonar.java.testing.JavaFileScannerContextForTests) context).newIssue()).forRule(this).onTree(name).withMessage("Class declaration")
        .withSecondaries(List.of(new org.sonar.plugins.java.api.JavaFileScannerContext.Location("Class body", tree))).withCost(2)
        .withQuickFix(() -> JavaQuickFix.newQuickFix("Rename class").addTextEdit(JavaTextEdit.replaceTree(name, "Replacement")).build()).report();
    }
  }
}
