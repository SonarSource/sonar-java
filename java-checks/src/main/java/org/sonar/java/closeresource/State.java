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
package org.sonar.java.closeresource;

public enum State {

  // * | C | O | I | N |
  // --+---+---+---+---|
  // C | C | O | I | C | <- CLOSED
  // --+---+---+---+---|
  // O | O | O | I | O | <- OPEN
  // --+---+---+---+---|
  // I | I | I | I | I | <- IGNORED
  // --+---+---+---+---|
  // N | C | O | I | N | <- NULL
  // ------------------+

  NULL {
    @Override
    public State merge(State s) {
      return s;
    }
  },
  CLOSED {
    @Override
    public State merge(State s) {
      if (s == NULL) {
        return this;
      }
      return s;
    }
  },
  OPEN {
    @Override
    public State merge(State s) {
      if (s == IGNORED) {
        return s;
      }
      return this;
    }
  },
  IGNORED {
    @Override
    public State merge(State s) {
      return this;
    }
  };

  public abstract State merge(State s);

  public boolean isIgnored() {
    return this.equals(IGNORED);
  }

  public boolean isOpen() {
    return this.equals(OPEN);
  }
}
