package checks.spring.s6856;

import lombok.Data;
import lombok.Setter;

class ExtractSetterPropertiesTestData {

  // Class with explicit setters
  static class ExplicitSetters {
    private String name;
    private int age;
    private boolean active;

    public void setName(String name) {
      this.name = name;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    // Not a setter - has wrong return type
    public String setInvalid(String value) {
      return value;
    }

    // Not a setter - has no parameters
    public void setEmpty() {
    }

    // Not a setter - has multiple parameters
    public void setMultiple(String a, String b) {
    }

    // Not a setter - is private
    private void setPrivate(String value) {
    }

    // Not a setter - is static
    public static void setStatic(String value) {
    }
  }

  // Class with Lombok @Data (generates setters for all non-final fields)
  @Data
  static class LombokData {
    private String project;
    private int year;
    private String month;
    private final String constant = "CONST";  // Should be excluded (final)
    private static String staticField;  // Should be excluded (static)
  }

  // Class with Lombok @Setter at class level
  @Setter
  static class LombokClassLevelSetter {
    private String firstName;
    private String lastName;
    private final String id = "ID";  // Should be excluded (final)
    private static int count;  // Should be excluded (static)
  }

  // Class with Lombok @Setter at field level
  static class LombokFieldLevelSetter {
    @Setter
    private String email;
    @Setter
    private int score;
    private String noSetter;  // No @Setter, should be excluded
    private static String staticField;  // Should be excluded (static)
  }

  // Mixed: explicit setters + Lombok field-level @Setter
  static class MixedSetters {
    @Setter
    private String lombokField;
    private String explicitField;

    public void setExplicitField(String value) {
      this.explicitField = value;
    }
  }

  // Class with no setters
  static class NoSetters {
    private String field;

    public String getField() {
      return field;
    }
  }

  // Empty class
  static class EmptyClass {
  }

  // Class with only getters
  static class OnlyGetters {
    private String name;
    private int age;

    public String getName() {
      return name;
    }

    public int getAge() {
      return age;
    }
  }
}
