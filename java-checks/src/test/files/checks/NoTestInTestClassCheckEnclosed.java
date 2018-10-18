import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@org.junit.runner.RunWith(org.junit.experimental.runners.Enclosed.class)
public class MyNewTest { // Noncompliant {{Add some tests to this class.}}
}

@RunWith(Enclosed.class)
public class MyNew2Test { // Noncompliant {{Add some tests to this class.}}
}

@RunWith(Enclosed.class)
public class EnclosedNoInnerClassesTest { // Noncompliant {{Add some tests to this class.}}
  @Test
  public void something() {
  }

  public void testSomething() {
  }
}

@RunWith(Enclosed.class)
class EnclosedIgnoreAbstractInnerClassTest { // Noncompliant {{Add some tests to this class.}}
  abstract public static class IgnoredTest {
    @Test
    public void ignored() {
    }
  }
}

@RunWith(Enclosed.class)
class EnclosedWithPublicStaticInnerClassTest { // no issue
  public static class PublicStaticInner {
    @Test
    public void publicStaticInner() {
    }
  }
}

@RunWith(Enclosed.class)
class EnclosedWithPublicInnerClassTest { // Noncompliant {{Add some tests to this class.}}
  public class PublicInner {
    @Test
    public void publicInner() {
    }
  }
}


@RunWith(Enclosed.class)
class EnclosedWithStaticInnerClassTest { // Noncompliant {{Add some tests to this class.}}
  static class StaticInner {
    @Test
    public void staticInner() {
    }
  }
}

@RunWith(Enclosed.class)
class EnclosedExtendsTestClassTest extends SimpleTest { // Noncompliant
}

@RunWith(Enclosed.class)
class EnclosedWithInnerStaticClassExtendsTestClass { // no issue
  public static class InnerClass extends SimpleTest {
  }
}

@RunWith(Enclosed.class)
class EnclosedWithInnerClassExtendsTest { // Noncompliant
  class InnerClass extends SimpleTest {
  }
}

@RunWith(Enclosed.class)
class EnclosedExtendsWithInnerPublicClassTest extends TestsWithInnerPublicTest { // Compliant
}

@RunWith(Enclosed.class)
class EnclosedExtendsWithInnerClassTest extends TestsWithInnerTest { // Noncompliant
}


class SimpleTest {
  @Test
  public void test() {
  }
}

class TestsWithInnerPublicTest {
  public static class InnerClass {
    @Test
    public void test() {
    }
  }
}

class TestsWithInnerTest { // Noncompliant
  static class InnerClass {
    @Test
    public void test() {
    }
  }
}

@RunWith(Enclosed.class)
class EnclosedExtendsWithInnerClassTest2 extends TestsWithInnerTest2 {
}

class TestsWithInnerTest2 {
  public static class InnerClass extends TestsWithInnerTest2 {
    @Test
    public void test() {
    }
  }
}
