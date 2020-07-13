import java.lang.Deprecated;
import org.junit.experimental.runners.Enclosed;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;
import org.junit.runner.Suite;
import org.junit.runners.JUnit4;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import com.googlecode.zohhak.api.TestWith;
import com.googlecode.zohhak.api.runners.ZohhakRunner;

public class NoTestInTestClassCustomPattern {
}

class TestJUnit4WithJUnit3 { // Noncompliant
  public void test() {
  }
}

class JUnit4WithJUnit3Test { // Noncompliant
  public void test() {
  }
}

class JUnit4WithJUnit3Tests { // Noncompliant
  public void test() {
  }
}

class JUnit4WithJUnit3TestCase { // Noncompliant
  public void test() {
  }
}
