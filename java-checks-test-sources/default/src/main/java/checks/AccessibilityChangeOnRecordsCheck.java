package checks;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

public class AccessibilityChangeOnRecordsCheck {
  record Person(String name, int age) {
  }

  private void modifyAccessibility() throws IllegalAccessException, NoSuchFieldException {
    Person person = new Person("A", 26);
    Field field = Person.class.getDeclaredField("name");
    field.setAccessible(true);
    field.set(person, "B"); // Noncompliant {{Remove this private field update which will never succeed}}[[secondary=-1]]


    Field nameField = Person.class.getDeclaredField("name");
    nameField.setAccessible(false);
    nameField.set(person, "B"); // Noncompliant {{Remove this private field update which will never succeed}}

    Field ageField = Person.class.getDeclaredField("age");
    ageField.setInt(person, 42); // Noncompliant {{Remove this private field update which will never succeed}}


    Person.class.getField("age").setInt(person, 42); // Noncompliant {{Remove this private field update which will never succeed}}
    Person.class.getDeclaredField("age").setInt(person, 42); // Noncompliant {{Remove this private field update which will never succeed}}

    Person.class.getFields()[0].set(person, null); // Noncompliant {{Remove this private field update which will never succeed}}
    Person.class.getDeclaredFields()[0].set(person, null); // Noncompliant {{Remove this private field update which will never succeed}}
  }

  class ChildOfAccessibleObject extends AccessibleObject {
    private void selfModify() {
      setAccessible(true); // Compliant as S6216 only checks Records
    }
  }

  class Individual {
    private String name;
    private int age;

    Individual(String name, int age) {
      this.name = name;
      this.age = age;
    }

    private int getZero() {
      return 0;
    }
  }

  void irrelevantModifications() throws NoSuchFieldException, IllegalAccessException {
    Individual individual = new Individual("A", 26);
    Field field = Individual.class.getField("name");
    field.setAccessible(true);
    field.set(individual, "B"); // Compliant as S6216 only checks Records
  }
}
