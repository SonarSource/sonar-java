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
package org.sonar.java.resolve.targets;

import org.sonar.java.resolve.targets.subpackage.MethodSymbolsParentTypes;

public class MethodSymbols {

  class A1 implements MethodSymbolsParentTypes.Interface {
    void foo(){};
    public int methodOverriden(int i) { //Overriden
      return 0;
    }
  }
  class A2 extends MethodSymbolsParentTypes.A implements MethodSymbolsParentTypes.SuperInterface{
    int method(int j){
      return 1;
    }

    public int bar(String str) {
      return 0;
    }

    public int methodOverriden(int i) {//Overriden
      Object obj = new MethodSymbolsParentTypes.ForAnonymousClass() {
        public int methodUndecidable() { //Not found as overidden for anonymous classes
          return 0;
        }
      };
      return 0;
    }
  }

  class A3 extends org.sonar.java.resolve.targets.subpackage.MethodSymbolsParentTypes.A {
    //
    public int bar(String str) {
      return super.bar(str);
    }
  }

  class Parent {
    private void method(){}
  }

  class A4 extends Parent {
    void method(){}
  }

}
