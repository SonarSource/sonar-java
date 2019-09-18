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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ClassesLayoutTest {

  private static final Layouter X86_64 = new HotSpotLayouter(new X86_64_DataModel());
  private static final Layouter X86_64_COOPS = new HotSpotLayouter(new X86_64_COOPS_DataModel());

  @Test
  void token() {
    assertAll(
      () -> assertThat(instanceSize(InternalSyntaxToken.class, X86_64)).isEqualTo(88),
      () -> assertThat(instanceSize(InternalSyntaxToken.class, X86_64_COOPS)).isEqualTo(56)
    );
  }

  @Test
  void identifier() {
    assertAll(
      () -> assertThat(instanceSize(IdentifierTreeImpl.class, X86_64)).isEqualTo(104),
      () -> assertThat(instanceSize(IdentifierTreeImpl.class, X86_64_COOPS)).isEqualTo(56)
    );
  }

  @Test
  void literal() {
    assertAll(
      () -> assertThat(instanceSize(LiteralTreeImpl.class, X86_64)).isEqualTo(80),
      () -> assertThat(instanceSize(LiteralTreeImpl.class, X86_64_COOPS)).isEqualTo(48)
    );
  }

  private static long instanceSize(Class<?> cls, Layouter layouter) {
    System.out.println("***** " + layouter);
    ClassLayout classLayout = ClassLayout.parseClass(cls, layouter);
    System.out.println(classLayout.toPrintable());
    return classLayout.instanceSize();
  }

}
