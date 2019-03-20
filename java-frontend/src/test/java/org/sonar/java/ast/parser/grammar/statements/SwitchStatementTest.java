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
package org.sonar.java.ast.parser.grammar.statements;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class SwitchStatementTest {

  @Test
  public void okSwitch() {
    assertThat(JavaLexer.SWITCH_STATEMENT)
      .matches("switch (foo) {}")
      .matches("switch (foo) { case 0: break; }")
      .matches("switch (foo) { case 0: break; default: break; }");
  }

  @Test
  public void switch_in_compilation_unit_context() {
    assertThat(JavaLexer.COMPILATION_UNIT)
      .matches("class A { void f() { boolean r = switch(x) { case A -> true; default -> false; }; } }");
  }

  @Test
  public void switch_statement_with_traditional_control_flow() {
    assertThat(JavaLexer.SWITCH_STATEMENT)
      .matches("" +
        "switch (i) {" +
        "  case 1: print('A'); break;" +
        "  case 2: print('B'); break;" +
        "  default: break;" +
        "}")
      // even traditional control flow can now support several "case" expressions
      .matches("" +
        "switch (i) {" +
        "  case 1, 2: print('A'); break;" +
        "  default: break;" +
        "}");
  }

  @Test
  public void switch_statement_with_simplified_control_flow() {
    assertThat(JavaLexer.SWITCH_STATEMENT)
      .matches("" +
        "switch (i) {" +
        "  case 1 -> print('A');" +
        "  case 2 -> { if (r == 0) print('A'); else print('B'); }" +
        "  default -> {}" +
        "}")
      .matches("" +
        "switch (i) {" +
        "  case ENUM1 -> print('A');" +
        "  case ENUM2 -> { if (r == 0) print('A'); else print('B'); }" +
        "  default -> {}" +
        "}")
      // cast expression should not be confuse with lambda
      .matches("switch (i) { case (char)C -> print('C'); default -> print('D'); }")
      // Limitation: we confuse the "case with arrow" and "lambda at the end of conditional expression" in the following valid java code:
      .notMatches("switch (i) { case true ? 1 : C -> print('C'); default -> print('B'); }")
      // Our parser is more tolerant than java 12, we wrongly accept:
      // empty statement
      .matches("switch (i) { case 1 -> ; }")
      // several statements
      .matches("switch (i) { case 1 -> i++; j++; }")
      // break
      .matches("switch (i) { case 1 -> break; default -> break; }")
      // not a block, expression or throw
      .matches("switch (i) { case 1 -> if (r == 0) print('A'); else print('B'); }");
  }

  @Test
  public void fall_through_with_traditional_control_flow() {
    assertThat(JavaLexer.SWITCH_STATEMENT)
      .matches("" +
        "switch (i) {" +
        "  case 1:" +
        "  case 2: print('A'); break;" +
        "}");
  }

  @Test
  public void fall_through_replacement_with_simplified_control_flow() {
    assertThat(JavaLexer.SWITCH_STATEMENT)
      .matches("" +
        "switch (i) {" +
        "  case 1, 2 -> print('A');" +
        "}")
      // Our parser is more tolerant than java 12, we wrongly accept fall through:
      .matches("switch (i) { case 1 -> case 2 -> print('A'); }");
  }

  @Test
  public void switch_expression() {
    assertThat(JavaLexer.STATEMENT)
      // with traditional control flow
      .matches("" +
        "r = switch (i) {" +
        "  case 1: break 10;" +
        "  case 2: break 20;" +
        "  default: break 0;" +
        "};")
      // with simplified control flow
      .matches("" +
        "r = switch (i) {" +
        "  case 1 -> 10;" +
        "  case 2 -> 20;" +
        "  default -> 0;" +
        "};")
      // with simplified control flow but still using break to return values
      .matches("" +
        "r = switch (i) {" +
        "  case 1 -> { break 10; }" +
        "  case 2 -> { print('A'); break 20; }" +
        "  default -> { break 20; }" +
        "};")
      // Our parser is more tolerant than java 12, we wrongly accept break without block
      .matches("r = switch (i) { case 1 -> break 10; default -> break 20; };")
      // a statement starting with switch is not considered as switch expression without parentheses
      .notMatches("switch (i) { default -> \"\"; }.length();")
      .matches("(switch (i) { default -> \"\"; }).length();");
  }

  @Test
  public void switch_with_simplified_control_flow_should_accept_throw_statement() {
    assertThat(JavaLexer.STATEMENT)
      // switch statement
      .matches("" +
        "switch (i) {" +
        "  case 1 -> print('A');" +
        "  case 2 -> throw new IllegalStateException(\"2\");" +
        "  default -> print('A');" +
        "}")
      // switch expression
      .matches("" +
        "r = switch (i) {" +
        "  case 1 -> 10;" +
        "  case 2 -> throw new IllegalStateException(\"2\");" +
        "  default -> throw new IllegalStateException(\"default\");" +
        "};");
  }

  @Test
  public void variable_declaration_in_a_case() {
    assertThat(JavaLexer.STATEMENT)
      // with traditional control flow
      .matches("" +
        "switch (i) {" +
        "  case 1: char a = 'A'; print(a); break;" +
        "}")
      // with simplified control flow
      .matches("" +
        "switch (i) {" +
        "  case 1 -> { char a = 'A'; print(a); }" +
        "}")
      // Our parser is more tolerant than java 12, we wrongly accept variable declaration without block
      .matches("" +
        "switch (i) {" +
        "  case 1 -> char a = 'A';" +
        "}");
  }

  @Test
  public void switch_statement_with_semicolon() {
    assertThat(JavaLexer.STATEMENT)
      .matches("switch (i) { case 1 -> print('A'); }")
      // adding a semicolon is like adding an extra empty statement
      .notMatches("switch (i) { case 1 -> print('A'); };");
  }

  @Test
  public void statement_with_switch_expression_and_semicolon_at_the_end() {
    assertThat(JavaLexer.STATEMENT)
      .matches("r = switch (i) { case 1 -> 10; default -> 0; };")
      // missing semicolon
      .notMatches("r = switch (i) { case 1 -> 10; default -> 0; }");
  }

  @Test
  public void break_to_label_and_value_break() {
    assertThat(JavaLexer.STATEMENT)
      // break to label n1 and n2
      .matches("" +
        "n1:" +
        "while(i < 0) {" +
        "  n2:" +
        "  switch (i) {" +
        "    case 1: print('A'); break n1;" +
        "    case 2: print('B'); break n2;" +
        "    default: break n1;" +
        "  }" +
        "}")
      // value break n1 and n2
      .matches("" +
        "{" +
        "  int n1 = 2;" +
        "  while(i < 0) {" +
        "    int n2 = 3;" +
        "    r = switch (i) {" +
        "      case 1: print('A'); break n1;" +
        "      case 2: print('B'); break n2;" +
        "      default: break n1;" +
        "    };" +
        "  }" +
        "}")
      // switch statement has no ambiguous reference to 'n1', it always resolve to labels
      // with traditional control flow
      .matches("" +
        "{" +
        "  int n1 = 3;" +
        "n1:" +
        "  switch (i) {" +
        "    case 1: break n1;" +
        "    default: break n1;" +
        "  };" +
        "}")
      // or with simplified control flow
      .matches("" +
        "{" +
        "  int n1 = 3;" +
        "n1:" +
        "  switch (i) {" +
        "    case 1 -> { break n1; }" +
        "    default -> { break n1; }" +
        "  };" +
        "}")
      // but switch expression can be ambiguous between label and value, even if the following statement
      // is syntactically valid, it is semantically ambiguous and the compiler will produce the following
      // error: ambiguous reference to 'n1'
      .matches("" +
        "{" +
        "  int n1 = 3;" +
        "n1:" +
        "  r = switch (i) {" +
        "    case 1: break n1;" +
        "    default: break n1;" +
        "  };" +
        "}")
      // Our parser is more tolerant than java 12, we wrongly accept value break in switch statement
      .matches("switch (i) { case 1: break 7; }");
  }

  @Test
  public void switch_and_return_statement() {
    assertThat(JavaLexer.STATEMENT)
      // returning values with traditional control flow
      .matches("" +
        "switch (i) {" +
        "  case 1: return 10;" +
        "  case 2: return 20;" +
        "  default: return 0;" +
        "}")
      // returning values with simplified control flow
      .matches("" +
        "return switch (i) {" +
        "  case 1 -> 10;" +
        "  case 2 -> 20;" +
        "  default -> 0;" +
        "};")
      // using return inside simplified control flow
      .matches("" +
        "switch (i) {" +
        "  case 1 -> { return 10; }" +
        "  case 2 -> { return 20; }" +
        "  default -> { return 0; }" +
        "}")
      // Our parser is more tolerant than java 12, we wrongly accept return without block
      .matches("" +
        "switch (i) {" +
        " case 1 -> return 10;" +
        " case 2 -> return 20;" +
        " default -> return 0;" +
        "}");
  }

}
