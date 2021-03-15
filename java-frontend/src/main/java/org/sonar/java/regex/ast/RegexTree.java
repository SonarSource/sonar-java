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
import org.sonar.java.regex.RegexSource;

public abstract class RegexTree extends AbstractRegexSyntaxElement implements AutomatonState {
  public enum Kind {
    BACK_REFERENCE,
    BOUNDARY,
    CHARACTER_CLASS,
    DISJUNCTION,
    DOT,
    ESCAPED_CHARACTER_CLASS,
    CAPTURING_GROUP,
    NON_CAPTURING_GROUP,
    ATOMIC_GROUP,
    LOOK_AROUND,
    CHARACTER,
    REPETITION,
    SEQUENCE,
    MISC_ESCAPE_SEQUENCE,
  }

  private final FlagSet activeFlags;

  protected RegexTree(RegexSource source, IndexRange range, FlagSet activeFlags) {
    super(source, range);
    this.activeFlags = activeFlags;
  }

  @Nonnull
  @Override
  public FlagSet activeFlags() {
    return activeFlags;
  }

  /**
   * This method should only be called by RegexBaseVisitor (or other implementations of the RegexVisitor interface).
   * Do not call this method to invoke a visitor, use visitor.visit(tree) instead.
   */
  public abstract void accept(RegexVisitor visitor);

  public abstract Kind kind();

  public boolean is(Kind... kinds) {
    Kind thisKind = kind();
    for (Kind kind : kinds) {
      if (thisKind == kind) {
        return true;
      }
    }
    return false;
  }

  private AutomatonState continuation;

  @Nonnull
  public AutomatonState continuation() {
    if (this.continuation == null) {
      throw new IllegalStateException("RegexTree.continuation() called before setContinuation");
    }
    return continuation;
  }

  public void setContinuation(AutomatonState continuation) {
    if (this.continuation != null) {
      throw new IllegalStateException("RegexTree.setContinuation called more than once");
    }
    this.continuation = continuation;
  }

}
