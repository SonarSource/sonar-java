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
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class BindingsTest {

  private final JSema sema = new JSema();

  @TempDir
  Path baseDir;

  @Test
  void comparisons_and_logical_operators_are_boolean() {
    Type result = Bindings.binaryType("<=", type("int"), type("long"));

    assertThat(result.is("boolean")).isTrue();
    assertThat(Bindings.binaryType("instanceof", Type.UNKNOWN, type("int")).isUnknown()).isTrue();
  }

  @Test
  void a_string_operand_makes_a_concatenation() {
    assertThat(Bindings.binaryType("+", type("int"), sema.getClassType("java.lang.String")).is("java.lang.String")).isTrue();
    assertThat(Bindings.binaryType("+", Type.UNKNOWN, sema.getClassType("java.lang.String")).isUnknown()).isTrue();
  }

  @Test
  void arithmetic_operands_are_promoted() {
    assertThat(Bindings.binaryType("*", type("byte"), type("short")).is("int")).isTrue();
    assertThat(Bindings.binaryType("-", type("int"), type("double")).is("double")).isTrue();
    assertThat(Bindings.binaryType("+", type("float"), type("long")).is("float")).isTrue();
    assertThat(Bindings.binaryType("%", type("long"), type("int")).is("long")).isTrue();
    assertThat(Bindings.binaryType("<<", type("long"), type("int")).is("long")).isTrue();
    assertThat(Bindings.binaryType(">>>", type("byte"), type("long")).is("int")).isTrue();
  }

  @Test
  void bitwise_operators_on_booleans_stay_boolean() {
    assertThat(Bindings.binaryType("&", type("boolean"), type("boolean")).is("boolean")).isTrue();
    assertThat(Bindings.binaryType("+", type("boolean"), type("int")).is("boolean")).isTrue();
    assertThat(Bindings.binaryType("^", type("int"), type("boolean")).is("int")).isTrue();
  }

  @Test
  void a_non_primitive_operand_keeps_the_known_side() {
    Type list = sema.getClassType("java.util.List");

    assertThat(Bindings.binaryType("-", Type.UNKNOWN, type("int")).is("int")).isTrue();
    assertThat(Bindings.binaryType("-", list, type("int"))).isSameAs(list);
    assertThat(Bindings.binaryType("-", type("int"), list)).isSameAs(type("int"));
  }

  @Test
  void unknown_operators_get_the_default_kinds() {
    assertThat(Bindings.binaryKind("??")).isEqualTo(Tree.Kind.PLUS);
    assertThat(Bindings.binaryKind("^")).isEqualTo(Tree.Kind.XOR);
    assertThat(Bindings.assignmentKind("=")).isEqualTo(Tree.Kind.ASSIGNMENT);
    assertThat(Bindings.unaryKind("!", true)).isEqualTo(Tree.Kind.LOGICAL_COMPLEMENT);
    assertThat(Bindings.unaryKind("--", false)).isEqualTo(Tree.Kind.POSTFIX_DECREMENT);
  }

  @Test
  void an_unknown_method_names_its_own_type() {
    JMethodSymbol unknown = new JMethodSymbol(sema, null, "m");
    unknown.unknown(true);

    assertThat(Bindings.identifierType(unknown)).isSameAs(unknown.type());
    assertThat(Bindings.identifierType(Symbol.UNKNOWN_SYMBOL).isUnknown()).isTrue();
  }

  @Test
  void arrays_keep_the_arguments_of_their_elements() {
    assertThat(Bindings.keepsArguments(type("int[]"))).isFalse();
    assertThat(Bindings.keepsArguments(Type.UNKNOWN)).isFalse();
    assertThat(Bindings.isKnownOrRecovered(Type.UNKNOWN)).isFalse();
    assertThat(Bindings.isKnownOrRecovered(type("int"))).isTrue();
  }

  @Test
  void a_switch_expression_has_the_type_all_its_yields_share() throws Exception {
    List<SwitchExpressionTree> switches = switches("""
      class S {
        Object f(int i) {
          return switch (i) {
            case 1 -> "a";
            case 2 -> {
              Runnable r = () -> {};
              Object o = new Object() { int g() { return switch (i) { default -> { yield 1; } }; } };
              yield "b";
            }
            default -> {
              int k = switch (i) { default -> 3; };
              yield "c";
            }
          };
        }
        Object g(int i) {
          return switch (i) { case 1 -> "a"; default -> 2; };
        }
        Object h(int i) {
          return switch (i) { default -> throw new IllegalStateException(); };
        }
        Object u(int i) {
          return switch (i) { default -> unknown(); };
        }
      }
      """);

    assertThat(Bindings.switchResultType((SwitchExpressionTreeImpl) switches.get(0)).is("java.lang.String")).isTrue();
    assertThat(Bindings.switchResultType((SwitchExpressionTreeImpl) switches.get(3)).isUnknown()).isTrue();
    assertThat(Bindings.switchResultType((SwitchExpressionTreeImpl) switches.get(4)).isUnknown()).isTrue();
    assertThat(Bindings.switchResultType((SwitchExpressionTreeImpl) switches.get(5)).isUnknown()).isTrue();
  }

  @Test
  void type_names_are_identifiers_selections_or_parameterized_types_of_types() throws Exception {
    CompilationUnitTree unit = parse("""
      import java.util.List;
      class N {
        Object f(String s, List<String> l) {
          String.valueOf(1);
          s.trim();
          java.lang.String.valueOf(2);
          l.get(0).trim();
          java.util.function.Function<List<String>, Integer> size = List<String>::size;
          return size;
        }
      }
      """);
    List<ExpressionTree> receivers = new ArrayList<>();
    unit.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree tree) {
        if (tree.methodSelect() instanceof org.sonar.plugins.java.api.tree.MemberSelectExpressionTree select && tree.arguments().size() <= 1
          && !"get".equals(select.identifier().name())) {
          receivers.add(select.expression());
        }
        super.visitMethodInvocation(tree);
      }

      @Override
      public void visitMethodReference(MethodReferenceTree tree) {
        receivers.add((ExpressionTree) tree.expression());
        super.visitMethodReference(tree);
      }
    });

    assertThat(receivers).extracting(Bindings::namesType).containsExactly(true, false, true, false, true);
  }

  private Type type(String name) {
    return sema.getClassType(name);
  }

  private List<SwitchExpressionTree> switches(String source) throws Exception {
    List<SwitchExpressionTree> found = new ArrayList<>();
    parse(source).accept(new BaseTreeVisitor() {
      @Override
      public void visitSwitchExpression(SwitchExpressionTree tree) {
        found.add(tree);
        super.visitSwitchExpression(tree);
      }
    });
    return found;
  }

  private CompilationUnitTree parse(String source) throws Exception {
    return Replays.parseWithEcj(baseDir, "BindingsSample", source).get();
  }
}
