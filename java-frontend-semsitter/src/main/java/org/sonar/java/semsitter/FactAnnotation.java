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
import org.sonar.java.model.AnnotationValueImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;

final class FactAnnotation implements AnnotationInstance {

  private final Symbol symbol;
  private final List<AnnotationValue> values;

  FactAnnotation(JSema sema, String fqn, List<AnnotationValue> values, boolean valuesKnown) {
    this.symbol = sema.typeSymbol(fqn);
    // An annotation known by name only keeps one unknown value, so a consumer that reads a value never meets an empty list.
    this.values = valuesKnown ? values : List.of(new AnnotationValueImpl("value", null));
  }

  @Override
  public Symbol symbol() {
    return symbol;
  }

  @Override
  public List<AnnotationValue> values() {
    return values;
  }
}
