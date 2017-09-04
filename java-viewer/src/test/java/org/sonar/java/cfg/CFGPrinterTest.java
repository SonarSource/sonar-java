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
package org.sonar.java.cfg;

import org.junit.Test;
import org.sonar.java.viewer.Viewer;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

public class CFGPrinterTest {

  @Test
  public void private_constructor() throws Exception {
    Constructor<CFGPrinter> constructor = CFGPrinter.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void simple_code() {
    String code = "class A {"
      + "  int foo(boolean a) {"
      + "    if (a) {"
      + "      return 42;"
      + "    }"
      + "    return 21;"
      + "  }"
      + "}";
    Viewer.Base base = new Viewer.Base(code);

    assertThat(CFGPrinter.toString(base.cfgFirstMethod)).isEqualTo(
      "Starts at B3\n"
        + "\n"
        + "B3\n0:\tIDENTIFIER                          \ta\n"
        + "T:\tIF_STATEMENT                        \tif ( a )\n"
        + "\tjumps to: B1(false) B2(true)\n"
        + "\n"
        + "B2\n"
        + "0:\tINT_LITERAL                         \t42\n"
        + "T:\tRETURN_STATEMENT                    \treturn 42 ;\n"
        + "\tjumps to: B0(exit)\n"
        + "\n"
        + "B1\n"
        + "0:\tINT_LITERAL                         \t21\n"
        + "T:\tRETURN_STATEMENT                    \treturn 21 ;\n"
        + "\tjumps to: B0(exit)\n"
        + "\n"
        + "B0 (Exit):\n"
        + "\n");
  }

  @Test
  public void code_with_exception() {
    String code = "abstract class A {"
      + "  int foo() {"
      + "    int result = 42;"
      + "    try {"
      + "      result = getValue();"
      + "    } catch (Exception e) {"
      + "      result = -1;"
      + "    }"
      + "    return result;"
      + "  }"
      + "  abstract int getValue() throws IllegalStateException;"
      + "}";

    Viewer.Base base = new Viewer.Base(code);

    assertThat(CFGPrinter.toString(base.cfgFirstMethod)).isEqualTo(
      "Starts at B5\n"
        + "\nB5"
        + "\n0:\tINT_LITERAL                         \t42\n"
        + "1:\tVARIABLE                            \tint result\n"
        + "2:\tTRY_STATEMENT                       \ttry { result = getValue ( ) ; } catch ( Exception e ) { result = - 1 ; }\n"
        + "\tjumps to: B4\n"
        + "\n"
        + "B4\n"
        + "0:\tIDENTIFIER                          \tgetValue\n"
        + "1:\tMETHOD_INVOCATION                   \tgetValue ( )\n"
        + "\tjumps to: B2\n"
        + "\texceptions to: B0 B3\n"
        + "\n"
        + "B3\n"
        + "0:\tVARIABLE                            \tException e\n"
        + "1:\tINT_LITERAL                         \t1\n"
        + "2:\tUNARY_MINUS                         \t- 1\n"
        + "3:\tASSIGNMENT                          \tresult = - 1\n"
        + "\tjumps to: B1\n"
        + "\n"
        + "B2\n"
        + "0:\tASSIGNMENT                          \tresult = getValue ( )\n"
        + "\tjumps to: B1\n"
        + "\n"
        + "B1\n"
        + "0:\tIDENTIFIER                          \tresult\n"
        + "T:\tRETURN_STATEMENT                    \treturn result ;\n"
        + "\tjumps to: B0(exit)\n"
        + "\n"
        + "B0 (Exit):\n"
        + "\n");
  }

  @Test
  public void member_select() {
    String code = "class A {"
      + "  int foo() {"
      + "    return this.a + this.getValue().a;"
      + "  }"
      + "}";

    Viewer.Base base = new Viewer.Base(code);

    assertThat(CFGPrinter.toString(base.cfgFirstMethod)).isEqualTo(
      "Starts at B1\n"
        + "\n"
        + "B1\n"
        + "0:\tIDENTIFIER                          \tthis\n"
        + "1:\tMEMBER_SELECT                       \tthis . a\n"
        + "2:\tIDENTIFIER                          \tthis\n"
        + "3:\tMETHOD_INVOCATION                   \tthis . getValue ( )\n"
        + "4:\tMEMBER_SELECT                       \ta\n"
        + "5:\tPLUS                                \tthis . a + a\n"
        + "T:\tRETURN_STATEMENT                    \treturn this . a + a ;\n"
        + "\tjumps to: B0(exit)\n"
        + "\n"
        + "B0 (Exit):\n"
        + "\n");
  }

  @Test
  public void new_class_tree() {
    String code = "class A {"
      + "  void foo() {"
      + "    A a = new A();"
      + "  }"
      + "}";

    Viewer.Base base = new Viewer.Base(code);

    assertThat(CFGPrinter.toString(base.cfgFirstMethod)).isEqualTo(
      "Starts at B1\n"
        + "\n"
        + "B1\n"
        + "0:\tNEW_CLASS                           \tnew A ( )\n"
        + "1:\tVARIABLE                            \tA a\n"
        + "\tjumps to: B0\n"
        + "\n"
        + "B0 (Exit):\n"
        + "\n");
  }
}
