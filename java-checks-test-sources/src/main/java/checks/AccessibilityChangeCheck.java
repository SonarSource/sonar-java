package checks;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

class AccessibilityChangeCheck {
  void makeItPublic(String methodName, java.lang.reflect.AccessibleObject[] arr, boolean someBool) throws NoSuchMethodException {
    this.getClass().getMethod(methodName).setAccessible(true); // Noncompliant {{This accessibility update should be removed.}}
    this.getClass().getMethod(methodName).setAccessible(arr, true); // Noncompliant {{This accessibility update should be removed.}}
    this.getClass().getMethod(methodName).setAccessible(arr, false); // compliant
    this.getClass().getMethod(methodName).setAccessible(arr, someBool); // compliant
  }

  void setItAnyway(String fieldName, int value) throws NoSuchFieldException, IllegalAccessException {
    this.getClass().getDeclaredField(fieldName).setInt(this, value); // Noncompliant {{This accessibility bypass should be removed.}}
  }

  record Person(String name, int age) {
  }

  void changeAccessibilityOfFieldRecord() throws NoSuchFieldException, IllegalAccessException {
    Person person = new Person("A", 26);
    Field field = Person.class.getDeclaredField("name");
    field.setAccessible(true); // Compliant because reported by S6216
    field.set(person, "B"); // Compliant because reported by S6216
    Person.class.getField("name").setAccessible(true); // Compliant because reported by S6216
    Person.class.getField("name").set(person, "B"); // Compliant because reported by S6216
    Person.class.getField("name").setAccessible(true); // Compliant because reported by S6216
    Person.class.getField("name").set(person, "B"); // Compliant because reported by S6216
  }
}
