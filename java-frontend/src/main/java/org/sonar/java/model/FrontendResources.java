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
package org.sonar.java.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Owns cleanup handles until the last supported semantic consumer finishes. */
public final class FrontendResources implements AutoCloseable {

  private final Map<Runnable, Runnable> cleaners = new LinkedHashMap<>();
  private boolean closed;

  public Runnable own(Runnable cleaner) {
    if (closed) {
      throw new IllegalStateException("Frontend resources are closed");
    }
    return cleaners.computeIfAbsent(cleaner, FrontendResources::idempotent);
  }

  static Runnable idempotent(Runnable cleaner) {
    return new Cleaner(cleaner);
  }

  /** Attempts every cleaner even after errors, then rethrows the first failure with later failures suppressed. */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    Throwable failure = null;
    try {
      for (Runnable cleaner : cleaners.values()) {
        try {
          cleaner.run();
        } catch (RuntimeException | Error e) {
          if (failure == null) {
            failure = e;
          } else if (failure != e) {
            failure.addSuppressed(e);
          }
        }
      }
    } finally {
      cleaners.clear();
    }
    if (failure instanceof Error error) {
      throw error;
    }
    if (failure instanceof RuntimeException exception) {
      throw exception;
    }
  }

  private static final class Cleaner implements Runnable {
    private final Runnable delegate;
    private boolean cleaned;

    private Cleaner(Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public synchronized void run() {
      if (!cleaned) {
        cleaned = true;
        delegate.run();
      }
    }

    @Override
    public boolean equals(Object other) {
      return this == other || (other instanceof Cleaner cleaner && delegate.equals(cleaner.delegate));
    }

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }
  }
}
