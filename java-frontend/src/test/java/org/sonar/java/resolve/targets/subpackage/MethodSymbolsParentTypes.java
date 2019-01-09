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
package org.sonar.java.resolve.targets.subpackage;

public class MethodSymbolsParentTypes {

  public static interface SuperInterface {
    int methodOverriden(int i);
  }
  public static interface Interface<T> extends SuperInterface {
    void foo(T t);
  }

  public static class A {
    int method(int j){return 1;}
    public int bar;
    public int bar() {return 1;}
    public int bar(int i) {return 1;}
    public int bar(String str) {return 1;}
    public int bar(int i, int j) {return 1;}
  }

  public static interface ForAnonymousClass {
    int methodUndecidable();
  }


}
