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
  private int unusedPrivateMethod() { // Noncompliant {{Private method 'unusedPrivateMethod' is never used.}}
    return 1;
  }

  private int unusedPrivateMethod(int a, String s) { // Noncompliant {{Private method 'unusedPrivateMethod' is never used.}}
    return 1;
  }

  private void varargs(String first, Object... objects) {
  }

  private void varargs(String... strings) { // Noncompliant {{Private method 'varargs' is never used.}}
    // false positive. see http://jira.sonarsource.com/browse/SONARJAVA-1149
  }

  public void usage() {
    varargs("", new Object(), new Object());
    varargs("", "", ""); // should resolve to 'String...' and not 'String, Object...'
  }

  public enum Attribute {
    ID("plop", "foo", true);

    Attribute(String prettyName, String type, boolean hidden) {
    }

    Attribute(String prettyName, String[][] martrix, int i) { // Noncompliant {{Private constructor 'Attribute' is never used.}}
    }

  }

  private class A {
    private A() { // Noncompliant {{Private constructor 'A' is never used.}}
    }

    private <T> T foo(T t) {
      return null;
    }

    public void bar() {
      foo("");
    }
  }

}

class OuterClass {

  private static <T> void genericMethod(T argument) {
    new Object() {
      private void unused() { // Noncompliant {{Private method 'unused' is never used.}}
      }
    };
  }

  private static <T extends java.util.List<String>> void complexGenericMethod(T argument) { // Compliant, false negative
    // currently skipped. see http://jira.sonarsource.com/browse/SONARJAVA-1150
  }

  class NestedGenericClass<T> {
    private NestedGenericClass(T argument) { // Compliant
    }

    private void genericMethod(T argument) { // Compliant
    }
  }

  class ComplexNestedGenericClass<T extends java.util.Collection<Object>> {
    private ComplexNestedGenericClass(T argument) { // Noncompliant {{Private constructor 'ComplexNestedGenericClass' is never used.}}
      // false positive. see http://jira.sonarsource.com/browse/SONARJAVA-1150
    }

    private void genericMethod(T argument) { // Noncompliant {{Private method 'genericMethod' is never used.}}
      // false positive. see http://jira.sonarsource.com/browse/SONARJAVA-1150
    }
  }

  public void test() {
    genericMethod("string");
    complexGenericMethod(new java.util.ArrayList<String>());
    new NestedGenericClass<java.util.List<Object>>(new java.util.ArrayList<Object>()).genericMethod(new java.util.LinkedList<Object>());
    new ComplexNestedGenericClass<java.util.List<Object>>(new java.util.ArrayList<Object>()).genericMethod(new java.util.LinkedList<Object>());
  }

}
