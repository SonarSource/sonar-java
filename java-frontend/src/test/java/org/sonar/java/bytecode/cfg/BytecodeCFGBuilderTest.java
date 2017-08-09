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
package org.sonar.java.bytecode.cfg;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class BytecodeCFGBuilderTest {

  @Test
  public void test() throws Exception {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/cfg/BytecodeCFGBuilderTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.MethodSymbol symbol = ((MethodTree) ((ClassTree) ((ClassTree) tree.types().get(0)).members().get(1)).members().get(0)).symbol();
    BytecodeCFGBuilder.BytecodeCFG cfg = BytecodeCFGBuilder.buildCFG(symbol, squidClassLoader);
    StringBuilder sb = new StringBuilder();
    cfg.blocks.forEach(b-> sb.append(b.printBlock()));
    assertThat(sb.toString()).isEqualTo(
      "B0(Exit)\n" +
      "B1\n" +
      "0: LDC\n" +
      "1: ARETURN\n" +
      "Jumps to: B0 \n" +
      "B2\n" +
      "0: ALOAD\n" +
      "Jumps to: B3 B4 \n" +
      "B3\n" +
      "0: LDC\n" +
      "1: ARETURN\n" +
      "Jumps to: B0 \n" +
      "B4\n" +
      "0: ACONST_NULL\n" +
      "1: ARETURN\n" +
      "Jumps to: B0 \n");
  }

  static class InnerClass {
    Object fun(boolean a, Object b) {
      if (a) {
        if (b == null) {
          return null;
        }
        return "";
      } else {
        return "not a";
      }
    }
  }

}
