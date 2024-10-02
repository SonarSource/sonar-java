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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

@interface AnotherAnnotation {
}

// NullMarked at the module level
@interface MyCheckForNull {
}

// NullMarked at the module level
abstract class PrimitivesMarkedNullableCheckSample {

  abstract int getInt1();

  @AnotherAnnotation
  abstract int getInt2();

  @NullUnmarked
  protected abstract boolean isBool(); // Noncompliant {{"@NullUnmarked" annotation should not be used on primitive types}} [[quickfixes=qf1]]
//                   ^^^^^^^
  // fix@qf1 {{Remove "@NullUnmarked"}}
  // edit@qf1 [[sl=-1;el=+0;sc=3;ec=3]] {{}}

  @NullUnmarked
  public double getDouble1() { return 0.0; } // Noncompliant {{"@NullUnmarked" annotation should not be used on primitive types}}

  public double getDouble2() { return 0.0; }

  @MyCheckForNull
  public double getDouble2_1() { return 0.0; } // Compliant, Nullable meta annotation are not taken into account

  @NullMarked
  public double getDouble2_2() { return 0.0; } // Compliant, Nonnull is useless, but is accepted as it can be added for consistency

  @NullUnmarked
  public Double getDouble3() { return 0.0; }

  @NullUnmarked
  public Double getDouble4() { return 0.0; }

  @NullUnmarked
  public Object getObj0() { return null; }

  @NullUnmarked
  public Object getObj1() { return null; }

  public Object getObj2() { return null; }

  protected abstract @NullUnmarked boolean isBool2(); // Noncompliant {{"@NullUnmarked" annotation should not be used on primitive types}} [[quickfixes=qf3]]
//                                 ^^^^^^^
  // fix@qf3 {{Remove "@NullUnmarked"}}
  // edit@qf3 [[sc=22;ec=36]] {{}}

  @NullUnmarked
  Object containsAnonymousClass() {
    return new PrimitivesMarkedNullableCheckParent() {
      int getInt0() {
        return 0;
      }
    };
  }

}

abstract class PrimitivesMarkedNullableCheckNullMarkedChild extends PrimitivesMarkedNullableCheckParent {

  abstract int getInt0(); // Compliant, not directly marked as CheckForNull, an issue will be on the parent if needed.

}
