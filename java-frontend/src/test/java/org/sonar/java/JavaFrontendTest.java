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
package org.sonar.java;

import java.io.File;
import java.util.*;

import org.junit.Test;
import org.sonar.java.JavaFrontend.ScannedFile;
import org.sonar.java.JavaFrontend.TaintSource;
import org.sonar.java.JavaFrontend.TaintSummary;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.tree.*;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaFrontendTest {

  private final ScannedFile src = new JavaFrontend().scan(new File("src/test/files/JavaFrontend.java"), this.getClass().getClassLoader());

  @Test
  public void return_const_literal_should_never_be_tainted() {
    TaintSummary summary = computeTaintSources("returnConstLiteral");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
  }

  @Test
  public void return_param_should_only_be_tainted_when_param_is_tainted() {
    TaintSummary summary = computeTaintSources("returnParam");

    assertThat(summary.resultTaintedPaths()).isEqualTo(1);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#returnParam(Ljava/lang/String;)Ljava/lang/String;#0")).isTrue();
  }

  @Test
  public void return_field_should_never_be_tainted() {
    TaintSummary summary = computeTaintSources("returnField");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
  }

  @Test
  public void return_param_through_local_variable_should_only_be_tainted_when_param_is_tainted() {
    TaintSummary summary = computeTaintSources("returnParamThroughLocalVariable");

    assertThat(summary.resultTaintedPaths()).isEqualTo(1);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#returnParamThroughLocalVariable(Ljava/lang/String;)Ljava/lang/String;#0")).isTrue();
  }

  @Test
  public void return_of_uninitialized_local_variable() {
    TaintSummary summary = computeTaintSources("returnOfUninitializedLocalVariable");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
  }

  @Test
  public void return_one_of_two_params_can_be_tainted() {
    TaintSummary summary = computeTaintSources("returnOneOfTwoParams");

    assertThat(summary.resultTaintedPaths()).isEqualTo(2);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#returnOneOfTwoParams(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;#1")).isTrue();
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#returnOneOfTwoParams(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;#2")).isTrue();
  }

  @Test
  public void return_param_through_two_paths_should_only_contain_one_tainted_source() {
    TaintSummary summary = computeTaintSources("returnParamThroughTwoPaths");

    assertThat(summary.resultTaintedPaths()).isEqualTo(1);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#returnParamThroughTwoPaths(ZLjava/lang/String;)Ljava/lang/String;#1")).isTrue();
  }

  @Test
  public void return_const_assigned_to_param_should_never_be_tainted() {
    TaintSummary summary = computeTaintSources("returnConstAssignedToParam");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
  }

  @Test
  public void return_field_later_reassigned_never_be_tainted() {
    TaintSummary summary = computeTaintSources("returnFieldReassignedLater");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
  }

  @Test
  public void return_of_another_method_should_be_tainted_when_return_value_is_tainted() {
    TaintSummary summary = computeTaintSources("returnOfAnotherMethod");

    assertThat(summary.resultTaintedPaths()).isEqualTo(1);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#myConst()Ljava/lang/String;")).isTrue();
  }

  @Test
  public void calling_the_same_method_can_return_different_results() {
    TaintSummary summary = computeTaintSources("callingSameMethodYieldDifferentResults");

    assertThat(summary.resultTaintedPaths()).isEqualTo(1);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#myConst()Ljava/lang/String;")).isTrue();
  }

  @Test
  public void return_my_object_should_be_taint_free() {
    TaintSummary summary = computeTaintSources("returnNonStringMethod");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
  }

  @Test
  public void return_of_forwarded_string() {
    TaintSummary summary = computeTaintSources("returnForwardedString");

    assertThat(summary.resultTaintedPaths()).isEqualTo(1);
    assertThat(summary.resultCanBeTaintedBy("my.pkg.MyClass#forward(Ljava/lang/String;Z)Ljava/lang/String;(my.pkg.MyClass#returnForwardedString(Ljava/lang/String;)Ljava/lang/String;#0, taint-free)")).isTrue();
  }

  @Test
  public void call_forwarded_method_but_return_literal() {
    TaintSummary summary = computeTaintSources("callForwardedMethod");

    assertThat(summary.resultTaintedPaths()).isEqualTo(0);
    assertThat(summary.callsMethod("my.pkg.MyClass#forward(Ljava/lang/String;Z)Ljava/lang/String;(my.pkg.MyClass#callForwardedMethod(Ljava/lang/String;)Ljava/lang/String;#0, taint-free)")).isTrue();
  }

  private TaintSummary computeTaintSources(String methodSimpleName) {
    MethodTree m = getMethod(methodSimpleName);
    CFG cfg = CFG.build(m);
    return JavaFrontend.computeTaintConditions(src, cfg);
  }

  private MethodTree getMethod(String simpleName) {
    MethodTree result = null;

    for (MethodTree m: getMethods(src.tree())) {
      if (m.simpleName().identifierToken().text().equals(simpleName)) {
        if (result != null) {
          throw new IllegalStateException();
        }
        result = m;
      }
    }

    if (result == null) {
      throw new NullPointerException();
    }

    return result;
  }

  private static Collection<MethodTree> getMethods(Tree tree) {
    List<MethodTree> result = new ArrayList<>();
    new BaseTreeVisitor() {
      {
        scan(tree);
      }

      @Override public void visitMethod(MethodTree methodTree) {
        super.visitMethod(methodTree);
        result.add(methodTree);
      }
    };
    return result;
  }

}
