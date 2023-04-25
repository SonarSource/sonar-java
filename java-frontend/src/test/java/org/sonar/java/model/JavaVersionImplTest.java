/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.plugins.java.api.JavaVersion;

import static org.assertj.core.api.Assertions.assertThat;

class JavaVersionImplTest {

  @Test
  void no_version_set() {
    JavaVersion version = new JavaVersionImpl();
    assertThat(version.isSet()).isFalse();
    assertThat(version.isNotSet()).isTrue();
    // not set is considered compatible with everything <= 8
    assertThat(version.isJava6Compatible()).isTrue();
    assertThat(version.isJava7Compatible()).isTrue();
    assertThat(version.isJava8Compatible()).isTrue();
    // all the rest are incompatible
    assertThat(version.isJava9Compatible()).isFalse();
    assertThat(version.isJava10Compatible()).isFalse();
    assertThat(version.isJava12Compatible()).isFalse();
    assertThat(version.isJava14Compatible()).isFalse();
    assertThat(version.isJava15Compatible()).isFalse();
    assertThat(version.isJava16Compatible()).isFalse();
    assertThat(version.isJava17Compatible()).isFalse();
    assertThat(version.isJava18Compatible()).isFalse();
    assertThat(version.isJava19Compatible()).isFalse();
    assertThat(version.asInt()).isEqualTo(-1);
  }

  @ParameterizedTest(name = "JavaVersion: \"{0}\"")
  @ValueSource(ints = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 42})
  void java_versions(int javaVersionAsInt) {
    JavaVersion version = new JavaVersionImpl(javaVersionAsInt);
    assertThat(version.isSet()).isTrue();
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.isJava6Compatible()).isEqualTo(javaVersionAsInt >= 6);
    assertThat(version.isJava7Compatible()).isEqualTo(javaVersionAsInt >= 7);
    assertThat(version.isJava8Compatible()).isEqualTo(javaVersionAsInt >= 8);
    assertThat(version.isJava9Compatible()).isEqualTo(javaVersionAsInt >= 9);
    assertThat(version.isJava10Compatible()).isEqualTo(javaVersionAsInt >= 10);
    assertThat(version.isJava12Compatible()).isEqualTo(javaVersionAsInt >= 12);
    assertThat(version.isJava14Compatible()).isEqualTo(javaVersionAsInt >= 14);
    assertThat(version.isJava15Compatible()).isEqualTo(javaVersionAsInt >= 15);
    assertThat(version.isJava16Compatible()).isEqualTo(javaVersionAsInt >= 16);
    assertThat(version.isJava17Compatible()).isEqualTo(javaVersionAsInt >= 17);
    assertThat(version.isJava18Compatible()).isEqualTo(javaVersionAsInt >= 18);
    assertThat(version.isJava19Compatible()).isEqualTo(javaVersionAsInt >= 19);
    assertThat(version.asInt()).isEqualTo(javaVersionAsInt);
  }

  @Test
  void compatibilityMesssages() {
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
  void test_effective_java_version() {
    assertThat(new JavaVersionImpl().effectiveJavaVersionAsString()).isEqualTo("19");
    assertThat(new JavaVersionImpl(10).effectiveJavaVersionAsString()).isEqualTo("10");
    assertThat(new JavaVersionImpl(-1).effectiveJavaVersionAsString()).isEqualTo("19");
  }

  @Test
  void test_toString() {
    JavaVersion version;
    version = new JavaVersionImpl();
    assertThat(version).hasToString("none");

    version = new JavaVersionImpl(7);
    assertThat(version).hasToString("7");
  }

  @Test
  void test_fromString() {
    JavaVersion version;
    version = JavaVersionImpl.fromString("-1");
    assertThat(version.isSet()).isFalse();
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);

    version = JavaVersionImpl.fromString("jdk1.6");
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);

    version = JavaVersionImpl.fromString("1.6");
    assertThat(version.isSet()).isTrue();
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

    version = JavaVersionImpl.fromString("15");
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(15);
    assertThat(version.isJava15Compatible()).isTrue();
    assertThat(version.isJava12Compatible()).isTrue();
    assertThat(version.isJava8Compatible()).isTrue();
  }
  
  @Test
  void test_fromMap() {
    JavaVersion version;
    version = JavaVersionImpl.fromMap(Map.of(JavaVersion.ENABLE_PREVIEW, "False", JavaVersion.SOURCE_VERSION, "17"));
    assertThat(version.isSet()).isTrue();
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(17);
    assertThat(version.arePreviewFeaturesEnabled()).isFalse();
    
    version = JavaVersionImpl.fromMap(Map.of(JavaVersion.ENABLE_PREVIEW, "True", JavaVersion.SOURCE_VERSION, "17"));
    assertThat(version.isSet()).isTrue();
    assertThat(version.isNotSet()).isFalse();
    assertThat(version.asInt()).isEqualTo(17);
    assertThat(version.arePreviewFeaturesEnabled()).isTrue();
    
    version = JavaVersionImpl.fromMap(Map.of(JavaVersion.ENABLE_PREVIEW, "True", JavaVersion.SOURCE_VERSION, ""));
    assertThat(version.isSet()).isFalse();
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);
    assertThat(version.arePreviewFeaturesEnabled()).isFalse();
    
    version = JavaVersionImpl.fromMap(Map.of(JavaVersion.ENABLE_PREVIEW, "True"));
    assertThat(version.isSet()).isFalse();
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);
    assertThat(version.arePreviewFeaturesEnabled()).isFalse();
    
    version = JavaVersionImpl.fromMap(Map.of());
    assertThat(version.isSet()).isFalse();
    assertThat(version.isNotSet()).isTrue();
    assertThat(version.asInt()).isEqualTo(-1);
    assertThat(version.arePreviewFeaturesEnabled()).isFalse();
  }
  
}
