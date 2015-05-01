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

import com.google.common.collect.Lists;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class LockState implements State {

  final List<Tree> changingStateTrees;

  public LockState(Tree tree) {
    changingStateTrees = Lists.newArrayList();
    changingStateTrees.add(tree);
  }
  public LockState(List<Tree> tree) {
    this.changingStateTrees = tree;
  }

  @Override
  public boolean shouldRaiseIssue() {
    return false;
  }

  @Override
  public List<Tree> reportingTrees() {
    return changingStateTrees;
  }

  public static class Unlocked extends LockState{
    public Unlocked(Tree tree) {
      super(tree);
    }

    @Override
    public State merge(State s) {
      if (s.equals(State.UNSET)) {
        return this;
      }
      if(!(s instanceof LockState)) {
        throw new IllegalStateException("Merging incompatible states");
      }
      return s;
    }
  }
  public static class Locked extends LockState{

    public Locked(Tree tree) {
      super(tree);
    }

    public Locked(List<Tree> changingStateTrees) {
      super(changingStateTrees);
    }

    @Override
    public State merge(State s) {
      if(s instanceof Locked) {
        List<Tree> trees = Lists.newArrayList(((Locked) s).changingStateTrees);
        trees.addAll(changingStateTrees);
        return new Locked(changingStateTrees);
      }
      return this;
    }

    @Override
    public boolean shouldRaiseIssue() {
      return true;
    }
  }






}
