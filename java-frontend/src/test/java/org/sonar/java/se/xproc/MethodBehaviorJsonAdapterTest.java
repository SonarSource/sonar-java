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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.Sema;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import static org.assertj.core.api.Assertions.assertThat;

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
  void deserialization() {
    MethodBehavior isInstancePrecomputed = cache.get(IS_INSTANCE_SIGNATURE);
    assertThat(isInstancePrecomputed).isNotNull();

    MethodBehavior isInstanceDeserialized = gson.fromJson(IS_INSTANCE_METHOD_BEHAVIOR, MethodBehavior.class);
    assertThat(isInstanceDeserialized).isEqualTo(isInstancePrecomputed);
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
