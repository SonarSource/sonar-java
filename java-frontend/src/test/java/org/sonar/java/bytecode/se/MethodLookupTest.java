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
package org.sonar.java.bytecode.se;

import com.google.common.collect.Lists;
import java.io.File;
import org.junit.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodLookupTest {

  private static final String TESTCLASS = "org.sonar.java.bytecode.cfg.testdata.MethodLookupTestData#";
  private static final MethodLookup.LookupMethodVisitor NOP_VISITOR = new MethodLookup.LookupMethodVisitor();

  SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));

  @Test
  public void lookup_should_contain_throws_declaration() {
    MethodLookup lookup = MethodLookup.lookup(TESTCLASS + "throwing()V", squidClassLoader, NOP_VISITOR);
    assertThat(lookup.declaredExceptions).containsExactly("java.io.IOException");
  }

  @Test
  public void lookup_method_from_superclass() {
    MethodLookup lookup = MethodLookup.lookup(TESTCLASS + "methodDefinedInSuperClass()V", squidClassLoader, NOP_VISITOR);
    assertThat(lookup.declaredExceptions).containsExactly("java.util.NoSuchElementException");

    lookup = MethodLookup.lookup(TESTCLASS + "methodDefinedInSuperClass2()V", squidClassLoader, NOP_VISITOR);
    assertThat(lookup.declaredExceptions).containsExactly("java.lang.IllegalArgumentException");

    lookup = MethodLookup.lookup(TESTCLASS + "ifaceMethod()V", squidClassLoader, NOP_VISITOR);
    assertThat(lookup.declaredExceptions).containsExactly("java.io.FileNotFoundException");

    lookup = MethodLookup.lookup(TESTCLASS + "ifaceMethod2()V", squidClassLoader, NOP_VISITOR);
    assertThat(lookup.declaredExceptions).containsExactly("java.lang.UnsupportedOperationException");
  }
}
