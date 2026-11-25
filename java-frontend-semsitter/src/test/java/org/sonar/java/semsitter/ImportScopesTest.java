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
package org.sonar.java.semsitter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import static org.assertj.core.api.Assertions.assertThat;

class ImportScopesTest {

  private static final String SOURCE = """
    package p;

    import p.A;
    import p.*;
    import static p.A.x;
    import q.B;

    @Ann(A.K)
    class A extends Missing {
      static final int K = 1;
      static int x;
      void m() {
        int local = 0;
        local++;
        x++;
        foo();
        new Other();
        new Other.Inner.Deep();
        unresolved++;
      }
      void foo() {
      }
    }

    class Other {
      static int y;
      static class Inner {
        static class Deep {
        }
      }
      void n() {
        y++;
        A.x++;
      }
    }

    class Sub extends Other {
      void k() {
        y++;
      }
    }

    @interface Ann {
      int value();
    }
    """;

  @TempDir
  Path baseDir;

  private CompilationUnitTree unit;

  @BeforeEach
  void parse() throws Exception {
    unit = (CompilationUnitTree) Replays.parseWithEcj(baseDir, "p/A", SOURCE).get();
  }

  @Test
  void own_package_imports_are_the_single_type_and_on_demand_ones_of_the_package() {
    List<ImportTree> imports = unit.imports().stream().map(ImportTree.class::cast).toList();

    assertThat(ImportScopes.isOwnPackage(imports.get(0), "p.A", "p")).isTrue();
    assertThat(ImportScopes.isOwnPackage(imports.get(1), "p.*", "p")).isTrue();
    assertThat(ImportScopes.isOwnPackage(imports.get(2), "p.A.x", "p")).isFalse();
    assertThat(ImportScopes.isOwnPackage(imports.get(3), "q.B", "p")).isFalse();
    assertThat(ImportScopes.isOwnPackage(imports.get(0), "p.A", "")).isFalse();
    assertThat(ImportScopes.dotted(imports.get(1).qualifiedIdentifier())).isEqualTo("p.*");
    assertThat(ImportScopes.dotted(unit.packageDeclaration().packageName())).isEqualTo("p");
  }

  @Test
  void members_of_enclosing_and_inherited_types_are_in_scope() {
    assertThat(inScope("local", 0)).isTrue();
    assertThat(inScope("x", 1)).isTrue();
    assertThat(inScope("y", 0)).isTrue();
    assertThat(inScope("y", 1)).isTrue();
    assertThat(inScope("unresolved", 0)).isTrue();
  }

  @Test
  void members_of_other_types_annotations_and_methods_of_incomplete_hierarchies_are_not_in_scope() {
    assertThat(inScope("x", 3)).isFalse();
    assertThat(inScope("K", 0)).isFalse();
    assertThat(inScope("foo", 0)).isFalse();
  }

  @Test
  void a_type_is_denoted_by_its_symbol_or_by_the_class_its_constructor_creates() {
    Symbol.TypeSymbol other = classSymbol("Other");
    List<NewClassTree> creations = creations();
    Symbol constructor = creations.get(0).methodSymbol();
    IdentifierTree deep = identifiers("Deep").get(0);

    assertThat(ImportScopes.denotesType(other, "p.Other", identifiers("Other").get(0))).isTrue();
    assertThat(ImportScopes.denotesType(method("foo"), "p.A.foo", identifiers("foo").get(0))).isFalse();
    assertThat(ImportScopes.denotesType(constructor, "p.Other", identifiers("Other").get(0))).isTrue();
    assertThat(ImportScopes.denotesType(constructor, "p.Other.Inner.Deep", deep)).isTrue();
    assertThat(ImportScopes.denotesType(constructor, "q.B", deep)).isFalse();
    assertThat(ImportScopes.denotesType(constructor, "q.B", identifiers("unresolved").get(0))).isFalse();
  }

  @Test
  void on_demand_imports_bring_the_types_of_a_package_or_the_members_of_a_type() {
    Symbol.TypeSymbol inner = classSymbol("Inner");
    Symbol.TypeSymbol deep = classSymbol("Deep");
    Symbol constructor = creations().get(0).methodSymbol();

    assertThat(ImportScopes.declaredIn(identifiers("unresolved").get(0).symbol(), "p", false)).isFalse();
    assertThat(ImportScopes.declaredIn(inner, "p.Other", true)).isTrue();
    assertThat(ImportScopes.declaredIn(deep, "p.Other", true)).isFalse();
    assertThat(ImportScopes.declaredIn(identifiers("y").get(0).symbol(), "p.Other", true)).isTrue();
    assertThat(ImportScopes.declaredIn(constructor, "p", false)).isTrue();
    assertThat(ImportScopes.declaredIn(inner, "q", false)).isFalse();
    assertThat(ImportScopes.declaredIn(inner, "p.Other", false)).isTrue();
    assertThat(ImportScopes.declaredIn(inner, "p", false)).isFalse();
    assertThat(ImportScopes.declaredIn(method("foo"), "p", false)).isFalse();
  }

  private boolean inScope(String name, int index) {
    IdentifierTree identifier = identifiers(name).get(index);
    return ImportScopes.isInScope(identifier.symbol(), identifier);
  }

  private List<IdentifierTree> identifiers(String name) {
    List<IdentifierTree> found = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitIdentifier(IdentifierTree tree) {
        if (name.equals(tree.name())) {
          found.add(tree);
        }
      }
    });
    return found;
  }

  private List<NewClassTree> creations() {
    List<NewClassTree> found = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitNewClass(NewClassTree tree) {
        found.add(tree);
      }
    });
    return found;
  }

  private Symbol.TypeSymbol classSymbol(String name) {
    List<Symbol.TypeSymbol> found = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitClass(ClassTree tree) {
        if (tree.simpleName() != null && name.equals(tree.simpleName().name())) {
          found.add(tree.symbol());
        }
        super.visitClass(tree);
      }
    });
    return found.get(0);
  }

  private Symbol method(String name) {
    List<Symbol> found = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethod(MethodTree tree) {
        if (name.equals(tree.simpleName().name())) {
          found.add(tree.symbol());
        }
      }
    });
    return found.get(0);
  }
}
