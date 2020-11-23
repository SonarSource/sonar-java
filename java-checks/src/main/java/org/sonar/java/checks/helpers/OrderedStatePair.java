/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.helpers;

import org.sonar.java.regex.ast.AutomatonState;

public class OrderedStatePair {
  public final AutomatonState state1;
  public final AutomatonState state2;

  public OrderedStatePair(AutomatonState state1, AutomatonState state2) {
    this.state1 = state1;
    this.state2 = state2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    } else {
      OrderedStatePair that = (OrderedStatePair) o;
      return state1 == that.state1 && state2 == that.state2;
    }
  }

  @Override
  public int hashCode() {
    return 31 * state1.hashCode() + state2.hashCode();
  }
}
