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
package org.sonar.java.model.pattern;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordPatternTreeImplTest {

  @Test
  void test_pattern_tokens(){
    var code =
      """
      class Foo {
      
        record Bar(int x, String y){}
      
        void foo(Bar b){
          switch (b) {
            case Bar(var x, var y) -> {}
          }
        }
      }
      """;
    var cu = JParserTestUtils.parse(code);
    var clazz = (ClassTree) cu.types().get(0);
    var method = ((MethodTree) clazz.members().get(1));
    var switchh = (SwitchStatementTree) method.block().body().get(0);
    var caze = switchh.cases().get(0);
    var pattern = (RecordPatternTreeImpl) caze.labels().get(0).expressions().get(0);
    var firstToken = pattern.firstToken();
    var lastToken = pattern.lastToken();
    var start = firstToken.range().start();
    var end = lastToken.range().start();
    assertEquals("Bar", firstToken.text());
    assertEquals(")", lastToken.text());
    assertEquals(firstToken.range().end(), pattern.openParenToken().range().start());
    assertEquals(lastToken, pattern.closeParenToken());
    assertEquals(new InternalPosition(7, 12), start);
    assertEquals(new InternalPosition(7, 28), end);
  }

}
