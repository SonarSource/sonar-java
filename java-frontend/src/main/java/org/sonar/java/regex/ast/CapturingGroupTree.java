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
package org.sonar.java.regex.ast;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CapturingGroupTree extends GroupTree {

  @Nullable
  private final String name;
  private final int groupNumber;

  public CapturingGroupTree(RegexSource source, IndexRange range, @Nullable String name, int groupNumber, RegexTree element, FlagSet activeFlags) {
    super(source, Kind.CAPTURING_GROUP, element, range, activeFlags);
    this.name = name;
    this.groupNumber = groupNumber;
    element.setContinuation(new EndOfCapturingGroupState(this, activeFlags));
  }

  @Override
  public void setContinuation(AutomatonState continuation) {
    setContinuation(continuation, null);
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitCapturingGroup(this);
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public int getGroupNumber() {
    return groupNumber;
  }

  @Nonnull
  @Override
  public RegexTree getElement() {
    return element;
  }
}
