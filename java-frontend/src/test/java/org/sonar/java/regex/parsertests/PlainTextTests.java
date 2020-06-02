package org.sonar.java.regex.parsertests;

import org.junit.jupiter.api.Test;

import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertPlainCharacter;

class PlainTextTests {

  @Test
  void testSimpleCharacter() {
    assertPlainCharacter('x', "x");
    assertPlainCharacter(' ', " ");
  }

  @Test
  void testSimpleEscapeSequences() {
    assertPlainCharacter('\b', "\\b");
    assertPlainCharacter('\t', "\\t");
    assertPlainCharacter('\n', "\\n");
    assertPlainCharacter('\f', "\\f");
    assertPlainCharacter('\r', "\\r");
    assertPlainCharacter('\\', "\\\\");
    assertPlainCharacter('"', "\\\"");
  }

  @Test
  void octalEscapeSequences() {
    assertPlainCharacter('\n', "\\012");
    assertPlainCharacter('\n', "\\12");
    assertPlainCharacter('D', "\\104");
  }

  @Test
  void unicodeEscapeSequences() {
    assertPlainCharacter('\t', "\\u0009");
    assertPlainCharacter('D', "\\u0044");
    assertPlainCharacter('ö', "\\u00F6");
  }

}
