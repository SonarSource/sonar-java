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
package org.sonar.java.bytecode.cfg.testdata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Test data for {@link org.sonar.java.bytecode.se.MethodLookup}
 */
public class MethodLookupTestData extends SuperClass implements Iface {

  void throwing() throws IOException {}

}


class SuperClass extends SuperClass2 {
  final void methodDefinedInSuperClass() throws NoSuchElementException {
    return;
  }
}

class SuperClass2 implements Iface2 {
  final void methodDefinedInSuperClass2() throws IllegalArgumentException {
    return;
  }
}

interface Iface {
  default void ifaceMethod() throws FileNotFoundException {}
}

interface Iface2 {
  default void ifaceMethod2() throws UnsupportedOperationException {}
}

