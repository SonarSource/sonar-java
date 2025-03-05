package org.sonar.java.classpath;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.classpath.DependencyVersion;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DependencyVersionInferenceServiceTest {

  @Test
  void inferAll() {
    var springClasspath = TestClasspathUtils
      .loadFromFile("../java-checks-test-sources/spring-3.2/target/test-classpath.txt");

    List<DependencyVersion> dependencyVersions = DependencyVersionInferenceService.make().inferAll(springClasspath);

    assertThat(dependencyVersions.size()).isGreaterThan(1);
  }
}
