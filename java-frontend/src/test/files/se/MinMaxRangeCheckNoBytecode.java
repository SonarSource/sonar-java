// place the class in the same package as the one used with bytecode
package org.sonar.java.resolve.targets.se;

// class is having the same name as the other one, which is compiled
class MinMaxRangeCheck {

  // in the other class, this constant is a string
  private static final char DOT_AS_STRING = '.';  // 46 as int
  private static final char SEMICOLUMN = ';'; // 59 as int

  public int rangeCheckWithOtherTypes(char num) {
    // constant value is resolved through bytecode, so we end up with "c" instead of 'c'
    int result = Math.min(num, DOT_AS_STRING);
    return Math.max(result, SEMICOLUMN); // Compliant
  }
}
