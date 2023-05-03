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
package org.sonar.java.se.xproc;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MethodBehaviorJsonAdapterTest {

  private static final String IS_INSTANCE_SIGNATURE = "java.lang.Class#isInstance(Ljava/lang/Object;)Z";
  private static final String IS_INSTANCE_METHOD_BEHAVIOR = "{\n"
    + "  \"signature\": \"java.lang.Class#isInstance(Ljava/lang/Object;)Z\",\n"
    + "  \"varArgs\": false,\n"
    + "  \"declaredExceptions\": [],\n"
    + "  \"yields\": [\n"
    + "    {\n"
    + "      \"parametersConstraints\": [\n"
    + "        [\n"
    + "          \"NOT_NULL\"\n"
    + "        ]\n"
    + "      ],\n"
    + "      \"resultIndex\": -1,\n"
    + "      \"resultConstraint\": [\n"
    + "        \"TRUE\"\n"
    + "      ]\n"
    + "    },\n"
    + "    {\n"
    + "      \"parametersConstraints\": [\n"
    + "        []\n"
    + "      ],\n"
    + "      \"resultIndex\": -1,\n"
    + "      \"resultConstraint\": [\n"
    + "        \"FALSE\"\n"
    + "      ]\n"
    + "    }\n"
    + "  ]\n"
    + "}";

  private BehaviorCache cache;
  private Gson gson;

  @BeforeEach
  void init() {
    cache = new BehaviorCache();
    cache.setFileContext(null);
    gson = MethodBehaviorJsonAdapter.gson();
  }

  @Test
  void serialization() {
    MethodBehavior isInstancePrecomputed = cache.get(IS_INSTANCE_SIGNATURE);
    assertThat(isInstancePrecomputed).isNotNull();

    String isInstanceSerialized = gson.toJson(isInstancePrecomputed);
    assertThat(isInstanceSerialized).isEqualTo(IS_INSTANCE_METHOD_BEHAVIOR);
  }

  @Test
  void serialization_unknown_yield_type_should_throw_an_exception() {
    MethodBehavior mb = newMethodBehavior("org.bar.A#foo()Z");
    mb.addYield(new CustomMethodYield(mb));
    mb.completed();

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> gson.toJson(mb));
    assertThat(e).hasMessage("Hardcoded yields should only be HappyPathYield or ExceptionalYield.");
  }

  private static class CustomMethodYield extends MethodYield {
    public CustomMethodYield(MethodBehavior behavior) {
      super(behavior);
    }

    @Override
    public String toString() {
      return "CustomMethodYield";
    }

    @Override
    public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
      Supplier<SymbolicValue> svSupplier) {
      return Stream.of(programState);
    }
  }

  @Test
  void serialization_unsupported_constraint_should_be_ignored() {
    MethodBehavior mb = newMethodBehavior("org.bar.A#foo()Z");
    HappyPathYield happyPathYield = new HappyPathYield(mb);
    happyPathYield.setResult(-1, ConstraintsByDomain.empty().put(CustomConstraint.SQUARE).put(ObjectConstraint.NOT_NULL));

    mb.addYield(happyPathYield);
    mb.completed();

    assertThat(gson.toJson(mb))
    .contains("resultConstraint\": [\n"
      + "        \"NOT_NULL\"\n"
      + "      ]")
    .doesNotContain("SQUARE");
  }

  private enum CustomConstraint implements Constraint {
    SQUARE
  }

  @Test
  void serialization_null_constraints_should_be_stored_as_null() {
    MethodBehavior mb = newMethodBehavior("org.bar.A#foo()Z");
    HappyPathYield happyPathYield = new HappyPathYield(mb);
    happyPathYield.setResult(-1, null);

    mb.addYield(happyPathYield);
    mb.completed();

    assertThat(gson.toJson(mb))
      .contains("\"resultConstraint\": null")
      .contains("\"parametersConstraints\": []");
  }

  @Test
  void deserialization_serialization() {
    MethodBehavior customBehavior = newMethodBehavior("org.bar.A#foo(Ljava/lang/Object;)Z");
    customBehavior.setDeclaredExceptions(Arrays.asList("org.foo.MyException", "org.bar.MyOtherException"));
    customBehavior.setVarArgs(true);
    HappyPathYield hpy = new HappyPathYield(customBehavior);
    hpy.setResult(-1, ConstraintsByDomain.empty().put(BooleanConstraint.TRUE));
    hpy.parametersConstraints.add(ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL));
    customBehavior.addYield(hpy);
    ExceptionalYield ey = new ExceptionalYield(customBehavior);
    ey.setExceptionType("org.bar.MyOtherException");
    ey.parametersConstraints.add(ConstraintsByDomain.empty().put(ObjectConstraint.NULL));
    customBehavior.addYield(ey);
    customBehavior.completed();

    String serialized = gson.toJson(customBehavior);
    assertThat(serialized).isEqualTo(
      "{\n"
      + "  \"signature\": \"org.bar.A#foo(Ljava/lang/Object;)Z\",\n"
      + "  \"varArgs\": true,\n"
      + "  \"declaredExceptions\": [\n"
      + "    \"org.foo.MyException\",\n"
      + "    \"org.bar.MyOtherException\"\n"
      + "  ],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        [\n"
      + "          \"NULL\"\n"
      + "        ]\n"
      + "      ],\n"
      + "      \"exception\": \"org.bar.MyOtherException\"\n"
      + "    },\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        [\n"
      + "          \"NOT_NULL\"\n"
      + "        ]\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstraint\": [\n"
      + "        \"TRUE\"\n"
      + "      ]\n"
      + "    }\n"
      + "  ]\n"
      + "}");
    MethodBehavior deserialized = gson.fromJson(serialized, MethodBehavior.class);
    assertThat(deserialized).isEqualTo(customBehavior);
  }

  @Test
  void deserialization() {
    MethodBehavior isInstancePrecomputed = cache.get(IS_INSTANCE_SIGNATURE);
    assertThat(isInstancePrecomputed).isNotNull();

    MethodBehavior isInstanceDeserialized = gson.fromJson(IS_INSTANCE_METHOD_BEHAVIOR, MethodBehavior.class);
    assertThat(isInstanceDeserialized).isEqualTo(isInstancePrecomputed);
  }

  @Test
  void deserialization_wrong_constraint_format() {
    String wronglySeperated = "BooleanConstraint:TRUE";
    String invalidMethodBehavior1 = "{\n"
      + "  \"signature\": \"org.bar.A#foo(Ljava/lang/Object;)Z\",\n"
      + "  \"varArgs\": false,\n"
      + "  \"declaredExceptions\": [],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        []\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstraint\": [\"" + wronglySeperated + "\"]\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    JsonSyntaxException e1 = assertThrows(
      JsonSyntaxException.class,
      () -> gson.fromJson(invalidMethodBehavior1, MethodBehavior.class));
    assertThat(e1).hasRootCauseInstanceOf(IllegalStateException.class);

    String wronglyQualified = ".TRUE";
    String invalidMethodBehavior2 = "{\n"
      + "  \"signature\": \"org.bar.A#foo(Ljava/lang/Object;)Z\",\n"
      + "  \"varArgs\": false,\n"
      + "  \"declaredExceptions\": [],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        []\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstraint\": [\"" + wronglyQualified + "\"]\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    JsonSyntaxException e2 = assertThrows(
      JsonSyntaxException.class,
      () -> gson.fromJson(invalidMethodBehavior2, MethodBehavior.class));
    assertThat(e2).hasRootCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void deserialization_unsupported_constraint() {
    String wrongConstraint = CustomConstraint.SQUARE.name();
    String invalidMethodBehavior = "{\n"
      + "  \"signature\": \"org.bar.A#foo(Ljava/lang/Object;)Z\",\n"
      + "  \"varArgs\": false,\n"
      + "  \"declaredExceptions\": [],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        []\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstraint\": [\"" + wrongConstraint + "\"]\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    JsonSyntaxException e = assertThrows(
      JsonSyntaxException.class,
      () -> gson.fromJson(invalidMethodBehavior, MethodBehavior.class));
    assertThat(e)
      .hasRootCauseInstanceOf(IllegalStateException.class)
      .hasMessage(
        "java.lang.IllegalStateException: Unsupported constraint \"SQUARE\". Only \"TRUE\", \"FALSE\", \"NULL\", \"NOT_NULL\", \"ZERO\" and \"NON_ZERO\" are supported.");
  }

  @Test
  void exceptional_yield_serialization_deserialaization() {
    MethodBehavior mb = newMethodBehavior("org.foo.A.bar(Ljava/lang/Object;)Z");
    ExceptionalYield yield = new ExceptionalYield(mb);
    yield.setExceptionType("java.lang.Exception");
    yield.parametersConstraints.add(ConstraintsByDomain.empty().put(ObjectConstraint.NULL));
    mb.addYield(yield);
    mb.completed();

    String serialized = gson.toJson(mb);
    assertThat(serialized).contains("\"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        [\n"
      + "          \"NULL\"\n"
      + "        ]\n"
      + "      ],\n"
      + "      \"exception\": \"java.lang.Exception\"\n"
      + "    }\n"
      + "  ]");

    MethodBehavior deserialized = gson.fromJson(serialized, MethodBehavior.class);
    assertThat(deserialized).isEqualTo(mb);
  }

  private static MethodBehavior newMethodBehavior(String signature) {
    return new MethodBehavior(signature, false);
  }
}
