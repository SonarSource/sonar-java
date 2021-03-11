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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.java.regex.RegexSource;

public class SequenceTree extends RegexTree {

  private final List<RegexTree> items;

  public SequenceTree(RegexSource source, IndexRange range, List<RegexTree> items, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.items = items;
    for (int i = 0; i < items.size() - 1; i++) {
      items.get(i).setContinuation(items.get(i + 1));
    }
  }

  public List<RegexTree> getItems() {
    return Collections.unmodifiableList(items);
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitSequence(this);
  }

  @Override
  public Kind kind() {
    return Kind.SEQUENCE;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.EPSILON;
  }

  @Nonnull
  @Override
  public List<AutomatonState> successors() {
    if (items.isEmpty()) {
      return Collections.singletonList(continuation());
    } else {
      return Collections.singletonList(items.get(0));
    }
  }

  @Override
  public void setContinuation(AutomatonState continuation) {
    super.setContinuation(continuation);
    if (!items.isEmpty()) {
      items.get(items.size() - 1).setContinuation(continuation);
    }
  }
}
