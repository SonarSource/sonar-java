/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.symexecengine;

import com.google.common.collect.Lists;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class State {
  private final List<Tree> changingStateTrees;

  public static final State UNSET = new State() {
    @Override
    public State merge(State s) {
      return s;
    }
  };

  private State() {
    changingStateTrees = Lists.newArrayList();
  }

  public State(Tree tree) {
    changingStateTrees = Lists.newArrayList();
    changingStateTrees.add(tree);
  }

  public State(List<Tree> trees) {
    changingStateTrees = trees;
  }

  public abstract State merge(State s);

  public List<Tree> reportingTrees() {
    return changingStateTrees;
  }
}
