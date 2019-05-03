/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.resolve.targets.bytecodeGenerics;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class SemanticOrderResolution {

  @Test
  public void verify_generic_substitution() {
    MyVisitor myVisitor = new MyVisitor();
    JavaAstScanner.scanSingleFileForTests(
      TestUtils.inputFile("src/test/java/org/sonar/java/resolve/targets/bytecodeGenerics/Main.java"),
      new VisitorsBridge(Lists.newArrayList(myVisitor),
      Lists.newArrayList(new File("target/test-classes")), null));
    assertThat(myVisitor.classes).isEqualTo(2);
  }

  private static class MyVisitor extends IssuableSubscriptionVisitor {
    int classes;

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
      classes++;
    }
  }
}
