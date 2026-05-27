package checks.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class OneTestLifecycleAnnotationCheckSample {

  @BeforeEach
  void setUp1() { // Noncompliant {{Only one method in a class should be annotated @BeforeEach.}}
//     ^^^^^^
    // pass
  }

  @BeforeEach
  void setUp2() { // Noncompliant {{Only one method in a class should be annotated @BeforeEach.}}
    // pass
  }

  @AfterAll
  static void classTearDown1() { // Noncompliant {{Only one method in a class should be annotated @AfterAll.}}
    // pass
  }

  @AfterEach
  void tearDown() {
    // pass
  }

  @AfterAll
  static void classTearDown2() { // Noncompliant {{Only one method in a class should be annotated @AfterAll.}}
    // pass
  }

  @Test
  void decoy() {
    fail("not important");
  }
}
