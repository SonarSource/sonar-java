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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.java.regex.RegexSource;

public class BackReferenceTree extends RegexTree {

  private final String groupName;
  @Nullable
  private final SourceCharacter key;
  @Nullable
  private CapturingGroupTree group;

  public BackReferenceTree(RegexSource source, SourceCharacter backslash, @Nullable SourceCharacter key, SourceCharacter start, SourceCharacter end, FlagSet activeFlags) {
    super(source, backslash.getRange().merge(end.getRange()), activeFlags);
    this.key = key;
    if (start.getCharacter() != '<') {
      // numerical case
      this.groupName = source.substringAt(start.getRange().merge(end.getRange()));
    } else {
      // named
      this.groupName = source.substringAt(
        new IndexRange(
          start.getRange().getBeginningOffset() + 1,
          end.getRange().getBeginningOffset()));
    }
  }

  public void setGroup(@Nullable CapturingGroupTree group) {
    this.group = group;
  }

  @Nullable
  public CapturingGroupTree group() {
    return group;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitBackReference(this);
  }

  @Override
  public RegexTree.Kind kind() {
    return RegexTree.Kind.BACK_REFERENCE;
  }

  public boolean isNamedGroup() {
    return key != null;
  }

  public boolean isNumerical() {
    return key == null;
  }

  public String groupName() {
    return groupName;
  }

  public int groupNumber() {
    if (!isNumerical()) {
      return -1;
    }
    return Integer.parseInt(groupName);
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.BACK_REFERENCE;
  }
}
