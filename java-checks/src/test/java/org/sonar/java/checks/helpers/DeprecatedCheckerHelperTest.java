package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeprecatedCheckerHelperTest {

  @Test
  void getAnnotationAttributeValue() {
    assertValue("@Deprecated(forRemoval = true)", "forRemoval", true, Boolean.class);
    assertValue("@Deprecated(forRemoval = false)", "forRemoval", false, Boolean.class);
    assertValue("@Deprecated(value = \"test\")", "value", "test", String.class);
    assertValue("@Deprecated(since = 42)", "since", 42, Integer.class);
  }

  private <T> void assertValue(String annotationSourceCode, String attributeName, T expectedValue, Class<T> type) {
    var classTree = parseClass(annotationSourceCode + " class A {}");
    var annotation = DeprecatedCheckerHelper.deprecatedAnnotation(classTree);
    T value = DeprecatedCheckerHelper.getAnnotationAttributeValue(annotation, attributeName, type).orElse(null);
    assertEquals(expectedValue, value);
  }

  private ClassTree parseClass(String code) {
    CompilationUnitTree compilationUnitTree = JParserTestUtils.parse(code);
    return (ClassTree) compilationUnitTree.types().get(0);
  }

}
