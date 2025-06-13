package checks;

import java.util.Iterator;

public class StringBufferAndBuilderConcatenationCheckSample {
  public String appendSimple() {
    return new StringBuilder()
      .append("text1")
      .append("text2")
      .toString();
  }

  public String concatThree() {
    return new StringBuilder()
      .append("text1" + "text2" + "text3") // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf2]]
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^
//            fix@qf2 {{Call "append" multiple times.}}
//            edit@qf2 [[sc=14;ec=43]] {{("text1").append("text2").append("text3")}}
      .toString();
  }

  public String concatIntegerNoncompliant() {
    return new StringBuilder()
      .append("text" + 1) // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf3]]
//            ^^^^^^^^^^
//            fix@qf3 {{Call "append" multiple times.}}
//            edit@qf3 [[sc=14;ec=26]] {{("text").append(1)}}
      .toString();
  }

  public String concatIntegerCompliant() {
    return new StringBuilder()
      .append(1 + 1)
      .toString();
  }

  public String complexArgumentList() {
    return new StringBuilder()
      .append("two " + (1 + 1)) // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf4]]
//            ^^^^^^^^^^^^^^^^
//            fix@qf4 {{Call "append" multiple times.}}
//            edit@qf4 [[sc=14;ec=32]] {{("two ").append(1 + 1)}}
      .toString();
  }

  public StringBuilder notChained() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("text1" + "text2"); // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf5]]
//                       ^^^^^^^^^^^^^^^^^
//                       fix@qf5 {{Call "append" multiple times.}}
//                       edit@qf5 [[sc=25;ec=44]] {{("text1").append("text2")}}
    return stringBuilder;
  }

  public void parameter(StringBuilder passed) {
    passed.append("hello " + "hello");  // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf6]]
//                ^^^^^^^^^^^^^^^^^^
//                fix@qf6 {{Call "append" multiple times.}}
//                edit@qf6 [[sc=18;ec=38]] {{("hello ").append("hello")}}
  }

  public String call(Iterator<String> iter) {
    return new StringBuilder()
      .append("<" + iter.next() + "/>") // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf7]]
//            ^^^^^^^^^^^^^^^^^^^^^^^^
//            fix@qf7 {{Call "append" multiple times.}}
//            edit@qf7 [[sc=14;ec=40]] {{("<").append(iter.next()).append("/>")}}
      .toString();
  }

  public String nestedCall(String s) {
    return new StringBuilder()
      .append(s.toLowerCase())
      .toString();
  }

  public String stringBufferAppendSimple() {
    return new StringBuffer()
      .append("text1")
      .append("text2")
      .toString();
  }

  public String appendWithStartEnd() {
    // Breaking this up would require changing the second and third argument.
    return new StringBuilder()
      .append("text1" + "text2" , 2, 3)
      .toString();
  }

  public String stringBufferConcatSimple() {
    return new StringBuffer()
      .append("text1" + "text2") // Noncompliant {{Use multiple calls to "append" instead of string concatenation.}} [[quickfixes=qf101]]
//            ^^^^^^^^^^^^^^^^^
//            fix@qf101 {{Call "append" multiple times.}}
//            edit@qf101 [[sc=14;ec=33]] {{("text1").append("text2")}}
      .toString();
  }
}
