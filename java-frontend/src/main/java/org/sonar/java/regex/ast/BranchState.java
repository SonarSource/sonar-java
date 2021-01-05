/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.regex.ast;

import java.util.List;
import javax.annotation.Nonnull;

public class BranchState extends ActiveFlagsState {

  private final RegexTree parent;

  private final List<AutomatonState> successors;

  public BranchState(RegexTree parent, List<AutomatonState> successors, FlagSet activeFlags) {
    super(activeFlags);
    this.parent = parent;
    this.successors = successors;
  }

  @Nonnull
  @Override
  public AutomatonState continuation() {
    return parent.continuation();
  }

  @Nonnull
  @Override
  public List<AutomatonState> successors() {
    return successors;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.EPSILON;
  }

}
