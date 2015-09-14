/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks.targets;

public class UnusedPrivateMethod {

  public UnusedPrivateMethod(String s) {
    init();
  }

  private void init() {
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    // this method should not be considered as dead code, see Serializable contract
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    // this method should not be considered as dead code, see Serializable contract
  }

  private Object writeReplace() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
    return null;
  }

  private Object readResolve() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
    return null;
  }

  private void readObjectNoData() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
  }

  @SuppressWarnings("unused")
  private int unusedPrivateMethod() {
    return 1;
  }
  private int unusedPrivateMethod(int a, String s) {
    return 1;
  }

  public enum Attribute {
    ID("plop", "foo", true);

    Attribute(String prettyName, String type, boolean hidden) {
    }

    Attribute(String prettyName, String[][] martrix, int i) {
    }

  }

  private class A {
    private A(){}
    private <T> T foo(T t) {
      return null;
    }

    public void bar() {
      foo("");
    }
  }

}
