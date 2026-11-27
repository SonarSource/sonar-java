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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;

import static org.assertj.core.api.Assertions.assertThat;

class JSymbolTest {

  private static final String SOURCE = """
    record R(int a) {
    }
    class T {
      void m() {
        try {
          m();
        } catch (IllegalStateException | IllegalArgumentException e) {
          m();
        }
      }
    }
    """;

  @TempDir
  Path baseDir;

  private final JSema sema = new JSema();

  @Test
  void symbols_are_equal_by_id_else_by_name_and_owner() {
    JTypeSymbol owner = sema.typeSymbol("p.A");
    JMethodSymbol first = new JMethodSymbol(sema, null, "m");
    first.owner(owner);
    JMethodSymbol second = new JMethodSymbol(sema, null, "m");
    second.owner(owner);
    JMethodSymbol elsewhere = new JMethodSymbol(sema, null, "m");
    JMethodSymbol identified = new JMethodSymbol(sema, "p.A#m()", "m");
    identified.owner(owner);

    assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo(elsewhere).isNotEqualTo("m");
    assertThat(identified).isEqualTo(new JMethodSymbol(sema, "p.A#m()", "other")).isNotEqualTo(new JVariableSymbol(sema, "p.A#m()", "m", "field"))
      .isEqualTo(first)
      .hasToString("p.A#m()");
    assertThat(first).hasToString("m");
  }

  @Test
  void members_of_interfaces_get_implicit_modifiers() {
    JTypeSymbol owner = type("p.I", sema.packageSymbol("p"));
    owner.interfaceSymbol(true);
    JVariableSymbol constant = new JVariableSymbol(sema, "p.I#C", "C", "field");
    constant.owner(owner);
    JMethodSymbol method = method("p.I#m()", owner);
    JMethodSymbol defaultMethod = method("p.I#d()", owner, "default");
    JMethodSymbol staticMethod = method("p.I#s()", owner, "static");
    JMethodSymbol privateMethod = method("p.I#p()", owner, "private");
    JTypeSymbol parameter = new JTypeSymbol(sema, "p.I<T>", "T", true);
    parameter.owner(owner);

    assertThat(constant.isStatic()).isTrue();
    assertThat(constant.isFinal()).isTrue();
    assertThat(constant.isPublic()).isTrue();
    assertThat(owner.isAbstract()).isFalse();
    assertThat(method.isAbstract()).isTrue();
    assertThat(defaultMethod.isAbstract()).isFalse();
    assertThat(staticMethod.isAbstract()).isFalse();
    assertThat(privateMethod.isAbstract()).isFalse();
    assertThat(privateMethod.isPublic()).isFalse();
    assertThat(parameter.isStatic()).isFalse();
    assertThat(parameter.isPublic()).isFalse();
  }

  @Test
  void nested_interfaces_enums_and_records_are_static_but_not_top_level_or_inner_classes() {
    JTypeSymbol outer = type("p.O", sema.packageSymbol("p"));
    JTypeSymbol nestedInterface = type("p.O$I", outer);
    nestedInterface.interfaceSymbol(true);
    JTypeSymbol nestedEnum = type("p.O$E", outer);
    nestedEnum.enumSymbol(true);
    JTypeSymbol nestedRecord = type("p.O$R", outer);
    sema.declare("p.O$R", unit().types().get(0));
    JTypeSymbol inner = type("p.O$C", outer);
    sema.declare("p.O$C", unit().types().get(1));
    JTypeSymbol topLevelEnum = type("p.E", sema.packageSymbol("p"));
    topLevelEnum.enumSymbol(true);

    assertThat(nestedInterface.isStatic()).isTrue();
    assertThat(nestedEnum.isStatic()).isTrue();
    assertThat(nestedRecord.isStatic()).isTrue();
    assertThat(inner.isStatic()).isFalse();
    assertThat(type("p.O$D", outer).isStatic()).isFalse();
    assertThat(topLevelEnum.isStatic()).isFalse();
  }

  @Test
  void enum_constants_are_public_static_final_and_enum_constructors_private() {
    JTypeSymbol owner = type("p.E", sema.packageSymbol("p"));
    owner.enumSymbol(true);
    JVariableSymbol constant = new JVariableSymbol(sema, "p.E#A", "A", "enum_member");
    constant.owner(owner);
    JMethodSymbol constructor = method("p.E#<init>()", owner);
    JMethodSymbol plain = new JMethodSymbol(sema, "p.C#<init>()", "<init>");

    assertThat(constant.isStatic()).isTrue();
    assertThat(constant.isFinal()).isTrue();
    assertThat(constant.isPublic()).isTrue();
    assertThat(new JMethodSymbol(sema, "p.E#<init>()", "<init>").isPrivate()).isFalse();
    assertThat(constructor.isPrivate()).isTrue();
    assertThat(method("p.E#values()", owner).isPrivate()).isFalse();
    assertThat(plain.isPrivate()).isFalse();
  }

  @Test
  void accessors_of_record_components_are_public_and_receivers_have_no_visibility() {
    JTypeSymbol recordType = type("p.R", sema.packageSymbol("p"));
    sema.declare("p.R", unit().types().get(0));
    JTypeSymbol plain = type("p.C", sema.packageSymbol("p"));
    sema.declare("p.C", unit().types().get(1));
    JVariableSymbol receiver = new JVariableSymbol(sema, "p.C#this", "this", "field");
    receiver.receiver(true);

    assertThat(method("p.R#a()", recordType).isPublic()).isTrue();
    assertThat(method("p.R#b()", recordType).isPublic()).isFalse();
    assertThat(method("p.C#a()", plain).isPublic()).isFalse();
    assertThat(method("p.D#a()", type("p.D", sema.packageSymbol("p"))).isPublic()).isFalse();
    JMethodSymbol declared = method("p.R#x()", recordType);
    sema.declare("p.R#x()", ((ClassTree) unit().types().get(1)).members().get(0));
    assertThat(declared.isPublic()).isFalse();
    assertThat(receiver.isPackageVisibility()).isFalse();
    assertThat(new JVariableSymbol(sema, "p.C#f", "f", "field").isPackageVisibility()).isTrue();
  }

  @Test
  void a_multi_catch_parameter_is_final() {
    List<CatchTree> catches = new ArrayList<>();
    unit().accept(new BaseTreeVisitor() {
      @Override
      public void visitCatch(CatchTree tree) {
        catches.add(tree);
      }
    });
    JVariableSymbol parameter = new JVariableSymbol(sema, "e", "e", "local");
    sema.declare("e", catches.get(0).parameter());
    JVariableSymbol undeclared = new JVariableSymbol(sema, "f", "f", "local");

    assertThat(parameter.isFinal()).isTrue();
    assertThat(undeclared.isFinal()).isFalse();
  }

  @Test
  void enclosing_class_walks_up_to_the_first_type() {
    JTypeSymbol top = type("p.A", sema.packageSymbol("p"));
    JMethodSymbol method = method("p.A#m()", top);
    JTypeSymbol local = type("p.A#m()$L", method);
    JTypeSymbol nested = type("p.A$N", top);
    JTypeSymbol methodParameter = new JTypeSymbol(sema, "p.A#m()<T>", "T", true);
    methodParameter.owner(method);
    JTypeSymbol classParameter = new JTypeSymbol(sema, "p.A<T>", "T", true);
    classParameter.owner(top);
    JVariableSymbol orphan = new JVariableSymbol(sema, "x", "x", "local");
    JVariableSymbol inPackage = new JVariableSymbol(sema, "y", "y", "local");
    inPackage.owner(sema.packageSymbol("p"));
    JVariableSymbol inMethod = new JVariableSymbol(sema, "z", "z", "local");
    inMethod.owner(method);

    assertThat(top.enclosingClass()).isSameAs(top);
    assertThat(local.enclosingClass()).isSameAs(top);
    assertThat(nested.enclosingClass()).isSameAs(top);
    assertThat(methodParameter.enclosingClass()).isNull();
    assertThat(classParameter.enclosingClass()).isSameAs(top);
    assertThat(orphan.enclosingClass()).isNull();
    assertThat(inPackage.enclosingClass()).isNull();
    assertThat(inMethod.enclosingClass()).isSameAs(top);
  }

  @Test
  void usages_and_declaration_need_an_id() {
    JVariableSymbol anonymous = new JVariableSymbol(sema, null, "x", "local");
    JVariableSymbol used = new JVariableSymbol(sema, "p.A#f", "f", "field");
    IdentifierTree use = identifier();
    sema.use("p.A#f", use);

    assertThat(anonymous.usages()).isEmpty();
    assertThat(anonymous.declaration()).isNull();
    assertThat(new JVariableSymbol(sema, "p.A#g", "g", "field").usages()).isEmpty();
    assertThat(used.usages()).containsExactly(use);
  }

  private JTypeSymbol type(String id, Symbol owner) {
    JTypeSymbol type = new JTypeSymbol(sema, id, id.substring(id.lastIndexOf('.') + 1), false);
    type.owner(owner);
    return type;
  }

  private JMethodSymbol method(String id, Symbol owner, String... modifiers) {
    JMethodSymbol method = new JMethodSymbol(sema, id, SemaFacts.memberName(id));
    method.owner(owner);
    method.addModifiers(List.of(modifiers));
    return method;
  }

  private IdentifierTree identifier() {
    List<IdentifierTree> found = new ArrayList<>();
    unit().accept(new BaseTreeVisitor() {
      @Override
      public void visitIdentifier(IdentifierTree tree) {
        found.add(tree);
      }
    });
    return found.get(0);
  }

  private CompilationUnitTree unit() {
    try {
      return (CompilationUnitTree) Replays.parseWithEcj(baseDir, "R", SOURCE).get();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
