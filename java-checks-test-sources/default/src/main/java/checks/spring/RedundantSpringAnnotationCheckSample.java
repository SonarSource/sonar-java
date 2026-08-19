package checks.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

// === Stereotype redundancy ===

@Component // Noncompliant {{Remove this "@Component" annotation, already implied by "@Service".}}
@Service
class ComponentWithService {
}

@Component // Noncompliant {{Remove this "@Component" annotation, already implied by "@Repository".}}
@Repository
class ComponentWithRepository {
}

@Component // Noncompliant {{Remove this "@Component" annotation, already implied by "@Controller".}}
@Controller
class ComponentWithController {
}

@Component // Noncompliant {{Remove this "@Component" annotation, already implied by "@Configuration".}}
@Configuration
class ComponentWithConfiguration {
}

// === RestController composition ===

@Controller // Noncompliant {{Remove this "@Controller" annotation, already implied by "@RestController".}}
@RestController
class ControllerWithRestController {
}

@ResponseBody // Noncompliant {{Remove this "@ResponseBody" annotation, already implied by "@RestController".}}
@RestController
class ResponseBodyWithRestController {
}

// === SpringBootApplication composition ===

@Configuration // Noncompliant {{Remove this "@Configuration" annotation, already implied by "@SpringBootApplication".}}
@SpringBootApplication
class ConfigurationWithSpringBootApp {
}

@EnableAutoConfiguration // Noncompliant {{Remove this "@EnableAutoConfiguration" annotation, already implied by "@SpringBootApplication".}}
@SpringBootApplication
class EnableAutoConfigWithSpringBootApp {
}

@ComponentScan // Noncompliant {{Remove this "@ComponentScan" annotation, already implied by "@SpringBootApplication".}}
@SpringBootApplication
class ComponentScanWithSpringBootApp {
}

@SpringBootConfiguration // Noncompliant {{Remove this "@SpringBootConfiguration" annotation, already implied by "@SpringBootApplication".}}
@SpringBootApplication
class SpringBootConfigWithSpringBootApp {
}

// === Multiple redundant annotations on same class ===

@Configuration // Noncompliant {{Remove this "@Configuration" annotation, already implied by "@SpringBootApplication".}}
@EnableAutoConfiguration // Noncompliant {{Remove this "@EnableAutoConfiguration" annotation, already implied by "@SpringBootApplication".}}
@ComponentScan // Noncompliant {{Remove this "@ComponentScan" annotation, already implied by "@SpringBootApplication".}}
@SpringBootApplication
class AllRedundantWithSpringBootApp {
}

// === Spring Test redundancy ===

@ExtendWith(SpringExtension.class) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@SpringBootTest".}}
@SpringBootTest
class ExtendWithSpringExtAndSpringBootTest {
  @Test
  void test() {
  }
}

@ExtendWith(SpringExtension.class) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@WebMvcTest".}}
@WebMvcTest
class ExtendWithSpringExtAndWebMvcTest {
}

@ExtendWith(SpringExtension.class) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@DataJpaTest".}}
@DataJpaTest
class ExtendWithSpringExtAndDataJpaTest {
}

@ExtendWith(SpringExtension.class) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@WebFluxTest".}}
@WebFluxTest
class ExtendWithSpringExtAndWebFluxTest {
}

@Transactional // Noncompliant {{Remove this "@Transactional" annotation, already implied by "@DataJpaTest".}}
@DataJpaTest
class TransactionalWithDataJpaTest {
}

@ExtendWith(SpringExtension.class) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@DataJpaTest".}}
@Transactional // Noncompliant {{Remove this "@Transactional" annotation, already implied by "@DataJpaTest".}}
@DataJpaTest
class MultipleRedundantWithDataJpaTest {
}

// === Compliant cases ===

@Service
class ServiceAlone {
}

@Component
class ComponentAlone {
}

@RestController
class RestControllerAlone {
}

@SpringBootApplication
class SpringBootAppAlone {
}

@Controller
@ResponseBody
class ControllerWithResponseBody {
  @GetMapping("/foo")
  public String get() {
    return "foo";
  }
}

@SpringBootApplication
@ComponentScan(basePackages = "com.example.custom")
class SpringBootAppWithCustomComponentScan {
}

@SpringBootTest
class SpringBootTestAlone {
  @Test
  void test() {
  }
}

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@SpringBootTest
class ExtendWithMockitoAndSpringBootTest {
  @Test
  void test() {
  }
}

@SpringBootTest
@Transactional
class TransactionalWithSpringBootTest {
}

@DataJpaTest
class DataJpaTestAlone {
}

@ExtendWith({SpringExtension.class, org.mockito.junit.jupiter.MockitoExtension.class})
@SpringBootTest
class MixedExtensionsWithSpringBootTest {
}

// === Compliant: @Transactional with custom attributes + @DataJpaTest ===

@Transactional(readOnly = true)
@DataJpaTest
class TransactionalReadOnlyWithDataJpaTest {
}

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DataJpaTest
class TransactionalNotSupportedWithDataJpaTest {
}

// === Compliant: @ComponentScan with filters/other attributes + @SpringBootApplication ===

@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.excluded"))
@SpringBootApplication
class ComponentScanWithExcludeFiltersAndSpringBootApp {
}

@ComponentScan(lazyInit = true)
@SpringBootApplication
class ComponentScanWithLazyInitAndSpringBootApp {
}

@ComponentScan(useDefaultFilters = false)
@SpringBootApplication
class ComponentScanWithUseDefaultFiltersAndSpringBootApp {
}

// === Compliant: @Component with explicit attributes (bean name) ===

@Component("orders")
@Service
class ComponentWithBeanNameAndService {
}

// === Compliant: @Controller with explicit attributes (bean name) + @RestController ===

@Controller("myCustomBeanName")
@RestController
class ControllerWithBeanNameAndRestController {
}

// === Compliant: @Configuration with explicit attributes + @SpringBootApplication ===

@Configuration(proxyBeanMethods = false)
@SpringBootApplication
class ConfigurationWithProxyBeanMethodsAndSpringBootApp {
}

// === Compliant: @EnableAutoConfiguration with explicit attributes + @SpringBootApplication ===

@EnableAutoConfiguration(exclude = Configuration.class)
@SpringBootApplication
class EnableAutoConfigWithExcludeAndSpringBootApp {
}

// === Compliant: Repeatable @ExtendWith — only SpringExtension instance reported, not MockitoExtension ===

@ExtendWith(SpringExtension.class) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@SpringBootTest".}}
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@SpringBootTest
class RepeatableExtendWithSpringBootTest {
}

// === Compliant: Repeatable @ComponentScan with custom attributes — neither reported ===

@ComponentScan("com.example.pkg1")
@ComponentScan("com.example.pkg2")
@SpringBootApplication
class RepeatableComponentScanWithSpringBootApp {
}

// === @ExtendWith with single-element array syntax ===

@ExtendWith({SpringExtension.class}) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@SpringBootTest".}}
@SpringBootTest
class ExtendWithArraySingleSpringExtension {
}

@ExtendWith({SpringExtension.class}) // Noncompliant {{Remove this "@ExtendWith" annotation, already implied by "@WebMvcTest".}}
@WebMvcTest
class ExtendWithArraySingleSpringExtensionWebMvc {
}

// === Compliant: @ExtendWith with explicit value= attribute (named parameter) ===

@ExtendWith(value = SpringExtension.class)
@SpringBootTest
class ExtendWithNamedValueSpringExtension {
}

// === Compliant: @ExtendWith with single-element array containing non-SpringExtension ===

@ExtendWith({org.mockito.junit.jupiter.MockitoExtension.class})
@SpringBootTest
class ExtendWithArraySingleMockitoExtension {
}

// === Records with redundant annotations ===

@Component // Noncompliant {{Remove this "@Component" annotation, already implied by "@Service".}}
@Service
record OrderServiceRecord(String name) {
}
