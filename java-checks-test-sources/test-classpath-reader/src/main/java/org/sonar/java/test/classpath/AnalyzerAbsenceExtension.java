/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.test.classpath;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Turns a failure caused by a missing SemSitter analyzer into a skipped test.
 *
 * <p>The analyzer is optional at build time: without {@code -Dsonar.java.semsitter.executable}
 * (or {@code SEMSITTER_ANALYZER}) and without a packaged binary, tests that parse Java abort
 * instead of failing. Any other failure is rethrown unchanged.
 */
public class AnalyzerAbsenceExtension
  implements BeforeAllCallback, BeforeEachCallback, TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler, InvocationInterceptor {

  private static int checkpoint = failedExtractions();

  /** Classes that failed to initialize for lack of an analyzer; the JVM reports every later use as a bare initialization error. */
  private static final Set<Class<?>> UNINITIALIZED = new HashSet<>();

  /** Set when a request failed for lack of an analyzer while the class was being set up, which every test of the class then sees. */
  private static boolean setupWithoutAnalyzer;

  @Override
  public void beforeAll(ExtensionContext context) {
    checkpoint = failedExtractions();
    setupWithoutAnalyzer = false;
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    int failed = failedExtractions();
    if (failed > checkpoint) {
      setupWithoutAnalyzer = true;
    }
    checkpoint = failed;
  }

  static final String MARKER = "SemSitter server is unavailable";

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    abortWhenAbsent(throwable);
  }

  @Override
  public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    abortWhenAbsent(throwable);
  }

  @Override
  public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    abortWhenAbsent(throwable);
  }

  @Override
  public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    abortWhenAbsent(throwable);
  }

  @Override
  public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    abortWhenAbsent(throwable);
  }

  /** A test class whose static initialization parses Java fails here, before any method runs. */
  @Override
  public <T> T interceptTestClassConstructor(Invocation<T> invocation, ReflectiveInvocationContext<Constructor<T>> invocationContext,
    ExtensionContext extensionContext) throws Throwable {
    int before = failedExtractions();
    try {
      return invocation.proceed();
    } catch (Throwable throwable) {
      if (failedExtractions() > before) {
        checkpoint = before;
      }
      Class<?> testClass = invocationContext.getTargetClass();
      if (UNINITIALIZED.contains(testClass) && throwable instanceof NoClassDefFoundError) {
        abortWhenAbsent(new IllegalStateException(MARKER, throwable));
      }
      try {
        abortWhenAbsent(throwable);
      } catch (TestAbortedException aborted) {
        UNINITIALIZED.add(testClass);
        throw aborted;
      }
      return null;
    }
  }

  static boolean isAbsence(Throwable throwable) {
    Set<Throwable> seen = new HashSet<>();
    Throwable current = throwable;
    while (current != null && seen.add(current)) {
      String message = current.getMessage();
      if (message != null && message.contains(MARKER)) {
        return true;
      }
      // A parse that failed inside a stubbing call leaves the mock half-configured for the next test.
      if (current.getClass().getName().equals("org.mockito.exceptions.misusing.UnfinishedStubbingException") && failedExtractions() > 0) {
        return true;
      }
      for (Throwable suppressed : current.getSuppressed()) {
        if (isAbsence(suppressed)) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  /** Counts the requests the frontend could not serve for lack of an analyzer; zero when the frontend is not on the classpath. */
  private static int failedExtractions() {
    try {
      return (int) Class.forName("org.sonar.java.semsitter.EmbeddedAnalyzer").getMethod("failedExtractions").invoke(null);
    } catch (ReflectiveOperationException | LinkageError notAvailable) {
      return 0;
    }
  }

  private static void abortWhenAbsent(Throwable throwable) throws Throwable {
    if (setupWithoutAnalyzer || failedExtractions() > checkpoint || isAbsence(throwable)) {
      checkpoint = failedExtractions();
      Assumptions.abort("SemSitter analyzer not configured: set sonar.java.semsitter.executable");
    }
    throw throwable;
  }
}
