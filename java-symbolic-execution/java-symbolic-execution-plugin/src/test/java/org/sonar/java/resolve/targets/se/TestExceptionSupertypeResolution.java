/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.resolve.targets.se;

abstract class TestExceptionSupertypeResolution {

  class Foo extends Exception { }

  Exception myException = new Exception() { };

  private void throwException() throws Exception {
    class Bar extends Exception { }

    if (test()) {
      throw myException;
    }
    if (test()) {
      throw new Exception() {};
    }
    if (test()) {
      throw new Bar();
    }
    if (test()) {
      throw new Bar() {};
    }
    if (test()) {
      throw new Foo();
    }
  }


  void call() throws Exception {
    throwException();
  }

  abstract boolean test();
}
