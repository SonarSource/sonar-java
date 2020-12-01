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

public class NonCapturingGroupTree extends GroupTree {

  private final FlagSet enabledFlags;

  private final FlagSet disabledFlags;

  public NonCapturingGroupTree(
    RegexSource source,
    IndexRange range,
    FlagSet enabledFlags,
    FlagSet disabledFlags,
    @Nullable RegexTree element,
    FlagSet activeFlags
  ) {
    super(source, RegexTree.Kind.NON_CAPTURING_GROUP, element, range, activeFlags);
    this.enabledFlags = enabledFlags;
    this.disabledFlags = disabledFlags;
  }

  @Override
  @CheckForNull
  public RegexTree getElement() {
    return super.getElement();
  }

  public FlagSet getEnabledFlags() {
    return enabledFlags;
  }

  public FlagSet getDisabledFlags() {
    return disabledFlags;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitNonCapturingGroup(this);
  }
}
