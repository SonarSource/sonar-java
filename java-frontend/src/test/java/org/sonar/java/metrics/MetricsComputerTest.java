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
package org.sonar.java.metrics;

import java.lang.reflect.Field;
import java.util.Map;
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

  @SuppressWarnings({"unused", "unchecked"})
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
    mc.getComplexityNodes(methodTree).size();
    var complexityNodes = (Map<Integer, ?>) getValue("methodComplexityNodes");
    assertThat(complexityNodes).containsKey(methodTree.hashCode());
    
    mc.methodComplexity(methodTree);
    var methodComplexity = (Map<Integer, ?>) getValue("methodComplexity");
    assertThat(methodComplexity).containsKey(methodTree.hashCode());
    
    mc.getNumberOfAccessedVariables(methodTree);
    var methodNumberOfAccessedVariables = (Map<Integer, ?>) getValue("methodNumberOfAccessedVariables");
    assertThat(methodNumberOfAccessedVariables).containsKey(methodTree.hashCode());
    
    mc.linesOfCode(methodTree);
    var treeLinesOfCode = (Map<Integer, ?>) getValue("treeLinesOfCode");
    assertThat(treeLinesOfCode).containsKey(methodTree.hashCode());
    
    mc.numberOfStatements(methodTree);
    var treeNumberOfStatements = (Map<Integer, ?>) getValue("treeNumberOfStatements");
    assertThat(treeNumberOfStatements).containsKey(methodTree.hashCode());
    
    mc.numberOfCommentedLines(cut);
    var treeNumberOfCommentedLines = (Map<Integer, ?>) getValue("treeNumberOfCommentedLines");
    assertThat(treeNumberOfCommentedLines).containsKey(cut.hashCode());
    
    mc.noSonarLines(cut);
    var treeNoSonarLines = (Map<Integer, ?>) getValue("treeNoSonarLines");
    assertThat(treeNoSonarLines).containsKey(cut.hashCode());
    
    mc.compilationUnitComplexity(cut);
    var compilationUnityComplexity = (Map<Integer, ?>) getValue("compilationUnityComplexity");
    assertThat(compilationUnityComplexity).containsKey(cut.hashCode());
    
    mc.methodNestingLevel(methodTree);
    var methodNestingLevel = (Map<Integer, ?>) getValue("methodNestingLevel");
    assertThat(methodNestingLevel).containsKey(methodTree.hashCode());
    
  }

  Object getValue(String fname) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    Field field = mc.getClass().getDeclaredField(fname);
    field.setAccessible(true);
    return field.get(mc);
  }
  
}
