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
package org.sonar.java.regex;

import java.util.NoSuchElementException;
import org.sonar.java.regex.ast.JavaCharacter;

public class CharacterBuffer {

  private static final int RESIZE_FACTOR = 2;

  private JavaCharacter[] contents;

  private int startIndex = 0;

  private int size = 0;

  public CharacterBuffer(int initialCapacity) {
    contents = new JavaCharacter[initialCapacity];
  }

  public JavaCharacter get(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("Invalid index " + index + " for buffer of size " + size + ".");
    }
    return contents[(startIndex + index) % contents.length];
  }

  public void add(JavaCharacter character) {
    if (size + 1 == contents.length) {
      resize(contents.length * RESIZE_FACTOR);
    }
    contents[(startIndex + size) % contents.length] = character;
    size++;
  }

  public void removeFirst() {
    if (size == 0) {
      throw new NoSuchElementException("Trying to delete from empty buffer.");
    }
    startIndex++;
    if (startIndex == contents.length) {
      startIndex = 0;
    }
    size--;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public int size() {
    return size;
  }

  private void resize(int newCapacity) {
    JavaCharacter[] newContents = new JavaCharacter[newCapacity];
    System.arraycopy(contents, startIndex, newContents, 0, contents.length - startIndex);
    System.arraycopy(contents, 0, newContents, contents.length - startIndex, startIndex);
    contents = newContents;
    startIndex = 0;
  }

}
