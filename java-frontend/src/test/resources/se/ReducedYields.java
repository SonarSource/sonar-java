package javax.annotation;

import java.util.Map;

@interface CheckForNull {}
@interface Nullable {}

abstract class ReducedYields {

  private static final String MY_CONST_1 = "1";
  private static final String MY_CONST_2 = "2";
  private static final String MY_CONST_3 = "3";
  private static final String MY_CONST_4 = "4";
  private static final String MY_CONST_5 = "5";
  private static final String MY_CONST_6 = "6";

  private void doSomething(Map<String, Object> values, boolean b) {
    values.put("a", formatValue(values.get("a"), b));
    values.put("b", formatValue(values.get("b"), b));
    values.put("c", formatValue(values.get("c"), b));
    values.put("d", formatValue(values.get("d"), b));
    values.put("e", formatValue(values.get("e"), b));
    values.put("f", formatValue(values.get("f"), b));
  }

  @CheckForNull
  private String formatValue(@Nullable Object value, boolean myTest) {
    if (value != null) {
      Double doubleValue = getValue();
      String data = getData();
      Object type = getType();
      if (giveBoolean() && data == null) {
        doubleValue = getValue();
      }
      if (type.equals(ReducedYields.MY_CONST_1) && doubleValue != null) {
        return otherformat(getValue(), doubleValue, ReducedYields.MY_CONST_1);
      }
      if (type.equals(ReducedYields.MY_CONST_2) && doubleValue != null) {
        return otherformat(getValue(), value, ReducedYields.MY_CONST_2);
      }
      if (myTest && type.equals(ReducedYields.MY_CONST_3) && doubleValue != null) {
        return otherformat(doubleValue, value, ReducedYields.MY_CONST_3) + "%";
      }
      if (!myTest && type.equals(ReducedYields.MY_CONST_4) && doubleValue != null) {
        return otherformat(getValue(), value, ReducedYields.MY_CONST_4);
      }
      if ((type.equals(ReducedYields.MY_CONST_5) || type.equals(ReducedYields.MY_CONST_6)) && data != null) {
        return data;
      }
    }
    if (!myTest && giveBoolean()) {
      throw new RuntimeException("Hasta la vista, Baby!");
    }
    return null;
  }

  @CheckForNull
  abstract Double getValue();
  @CheckForNull
  abstract String getData();
  abstract Object getType();
  abstract boolean giveBoolean();
  @CheckForNull
  abstract String otherformat(Double doubleValue, Object value, String label);
}
