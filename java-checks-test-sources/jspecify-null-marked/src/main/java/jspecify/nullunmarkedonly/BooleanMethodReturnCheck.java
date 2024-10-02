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
package jspecify.nullunmarkedonly;

import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.jspecify.annotations.NullUnmarked;

// NullMarked at the module level
class BooleanMethodReturnCheckJSpecifySampleA {
  public Boolean myMethod() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }

  public Boolean myOtherMethod() {
    return Boolean.TRUE; // Compliant
  }

  BooleanMethodReturnCheckJSpecifySampleA() {
    // constructor (with null return type) are not covered by the rule
    return;
  }

  @Nullable
  public Boolean foo() {
    return null; // Compliant
  }

  @NullUnmarked
  public Boolean bar() {
    return null; // Compliant
  }

  public Boolean foobar() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }
  
}

// NullMarked at the module level
class BooleanMethodReturnCheckJSpecifySampleB {
  private class Boolean {
  }

  public Boolean myMethodFailing() {
    return null; // Compliant
  }

  public java.lang.Boolean myOtherMethod() {
    class BooleanMethodReturnCheckSampleC {
      private java.lang.Boolean myInnerMethod() {
        return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
      }
      private BooleanMethodReturnCheckSampleC foo() {
        return null; // Compliant
      }
    }
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
//         ^^^^
  }

  @CheckForNull
  public java.lang.Boolean myMethod2() {
    return null; // compliant method is annotated with @CheckForNull
  }
}

// NullMarked at the module level
class BooleanMethodReturnCheckJSpecifySampleD {
  public Boolean foo() {
    class BooleanMethodReturnCheckSampleE {
      void bar() {
        return;
      }
    }
    Stream.of("A").forEach(a -> {
      return; // Compliant
    });
    return true;
  }
}
