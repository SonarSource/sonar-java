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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class NonCapturingGroupTree extends RegexTree {

  @CheckForNull
  private final RegexTree element;

  private final int enabledFlags;

  private final int disabledFlags;

  public NonCapturingGroupTree(RegexSource source, IndexRange range, int enabledFlags, int disabledFlags, @Nullable RegexTree element) {
    super(source, range);
    this.enabledFlags = enabledFlags;
    this.disabledFlags = disabledFlags;
    this.element = element;
  }

  @CheckForNull
  public RegexTree getElement() {
    return element;
  }

  public int getEnabledFlags() {
    return enabledFlags;
  }

  public int getDisabledFlags() {
    return disabledFlags;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitNonCapturingGroup(this);
  }

  @Override
  public Kind kind() {
    return RegexTree.Kind.NON_CAPTURING_GROUP;
  }
}
