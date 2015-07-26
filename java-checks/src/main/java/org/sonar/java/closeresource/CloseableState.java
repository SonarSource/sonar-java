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
package org.sonar.java.closeresource;

import com.google.common.collect.Lists;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class CloseableState extends State {

  public CloseableState(Tree tree) {
    super(tree);
  }

  public CloseableState(List<Tree> trees) {
    super(trees);
  }

  public static class Closed extends CloseableState {
    public Closed(Tree tree) {
      super(tree);
    }

    @Override
    public State merge(State s) {
      return s;
    }
  }

  public static class Open extends CloseableState {
    public Open(Tree tree) {
      super(tree);
    }

    public Open(List<Tree> changingStateTrees) {
      super(changingStateTrees);
    }

    @Override
    public State merge(State s) {
      if (s instanceof Open) {
        List<Tree> trees = Lists.newArrayList(s.reportingTrees());
        trees.addAll(reportingTrees());
        return new Open(trees);
      } else if (s instanceof Ignored) {
        return s;
      }
      return this;
    }
  }

  public static class Ignored extends CloseableState {
    public Ignored(Tree tree) {
      super(tree);
    }

    @Override
    public State merge(State s) {
      return this;
    }
  }

  public boolean isIgnored() {
    return this instanceof Ignored;
  }

}
