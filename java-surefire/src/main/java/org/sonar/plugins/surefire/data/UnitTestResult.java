/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.plugins.surefire.data;

import java.util.UUID;

public final class UnitTestResult {
  public static final String STATUS_OK = "ok";
  public static final String STATUS_ERROR = "error";
  public static final String STATUS_FAILURE = "failure";
  public static final String STATUS_SKIPPED = "skipped";

  private String name;
  private String testSuiteClassName;
  private String status;
  private String stackTrace;
  private String message;
  private long durationMilliseconds = 0L;

  public UnitTestResult() {
    name = UUID.randomUUID().toString();
  }

  public String getName() {
    return name;
  }

  public UnitTestResult setName(String name) {
    this.name = name;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public UnitTestResult setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public UnitTestResult setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public UnitTestResult setMessage(String message) {
    this.message = message;
    return this;
  }

  public long getDurationMilliseconds() {
    return durationMilliseconds;
  }

  public UnitTestResult setDurationMilliseconds(long l) {
    this.durationMilliseconds = l;
    return this;
  }

  public boolean isErrorOrFailure() {
    return STATUS_ERROR.equals(status) || STATUS_FAILURE.equals(status);
  }

  public boolean isError() {
    return STATUS_ERROR.equals(status);
  }

  public UnitTestResult setTestSuiteClassName(String testSuiteClassName) {
    this.testSuiteClassName = testSuiteClassName;
    return this;
  }

  public String getTestSuiteClassName() {
    return testSuiteClassName;
  }
}
