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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.springcontext.InjectionPoint;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

  // ---- getBeanMethods / extractBeanNameFromMethod --------------------------------

  @Nested
  class ConfigurationClassWithBeanMethods {
    private final CompilationUnitTree compilationUnit = JParserTestUtils.parse("A", """
      class A {
        @org.springframework.context.annotation.Bean("beanName")
        Object beanMethod() { return new Object(); }

        Object nonBeanMethod() { return new Object(); }

        @org.springframework.context.annotation.Bean({"aliasOne", "aliasTwo"})
        Object anotherBeanMethod() { return new Object(); }
      
        @org.springframework.context.annotation.Bean(name = "namedBean")
        ApplicationContext namedBeanMethod() {
          return null;
        }
    
        @org.springframework.context.annotation.Bean(name = {})
        ApplicationContext emptyNameArrayMethod() {
          return null;
        }

        int field;
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    private final ClassTreeImpl configurationClass = (ClassTreeImpl) compilationUnit.types().get(0);

    @Test
    void get_bean_methods_returns_only_methods_annotated_with_bean() {
      assertThat(SpringUtils.getBeanMethods(configurationClass))
        .extracting(beanMethod -> beanMethod.simpleName().name())
        .containsExactly("beanMethod", "anotherBeanMethod", "namedBeanMethod", "emptyNameArrayMethod");
    }

    @Test
    void extract_bean_name_from_method_returns_explicit_name_or_falls_back_to_method_name() {
      var beanMethod = (MethodTreeImpl) configurationClass.members().get(0);
      var nonBeanMethod = (MethodTreeImpl) configurationClass.members().get(1);
      var anotherBeanMethod = (MethodTreeImpl) configurationClass.members().get(2);
      var namedBeanMethod = (MethodTreeImpl) configurationClass.members().get(3);
      var emptyNameArrayMethod = (MethodTreeImpl) configurationClass.members().get(4);


      assertThat(SpringUtils.extractBeanNameFromMethod(beanMethod)).containsExactly("beanName");
      assertThat(SpringUtils.extractBeanNameFromMethod(nonBeanMethod)).containsExactly("nonBeanMethod");
      assertThat(SpringUtils.extractBeanNameFromMethod(anotherBeanMethod)).containsExactly("aliasOne", "aliasTwo");
      assertThat(SpringUtils.extractBeanNameFromMethod(namedBeanMethod)).containsExactly("namedBean");
      assertThat(SpringUtils.extractBeanNameFromMethod(emptyNameArrayMethod)).containsExactly("emptyNameArrayMethod");
    }
  }

  @Test
  void get_bean_methods_returns_empty_list_when_no_bean_methods() {
    var compilationUnit = JParserTestUtils.parse("A", """
      class A {
        Object nonBeanMethod() { return new Object(); }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var configurationClass = (ClassTreeImpl) compilationUnit.types().get(0);

    assertThat(SpringUtils.getBeanMethods(configurationClass)).isEmpty();
  }

  @Test
  void extract_bean_name_from_method_falls_back_to_method_name_when_name_array_is_empty() {
    var compilationUnit = JParserTestUtils.parse("A", """
      class A {
        @org.springframework.context.annotation.Bean({})
        Object beanMethod() { return new Object(); }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var configurationClass = (ClassTreeImpl) compilationUnit.types().get(0);
    var beanMethod = (MethodTreeImpl) configurationClass.members().get(0);

    assertThat(SpringUtils.extractBeanNameFromMethod(beanMethod)).containsExactly("beanMethod");
  }

  // ---- extractBeanNameFromAnnotation ---------------------------------------------

  @Nested
  class StereotypeAnnotatedClasses {
    private final CompilationUnitTree compilationUnit = JParserTestUtils.parse("A", """
      @org.springframework.stereotype.Component
      class NoExplicitName {}

      @org.springframework.stereotype.Component("explicitName")
      class WithExplicitName {}

      @org.springframework.stereotype.Component("")
      class WithBlankName {}

      class NoStereotypeAnnotation {}
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());

    @Test
    void falls_back_to_decapitalized_simple_name_when_no_explicit_name() {
      var clazz = (ClassTreeImpl) compilationUnit.types().get(0);
      assertThat(SpringUtils.extractBeanNameFromAnnotation(clazz.symbol().metadata(), clazz.simpleName().name()))
        .isEqualTo("noExplicitName");
    }

    @Test
    void uses_explicit_name_from_value_attribute() {
      var clazz = (ClassTreeImpl) compilationUnit.types().get(1);
      assertThat(SpringUtils.extractBeanNameFromAnnotation(clazz.symbol().metadata(), clazz.simpleName().name()))
        .isEqualTo("explicitName");
    }

    @Test
    void falls_back_to_decapitalized_simple_name_when_explicit_name_is_blank() {
      var clazz = (ClassTreeImpl) compilationUnit.types().get(2);
      assertThat(SpringUtils.extractBeanNameFromAnnotation(clazz.symbol().metadata(), clazz.simpleName().name()))
        .isEqualTo("withBlankName");
    }

    @Test
    void falls_back_to_decapitalized_simple_name_when_no_stereotype_annotation() {
      var clazz = (ClassTreeImpl) compilationUnit.types().get(3);
      assertThat(SpringUtils.extractBeanNameFromAnnotation(clazz.symbol().metadata(), clazz.simpleName().name()))
        .isEqualTo("noStereotypeAnnotation");
    }
  }

  // ---- extractQualifierValue ------------------------------------------------------

  @Nested
  class QualifierAnnotatedParameters {
    private final CompilationUnitTree compilationUnit = JParserTestUtils.parse("A", """
      class A {
        void m(
          @org.springframework.beans.factory.annotation.Qualifier("qualifierValue") Object withQualifier,
          @org.springframework.beans.factory.annotation.Qualifier("") Object withBlankQualifier,
          Object withoutQualifier) {
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    private final MethodTreeImpl method = (MethodTreeImpl) ((ClassTreeImpl) compilationUnit.types().get(0)).members().get(0);

    @Test
    void returns_qualifier_value_when_present() {
      var withQualifier = method.parameters().get(0);
      assertThat(SpringUtils.extractQualifierValue(withQualifier.symbol().metadata())).isEqualTo("qualifierValue");
    }

    @Test
    void returns_null_when_qualifier_value_is_blank() {
      var withBlankQualifier = method.parameters().get(1);
      assertThat(SpringUtils.extractQualifierValue(withBlankQualifier.symbol().metadata())).isNull();
    }

    @Test
    void returns_null_when_no_qualifier_annotation() {
      var withoutQualifier = method.parameters().get(2);
      assertThat(SpringUtils.extractQualifierValue(withoutQualifier.symbol().metadata())).isNull();
    }
  }

  // ---- collectAutowiredDependenciesOnClass -----------------------------------------

  @Nested
  class AutowiredFieldsAndSetters {
    private final CompilationUnitTree compilationUnit = JParserTestUtils.parse("OrderService", """
      class OrderService {
        @org.springframework.beans.factory.annotation.Autowired
        PaymentProcessor paymentProcessor;

        @org.springframework.beans.factory.annotation.Autowired
        @org.springframework.beans.factory.annotation.Qualifier("special")
        PaymentProcessor specialProcessor;

        NotificationService notInjected;

        @org.springframework.beans.factory.annotation.Autowired
        void setEmailService(EmailService emailService) {
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    private final ClassTreeImpl orderService = (ClassTreeImpl) compilationUnit.types().get(0);
    private final InputFile inputFile = mock(InputFile.class);
    private final Map<String, Set<InjectionPoint>> dependencies = SpringUtils.collectAutowiredDependenciesOnClass(orderService, inputFile);

    @Test
    void autowired_field_without_qualifier_is_registered_using_its_field_name() {
      assertThat(dependencies.get("PaymentProcessor"))
        .extracting(InjectionPoint::name)
        .contains("paymentProcessor");
    }

    @Test
    void autowired_field_with_qualifier_uses_qualifier_value_instead_of_field_name() {
      assertThat(dependencies.get("PaymentProcessor"))
        .extracting(InjectionPoint::name)
        .contains("special");
    }

    @Test
    void non_autowired_field_is_not_registered() {
      assertThat(dependencies).doesNotContainKey("NotificationService");
    }

    @Test
    void autowired_setter_parameter_is_registered() {
      assertThat(dependencies.get("EmailService"))
        .extracting(InjectionPoint::name)
        .containsExactly("emailService");
    }

    @Test
    void injection_points_carry_the_given_input_file() {
      assertThat(dependencies.values().stream().flatMap(Set::stream))
        .extracting(point -> point.location().inputFile())
        .containsOnly(inputFile);
    }
  }

  @Test
  void collect_autowired_dependencies_uses_implicit_single_constructor_when_none_is_autowired() {
    var compilationUnit = JParserTestUtils.parse("OrderService", """
      class OrderService {
        OrderService(PaymentProcessor paymentProcessor) {
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var orderService = (ClassTreeImpl) compilationUnit.types().get(0);
    var inputFile = mock(InputFile.class);

    var dependencies = SpringUtils.collectAutowiredDependenciesOnClass(orderService, inputFile);

    assertThat(dependencies.get("PaymentProcessor"))
      .extracting(InjectionPoint::name)
      .containsExactly("paymentProcessor");
  }

  @Test
  void collect_autowired_dependencies_ignores_multiple_unannotated_constructors() {
    var compilationUnit = JParserTestUtils.parse("OrderService", """
      class OrderService {
        OrderService() {
        }
        OrderService(PaymentProcessor paymentProcessor) {
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var orderService = (ClassTreeImpl) compilationUnit.types().get(0);
    var inputFile = mock(InputFile.class);

    assertThat(SpringUtils.collectAutowiredDependenciesOnClass(orderService, inputFile)).isEmpty();
  }

  @Test
  void collect_autowired_dependencies_uses_only_the_autowired_constructor_when_others_are_unannotated() {
    var compilationUnit = JParserTestUtils.parse("OrderService", """
      class OrderService {
        @org.springframework.beans.factory.annotation.Autowired
        OrderService(PaymentProcessor paymentProcessor) {
        }
        OrderService(EmailService emailService) {
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var orderService = (ClassTreeImpl) compilationUnit.types().get(0);
    var inputFile = mock(InputFile.class);

    var dependencies = SpringUtils.collectAutowiredDependenciesOnClass(orderService, inputFile);

    assertThat(dependencies).containsOnlyKeys("PaymentProcessor");
  }

  // ---- collectDependenciesOnMethod --------------------------------------------------

  @Nested
  class MethodParametersAsDependencies {
    private final CompilationUnitTree compilationUnit = JParserTestUtils.parse("BeanFactory", """
      class BeanFactory {
        @org.springframework.context.annotation.Bean
        Object createBean(
          PaymentProcessor paymentProcessor,
          @org.springframework.beans.factory.annotation.Qualifier("special") PaymentProcessor specialProcessor,
          EmailService emailService) {
          return new Object();
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    private final MethodTreeImpl createBean = (MethodTreeImpl) ((ClassTreeImpl) compilationUnit.types().get(0)).members().get(0);
    private final InputFile inputFile = mock(InputFile.class);
    private final Map<String, Set<InjectionPoint>> dependencies = SpringUtils.collectDependenciesOnMethod(createBean, inputFile);

    @Test
    void parameters_are_grouped_by_declared_type() {
      assertThat(dependencies).containsOnlyKeys("PaymentProcessor", "EmailService");
    }

    @Test
    void parameter_without_qualifier_uses_its_own_name() {
      assertThat(dependencies.get("PaymentProcessor"))
        .extracting(InjectionPoint::name)
        .contains("paymentProcessor");
    }

    @Test
    void parameter_with_qualifier_uses_the_qualifier_value() {
      assertThat(dependencies.get("PaymentProcessor"))
        .extracting(InjectionPoint::name)
        .contains("special");
    }

    @Test
    void injection_points_carry_the_given_input_file() {
      assertThat(dependencies.values().stream().flatMap(Set::stream))
        .extracting(point -> point.location().inputFile())
        .containsOnly(inputFile);
    }
  }

  @Test
  void collect_dependencies_on_method_returns_empty_map_when_no_parameters() {
    var compilationUnit = JParserTestUtils.parse("BeanFactory", """
      class BeanFactory {
        Object createBean() {
          return new Object();
        }
      }
      """, TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    var createBean = (MethodTreeImpl) ((ClassTreeImpl) compilationUnit.types().get(0)).members().get(0);
    var inputFile = mock(InputFile.class);

    assertThat(SpringUtils.collectDependenciesOnMethod(createBean, inputFile)).isEmpty();
  }

}
