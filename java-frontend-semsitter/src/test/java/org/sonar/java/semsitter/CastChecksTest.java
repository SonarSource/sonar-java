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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import static org.assertj.core.api.Assertions.assertThat;

class CastChecksTest {

  private static final String TYPES = """
    {
      "java.lang.Object": {"origin": "classpath", "kind": "class", "modifiers": ["public"], "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "java.lang.Integer": {"origin": "classpath", "kind": "class", "modifiers": ["public", "final"], "super": "java.lang.Number", "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "java.lang.Number": {"origin": "classpath", "kind": "class", "modifiers": ["public", "abstract"], "super": "java.lang.Object", "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []},
      "java.lang.String": {"origin": "classpath", "kind": "class", "modifiers": ["public", "final"], "super": "java.lang.Object", "interfaces": [], "typeParameters": 0, "typeVars": [], "members": []}
    }
    """;

  private static final String SOURCE = """
    import java.util.List;
    import java.util.function.Supplier;

    class C {
      Object field;

      void casts(Object o, int i, long l, byte b) {
        var inferred = (String) o;
        String declared = (String) o;
        field = (String) o;
        i += (int) l;
        int element = ((int[]) o)[0];
        int length = ((Object[]) o).length;
        Supplier<Object> expression = () -> (String) o;
        Supplier<Object> block = () -> { return (String) o; };
        take((String) o);
        take(() -> (String) o);
        take(() -> { return ((String) o); });
        long shifted = (long) i << 2;
        int promoted = (int) b << 2;
        long sum = (long) i + l;
        double mixed = (double) i * 2;
      }

      Object returned(Object o) {
        return (String) o;
      }

      void take(Object o) {
      }
    }
    """;

  @TempDir
  Path baseDir;

  private final JSema sema = new JSema();
  private final List<TypeCastTree> casts = new ArrayList<>();
  private final List<BinaryExpressionTree> binaries = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    sema.loadPackages(name -> null);
    sema.loadTypes(names -> new JsonObject());
    sema.ingestTypes(JsonParser.parseString(TYPES).getAsJsonObject());
    Replays.parseWithEcj(baseDir, "CastChecksSample", SOURCE).get().accept(new BaseTreeVisitor() {
      @Override
      public void visitTypeCast(TypeCastTree tree) {
        casts.add(tree);
        super.visitTypeCast(tree);
      }

      @Override
      public void visitBinaryExpression(BinaryExpressionTree tree) {
        binaries.add(tree);
        super.visitBinaryExpression(tree);
      }
    });
  }

  @Test
  void primitive_assignability_follows_widening() {
    assertThat(assignable("int", "long")).isTrue();
    assertThat(assignable("int", "int")).isTrue();
    assertThat(assignable("char", "int")).isTrue();
    assertThat(assignable("char", "short")).isFalse();
    assertThat(assignable("byte", "char")).isFalse();
    assertThat(assignable("long", "int")).isFalse();
    assertThat(assignable("boolean", "int")).isFalse();
    assertThat(assignable("int", "boolean")).isFalse();
  }

  @Test
  void only_a_narrowing_primitive_cast_changes_the_value() {
    assertThat(castKeepsValue("int", "byte")).isFalse();
    assertThat(castKeepsValue("long", "int")).isFalse();
    assertThat(castKeepsValue("int", "long")).isTrue();
    assertThat(castKeepsValue("int", "int")).isTrue();
    assertThat(castKeepsValue("java.lang.Object", "java.lang.String")).isTrue();
  }

  @Test
  void boxing_and_unboxing_are_assignments() {
    assertThat(assignable("int", "java.lang.Integer")).isTrue();
    assertThat(assignable("int", "java.lang.Number")).isTrue();
    assertThat(assignable("int", "java.lang.String")).isFalse();
    assertThat(assignable("java.lang.Integer", "long")).isTrue();
    assertThat(assignable("java.lang.String", "int")).isFalse();
    assertThat(assignable("java.lang.String", "java.lang.Object")).isTrue();
    assertThat(assignable("java.lang.Object", "java.lang.String")).isFalse();
  }

  @Test
  void display_names_spell_arrays_and_type_arguments() {
    Type list = JType.named(sema, "java.util.List", List.of((JType) sema.getClassType("java.lang.String")));
    Type array = sema.getClassType("int[][]");

    assertThat(CastChecks.displayName(list)).isEqualTo("List<String>");
    assertThat(CastChecks.displayName(array)).isEqualTo("int[][]");
    assertThat(CastChecks.displayName(JType.nullType(sema))).isEqualTo("null");
    assertThat(CastChecks.elementType(array).fullyQualifiedName()).isEqualTo("int");
  }

  @Test
  void display_names_spell_member_types_with_their_enclosing_types() {
    Type entry = JType.named(sema, "java.util.Map$Entry", List.of((JType) sema.getClassType("java.lang.String"), (JType) sema.getClassType("java.lang.Integer")));

    assertThat(CastChecks.displayName(sema.getClassType("java.util.Map$Entry"))).isEqualTo("Map.Entry");
    assertThat(CastChecks.displayName(entry)).isEqualTo("Map.Entry<String,Integer>");
    assertThat(CastChecks.displayName(JType.named(sema, "a.Outer$1Local", List.of()))).isEqualTo("Local");
    assertThat(CastChecks.displayName(JType.named(sema, "a.Outer$1Local$Inner", List.of()))).isEqualTo("Local.Inner");
  }

  @Test
  void the_assigned_type_is_the_declared_type_of_the_target() {
    assertThat(assignedTo(0)).isEmpty();
    assertThat(assignedTo(1)).hasValueSatisfying(type -> assertThat(type.is("java.lang.String")).isTrue());
    assertThat(assignedTo(2)).hasValueSatisfying(type -> assertThat(type.is("java.lang.Object")).isTrue());
    assertThat(assignedTo(3)).isEmpty();
    assertThat(assignedTo(6)).hasValueSatisfying(type -> assertThat(type.is("java.lang.Object")).isTrue());
    assertThat(assignedTo(7)).isEmpty();
    assertThat(assignedTo(8)).isEmpty();
    assertThat(assignedTo(9)).isEmpty();
    assertThat(assignedTo(casts.size() - 1)).hasValueSatisfying(type -> assertThat(type.is("java.lang.Object")).isTrue());
  }

  @Test
  void an_argument_has_the_parameter_type_only_when_no_overload_of_the_owner_or_its_supertypes_could_take_it() throws Exception {
    ReplayTest.loadRecordings();
    List<TypeCastTree> arguments = new ArrayList<>();
    ReplayTest.translate("CastContexts").get().accept(new BaseTreeVisitor() {
      @Override
      public void visitTypeCast(TypeCastTree tree) {
        if (tree.parent() instanceof Arguments && tree.parent().parent() instanceof MethodInvocationTree) {
          arguments.add(tree);
        }
        super.visitTypeCast(tree);
      }
    });

    assertThat(CastChecks.assignedTo(castPassedTo(arguments, "take"))).hasValueSatisfying(type -> assertThat(type.is("java.lang.Object")).isTrue());
    assertThat(CastChecks.assignedTo(castPassedTo(arguments, "overloaded"))).isEmpty();
    assertThat(CastChecks.assignedTo(castPassedTo(arguments, "varargs"))).isEmpty();
  }

  private static TypeCastTree castPassedTo(List<TypeCastTree> casts, String method) {
    return casts.stream()
      .filter(cast -> ((MethodInvocationTree) cast.parent().parent()).methodSymbol().name().equals(method))
      .findFirst()
      .orElseThrow();
  }

  @Test
  void an_indexed_array_is_the_expression_of_an_array_access() {
    assertThat(CastChecks.isIndexedArray(casts.get(4))).isTrue();
    assertThat(CastChecks.isIndexedArray(casts.get(5))).isFalse();
    assertThat(CastChecks.isIndexedArray(casts.get(1))).isFalse();
  }

  @Test
  void the_result_of_a_lambda_argument_can_fix_the_inferred_call() {
    assertThat(CastChecks.isResultOfArgumentLambda(casts.get(9))).isTrue();
    assertThat(CastChecks.isResultOfArgumentLambda(casts.get(10))).isTrue();
    assertThat(CastChecks.isResultOfArgumentLambda(casts.get(6))).isFalse();
    assertThat(CastChecks.isResultOfArgumentLambda(casts.get(7))).isFalse();
    assertThat(CastChecks.isResultOfArgumentLambda(casts.get(1))).isFalse();
  }

  @Test
  void a_widening_cast_is_redundant_when_the_promotion_gives_the_same_types() {
    assertThat(sameOperandTypes(binaries.get(0))).isFalse();
    assertThat(sameOperandTypes(binaries.get(1))).isTrue();
    assertThat(sameOperandTypes(binaries.get(2))).isTrue();
    assertThat(sameOperandTypes(binaries.get(3))).isFalse();
    assertThat(CastChecks.isWideningPrimitiveCast(binaries.get(0))).isFalse();
    assertThat(CastChecks.castMessage(casts.get(11))).isEqualTo("Unnecessary cast from int to long");
  }

  private boolean sameOperandTypes(BinaryExpressionTree binary) {
    ExpressionTree left = ExpressionUtils.skipParentheses(binary.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binary.rightOperand());
    Tree.Kind kind = binary.kind();
    return CastChecks.sameOperandTypes(kind, CastChecks.promotionInput(left, true), CastChecks.promotionInput(right, false), left, right);
  }

  private Optional<Type> assignedTo(int index) {
    return CastChecks.assignedTo(casts.get(index));
  }

  private boolean castKeepsValue(String operand, String cast) {
    return CastChecks.castKeepsValue(sema.getClassType(operand), sema.getClassType(cast), sema.substitution());
  }

  private boolean assignable(String from, String to) {
    return CastChecks.isAssignable(sema.getClassType(from), sema.getClassType(to), sema.substitution());
  }
}
