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

import java.util.List;

public class TypeParameters<T, S extends CharSequence> extends ParametrizedExtend<ParametrizedExtend<T>> implements ParametrizedInterface1<S>, ParametrizedInterface2<S> {

  public T field;
  public T fun(T t) {
    return null;
  }

  public <W extends Exception> List<W> foo(W[] w, int a, long b) throws W {
    return null;
  }
}

class ParametrizedExtend<S> {
  S parentField;
  class InnerClass {
     S innerMethod(){
       return null;
     }
  }
}

interface ParametrizedInterface1<U> {}
interface ParametrizedInterface2<V> {}

class A {
  <T> void foo() {
    class B{
      void bar() {
        class C {
          T qix(){ return null;}
        }
      }
    }

  }
}