/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

public class FlagsTest {

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = Flags.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  /**
   * Flags can be easily loaded from class-files into symbols.
   */
  @Test
  public void flags_match_asm_opcodes() {
    assertThat(Flags.PUBLIC).isEqualTo(Opcodes.ACC_PUBLIC);
    assertThat(Flags.PRIVATE).isEqualTo(Opcodes.ACC_PRIVATE);
    assertThat(Flags.PROTECTED).isEqualTo(Opcodes.ACC_PROTECTED);
    assertThat(Flags.STATIC).isEqualTo(Opcodes.ACC_STATIC);
    assertThat(Flags.FINAL).isEqualTo(Opcodes.ACC_FINAL);
    assertThat(Flags.SYNCHRONIZED).isEqualTo(Opcodes.ACC_SYNCHRONIZED);
    assertThat(Flags.VOLATILE).isEqualTo(Opcodes.ACC_VOLATILE);
    assertThat(Flags.TRANSIENT).isEqualTo(Opcodes.ACC_TRANSIENT);
    assertThat(Flags.NATIVE).isEqualTo(Opcodes.ACC_NATIVE);
    assertThat(Flags.INTERFACE).isEqualTo(Opcodes.ACC_INTERFACE);
    assertThat(Flags.ABSTRACT).isEqualTo(Opcodes.ACC_ABSTRACT);
    assertThat(Flags.STRICTFP).isEqualTo(Opcodes.ACC_STRICT);
    assertThat(Flags.SYNTHETIC).isEqualTo(Opcodes.ACC_SYNTHETIC);
    assertThat(Flags.ANNOTATION).isEqualTo(Opcodes.ACC_ANNOTATION);
    assertThat(Flags.ENUM).isEqualTo(Opcodes.ACC_ENUM);
  }

}
