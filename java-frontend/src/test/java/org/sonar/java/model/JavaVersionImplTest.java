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

import org.junit.Test;
import org.sonar.plugins.java.api.JavaVersion;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaVersionImplTest {

  @Test
  public void no_version_set() throws Exception {
    JavaVersion version = new JavaVersionImpl();
    assertThat(version.isJava7Compatible()).isTrue();
    assertThat(version.isJava8Compatible()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);
  }

  @Test
  public void java_5() throws Exception {
    JavaVersion version = new JavaVersionImpl(5);
    assertThat(version.isJava6Compatible()).isFalse();
    assertThat(version.isJava7Compatible()).isFalse();
    assertThat(version.isJava8Compatible()).isFalse();
    assertThat(version.asInt()).isEqualTo(5);
  }

  @Test
  public void java_6() throws Exception {
    JavaVersion version = new JavaVersionImpl(6);
    assertThat(version.isJava6Compatible()).isTrue();
    assertThat(version.isJava7Compatible()).isFalse();
    assertThat(version.isJava8Compatible()).isFalse();
    assertThat(version.asInt()).isEqualTo(6);
  }

  @Test
  public void java_7() throws Exception {
    JavaVersion version = new JavaVersionImpl(7);
    assertThat(version.isJava6Compatible()).isTrue();
    assertThat(version.isJava7Compatible()).isTrue();
    assertThat(version.isJava8Compatible()).isFalse();
    assertThat(version.asInt()).isEqualTo(7);
  }

  @Test
  public void java_8() throws Exception {
    JavaVersion version = new JavaVersionImpl(8);
    assertThat(version.isJava6Compatible()).isTrue();
    assertThat(version.isJava7Compatible()).isTrue();
    assertThat(version.isJava8Compatible()).isTrue();
    assertThat(version.asInt()).isEqualTo(8);
  }

  @Test
  public void java_12() {
    JavaVersion version = new JavaVersionImpl(12);
    assertThat(version.isJava6Compatible()).isTrue();
    assertThat(version.isJava7Compatible()).isTrue();
    assertThat(version.isJava8Compatible()).isTrue();
    assertThat(version.isJava12Compatible()).isTrue();
    assertThat(version.asInt()).isEqualTo(12);
  }

  @Test
  public void compatibilityMesssages() throws Exception {
    JavaVersion version;
    version = new JavaVersionImpl();
    assertThat(version.java6CompatibilityMessage()).isEqualTo(" (sonar.java.source not set. Assuming 6 or greater.)");
    assertThat(version.java7CompatibilityMessage()).isEqualTo(" (sonar.java.source not set. Assuming 7 or greater.)");
    assertThat(version.java8CompatibilityMessage()).isEqualTo(" (sonar.java.source not set. Assuming 8 or greater.)");

    version = new JavaVersionImpl(6);
    assertThat(version.java6CompatibilityMessage()).isEmpty();
    assertThat(version.java7CompatibilityMessage()).isEmpty();
    assertThat(version.java8CompatibilityMessage()).isEmpty();
  }

  @Test
  public void test_toString() throws Exception {
    JavaVersion version;
    version = new JavaVersionImpl();
    assertThat(version.toString()).isEqualTo("none");

    version = new JavaVersionImpl(7);
    assertThat(version.toString()).isEqualTo("7");
  }

  @Test
  public void test_fromString() throws Exception {
    JavaVersion version;
    version = JavaVersionImpl.fromString(null);
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);

    version = JavaVersionImpl.fromString("jdk1.6");
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);

    version = JavaVersionImpl.fromString("1.6");
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(6);

    version = JavaVersionImpl.fromString("7");
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(7);

    version = JavaVersionImpl.fromString("10");
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(10);
    assertThat(version.isJava8Compatible()).isTrue();

    version = JavaVersionImpl.fromString("12");
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(12);
    assertThat(version.isJava12Compatible()).isTrue();
  }
}
