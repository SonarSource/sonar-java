/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
 * long with this program; if not, see https://sonarsource.com/license/ssal/
 */
class ObjectFinalizeOverriddenCheckSample {
  class Foo {

    @Override
    protected void finalize() throws Throwable { // Noncompliant {{Do not override the Object.finalize() method.}}
//                 ^^^^^^^^
    }

    public void foo() { // Compliant
    }

    @Override
    protected boolean finalize() { // Compliant
    }

  }

  class CompliantFoo {

    @Override
    protected final void finalize() throws Throwable { // Compliant
    }

    public void bar() { // Compliant
    }

    @Override
    protected boolean finalize() { // Compliant
      doSomething();
    }
  }

  class NoncompliantFoo2 {
    @Override
    protected final void finalize() throws Throwable { // Noncompliant
      doSomething();
    }

  }

  private void doSomething() {
    // does something
  }

}
