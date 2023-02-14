/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.verifier.internal;

class InternalMockedSonarAPI {

  private final Class<?> clazz;

  InternalMockedSonarAPI() {
    clazz = this.getClass();
  }

  NotSupportedException notSupportedException(String methodSignature) {
    return new NotSupportedException(clazz, methodSignature);
  }

  static final class NotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 6465870479166535810L;
    private static final String EXCEPTION_MESSAGE = "Method unsuported by the rule verifier framework: '%s::%s'";

    private NotSupportedException(Class<?> clazz, String methodSignature) {
      super(String.format(EXCEPTION_MESSAGE, clazz.getSimpleName(), methodSignature));
    }
  }
}
