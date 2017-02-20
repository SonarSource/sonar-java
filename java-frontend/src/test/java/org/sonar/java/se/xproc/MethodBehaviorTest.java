/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.Test;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;

public class MethodBehaviorTest {

  @Test
  public void method_behavior_yields() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodYields.java");

    MethodBehavior mb = getMethodBehavior(sev, "method");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(3);

    List<HappyPathYield> trueResults = mb.happyPathYields().filter(my -> BooleanConstraint.TRUE.equals(my.resultConstraint().get(BooleanConstraint.class))).collect(Collectors.toList());
    assertThat(trueResults).hasSize(1);
    HappyPathYield trueResult = trueResults.get(0);

    // 'a' has constraint "null"
    assertThat(((ObjectConstraint) trueResult.parametersConstraints.get(0).get(ObjectConstraint.class)).isNull()).isTrue();
    // no constraint on 'b'
    assertThat(((ObjectConstraint) trueResult.parametersConstraints.get(0).get(ObjectConstraint.class)).isNull()).isTrue();
    // result SV is a different SV than 'a' and 'b'
    assertThat(trueResult.resultIndex()).isEqualTo(-1);

    List<HappyPathYield> falseResults = mb.happyPathYields().filter(my -> BooleanConstraint.FALSE.equals(my.resultConstraint().get(BooleanConstraint.class))).collect(Collectors.toList());
    assertThat(falseResults).hasSize(2);
    // for both "False" results, 'a' has the constraint "not null"
    assertThat(falseResults.stream().filter(my -> !((ObjectConstraint) my.parametersConstraints.get(0).get(ObjectConstraint.class)).isNull()).count()).isEqualTo(2);
    // 1) 'b' has constraint "false", result is 'b'
    assertThat(falseResults.stream().filter(my -> BooleanConstraint.FALSE.equals(my.parametersConstraints.get(1).get(BooleanConstraint.class)) && my.resultIndex() == 1).count()).isEqualTo(1);

    // 2) 'b' is "true", result is a different SV than 'a' and 'b'
    assertThat(falseResults.stream().filter(my -> BooleanConstraint.TRUE.equals(my.parametersConstraints.get(1).get(BooleanConstraint.class)) && my.resultIndex() == -1).count()).isEqualTo(1);

  }

  @Test
  public void method_behavior_handling_finally() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/ReturnAndFinally.java");
    assertThat(sev.behaviorCache.behaviors.entrySet()).hasSize(2);

    MethodBehavior foo = getMethodBehavior(sev, "foo");
    assertThat(foo.yields()).hasSize(4);
    assertThat(foo.happyPathYields().count()).isEqualTo(2);
    assertThat(foo.exceptionalPathYields().count()).isEqualTo(2);

    MethodBehavior qix = getMethodBehavior(sev, "qix");
    List<MethodYield> qixYield = qix.yields();
    assertThat(qixYield.stream()
      .filter(y ->  y.parametersConstraints.get(0).get(ObjectConstraint.class) != ObjectConstraint.NULL)
      .allMatch(y -> y instanceof ExceptionalYield)).isTrue();

    assertThat(qixYield.stream()
      .filter(y -> y.parametersConstraints.get(0).get(ObjectConstraint.class) == ObjectConstraint.NULL && y instanceof ExceptionalYield)
      .count()).isEqualTo(2);

    assertThat(qixYield.stream()
      .filter(y -> y instanceof HappyPathYield)
      .allMatch(y -> y.parametersConstraints.get(0).get(ObjectConstraint.class) == ObjectConstraint.NULL)).isTrue();
  }

}
