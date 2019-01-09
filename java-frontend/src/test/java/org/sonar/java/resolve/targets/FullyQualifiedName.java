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
package org.sonar.java.resolve.targets;

public class FullyQualifiedName /* org.sonar.java.resolve.targets.FullyQualifiedName */ {

  Object objA = new Object() /* org.sonar.java.resolve.targets.FullyQualifiedName$1 */ {
    Object objB = new Object() /* org.sonar.java.resolve.targets.FullyQualifiedName$1$1 */ {
    };
  };
  class Inner /* org.sonar.java.resolve.targets.FullyQualifiedName$Inner */ {
    class Inner2 /* org.sonar.java.resolve.targets.FullyQualifiedName$Inner$Inner2 */ {
      Object objC = new Object() /* org.sonar.java.resolve.targets.FullyQualifiedName$Inner$Inner2$1 */ {
      };
    }
  }
  public void meth1() {
    class class11 /* org.sonar.java.resolve.targets.FullyQualifiedName$1class11 */ {
      void foo() {
        Object objD = new Object() /* org.sonar.java.resolve.targets.FullyQualifiedName$1class11$1 */ {
        };
      }
    }
    class class22 /* org.sonar.java.resolve.targets.FullyQualifiedName$1class22 */ {
    }
  }
  public void meth1(int a) {
    class class21 /* org.sonar.java.resolve.targets.FullyQualifiedName$1class21 */ {
    }
    class class22 /* org.sonar.java.resolve.targets.FullyQualifiedName$2class22 */ {
    }
  }
  public void other(int a) {
    class class21 /* org.sonar.java.resolve.targets.FullyQualifiedName$2class21 */ {
    }
    class class22 /* org.sonar.java.resolve.targets.FullyQualifiedName$3class22 */ {
    }
  }
  interface A /* org.sonar.java.resolve.targets.FullyQualifiedName$A */ {
  }
  class ParametrizedExtend<S> /* org.sonar.java.resolve.targets.FullyQualifiedName$ParametrizedExtend */ {
    S parentField;
    class InnerClass /* org.sonar.java.resolve.targets.FullyQualifiedName$ParametrizedExtend$InnerClass */ {
      S innerMethod(){
        return null;
      }
    }
  }
}
