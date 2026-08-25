/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.utils;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.test.classpath.TestClasspathUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SpringUtilsTest {

  @Test
  void is_autowired() {
    var cu = JParserTestUtils.parse("""
      class A {
        @org.springframework.beans.factory.annotation.Autowired
        Object autowiredObject;
        
        @Autowired
        Object noSemaAnnotation;

        @javax.annotation.Nullable
        Object nullableObject;
      }
      """);
    var clazz = (ClassTreeImpl) cu.types().get(0);
    var obj = (VariableTreeImpl) clazz.members().get(0);
    assertThat(SpringUtils.isAutowired(obj.symbol())).isTrue();
    var goo = (VariableTreeImpl) clazz.members().get(1);
    assertThat(SpringUtils.isAutowired(goo.symbol())).isFalse();
    var hoo = (VariableTreeImpl) clazz.members().get(2);
    assertThat(SpringUtils.isAutowired(hoo.symbol())).isFalse();
  }

  // ---- isScopeSingleton -------------------------------------------------------

  @Test
  void is_scope_singleton_no_annotation_returns_true() {
    var cu = JParserTestUtils.parse("A", """
      @org.springframework.stereotype.Component
      class A {}
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    assertThat(SpringUtils.isScopeSingleton(clazz.symbol().metadata())).isTrue();
  }

  @Test
  void is_scope_singleton_with_singleton_scope_returns_true() {
    var cu = JParserTestUtils.parse("A", """
      @org.springframework.context.annotation.Scope("singleton")
      class A {}
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    assertThat(SpringUtils.isScopeSingleton(clazz.symbol().metadata())).isTrue();
  }

  @Test
  void is_scope_singleton_with_prototype_scope_returns_false() {
    var cu = JParserTestUtils.parse("A", """
      @org.springframework.context.annotation.Scope("prototype")
      class A {}
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    assertThat(SpringUtils.isScopeSingleton(clazz.symbol().metadata())).isFalse();
  }

  @Test
  void is_scope_singleton_with_scope_name_attribute_and_prototype_returns_false() {
    var cu = JParserTestUtils.parse("A", """
      @org.springframework.context.annotation.Scope(scopeName = "prototype")
      class A {}
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    assertThat(SpringUtils.isScopeSingleton(clazz.symbol().metadata())).isFalse();
  }

  // ---- isSpringBootTestClass --------------------------------------------------

  @Test
  void is_spring_boot_test_class_with_annotation_returns_true() {
    var cu = JParserTestUtils.parse("A", """
      @org.springframework.boot.test.context.SpringBootTest
      class A {}
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    assertThat(SpringUtils.isSpringBootTestClass(clazz.symbol())).isTrue();
  }

  @Test
  void is_spring_boot_test_class_without_annotation_returns_false() {
    var cu = JParserTestUtils.parse("class A {}");
    var clazz = (ClassTreeImpl) cu.types().get(0);
    assertThat(SpringUtils.isSpringBootTestClass(clazz.symbol())).isFalse();
  }

  // ---- isSpringBootUnitTest ---------------------------------------------------

  @Test
  void is_spring_boot_unit_test_method_in_interface_returns_false() {
    // getParentOfType(method, CLASS) returns null for methods inside interfaces (kind is INTERFACE, not CLASS)
    var cu = JParserTestUtils.parse("interface A { default void m() {} }");
    var iface = (ClassTreeImpl) cu.types().get(0);
    var method = (MethodTreeImpl) iface.members().get(0);
    assertThat(SpringUtils.isSpringBootUnitTest(method)).isFalse();
  }

  @Test
  void is_spring_boot_unit_test_in_spring_boot_test_class_returns_true() {
    var cu = JParserTestUtils.parse("A", """
      import org.junit.jupiter.api.Test;
      @org.springframework.boot.test.context.SpringBootTest
      class A {
        @Test
        void myTest() {}
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    var method = (MethodTreeImpl) clazz.members().get(0);
    assertThat(SpringUtils.isSpringBootUnitTest(method)).isTrue();
  }

  @Test
  void is_spring_boot_unit_test_in_non_spring_class_returns_false() {
    var cu = JParserTestUtils.parse("A", """
      import org.junit.jupiter.api.Test;
      class A {
        @Test
        void myTest() {}
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var clazz = (ClassTreeImpl) cu.types().get(0);
    var method = (MethodTreeImpl) clazz.members().get(0);
    assertThat(SpringUtils.isSpringBootUnitTest(method)).isFalse();
  }

}
