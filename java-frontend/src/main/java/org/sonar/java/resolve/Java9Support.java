/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.resolve;

import org.objectweb.asm.Opcodes;

import java.nio.ByteBuffer;

class Java9Support {

  private static final short JAVA_8_MAJOR_VERSION = Opcodes.V1_8;
  private static final short JAVA_9_MAJOR_VERSION = JAVA_8_MAJOR_VERSION + 1;
  private static final int MAJOR_VERSION_OFFSET = 6;

  private Java9Support() {
    // utility class
  }

  static boolean isJava9Class(byte[] bytecode) {
    return ByteBuffer.wrap(bytecode).getShort(MAJOR_VERSION_OFFSET) == JAVA_9_MAJOR_VERSION;
  }

  static void setJava8MajorVersion(byte[] bytecode) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytecode);
    byteBuffer.putShort(MAJOR_VERSION_OFFSET, JAVA_8_MAJOR_VERSION);
  }
}
