/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.locks;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;

public class ExecutionState {
  @Nullable
  ExecutionState parent;
  private Map<Symbol, LockedOccurence> lockedOccurencesBySymbol = Maps.newHashMap();
  private IssuableSubscriptionVisitor check;

  ExecutionState(IssuableSubscriptionVisitor check) {
    this.check = check;
  }

  public ExecutionState(ExecutionState parent) {
    this.parent = parent;
    this.check = parent.check;
  }

  public ExecutionState merge(ExecutionState executionState) {
    for (Map.Entry<Symbol, LockedOccurence> entry : executionState.lockedOccurencesBySymbol.entrySet()) {
      Symbol symbol = entry.getKey();
      LockedOccurence currentOccurence = getLockedOccurence(symbol);
      LockedOccurence occurenceToMerge = entry.getValue();
      if (currentOccurence != null) {
        currentOccurence.state = currentOccurence.state.merge(occurenceToMerge.state);
        lockedOccurencesBySymbol.put(symbol, currentOccurence);
      } else {
        // possible way to solve the problem of variable defined in outer Execution state. lockedOccurencesBySymbol.put(symbol,
        // occurenceToMerge);
        if (occurenceToMerge.state.isLocked()) {
          insertIssue(occurenceToMerge.lastAssignment);
        }
      }
    }
    return this;
  }

  public ExecutionState overrideBy(ExecutionState currentES) {
    for (Map.Entry<Symbol, LockedOccurence> entry : currentES.lockedOccurencesBySymbol.entrySet()) {
      Symbol symbol = entry.getKey();
      LockedOccurence occurence = entry.getValue();
      if (getLockedOccurence(symbol) != null) {
        markAs(symbol, occurence.state);
      } else {
        lockedOccurencesBySymbol.put(symbol, occurence);
      }
    }
    return this;
  }

  public ExecutionState restoreParent() {
    if (parent != null) {
      // insertIssues();
      return parent.merge(this);
    }
    return this;
  }

  void insertIssues() {
    for (Tree tree : getLeftLocked()) {
      insertIssue(tree);
    }
  }

  private void insertIssue(Tree tree) {
    check.addIssue(tree, "Make sure this lock is released before the end of this method.");
  }

  void addLockable(Symbol symbol, Tree lockInvocationTree) {
    LockedOccurence knownOccurence = getLockedOccurence(symbol);
    if (knownOccurence != null) {
      if (knownOccurence.state == State.LOCKED) {
        insertIssue(knownOccurence.lastAssignment);
      }
    } else {
      // no known occurence, means its a field or a method param.
      createValueInTopExecutionState(symbol, lockInvocationTree, State.NULL);
    }
    lockedOccurencesBySymbol.put(symbol, new LockedOccurence(lockInvocationTree, State.LOCKED));
  }

  private void createValueInTopExecutionState(Symbol symbol, Tree tree, State state) {
    ExecutionState top = this;
    while (top.parent != null) {
      top = top.parent;
    }
    top.lockedOccurencesBySymbol.put(symbol, new LockedOccurence(tree, state));
  }

  void newValueForSymbol(Symbol symbol, Tree definition) {
    checkCreationOfIssue(symbol);
    lockedOccurencesBySymbol.put(symbol, new LockedOccurence(definition, State.NULL));
  }

  @CheckForNull
  private LockedOccurence checkCreationOfIssue(Symbol symbol) {
    LockedOccurence knownOccurence = getLockedOccurence(symbol);
    if (knownOccurence != null) {
      LockedOccurence currentOccurence = lockedOccurencesBySymbol.get(symbol);
      if (currentOccurence != null && currentOccurence.state.isLocked()) {
        insertIssue(knownOccurence.lastAssignment);
      }
      lockedOccurencesBySymbol.remove(symbol);
    }
    return knownOccurence;
  }

  void markAsUnlocked(Symbol symbol) {
    markAs(symbol, State.UNLOCKED);
  }

  private void markAs(Symbol symbol, State state) {
    LockedOccurence occurence = getLockedOccurence(symbol);
    if (occurence != null) {
      lockedOccurencesBySymbol.put(symbol, new LockedOccurence(occurence.lastAssignment, state));
    }
  }

  private Set<Tree> getLeftLocked() {
    Set<Tree> results = Sets.newHashSet();
    for (LockedOccurence occurence : lockedOccurencesBySymbol.values()) {
      if (occurence.state.isLocked()) {
        results.add(occurence.lastAssignment);
      }
    }
    return results;
  }

  @CheckForNull
  private LockedOccurence getLockedOccurence(Symbol symbol) {
    LockedOccurence occurence = lockedOccurencesBySymbol.get(symbol);
    if (occurence != null) {
      return new LockedOccurence(occurence.lastAssignment, occurence.state);
    } else if (parent != null) {
      return parent.getLockedOccurence(symbol);
    }
    return null;
  }

}
