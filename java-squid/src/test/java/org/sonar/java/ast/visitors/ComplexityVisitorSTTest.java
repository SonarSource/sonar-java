/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ComplexityVisitorSTTest {

  private final String[] dirs = {
      "src/main/java",
      "src/test/java",
      "src/test/files",
      "target/test-projects/struts-core-1.3.9/src",
      "target/test-projects/commons-collections-3.2.1/src"
  };

  @Test
  public void no_regression_in_complexity_computation() {

    for (String dir : dirs) {
      for (File file : FileUtils.listFiles(new File(dir), new String[]{"java"}, true)) {
        final int[] complexity = new int[1];
        SourceFile sourceFile = JavaAstScanner.scanSingleFile(file, new VisitorsBridge(new SubscriptionVisitor() {
          @Override
          public List<Tree.Kind> nodesToVisit() {
            return ImmutableList.of(Tree.Kind.COMPILATION_UNIT);
          }

          @Override
          public void visitNode(Tree tree) {
            complexity[0] = new ComplexityVisitorST().scan(tree);
          }
        }));
        int fileCplxity = sourceFile.getInt(JavaMetric.COMPLEXITY);
        assertThat(fileCplxity).as(sourceFile.getName() + " : old complexity : " + fileCplxity + " - " + complexity[0]).isEqualTo(complexity[0]).as(sourceFile.getName());
      }
    }

  }
}