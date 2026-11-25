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

/** Return and thrown types of invocations and diamonds, as declared until inference from arguments and targets lands. */
class Inference {

  /**
   * The type an invocation of {@code method} evaluates to: its declared return type.
   * @param receiver type of the receiver, used once inference lands
   * @param arguments types of the arguments, used once inference lands
   */
  Type invocationReturn(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    Type declared = method.returnType().type();
    return declared == null ? Type.UNKNOWN : declared;
  }

  /** The erased return type of a call on a raw receiver, or null when it does not apply. */
  @Nullable
  Type rawReturn(JMethodSymbol method) {
    return null;
  }

  /** The thrown types of a call inferred from its arguments, or null when the erased thrown types stand. */
  @Nullable
  List<Type> invocationThrown(JMethodSymbol method, @Nullable Type receiver, List<Type> arguments) {
    return null;
  }

  /** The type {@code method} returns when its target fixes its type variables, or null when they stay open. */
  @Nullable
  Type targetedReturn(JMethodSymbol method, Type target) {
    return null;
  }

  /** The result of a call retargeted to a supertype of its element, or null when the result stands. */
  @Nullable
  Type retargetedElement(JMethodSymbol method, Type receiver, Type returned, Type target) {
    return null;
  }

  /**
   * The parameterization of {@code created} a diamond infers from {@code target}.
   * @param target type the diamond is assigned to, used once inference lands
   */
  Type inferDiamond(Type created, Type target) {
    return created;
  }

  /**
   * The parameterization of {@code created} a diamond infers from the arguments of its constructor.
   * @param constructor constructor the diamond calls, used once inference lands
   * @param arguments types of the arguments, used once inference lands
   */
  Type inferDiamondFromArguments(JMethodSymbol constructor, Type created, List<Type> arguments) {
    return created;
  }

  /** The type a lambda or method reference implements for a wildcard-parameterized target. */
  Type groundType(Type target) {
    return target;
  }

  /** Capture conversion of the receiver's wildcard arguments. */
  Type captureWildcards(Type receiver) {
    return receiver;
  }

  /**
   * Whether {@code method} declares type variables of its own.
   * @param method method to inspect, used once inference lands
   * @param substituted whether its signature is already substituted, used once inference lands
   */
  boolean declaresTypeVariables(JMethodSymbol method, boolean substituted) {
    return false;
  }
}
