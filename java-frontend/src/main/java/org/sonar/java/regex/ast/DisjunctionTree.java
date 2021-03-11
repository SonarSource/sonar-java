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

public class DisjunctionTree extends RegexTree {

  private final List<RegexTree> alternatives;

  private final List<SourceCharacter> orOperators;

  public DisjunctionTree(RegexSource source, IndexRange range, List<RegexTree> alternatives, List<SourceCharacter> orOperators, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.alternatives = Collections.unmodifiableList(alternatives);
    this.orOperators = Collections.unmodifiableList(orOperators);
  }

  public List<RegexTree> getAlternatives() {
    return alternatives;
  }

  public List<SourceCharacter> getOrOperators() {
    return orOperators;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitDisjunction(this);
  }

  @Override
  public Kind kind() {
    return Kind.DISJUNCTION;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.EPSILON;
  }

  @Nonnull
  @Override
  public List<? extends AutomatonState> successors() {
    return alternatives;
  }

  @Override
  public void setContinuation(AutomatonState continuation) {
    super.setContinuation(continuation);
    for (RegexTree alternative : alternatives) {
      alternative.setContinuation(continuation);
    }
  }

}
