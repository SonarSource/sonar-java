/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
