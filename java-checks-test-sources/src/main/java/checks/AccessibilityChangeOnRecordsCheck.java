package checks;

import java.lang.reflect.Field;

public class AccessibilityChangeOnRecordsCheck {
  record Person(String name, int age) {
  }

  private void modifyAccessibility() throws IllegalAccessException, NoSuchFieldException {
    Person person = new Person("A", 26);
    Field field = Person.class.getDeclaredField("name");
    field.setAccessible(true);
    field.set(person, "B"); // Noncompliant {Remove this private field update which will never succeed}[[secondary=-1]]

    Field ageField = Person.class.getDeclaredField("age");
    ageField.setInt(person, 42); // Noncompliant {Remove this private field update which will never succeed}

    Person.class.getField("age").setInt(person, 42);// Noncompliant {Remove this private field update which will never succeed}
    Person.class.getDeclaredField("age").setInt(person, 42);// Noncompliant {Remove this private field update which will never succeed}

    Person.class.getFields()[0].set(person, null);// Noncompliant {Remove this private field update which will never succeed}
    Person.class.getDeclaredFields()[0].set(person, null);// Noncompliant {Remove this private field update which will never succeed}
  }
}
