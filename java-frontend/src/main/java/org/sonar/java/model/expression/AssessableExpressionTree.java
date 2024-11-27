/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model.expression;

import java.util.Optional;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;

/**
 * Parent class for all ExpressionTrees, which allows to express them as their constant value
 */
public abstract class AssessableExpressionTree extends AbstractTypedTree implements ExpressionTree {

  @Override
  public Optional<Object> asConstant() {
    return Optional.ofNullable(ExpressionUtils.resolveAsConstant(this));
  }

  @Override
  public <T> Optional<T> asConstant(Class<T> type) {
    return asConstant().filter(type::isInstance).map(type::cast);
  }

}
