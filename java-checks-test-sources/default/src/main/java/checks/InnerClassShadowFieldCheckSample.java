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

/**
 * Test cases for InnerClassShadowFieldCheck (S9396).
 */
public class InnerClassShadowFieldCheckSample {

  private String outerField;
  private int outerCount;
  private static String staticOuterField;

  public class Inner {
    private String outerField; // Noncompliant {{Rename "outerField" which hides the field declared in "InnerClassShadowFieldCheckSample".}}
    private int innerCount;
  }

  public class InnerWithDifferentName {
    private String innerField; // Compliant
    private int innerCount; // Compliant
  }

  public class InnerWithStaticOuter {
    private String staticOuterField; // Compliant - static fields are not shadowed
  }
}
