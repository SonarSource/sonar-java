/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks;

public class InnerClassShadowFieldCheckSample {

  private String outerField;
  private int outerCount;
  private static String staticOuterField;

  public class Inner {
    private String outerField; // Noncompliant {{Rename "outerField" which hides the field declared in "InnerClassShadowFieldCheckSample".}}
//                 ^^^^^^^^^^
    private int innerCount;
  }

  public class InnerWithDifferentName {
    private String innerField;
    private int innerCount;
  }

  public class InnerWithStaticOuter {
    private String staticOuterField; // Noncompliant {{Rename "staticOuterField" which hides the field declared in "InnerClassShadowFieldCheckSample".}}
//                 ^^^^^^^^^^^^^^^^
  }

  public class InnerWithSerialVersionUID {
    private static final long serialVersionUID = 1L;
  }

  public class InnerWithStaticField {
    private static String outerField;
  }

  public static class StaticNested {
    private String outerField; // Compliant - static nested class has no enclosing instance
  }

  public class DeepNesting {
    private String deepField;

    public class Level2 {
      private String level2Field;

      public class Level3 {
        private String deepField; // Noncompliant {{Rename "deepField" which hides the field declared in "DeepNesting".}}
//                     ^^^^^^^^^
        private String outerField; // Noncompliant {{Rename "outerField" which hides the field declared in "InnerClassShadowFieldCheckSample".}}
//                     ^^^^^^^^^^
        private String level2Field; // Noncompliant {{Rename "level2Field" which hides the field declared in "Level2".}}
//                     ^^^^^^^^^^^
      }
    }
  }

  void method() {
    new Object() {
      private String outerField; // Noncompliant {{Rename "outerField" which hides the field declared in "InnerClassShadowFieldCheckSample".}}
//                   ^^^^^^^^^^
    };
  }

  enum InnerEnum {
    A, B;
    private String outerField; // Compliant - nested enums are implicitly static
    private int enumOnlyField;
  }

  record InnerRecord(String outerField) { // Compliant - nested records are implicitly static
  }

  static void staticMethod() {
    class LocalInStatic {
      private String outerField; // Compliant - local class in static method has no enclosing instance
    }
    new Object() {
      private String outerField; // Compliant - anonymous class in static method has no enclosing instance
    };
  }

  class OuterA {
    private String fieldA;

    class InnerA {
      private String fieldA; // Noncompliant {{Rename "fieldA" which hides the field declared in "OuterA".}}
//                   ^^^^^^
    }
  }
}
