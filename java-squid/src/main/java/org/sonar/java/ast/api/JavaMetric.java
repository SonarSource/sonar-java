/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.api;

import org.sonar.squidbridge.measures.CalculatedMetricFormula;
import org.sonar.squidbridge.measures.MetricDef;

public enum JavaMetric implements MetricDef {

  LINES(false),
  LINES_OF_CODE(false),
  COMMENT_LINES_WITHOUT_HEADER(false),
  STATEMENTS,
  COMPLEXITY,
  METHODS,
  CLASSES,
  TESTS;

  private boolean aggregateIfThereIsAlreadyAValue = true;

  JavaMetric() {
  }

  JavaMetric(boolean aggregateIfThereIsAlreadyAValue) {
    this.aggregateIfThereIsAlreadyAValue = aggregateIfThereIsAlreadyAValue;
  }

  public String getName() {
    return name();
  }

  public boolean isCalculatedMetric() {
    return false;
  }

  public boolean aggregateIfThereIsAlreadyAValue() {
    return aggregateIfThereIsAlreadyAValue;
  }

  public boolean isThereAggregationFormula() {
    return true;
  }

  public CalculatedMetricFormula getCalculatedMetricFormula() {
    return null;
  }

}
