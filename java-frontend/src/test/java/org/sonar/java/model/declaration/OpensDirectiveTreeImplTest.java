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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.OpensDirectiveTree;
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class OpensDirectiveTreeImplTest {

  private final ActionParser<Tree> p = JavaParser.createParser();

  private CompilationUnitTree createTree(String... lines) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(Arrays.stream(lines).collect(Collectors.joining("\n")));
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return compilationUnitTree;
  }

  private OpensDirectiveTree moduleDirective(String exportsDirective) {
    CompilationUnitTree compilationUnitTree = createTree("module org.foo {\n  " + exportsDirective + "\n}");
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return (OpensDirectiveTree) compilationUnitTree.moduleDeclaration().moduleDirectives().get(0);
  }

  @Test
  public void simple_opens() {
    OpensDirectiveTree exports = moduleDirective("opens foo;");

    assertThat(exports.kind()).isEqualTo(Tree.Kind.OPENS_DIRECTIVE);
    assertThat(exports.directiveKeyword().text()).isEqualTo("opens");
    assertThat(((IdentifierTree) exports.packageName()).name()).isEqualTo("foo");
    assertThat(exports.toKeyword()).isNull();
    assertThat(exports.moduleNames()).isEmpty();
    assertThat(exports.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  public void opens_with_modules() {
    OpensDirectiveTree exports = moduleDirective("opens org.foo to com.module1, module2;");

    assertThat(exports.kind()).isEqualTo(Tree.Kind.OPENS_DIRECTIVE);
    assertThat(exports.directiveKeyword().text()).isEqualTo("opens");
    ExpressionTree packageName = exports.packageName();
    assertThat(packageName.is(Tree.Kind.MEMBER_SELECT)).isTrue();
    MemberSelectExpressionTree mset = (MemberSelectExpressionTree) packageName;
    assertThat(((IdentifierTree) mset.expression()).name()).isEqualTo("org");
    assertThat(mset.identifier().name()).isEqualTo("foo");
    assertThat(exports.toKeyword().text()).isEqualTo("to");

    ListTree<ModuleNameTree> moduleNames = exports.moduleNames();
    assertThat(moduleNames).hasSize(2);
    assertThat(moduleNames.get(0).stream().map(IdentifierTree::name)).containsExactly("com", "module1");
    assertThat(moduleNames.separators()).hasSize(1);
    assertThat(moduleNames.separators().iterator().next().text()).isEqualTo(",");
    assertThat(moduleNames.get(1).stream().map(IdentifierTree::name)).containsExactly("module2");

    assertThat(exports.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  public void test_BaseTreeVisitor() {
    CompilationUnitTree cut = createTree(
      "@org.foo.Bar",
      "open module com.greetings {",
      "  exports foo;",
      "  requires org.gul;",
      "  opens org.bar to com.module1, module2;",
      "}");
    OpensDirectiveVisitor moduleDeclarationVisitor = new OpensDirectiveVisitor();
    cut.accept(moduleDeclarationVisitor);
    assertThat(moduleDeclarationVisitor.visited).isTrue();
    assertThat(moduleDeclarationVisitor.directives).hasSize(1);
    assertThat(moduleDeclarationVisitor.identifiers).containsExactly("com", "greetings", "com", "module1", "module2");
  }

  private static class OpensDirectiveVisitor extends BaseTreeVisitor {

    boolean visited = false;
    List<OpensDirectiveTree> directives = new ArrayList<>();
    List<String> identifiers = new ArrayList<>();

    @Override
    public void visitOpensDirective(OpensDirectiveTree tree) {
      visited = true;
      directives.add(tree);
      super.visitOpensDirective(tree);
    }

    @Override
    public void visitRequiresDirectiveTree(RequiresDirectiveTree tree) {
      // skip requires directives
    }

    @Override
    public void visitExportsDirectiveTree(ExportsDirectiveTree tree) {
      // skip exports directives
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      // skip all member select
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      identifiers.add(tree.name());
    }
  }
}
