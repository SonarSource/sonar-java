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
import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.tree.*;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaFrontendTest {

  private final ScannedFile src = new JavaFrontend().scan(new File("src/test/files/JavaFrontend.java"), this.getClass().getClassLoader());

  @Test
  public void return_const_literal_should_never_be_tainted() {
    Set<TaintSource> conditions = computeTaintSources("constLiteral");

    assertThat(conditions).isEmpty();
  }

  @Test
  public void return_param_should_only_be_tainted_when_param_is_tainted() {
    Set<TaintSource> conditions = computeTaintSources("returnParam");

    assertThat(conditions).hasOnlyOneElementSatisfying(ts -> {
      assertThat(ts.toString()).isEqualTo("returnParam#a");
    });
  }

  @Test
  public void return_one_of_two_params_can_be_tainted() {
    Set<TaintSource> conditions = computeTaintSources("returnOneOfTwoParams");

    System.out.println("Return taint conditions");
    for (TaintSource ts: conditions) {
      System.out.println("  - " + ts);
    }

    assertThat(conditions).hasSize(2);
  }

  private Set<TaintSource> computeTaintSources(String methodSimpleName) {
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
