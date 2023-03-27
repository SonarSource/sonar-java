package checks.design;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Assertions;
import org.sonar.api.SonarProduct;

import java.util.ArrayList;
import java.util.List;

public class ClassImportCouplingCheck { // Noncompliant

    List<A> a = new ArrayList<>();
    SonarProduct sonarProduct;

    TestClass test;

    List<TestClass> someList;

    public TestClass2 method(Math math, TestClass testClass, TestClass2 testClass2) {
        TestClass2 novi = new TestClass2();
        if (novi.equals(new Object())) {


        }
        Assertions.assertTrue(true);
        return null;
    }
}
