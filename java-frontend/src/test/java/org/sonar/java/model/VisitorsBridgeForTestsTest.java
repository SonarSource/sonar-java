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
package org.sonar.java.model;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class VisitorsBridgeForTestsTest {

  @Test
  public void test_semantic_disabled() {
    Tree parse = JavaParser.createParser(Charset.defaultCharset()).parse("class A{}");
    VisitorsBridgeForTests visitorsBridgeForTests = new VisitorsBridgeForTests(Collections.singletonList(new DummyVisitor()));
    visitorsBridgeForTests.setCurrentFile(new File("dummy.java"));
    visitorsBridgeForTests.visitFile(parse);
    assertThat(visitorsBridgeForTests.lastCreatedTestContext().getSemanticModel()).isNull();

    parse = JavaParser.createParser(Charset.defaultCharset()).parse("class A{}");
    visitorsBridgeForTests = new VisitorsBridgeForTests(new DummyVisitor());
    visitorsBridgeForTests.setCurrentFile(new File("dummy.java"));
    visitorsBridgeForTests.visitFile(parse);
    assertThat(visitorsBridgeForTests.lastCreatedTestContext().getSemanticModel()).isNotNull();

  }

  private static class DummyVisitor implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
    }
  }
}