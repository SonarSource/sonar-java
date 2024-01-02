/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.Sema;
import org.sonar.java.se.Pair;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.utils.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.utils.SETestUtils.createSymbolicExecutionVisitorAndSemantic;
import static org.sonar.java.se.utils.SETestUtils.getMethodBehavior;

class MethodBehaviorTest {

  @Test
  void method_behavior_signature() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodYields.java",
      new NullDereferenceCheck());

    MethodBehavior mb = getMethodBehavior(sev, "method");

    assertThat(mb.signature()).isEqualTo("MethodYields#method(Ljava/lang/Object;Z)Z");
    assertThat(mb.signature()).isNotNull();
  }

  @Test
  void method_behavior_yields() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodYields.java",
      new NullDereferenceCheck());

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
  void method_behavior_handling_finally() {
    Pair<SymbolicExecutionVisitor, Sema> visitorAndSemantic =
      createSymbolicExecutionVisitorAndSemantic("src/test/resources/se/ReturnAndFinally.java", new NullDereferenceCheck());
    SymbolicExecutionVisitor sev = visitorAndSemantic.a;
    Sema semanticModel = visitorAndSemantic.b;
    assertThat(sev.behaviorCache.behaviors).hasSize(5);

    MethodBehavior foo = getMethodBehavior(sev, "foo");
    assertThat(foo.yields()).hasSize(4);
    assertThat(foo.happyPathYields()).hasSize(2);
    assertThat(foo.exceptionalPathYields()).hasSize(2);

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

    MethodBehavior returnInFinally = getMethodBehavior(sev, "returnInFinally");
    assertThat(returnInFinally.yields()).hasSize(1);
    assertThat(returnInFinally.happyPathYields()).hasSize(1);

    MethodBehavior returningException = getMethodBehavior(sev, "returningException");
    assertThat(returningException.yields()).hasSize(3);
    assertThat(returningException.happyPathYields()).hasSize(2);
    assertThat(returningException.exceptionalPathYields()).hasSize(1);

    MethodBehavior rethrowingException = getMethodBehavior(sev, "rethrowingException");
    assertThat(rethrowingException.yields()).hasSize(4);
    assertThat(rethrowingException.happyPathYields()).hasSize(1);
    assertThat(rethrowingException.exceptionalPathYields()).hasSize(3);
    assertThat(rethrowingException.exceptionalPathYields().filter(y -> y.exceptionType(semanticModel).isUnknown())).hasSize(1);
    assertThat(rethrowingException.exceptionalPathYields().filter(y -> y.exceptionType(semanticModel).is("java.lang.Exception"))).hasSize(1);
    assertThat(rethrowingException.exceptionalPathYields().filter(y -> y.exceptionType(semanticModel).is("org.foo.MyException"))).hasSize(1);

  }

  @Test
  void test_reducing_of_yields_on_arguments() {
    MethodBehavior mb = newMethodBehavior("foo(Ljava/lang/Object;)V");
    addYield(mb, null, ObjectConstraint.NOT_NULL);
    addYield(mb, null, ObjectConstraint.NULL);
    mb.completed();
    assertThat(mb.yields()).hasSize(1);
    assertThat(mb.yields().get(0).parametersConstraints).contains(ConstraintsByDomain.empty());

    mb = newMethodBehavior("foo(Z)V");
    addYield(mb, null, BooleanConstraint.TRUE);
    addYield(mb, null, BooleanConstraint.FALSE);
    mb.completed();
    assertThat(mb.yields()).hasSize(1);
    assertThat(mb.yields().get(0).parametersConstraints).contains(ConstraintsByDomain.empty());
  }

  @Test
  void result_with_boolean_constraint_should_be_reduced() {
    MethodBehavior mb = newMethodBehavior("foo()Z");
    addYield(mb, BooleanConstraint.TRUE);
    addYield(mb, BooleanConstraint.FALSE);
    mb.completed();
    assertThat(mb.yields()).hasSize(1);
    assertThat(((HappyPathYield) mb.yields().get(0)).resultConstraint()).isNull();

    mb = newMethodBehavior("foo()Z");
    addYield(mb, BooleanConstraint.TRUE, ObjectConstraint.NULL);
    addYield(mb, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL);
    mb.completed();
    assertThat(mb.yields()).hasSize(2);
    List<Constraint> resultConstraints = mb.yields().stream().map(y -> ((HappyPathYield) y).resultConstraint().get(BooleanConstraint.class)).collect(Collectors.toList());
    assertThat(resultConstraints).contains(BooleanConstraint.TRUE, BooleanConstraint.FALSE);
  }

  @Test
  void result_with_unreducible_constraint_should_not_be_reduced() {
    MethodBehavior mb = newMethodBehavior("foo()Ljava/lang/Object;");
    addYield(mb, ObjectConstraint.NOT_NULL);
    addYield(mb, ObjectConstraint.NULL);
    mb.completed();
    assertThat(mb.yields()).hasSize(2);
    List<Constraint> resultConstraints = mb.yields().stream().map(y -> ((HappyPathYield) y).resultConstraint().get(ObjectConstraint.class)).collect(Collectors.toList());
    assertThat(resultConstraints).contains(ObjectConstraint.NULL, ObjectConstraint.NOT_NULL);
  }

  @Test
  void equality() {
    MethodBehavior mb = newMethodBehavior("foo()Ljava/lang/Object;");
    addYield(mb, ObjectConstraint.NOT_NULL);
    addYield(mb, ObjectConstraint.NULL);

    MethodBehavior sameYields = newMethodBehavior("foo()Ljava/lang/Object;");
    addYield(sameYields, ObjectConstraint.NOT_NULL);
    addYield(sameYields, ObjectConstraint.NULL);

    MethodBehavior differentType = new MethodBehavior("foo()Ljava/lang/Object;", false) {
      private Object o = new Object();
    };
    addYield(differentType, ObjectConstraint.NOT_NULL);
    addYield(differentType, ObjectConstraint.NULL);

    MethodBehavior differentSignature = newMethodBehavior("bar()Ljava/lang/Object;");
    addYield(differentSignature, ObjectConstraint.NOT_NULL);
    addYield(differentSignature, ObjectConstraint.NULL);

    assertThat(mb)
      .isEqualTo(mb)
      .isEqualTo(sameYields)
      .isNotEqualTo(differentType)
      .isNotEqualTo(differentSignature)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object());
  }

  @Test
  void hashcode() {
    MethodBehavior mb = newMethodBehavior("foo()Ljava/lang/Object;");
    addYield(mb, ObjectConstraint.NOT_NULL);
    addYield(mb, ObjectConstraint.NULL);

    MethodBehavior sameSignatureAndYield = newMethodBehavior("foo()Ljava/lang/Object;");
    addYield(sameSignatureAndYield, ObjectConstraint.NOT_NULL);
    addYield(sameSignatureAndYield, ObjectConstraint.NULL);

    MethodBehavior differentYield = newMethodBehavior("foo()Ljava/lang/Object;");
    addYield(differentYield, ObjectConstraint.NULL);

    MethodBehavior differentSignature = newMethodBehavior("bar()Ljava/lang/Object;");
    addYield(differentSignature, ObjectConstraint.NOT_NULL);
    addYield(differentSignature, ObjectConstraint.NULL);

    assertThat(mb).hasSameHashCodeAs(sameSignatureAndYield);
    assertThat(mb.hashCode())
      .isNotEqualTo(differentYield.hashCode())
      .isNotEqualTo(differentSignature.hashCode());
  }

  @Test
  void to_String_display_number_of_yields() {
    String signature = "foo()Ljava/lang/Object;";
    MethodBehavior mb1 = newMethodBehavior(signature);
    addYield(mb1, ObjectConstraint.NOT_NULL);
    addYield(mb1, ObjectConstraint.NULL);

    assertThat(mb1).hasToString("foo()Ljava/lang/Object; [2 yield(s)]");
  }

  @Test
  void anonymous_classes_used_as_exception_should_be_resolved_to_supertype() {
    Pair<SymbolicExecutionVisitor, Sema> visitorAndSemantic = createSymbolicExecutionVisitorAndSemantic(
      "src/test/java/org/sonar/java/resolve/targets/se/TestExceptionSupertypeResolution.java",
      new NullDereferenceCheck());
    SymbolicExecutionVisitor sev = visitorAndSemantic.a;
    Sema semanticModel = visitorAndSemantic.b;
    MethodBehavior mb = getMethodBehavior(sev, "throwException");
    List<ExceptionalYield> exceptionYields = mb.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionYields).hasSize(3);
    assertThat(exceptionYields.stream()
      .map(ey -> ey.exceptionType(semanticModel))
      .map(exceptionType -> (exceptionType == null || exceptionType.isUnknown() ? null : exceptionType.fullyQualifiedName()))
      .collect(Collectors.toSet()))
        .containsOnly("org.sonar.java.resolve.targets.se.TestExceptionSupertypeResolution$Foo", "java.lang.Exception", null);
  }

  private void addYield(MethodBehavior mb, @Nullable Constraint result, Constraint... constraints) {
    HappyPathYield yield = new HappyPathYield(mb);
    for (Constraint constraint : constraints) {
      yield.parametersConstraints.add(ConstraintsByDomain.empty().put(constraint));
    }
    if (result != null) {
      yield.setResult(-1, ConstraintsByDomain.empty().put(result));
    }
    mb.addYield(yield);
  }

  private static MethodBehavior newMethodBehavior(String signature) {
    return new MethodBehavior(signature, false);
  }
}
