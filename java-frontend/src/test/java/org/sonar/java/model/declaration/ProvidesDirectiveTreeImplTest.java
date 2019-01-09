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
package org.sonar.java.model.declaration;

import com.sonar.sslr.api.typed.ActionParser;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExportsDirectiveTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ProvidesDirectiveTreeImplTest {

  private final ActionParser<Tree> p = JavaParser.createParser();

  private CompilationUnitTree createTree(String... lines) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(Arrays.stream(lines).collect(Collectors.joining("\n")));
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return compilationUnitTree;
  }

  private ProvidesDirectiveTree providesDirective(String exportsDirective) {
    CompilationUnitTree compilationUnitTree = createTree("module org.foo {\n  " + exportsDirective + "\n}");
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return (ProvidesDirectiveTree) compilationUnitTree.moduleDeclaration().moduleDirectives().get(0);
  }

  @Test
  public void provides_with_modules() {
    ProvidesDirectiveTree exports = providesDirective("provides org.MyInterface with com.MyInterface, MyInterface2;");

    assertThat(exports.kind()).isEqualTo(Tree.Kind.PROVIDES_DIRECTIVE);
    assertThat(exports.directiveKeyword().text()).isEqualTo("provides");
    TypeTree typeName = exports.typeName();
    assertThat(typeName.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    MemberSelectExpressionTree mset = (MemberSelectExpressionTree) typeName;
    assertThat(((IdentifierTree) mset.expression()).name()).isEqualTo("org");
    assertThat(mset.identifier().name()).isEqualTo("MyInterface");
    assertThat(exports.withKeyword().text()).isEqualTo("with");

    ListTree<TypeTree> typeNames = exports.typeNames();
    assertThat(typeNames).hasSize(2);
    assertThat(((MemberSelectExpressionTree) typeNames.get(0)).identifier().name()).isEqualTo("MyInterface");
    assertThat(typeNames.separators()).hasSize(1);
    assertThat(typeNames.separators().iterator().next().text()).isEqualTo(",");
    assertThat(((IdentifierTree) typeNames.get(1)).name()).isEqualTo("MyInterface2");

    assertThat(exports.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  public void test_BaseTreeVisitor() {
    CompilationUnitTree cut = createTree(
      "import org.foo.Bar;",
      "",
      "@Bar",
      "open module com.greetings {",
      "  exports org.bar to com.module1, module2;",
      "  provides org.MyInterface with com.MyInterface, MyInterface2;",
      "}");
    ProvidesDirectiveVisitor moduleDeclarationVisitor = new ProvidesDirectiveVisitor();
    cut.accept(moduleDeclarationVisitor);
    assertThat(moduleDeclarationVisitor.visited).isTrue();
    assertThat(moduleDeclarationVisitor.directives).hasSize(1);
    assertThat(moduleDeclarationVisitor.identifiers).containsExactly("Bar", "Bar", "com", "greetings", "MyInterface", "MyInterface", "MyInterface2");
  }

  private static class ProvidesDirectiveVisitor extends BaseTreeVisitor {

    boolean visited = false;
    List<ProvidesDirectiveTree> directives = new ArrayList<>();
    List<String> identifiers = new ArrayList<>();

    @Override
    public void visitProvidesDirective(ProvidesDirectiveTree tree) {
      visited = true;
      directives.add(tree);
      super.visitProvidesDirective(tree);
    }

    @Override
    public void visitExportsDirectiveTree(ExportsDirectiveTree tree) {
      // skip exports directives
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
