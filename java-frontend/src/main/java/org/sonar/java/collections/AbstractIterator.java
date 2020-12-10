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
package org.sonar.java.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.sonar.java.Preconditions;

public abstract class AbstractIterator<T> implements Iterator<T> {
  private State state;
  
  private T next;

  protected AbstractIterator() {
    this.state = State.NOT_READY;
  }

  @Nullable
  protected abstract T computeNext();

  @Nullable
  protected final T endOfData() {
    this.state = State.DONE;
    return null;
  }

  public final boolean hasNext() {
    Preconditions.checkState(this.state != State.FAILED);
    switch(this.state) {
      case DONE:
        return false;
      case READY:
        return true;
      default:
        return this.tryToComputeNext();
    }
  }

  private boolean tryToComputeNext() {
    this.state = State.FAILED;
    this.next = this.computeNext();
    if (this.state != State.DONE) {
      this.state = State.READY;
      return true;
    } else {
      return false;
    }
  }

  @Nullable
  public final T next() {
    if (!this.hasNext()) {
      throw new NoSuchElementException();
    } else {
      this.state = State.NOT_READY;
      T result = this.next;
      this.next = null;
      return result;
    }
  }

  @Nullable
  public final T peek() {
    if (!this.hasNext()) {
      throw new NoSuchElementException();
    } else {
      return this.next;
    }
  }

  private enum State {
    READY,
    NOT_READY,
    DONE,
    FAILED;

    State() {
    }
  }
}
