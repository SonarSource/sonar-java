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

import java.util.function.Predicate;
import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.AutomatonState.TransitionType;

public class SubAutomaton {
  public final AutomatonState start;
  public final AutomatonState end;
  public final boolean allowPrefix;
  public final boolean followMatchedCharacters;

  public SubAutomaton(AutomatonState start, AutomatonState end, boolean allowPrefix) {
    this(start, end, allowPrefix, false);
  }

  public SubAutomaton(AutomatonState start, AutomatonState end, boolean allowPrefix, boolean followMatchedCharacters) {
    this.start = start;
    this.end = end;
    this.allowPrefix = allowPrefix;
    this.followMatchedCharacters = followMatchedCharacters;
  }

  public TransitionType incomingTransitionType() {
    return start.incomingTransitionType();
  }

  public boolean isAtEnd() {
    return start == end;
  }

  public boolean anySuccessorMatch(Predicate<SubAutomaton> predicate, boolean followMatchedCharacter) {
    for (AutomatonState successor : start.successors()) {
      if (predicate.test(new SubAutomaton(successor, end, allowPrefix, this.followMatchedCharacters || followMatchedCharacter))) {
        return true;
      }
    }
    return false;
  }

  public boolean allSuccessorMatch(Predicate<SubAutomaton> predicate, boolean followMatchedCharacter) {
    for (AutomatonState successor : start.successors()) {
      if (!predicate.test(new SubAutomaton(successor, end, allowPrefix, this.followMatchedCharacters || followMatchedCharacter))) {
        return false;
      }
    }
    return true;
  }

}
