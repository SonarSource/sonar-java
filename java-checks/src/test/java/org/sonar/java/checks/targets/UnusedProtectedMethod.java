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

public class UnusedProtectedMethod extends AbstractUnusedProtectedMethod {

  public UnusedProtectedMethod() {
    init();
  }

  protected void init() {
  }

  protected void unusedProtectedMethod() {
  }

  @Override
  protected void abstractProtectedMethod() {
    // this method should not be considered as dead code
  }

  protected void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    // this method should not be considered as dead code, see Serializable contract
  }

  protected void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    // this method should not be considered as dead code, see Serializable contract
  }

  protected Object writeReplace() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
    return null;
  }

  protected Object readResolve() throws java.io.ObjectStreamException {
    // this method should not be considered as dead code, see Serializable contract
    return null;
  }

}

abstract class AbstractUnusedProtectedMethod {

  protected abstract void abstractProtectedMethod();

}
