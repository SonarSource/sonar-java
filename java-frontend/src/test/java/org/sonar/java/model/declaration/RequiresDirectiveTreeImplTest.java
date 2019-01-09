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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RequiresDirectiveTreeImplTest {

  private final ActionParser<Tree> p = JavaParser.createParser();

  private CompilationUnitTree createTree(String... lines) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(Arrays.stream(lines).collect(Collectors.joining("\n")));
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return compilationUnitTree;
  }

  private RequiresDirectiveTree requireDirective(String requiresDirective) {
    CompilationUnitTree compilationUnitTree = createTree("module org.foo {\n  " + requiresDirective + "\n}");
    return (RequiresDirectiveTree) compilationUnitTree.moduleDeclaration().moduleDirectives().get(0);
  }

  @Test
  public void transitive_as_name() {
    RequiresDirectiveTree requires = requireDirective("requires transitive;");

    assertThat(requires.kind()).isEqualTo(Tree.Kind.REQUIRES_DIRECTIVE);
    assertThat(requires.directiveKeyword().text()).isEqualTo("requires");
    assertThat(requires.modifiers()).isEmpty();
    assertThat(requires.moduleName().stream().map(IdentifierTree::name)).containsExactly("transitive");
    assertThat(requires.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  public void transitive_as_name_with_static() {
    RequiresDirectiveTree requires = requireDirective("requires static transitive;");

    assertThat(requires.kind()).isEqualTo(Tree.Kind.REQUIRES_DIRECTIVE);
    assertThat(requires.directiveKeyword().text()).isEqualTo("requires");
    assertThat(requires.modifiers()).hasSize(1);
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.STATIC)).isTrue();
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.TRANSITIVE)).isFalse();
    assertThat(requires.moduleName().stream().map(IdentifierTree::name)).containsExactly("transitive");
    assertThat(requires.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  public void requires() {
    RequiresDirectiveTree requires = requireDirective("requires static transitive foo.bar;");

    assertThat(requires.kind()).isEqualTo(Tree.Kind.REQUIRES_DIRECTIVE);
    assertThat(requires.directiveKeyword().text()).isEqualTo("requires");
    assertThat(requires.modifiers()).hasSize(2);
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.STATIC)).isTrue();
    assertThat(ModifiersUtils.hasModifier(requires.modifiers(), Modifier.TRANSITIVE)).isTrue();
    ModuleNameTree moduleName = requires.moduleName();
    assertThat(moduleName).hasSize(2);
    assertThat(moduleName.stream().map(IdentifierTree::name)).containsExactly("foo", "bar");
    assertThat(moduleName.separators()).hasSize(1);
    assertThat(requires.semicolonToken().text()).isEqualTo(";");
  }

  @Test
  public void test_BaseTreeVisitor() {
    CompilationUnitTree cut = createTree(
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
