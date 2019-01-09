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
package org.sonar.java.bytecode.cfg;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BytecodeListingParserTest {

  private static void assertCFG(String listing, String expected) {
    BytecodeCFG cfg = BytecodeListingParser.getCFG(listing);
    StringBuilder sb = new StringBuilder();
    cfg.blocks.forEach(b -> sb.append(b.printBlock()));
    assertThat(sb.toString()).isEqualTo(expected);
  }

  @Test
  public void test_no_operand() throws Exception {
    assertCFG("L0\n" +
      "ICONST_0\n",
      "B0(Exit)\n" +
        "B1\n" +
        "0: ICONST_0\n" +
        "Jumps to: B0 \n");
  }

  @Test
  public void copy_paste_from_asm_output() throws Exception {
    String listing = "L0\n" +
      "    LINENUMBER 465 L0\n" +
      "    ICONST_0\n" +
      "    ISTORE 1\n" +
      "   L1\n" +
      "    LINENUMBER 466 L1\n" +
      "   FRAME APPEND [I]\n" +
      "    ILOAD 0\n" +
      "    GETSTATIC org/sonar/java/bytecode/cfg/BytecodeCFGBuilderTest.sizeTable : [I\n" +
      "    ILOAD 1\n" +
      "    IALOAD\n" +
      "    IF_ICMPGT L2\n" +
      "   L3\n" +
      "    LINENUMBER 467 L3\n" +
      "    ILOAD 1\n" +
      "    ICONST_1\n" +
      "    IADD\n" +
      "    IRETURN\n" +
      "   L2\n" +
      "    LINENUMBER 465 L2\n" +
      "   FRAME SAME\n" +
      "    IINC 1 1\n" +
      "    GOTO L1\n" +
      "   L4\n" +
      "    LOCALVARIABLE i I L1 L4 1\n" +
      "    LOCALVARIABLE x I L0 L4 0\n" +
      "    MAXSTACK = 3\n" +
      "    MAXLOCALS = 2";
    String expected = "B0(Exit)\n" +
      "B1\n" +
      "0: ICONST_0\n" +
      "1: ISTORE\n" +
      "Jumps to: B2 \n" +
      "B2\n" +
      "0: ILOAD\n" +
      "1: GETSTATIC\n" +
      "2: ILOAD\n" +
      "3: IALOAD\n" +
      "IF_ICMPGT Jumps to: B3(true) B4(false) \n" +
      "B3\n" +
      "0: IINC\n" +
      "GOTO Jumps to: B2 \n" +
      "B4\n" +
      "0: ILOAD\n" +
      "1: ICONST_1\n" +
      "2: IADD\n" +
      "3: IRETURN\n" +
      "Jumps to: B0 \n";
    assertCFG(listing, expected);
  }
}
