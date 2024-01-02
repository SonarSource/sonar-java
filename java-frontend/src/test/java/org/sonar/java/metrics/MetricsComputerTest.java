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
package org.sonar.java.metrics;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsComputerTest implements MetricsScannerContext {

  MetricsComputer mc = new MetricsComputer();

  @Override
  public MetricsComputer getMetricsComputer() {
    return mc;
  }

  @Test
  void testMetricsPresence() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {" +
        "  Object foo(){" +
        "    if(a) { " +
        "      for(int i=0; i<3; i++) { if(i==2){ return null; } }" +
        "      for(int i : new int[]{1,2}) { if(i==2){ return null; } }" +
        "      return new Object();   " +
        "    };" +
        "  } " +
        "}");
    
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);

    assertThat(mc.getMethodComplexityNodes()).isEmpty();
    mc.getComplexityNodes(methodTree).size();
    assertThat(mc.getMethodComplexityNodes()).containsKey(methodTree.hashCode());

    assertThat(mc.getMethodComplexity()).isEmpty();
    mc.getMethodComplexity(methodTree);
    assertThat(mc.getMethodComplexity()).containsKey(methodTree.hashCode());
    
    assertThat(mc.getMethodNumberOfAccessedVariables()).isEmpty();
    mc.getNumberOfAccessedVariables(methodTree);
    assertThat(mc.getMethodNumberOfAccessedVariables()).containsKey(methodTree.hashCode());
    
    assertThat(mc.getTreeLinesOfCode()).isEmpty();
    mc.getLinesOfCode(methodTree);
    assertThat(mc.getTreeLinesOfCode()).containsKey(methodTree.hashCode());
    
    assertThat(mc.getTreeNumberOfStatements()).isEmpty();
    mc.getNumberOfStatements(methodTree);
    assertThat(mc.getTreeNumberOfStatements()).containsKey(methodTree.hashCode());
    
    assertThat(mc.getTreeNumberOfCommentedLines()).isEmpty();
    mc.getNumberOfCommentedLines(cut);
    assertThat(mc.getTreeNumberOfCommentedLines()).containsKey(cut.hashCode());
    
    assertThat(mc.getTreeNoSonarLines()).isEmpty();
    mc.getNoSonarLines(cut);
    assertThat(mc.getTreeNoSonarLines()).containsKey(cut.hashCode());
    
    assertThat(mc.getCompilationUnityComplexity()).isEmpty();
    mc.getCompilationUnitComplexity(cut);
    assertThat(mc.getCompilationUnityComplexity()).containsKey(cut.hashCode());
    
    assertThat(mc.getMethodNestingLevel()).isEmpty();
    mc.getMethodNestingLevel(methodTree);
    assertThat(mc.getMethodNestingLevel()).containsKey(methodTree.hashCode());
    
  }
  
}
