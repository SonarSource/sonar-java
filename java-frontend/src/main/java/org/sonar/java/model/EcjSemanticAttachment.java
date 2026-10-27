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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IBinding;

final class EcjSemanticAttachment<B extends IBinding, T> implements Supplier<T> {

  final B binding;
  private final Function<B, T> factory;
  private T value;

  EcjSemanticAttachment(B binding, Function<B, T> factory) {
    this.binding = binding;
    this.factory = factory;
  }

  @Override
  public T get() {
    if (value == null) {
      value = Objects.requireNonNull(factory.apply(binding));
    }
    return value;
  }

  @Nullable
  static IBinding getBinding(Supplier<?> attachment) {
    return attachment instanceof EcjSemanticAttachment<?, ?> ecj ? ecj.binding : null;
  }
}
