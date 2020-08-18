/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.Sema;
import org.sonar.java.se.ProgramState;
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
    + "  \"arity\": 1,\n"
    + "  \"varArgs\": false,\n"
    + "  \"declaredExceptions\": [],\n"
    + "  \"yields\": [\n"
    + "    {\n"
    + "      \"parametersConstraints\": [\n"
    + "        [\n"
    + "          \"ObjectConstraint.NOT_NULL\"\n"
    + "        ]\n"
    + "      ],\n"
    + "      \"resultIndex\": -1,\n"
    + "      \"resultConstaint\": [\n"
    + "        \"BooleanConstraint.TRUE\"\n"
    + "      ],\n"
    + "      \"isExceptional\": false\n"
    + "    },\n"
    + "    {\n"
    + "      \"parametersConstraints\": [\n"
    + "        []\n"
    + "      ],\n"
    + "      \"resultIndex\": -1,\n"
    + "      \"resultConstaint\": [\n"
    + "        \"BooleanConstraint.FALSE\"\n"
    + "      ],\n"
    + "      \"isExceptional\": false\n"
    + "    }\n"
    + "  ]\n"
    + "}";

  private BehaviorCache cache;
  private Gson gson;

  @BeforeEach
  void init() {
    Sema semanticModel = ((JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("class A { }")).sema;
    cache = new BehaviorCache();
    cache.setFileContext(null, semanticModel);
    gson = MethodBehaviorJsonAdapter.gson(semanticModel);
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
    MethodBehavior mb = new MethodBehavior("org.bar.A#foo()Z");
    MethodYield customMethodYield = new MethodYield(mb) {
      @Override
      public String toString() {
        return "CustomYield";
      }

      @Override
      public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
        Supplier<SymbolicValue> svSupplier) {
        return Stream.of(programState);
      }
    };
    mb.addYield(customMethodYield);
    mb.completed();

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> gson.toJson(mb));
    assertThat(e).hasMessage("Hardcoded yields should only be HappyPathYield or ExceptionalYield.");
  }

  @Test
  void serialization_null_constraints_should_be_stored_as_null() {
    MethodBehavior mb = new MethodBehavior("org.bar.A#foo()Z");
    HappyPathYield happyPathYield = new HappyPathYield(mb);
    happyPathYield.setResult(-1, null);

    mb.addYield(happyPathYield);
    mb.completed();

    assertThat(gson.toJson(mb))
      .contains("\"resultConstaint\": null,")
      .contains("\"parametersConstraints\": [],");
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
      + "  \"arity\": 1,\n"
      + "  \"varArgs\": false,\n"
      + "  \"declaredExceptions\": [],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        []\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstaint\": [\"" + wronglySeperated + "\"],\n"
      + "      \"isExceptional\": false\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    JsonSyntaxException e1 = assertThrows(
      JsonSyntaxException.class,
      () -> gson.fromJson(invalidMethodBehavior1, MethodBehavior.class));
    assertThat(e1).hasRootCauseInstanceOf(IllegalStateException.class);

    String wronglyQualified = "BooleanConstraint..TRUE";
    String invalidMethodBehavior2 = "{\n"
      + "  \"signature\": \"org.bar.A#foo(Ljava/lang/Object;)Z\",\n"
      + "  \"arity\": 1,\n"
      + "  \"varArgs\": false,\n"
      + "  \"declaredExceptions\": [],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        []\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstaint\": [\"" + wronglyQualified + "\"],\n"
      + "      \"isExceptional\": false\n"
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
    String wrongConstraint = "MyConstraint.SQUARE";
    String invalidMethodBehavior = "{\n"
      + "  \"signature\": \"org.bar.A#foo(Ljava/lang/Object;)Z\",\n"
      + "  \"arity\": 1,\n"
      + "  \"varArgs\": false,\n"
      + "  \"declaredExceptions\": [],\n"
      + "  \"yields\": [\n"
      + "    {\n"
      + "      \"parametersConstraints\": [\n"
      + "        []\n"
      + "      ],\n"
      + "      \"resultIndex\": -1,\n"
      + "      \"resultConstaint\": [\"" + wrongConstraint + "\"],\n"
      + "      \"isExceptional\": false\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    JsonSyntaxException e = assertThrows(
      JsonSyntaxException.class,
      () -> gson.fromJson(invalidMethodBehavior, MethodBehavior.class));
    assertThat(e)
      .hasRootCauseInstanceOf(IllegalStateException.class)
      .hasMessage(
        "java.lang.IllegalStateException: Unsupported Domain constraint \"MyConstraint\". Only BooleanConstraint (TRUE, FALSE) and ObjectConstraint (NULL, NOT_NULL) are supported.");
  }

  @Test
  void exceptional_yield_serialization_deserialaization() {
    MethodBehavior mb = new MethodBehavior("org.foo.A.bar(Ljava/lang/Object;)Z");
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
      + "          \"ObjectConstraint.NULL\"\n"
      + "        ]\n"
      + "      ],\n"
      + "      \"exception\": \"java.lang.Exception\",\n"
      + "      \"isExceptional\": true\n"
      + "    }\n"
      + "  ]");

    MethodBehavior deserialized = gson.fromJson(serialized, MethodBehavior.class);
    assertThat(deserialized).isEqualTo(mb);
  }
}
