/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model.declaration;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class RequiresDirectiveTreeImplTest {

  private static RequiresDirectiveTree requireDirective(String requiresDirective) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parseModule("module org.foo {\n  " + requiresDirective + "\n}");
    return (RequiresDirectiveTree) compilationUnitTree.moduleDeclaration().moduleDirectives().get(0);
  }

  @Test
  void transitive_as_name() {
    RequiresDirectiveTree requires = requireDirective("requires transitive;");

    assertThat(requires.kind()).isEqualTo(Tree.Kind.REQUIRES_DIRECTIVE);
    assertThat(requires.directiveKeyword().text()).isEqualTo("requires");
    // FIXME ECJ bug - transitive also counted as modifier
    assertThat(requires.modifiers()).hasSize(1);
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.TRANSITIVE)).isTrue();
    assertThat(requires.moduleName().stream().map(IdentifierTree::name)).containsExactly("transitive");
    assertThat(requires.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  void transitive_as_name_with_static() {
    RequiresDirectiveTree requires = requireDirective("requires static transitive;");

    assertThat(requires.kind()).isEqualTo(Tree.Kind.REQUIRES_DIRECTIVE);
    assertThat(requires.directiveKeyword().text()).isEqualTo("requires");
    // FIXME ECJ bug
    assertThat(requires.modifiers()).hasSize(2);
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.STATIC)).isTrue();
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.TRANSITIVE)).isTrue();
    assertThat(requires.moduleName().stream().map(IdentifierTree::name)).containsExactly("transitive");
    assertThat(requires.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  void requires() {
    RequiresDirectiveTree requires = requireDirective("requires static transitive foo.bar;");

    assertThat(requires.kind()).isEqualTo(Tree.Kind.REQUIRES_DIRECTIVE);
    assertThat(requires.directiveKeyword().text()).isEqualTo("requires");
    assertThat(requires.modifiers()).hasSize(2);
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.STATIC)).isTrue();
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.TRANSITIVE)).isTrue();
    ModuleNameTree moduleName = requires.moduleName();
    assertThat(moduleName).hasSize(2);
    assertThat(moduleName.stream().map(IdentifierTree::name)).containsExactly("foo", "bar");
    // FIXME missing separators
    assertThat(moduleName.separators()).isEmpty();
    assertThat(requires.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  void test_BaseTreeVisitor() {
    CompilationUnitTree cut = JParserTestUtils.parseModule(
      "@org.foo.Bar",
      "open module com.greetings {",
      "  requires static transitive bos.gul;",
      "  requires gla.qix;",
      "}");
    RequiresDirectiveVisitor moduleDeclarationVisitor = new RequiresDirectiveVisitor();
    cut.accept(moduleDeclarationVisitor);
    assertThat(moduleDeclarationVisitor.visited).isTrue();
    assertThat(moduleDeclarationVisitor.directives).hasSize(2);
    assertThat(moduleDeclarationVisitor.identifiers).containsExactly("com", "greetings", "bos", "gul", "gla", "qix");
  }

  private static class RequiresDirectiveVisitor extends BaseTreeVisitor {

    boolean visited = false;
    List<RequiresDirectiveTree> directives = new ArrayList<>();
    List<String> identifiers = new ArrayList<>();

    @Override
    public void visitRequiresDirectiveTree(RequiresDirectiveTree tree) {
      visited = true;
      directives.add(tree);
      super.visitRequiresDirectiveTree(tree);
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      // skip member selects
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiers.add(tree.name());
    }
  }

}
