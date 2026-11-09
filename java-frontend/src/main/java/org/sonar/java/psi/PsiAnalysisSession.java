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
package org.sonar.java.psi;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.sonar.java.model.JavaTree;

/** Keeps shared PSI alive until both parsing and every returned compilation unit have released it. */
final class PsiAnalysisSession implements AutoCloseable {
  private static final Cleaner CLEANER = Cleaner.create();
  private final JavaPsiEnvironment environment;
  private final AtomicInteger references = new AtomicInteger(1);
  private final AtomicBoolean ownerReleased = new AtomicBoolean();

  PsiAnalysisSession(JavaPsiEnvironment environment) {
    this.environment = environment;
  }

  void attach(JavaTree.CompilationUnitTreeImpl tree) {
    references.incrementAndGet();
    var lease = CLEANER.register(tree, this::release);
    tree.setSemanticModel(tree.sema, tree.semanticProblems(), lease::clean);
  }

  private void release() {
    if (references.decrementAndGet() == 0) {
      environment.close();
    }
  }

  void abort() {
    environment.close();
  }

  @Override
  public void close() {
    if (ownerReleased.compareAndSet(false, true)) {
      release();
    }
  }
}
