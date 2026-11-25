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
package org.sonar.java.semsitter;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Type;

/** Parameter and result types of lambdas and method references, unknown until inference from their target lands. */
class FunctionalInference {

  /** The functional interface {@code parameter} as a function producing {@code produced}, or null when unknown. */
  @Nullable
  Type functionProducing(Type parameter, Type produced) {
    return null;
  }

  /**
   * Whether {@code method} returns a type variable.
   * @param method method to inspect, used once inference lands
   */
  boolean returnsClassVariable(JMethodSymbol method) {
    return false;
  }

  /** The result of an unbound method reference to {@code method} implementing {@code function}, or null when unknown. */
  @Nullable
  Type unboundReferenceResult(JMethodSymbol method, Type function) {
    return null;
  }

  /** The parameter types of {@code method} fixed by {@code target}, or null when unknown. */
  @Nullable
  List<Type> targetedParameters(JMethodSymbol method, Type target) {
    return null;
  }
}
