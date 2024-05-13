package checks;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

public class AccessibilityChangeCheckWithRecordSupport {
  record Person(String name, int age) {
    Class getClass(Class received) {
      return received;
    }
    void doSomething() throws NoSuchFieldException, IllegalAccessException {
      getClass().getField("age").setAccessible(true); // Compliant because reported by S6216
      getClass().getField("age").set(this, 42); // Compliant because reported by S6216
      this.getClass().getField("name").setAccessible(true); // Compliant because reported by S6216
      this.getClass().getField("name").set(this, "B"); // Compliant because reported by S6216

      // Wrong getClass
      getClass(null).getField("name").setAccessible(true); // Noncompliant
      getClass(null).getField("name").set(this, "B"); // Noncompliant

      Class getClass = null;
      getClass.getField("name").setAccessible(true); // Noncompliant
      getClass.getField("name").set(this, "B"); // Noncompliant
    }
  }

  void accessibilityChangeOnRecordFields() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
    Person person = new Person("A", 26);

    Field field = Person.class.getDeclaredField("name");
    field.setAccessible(true); // Compliant because reported by S6216
    field.set(person, "B"); // Compliant because reported by S6216

    Person.class.getField("name").setAccessible(true); // Compliant because reported by S6216
    Person.class.getField("name").set(person, "B"); // Compliant because reported by S6216


    Field[] fields = Person.class.getDeclaredFields();
    fields[0].setAccessible(true); // Compliant because reported by S6216
    fields[0].set(person, "B"); // Compliant because reported by S6216
    fields[0].setAccessible(true); // Compliant because reported by S6216
    fields[0].set(person, "B"); // Compliant because reported by S6216

    Person.class.getFields()[0].setAccessible(true); // Compliant because reported by S6216
    Person.class.getFields()[0].set(person, "B"); // Compliant because reported by S6216
    Person.class.getDeclaredFields()[0].setAccessible(true); // Compliant because reported by S6216
    Person.class.getDeclaredFields()[0].set(person, "B"); // Compliant because reported by S6216

    Field someMagicField = getAField();
    someMagicField.setAccessible(true); // Noncompliant
    someMagicField.set(person, "B"); // Noncompliant

    getAField().setAccessible(true); // Noncompliant
    getAField().set(person, "B"); // Noncompliant

    Field nullInitializedField = null;
    nullInitializedField.setAccessible(true); // Noncompliant
    nullInitializedField.set(person, "B"); // Noncompliant

    Field fieldFromDynamicallyLoadedClass = Class.forName("org.sonar.some.package.SomeClass").getDeclaredField("");
    fieldFromDynamicallyLoadedClass.setAccessible(true); // Noncompliant
    fieldFromDynamicallyLoadedClass.set(person, "B"); // Noncompliant

    Class<? extends Record> someType = Person.class;
    someType.getFields()[0].setAccessible(true); // Compliant because reported by S6216
    someType.getFields()[0].set(person, "B"); // Compliant because reported by S6216
  }

  Field getAField() {
    return Person.class.getDeclaredFields()[0];
  }
}
