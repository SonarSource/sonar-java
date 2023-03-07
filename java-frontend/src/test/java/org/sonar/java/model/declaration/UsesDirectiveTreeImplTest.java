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
package org.sonar.java.model.declaration;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UsesDirectiveTree;

import static org.assertj.core.api.Assertions.assertThat;

class UsesDirectiveTreeImplTest {

  private static UsesDirectiveTree moduleDirective(String exportsDirective) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parseModule("module org.foo {\n  " + exportsDirective + "\n}");
    return (UsesDirectiveTree) compilationUnitTree.moduleDeclaration().moduleDirectives().get(0);
  }

  @Test
  void simple_uses() {
    UsesDirectiveTree exports = moduleDirective("uses foo.MyInterface;");

    assertThat(exports.kind()).isEqualTo(Tree.Kind.USES_DIRECTIVE);
    assertThat(exports.directiveKeyword().text()).isEqualTo("uses");
    TypeTree typeName = exports.typeName();
    assertThat(typeName.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    assertThat(((MemberSelectExpressionTree) typeName).identifier().name()).isEqualTo("MyInterface");
    assertThat(exports.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  void test_BaseTreeVisitor() {
    CompilationUnitTree cut = JParserTestUtils.parseModule(
      "open module com.greetings {",
      "  uses org.bar.MyInterface;",
      "  uses org.gul.MyOtherInterface;",
      "}");
    UsesDirectiveVisitor moduleDeclarationVisitor = new UsesDirectiveVisitor();
    cut.accept(moduleDeclarationVisitor);
    assertThat(moduleDeclarationVisitor.visited).isTrue();
    assertThat(moduleDeclarationVisitor.directives).hasSize(2);
    assertThat(moduleDeclarationVisitor.identifiers).containsExactly("com", "greetings", "MyInterface", "MyOtherInterface");
  }

  private static class UsesDirectiveVisitor extends BaseTreeVisitor {

    boolean visited = false;
    List<UsesDirectiveTree> directives = new ArrayList<>();
    List<String> identifiers = new ArrayList<>();

    @Override
    public void visitUsesDirective(UsesDirectiveTree tree) {
      visited = true;
      directives.add(tree);
      super.visitUsesDirective(tree);
    }


    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      // skip all the rest
      scan(tree.identifier());
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiers.add(tree.name());
    }
  }

}
