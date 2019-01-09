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
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDeclarationTreeImplTest {

  private final ActionParser<Tree> p = JavaParser.createParser();

  private CompilationUnitTree createTree(String... lines) {
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) p.parse(Arrays.stream(lines).collect(Collectors.joining("\n")));
    SemanticModel.createFor(compilationUnitTree, new SquidClassLoader(Collections.emptyList()));
    return compilationUnitTree;
  }

  @Test
  public void no_module() {
    CompilationUnitTree cut = createTree("package org.foo;");
    assertThat(cut.moduleDeclaration()).isNull();
  }

  @Test
  public void with_module() {
    CompilationUnitTree cut = createTree("module org.foo { }");
    ModuleDeclarationTree moduleDeclaration = cut.moduleDeclaration();
    assertThat(moduleDeclaration).isNotNull();
    assertThat(moduleDeclaration.is(Tree.Kind.MODULE)).isTrue();

    assertThat(moduleDeclaration.openKeyword()).isNull();
    assertThat(moduleDeclaration.moduleKeyword().text()).isEqualTo("module");
    assertThat(moduleDeclaration.moduleDirectives()).isEmpty();

    ModuleNameTree moduleName = moduleDeclaration.moduleName();
    assertThat(moduleName).hasSize(2);
    assertThat(moduleName.stream().map(IdentifierTree::name)).containsExactly("org", "foo");

    assertThat(moduleDeclaration.openBraceToken().text()).isEqualTo("{");
    assertThat(moduleDeclaration.closeBraceToken().text()).isEqualTo("}");
  }

  @Test
  public void test_BaseTreeVisitor() {
    CompilationUnitTree cut = createTree(
      "import org.foo.Bar;",
      "",
      "@Bar",
      "open module com.greetings {",
      "}");
    ModuleDeclarationVisitor moduleDeclarationVisitor = new ModuleDeclarationVisitor();
    cut.accept(moduleDeclarationVisitor);
    assertThat(moduleDeclarationVisitor.visited).isTrue();
    assertThat(moduleDeclarationVisitor.annotations).hasSize(1);
    AnnotationTree annotation = moduleDeclarationVisitor.annotations.iterator().next();
    assertThat(((IdentifierTree) annotation.annotationType()).name()).isEqualTo("Bar");
  }

  private static class ModuleDeclarationVisitor extends BaseTreeVisitor {

    private boolean visited = false;
    private List<AnnotationTree> annotations = null;

    @Override
    public void visitModule(ModuleDeclarationTree module) {
      visited = true;
      annotations = module.annotations();
      super.visitModule(module);
    }
  }

}
