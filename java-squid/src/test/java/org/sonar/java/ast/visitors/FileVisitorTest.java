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

import com.google.common.base.Charsets;
import org.junit.Test;
import org.sonar.java.ast.parser.ActionParser2;
import org.sonar.java.ast.parser.JavaParser;

import static org.fest.assertions.Assertions.assertThat;

public class FileVisitorTest {

  @Test
  public void getPackageKey() {
    ActionParser2 parser = JavaParser.createParser(Charsets.UTF_8, false);
    assertThat(FileVisitor.getPackageKey(parser.parse("public class Foo {}"))).isEqualTo("");
    assertThat(FileVisitor.getPackageKey(parser.parse("package foo;"))).isEqualTo("foo");
    assertThat(FileVisitor.getPackageKey(parser.parse("  /* comments */ package foo;"))).isEqualTo("foo");
    assertThat(FileVisitor.getPackageKey(parser.parse("package foo.bar;"))).isEqualTo("foo/bar");
    assertThat(FileVisitor.getPackageKey(parser.parse("/* comments */  @NonNull package foo.bar.baz; public class Foo {}"))).isEqualTo("foo/bar/baz");

    // TODO Should be default package?
    assertThat(FileVisitor.getPackageKey(parser.parse(""))).isEqualTo(FileVisitor.UNRESOLVED_PACKAGE);
  }

}
