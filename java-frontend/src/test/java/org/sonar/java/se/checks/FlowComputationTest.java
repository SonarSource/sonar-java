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
package org.sonar.java.se.checks;

import org.junit.Test;

import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.JavaCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FlowComputationTest {

  @Test
  public void test() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputation.java", new NullDereferenceCheck(), new ConditionAlwaysTrueOrFalseCheck());
  }

  @Test
  public void test_catof() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationCATOF.java", new NullDereferenceCheck(), new ConditionAlwaysTrueOrFalseCheck());
  }

  @Test
  public void test_messages_on_method_invocation() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationMIT.java", new NullDereferenceCheck(), new ConditionAlwaysTrueOrFalseCheck());
  }

  @Test
  public void test_singleton() throws Exception {
    MethodInvocationTreeImpl mockTree = mock(MethodInvocationTreeImpl.class);
    Set<List<JavaFileScannerContext.Location>> singleton = FlowComputation.singleton("singleton msg", mockTree);
    assertThat(singleton).hasSize(1);
    List<JavaFileScannerContext.Location> flow = singleton.iterator().next();
    assertThat(flow).hasSize(1);
    assertThat(flow.get(0).msg).isEqualTo("singleton msg");
  }

  @Test
  public void test_relational_sv_operands() throws Exception {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationRelSV.java", new NullDereferenceCheck(), new ConditionAlwaysTrueOrFalseCheck());
  }

  @Test
  public void test_multiple_paths() {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationMultiplePath.java", new NullDereferenceCheck(), new ConditionAlwaysTrueOrFalseCheck());
  }

  @Test
  public void test_multiple_paths_xproc() {
    JavaCheckVerifier.verify("src/test/files/se/FlowComputationMultiplePathXProc.java", new NullDereferenceCheck(), new ConditionAlwaysTrueOrFalseCheck());

  }
}
