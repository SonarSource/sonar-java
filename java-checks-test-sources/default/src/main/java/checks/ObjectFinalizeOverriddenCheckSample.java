/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

package checks;

class ObjectFinalizeOverriddenCheckSample {
  class Foo {

    @Override
    protected void finalize() throws Throwable { // Noncompliant {{Do not override the Object.finalize() method.}}
//                 ^^^^^^^^
    }

    public void foo() { // Compliant
    }
  }

  class CompliantFoo {

    @Override
    protected final void finalize() throws Throwable { // Compliant
    }

    public void bar() { // Compliant
    }
  }

  class NoncompliantFoo2 {
    @Override
    protected final void finalize() throws Throwable { // Noncompliant
      doSomething();
    }

  }

  class NoncompliantFoo3 {
    @Override
    protected void finalize() { // Noncompliant
      doSomething();
    }
  }

  private void doSomething() {
    // does something
  }

}
