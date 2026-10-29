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
package org.sonar.java.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Retains lazy fixtures through instance initialization, nested tests and all assertions in a test class. */
public final class TestResourceScope implements BeforeAllCallback, AfterAllCallback {

  private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);
  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TestResourceScope.class);

  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @ExtendWith(TestResourceScope.class)
  @Execution(ExecutionMode.SAME_THREAD)
  public @interface Scoped {
  }

  public static Scope open() {
    Scope scope = new Scope();
    SCOPES.get().push(scope);
    return scope;
  }

  public static <T extends AutoCloseable> T retain(T resource) {
    Scope scope = SCOPES.get().peek();
    if (scope == null) {
      IllegalStateException failure = new IllegalStateException("Parser fixtures require @TestResourceScope.Scoped or an explicit scope");
      try {
        resource.close();
      } catch (Exception | Error cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
    scope.resources.push(resource);
    return resource;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    context.getStore(NAMESPACE).put("scope", open());
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Scope scope = context.getStore(NAMESPACE).remove("scope", Scope.class);
    if (scope != null) {
      scope.close();
    }
  }

  public static final class Scope implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private boolean closed;

    private Scope() {
    }

    @Override
    public void close() throws Exception {
      if (closed) {
        return;
      }
      if (SCOPES.get().peek() != this) {
        throw new IllegalStateException("Test resource scopes must close in reverse order on their owning thread");
      }
      closed = true;
      SCOPES.get().pop();
      if (SCOPES.get().isEmpty()) {
        SCOPES.remove();
      }
      Throwable failure = null;
      while (!resources.isEmpty()) {
        try {
          resources.pop().close();
        } catch (Exception | Error e) {
          if (failure == null) {
            failure = e;
          } else if (failure != e) {
            failure.addSuppressed(e);
          }
        }
      }
      if (failure instanceof Exception exception) {
        throw exception;
      }
      if (failure instanceof Error error) {
        throw error;
      }
    }
  }
}
