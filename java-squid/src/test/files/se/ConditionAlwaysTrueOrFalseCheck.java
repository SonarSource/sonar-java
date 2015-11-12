public static class Class extends SuperClass {

  private static class Class {
    Object field;

    Object method() {
      return null;
    }
  }

  private boolean field, field1, field2;

  public void assign(boolean parameter) {
    parameter = false;
    if (parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      if (parameter) { // Compliant, always false
      }
    }
    if (!parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      // false positive
      if (!parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
  }

  public void conditional_and(boolean parameter1, boolean parameter2) {
    if (false && false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false && true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false && parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    // Noncompliant@+1
    if (true && false) { // Noncompliant
    }
    // Noncompliant@+1 {{Change this condition so that it does not always evaluate to "true"}}
    if (true && true) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true && parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter1 && false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter1 && true) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter1 && parameter2) { // Compliant, unknown
    }
  }

  public void bitwise_and(boolean parameter1, boolean parameter2) {
    if (false & false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false & true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false & parameter2) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true & false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true & true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true & parameter2) { // Compliant, unknown
    }
    if (parameter1 & false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter1 & true) { // Compliant, unknown
    }
    if (parameter1 & parameter2) { // Compliant, unknown
    }
  }

  public void conditional_or(boolean parameter1, boolean parameter2) {
    // Noncompliant@+1
    if (false || false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    // Noncompliant@+1
    if (false || true) { // Noncompliant
    }
    if (false || parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true || false) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true || true) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true || parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter1 || false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter1 || true) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter1 || parameter2) { // Compliant, unknown
    }
  }

  public void bitwise_or(boolean parameter1, boolean parameter2) {
    if (false | false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false | true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (false | parameter2) { // Compliant, unknown
    }
    if (true | false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true | true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true | parameter2) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter1 | false) { // Compliant, unknown
    }
    if (parameter1 | true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter1 | parameter2) { // Compliant, unknown
    }
  }

  public void conditional_bitwise_xor(boolean parameter1, boolean parameter2) {
    if (false ^ false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false ^ true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (false ^ parameter2) { // Compliant, unknown
    }
    if (true ^ false) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (true ^ true) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true ^ parameter2) { // Compliant, unknown
    }
    if (parameter1 ^ false) { // Compliant, unknown
    }
    if (parameter1 ^ true) { // Compliant, unknown
    }
    if (parameter1 ^ parameter2) { // Compliant, unknown
    }
  }

  public void identifier_field() {
    if (field == false && field == true) { // Compliant
    }
    if (field == false || field == true) { // Compliant
    }
  }

  public void identifier_local() {
    // local variables
    boolean localFalse = false;
    if (localFalse) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    boolean localTrue = true;
    if (localTrue) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    boolean localUnknown;
    if (localUnknown) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void identifier_parameter(boolean parameter) {
    if (parameter) { // Compliant
    }
    if (parameter && !parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter & !parameter) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter || !parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter | !parameter) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (parameter ^ parameter) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter ^ !parameter) { // b-Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void instanceOf() {
    Object object = new Object();
    if (object instanceof Object) { // Compliant, false negative
    }
    if (object instanceof String) { // Compliant
    }
    object = "string";
    if (object instanceof String) { // Compliant, false negative
    }
  }

  public void literals() {
    // literals
    if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void member_select() {
    // member select
    Class instance = new Class();
    if (instance.field != null && instance.field == null) { // Compliant
    }
  }

  public void method_invocation() {
    Class instance = new Class();
    if (instance.method() != null && instance.method() == null) { // Compliant
    }
  }

  public void unary_logical_complement() {
    // unary logical complement
    if (!false) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void relational_equal(boolean parameter1, boolean parameter2, boolean condition) {
    if (parameter1 == parameter2) {
      if (parameter1 == parameter2) { // False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 >= parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 > parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 <= parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 < parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 != parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (parameter1 == parameter2) { // Compliant
    }
  }

  public void relational_ge(boolean parameter1, boolean parameter2) {
    if (parameter1 >= parameter2) {
      if (parameter1 >= parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 < parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (parameter1 >= parameter2) {
      if (parameter1 == parameter2) { // Compliant
      }
    }
    if (parameter1 >= parameter2) {
      if (parameter1 > parameter2) { // Compliant
      }
    }
    if (parameter1 >= parameter2) {
      if (parameter1 <= parameter2) { // Compliant
      }
    }
    if (parameter1 >= parameter2) {
      if (parameter1 != parameter2) { // Compliant
      }
    }
  }

  public void relational_g(boolean parameter1, boolean parameter2) {
    if (parameter1 > parameter2) {
      if (parameter1 == parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 >= parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 > parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 <= parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 < parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 != parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 > parameter2) { // Compliant
    }
  }

  public void test_invalidate_relations(int i, int j, int k) {
    if (j > i && j < k) {
      i = 1;
      if (i < j) { // Compliant
      }
      if (j > i) { // Compliant
      }
      if (j < k) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (k > j) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (i < j) {
      ++i;
      if (i < j) { // Compliant
      }
    }
    if (i < j) {
      i--;
      if (i < j) { // Compliant
      }
    }
  }

  public void statement_assign_variable() {
    boolean condition1 = true, condition2;
    if (condition1) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    condition2 = true;
    if (condition2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void statement_control_flow(boolean condition1, boolean condition2, boolean condition3, boolean condition4) {
    for (; ; ) {
      if (condition1) {
        if (condition1) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
        }
        break;
        if (condition1) { // Compliant, unreachable
        }
      }
      if (condition2) {
        if (condition2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
        }
        continue;
        if (condition2) { // Compliant, unreachable
        }
      }
      if (condition3) {
        if (condition3) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
        }
        return;
        if (condition3) { // Compliant, unreachable
        }
      }
      if (condition4) {
        if (condition4) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
        }
        throw new RuntimeException("");
        if (condition4) { // Compliant, unreachable
        }
      }
    }
  }

  public void statement_do_while(boolean parameter1, boolean parameter2) {
    if (parameter1 == parameter2) {
      do {
      } while (parameter1 == parameter2); // False negative, while loop
    }
    do {
    } while (parameter1 == parameter2); // Compliant
    if (parameter1 == parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void statement_for(boolean parameter1, boolean parameter2) {
    for (; parameter1 == parameter2; ) {
      if (parameter1 == parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 == parameter2) { // False negative
    }
  }

  public void statement_if(boolean parameter1, boolean parameter2) {
    if (parameter1 == parameter2) {
      if (parameter1 == parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 == parameter2) { // Compliant
    }
  }

  public void statement_switch() {
    switch (expression) {
      case 1:
      case 2:
      case 3:
        ;
    }
    condition = true;
    if (condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void statement_synchronized(boolean condition) {
    synchronized (condition = true) {
      if (condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
  }

  public void statement_while(boolean parameter1, boolean parameter2) {
    while (parameter1 == parameter2) {
      if (parameter1 == parameter2) { // False negative:Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 == parameter2) { // False negative
    }
  }

  public void tests(boolean parameter1, boolean parameter2, boolean condition) {
    if (parameter1 == parameter2) { // Compliant
    }
    if (parameter1 == parameter2 && parameter1 == parameter2) { // Compliant
    }
    if (parameter1 == parameter2 || parameter1 == parameter2) { // Compliant
    }
    // False negative:Noncompliant@+1 {{Change this condition so that it does not always evaluate to "false"}}
    if (parameter1 == parameter2 && parameter1 != parameter2) {
    }
    // False negative:Noncompliant@+1 {{Change this condition so that it does not always evaluate to "false"}}
    if (parameter1 == parameter2 && parameter1 > parameter2) {
    }
    // False negative:Noncompliant@+1 {{Change this condition so that it does not always evaluate to "false"}}
    if (parameter1 == parameter2 && parameter1 < parameter2) {
    }
    // False negative:Noncompliant@+1 {{Change this condition so that it does not always evaluate to "true"}}
    if (parameter1 == parameter2 || parameter1 != parameter2) {
    }
    if (condition && !condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (condition || !condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    //False negative: Noncompliant@+1 {{Change this condition so that it does not always evaluate to "false"}}
    if ((parameter1 == parameter2 || condition) && !(parameter1 == parameter2 || condition)) {
    }
    // Noncompliant@+1 {{Change this condition so that it does not always evaluate to "true"}}
    if ((parameter1 == parameter2 || condition) || !(parameter1 == parameter2 || condition)) {
    }
    // Noncompliant@+1 {{Change this condition so that it does not always evaluate to "false"}}
    if (!(parameter1 == parameter2 || condition) && (parameter1 == parameter2 || condition)) {
    }
    // Noncompliant@+1 {{Change this condition so that it does not always evaluate to "true"}}
    if (!(parameter1 == parameter2 || condition) || (parameter1 == parameter2 || condition)) {
    }
  }

  public <T> T newQualifiedIdentifier(T param) {
    Object result;
    return (T) result;
  }

  public void test_assign_invalidate(boolean condition) {
    boolean local1 = true;
    do {
      if (local1) { // Compliant
      }
      local1 = false;
      if (local1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } while (condition);
    //false positive
    if (local1) { // Noncompliant
    }
  }

  public void test_assign_invalidate(boolean condition) {
    boolean local2 = true;
    for (Object object : new ArrayList<Object>()) {
      if (local2) { // Compliant
      }
      local2 = false;
      if (local2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (local2) { // compliant
    }
  }

  public void test_assign_invalidate(boolean condition) {
    boolean local3 = true;
    for (; condition; ) {
      if (local3) { // Compliant
      }
      local3 = false;
      if (local3) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    //false positive
    if (local3) { // Noncompliant
    }
  }

  public void test_assign_invalidate(boolean condition) {
    boolean local2 = true;
    while (condition) {
      if (local2) { // Compliant
      }
      local2 = false;
      if (local2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (local2) { // Compliant
    }
  }

  public void test_label() {
    label:
    while (true) { // Compliant
    }
  }

  public void statement_if2(boolean parameter1, boolean parameter2) {
    if (parameter1 == parameter2) {
      if (parameter1) {
        parameter1 = false;
      } else {
        parameter1 = true;
      }
      if (parameter1) { // Compliant
      }
    } else {
      if (parameter1) {
        parameter1 = false;
      } else {
        parameter1 = true;
      }
    }
    if (parameter1) { // Compliant
    }
  }

  public void test_assign(boolean param1, boolean param2, boolean falseParam, boolean trueParam) {
    boolean boolAnd1 = true;
    boolAnd1 = param1 && param2;
    if (!boolAnd1) { // Compliant
    }

    boolean boolAnd2 = true;
    boolAnd2 = falseParam && param2;
    if (!boolAnd2) { // Compliant
    }

    boolean boolAnd3 = true;
    boolAnd3 = param1 && falseParam;
    if (!boolAnd3) { // Compliant
    }
  }

  public void test_assign2(boolean param1, boolean param2, boolean falseParam, boolean trueParam) {
    boolean boolOr1 = true;
    boolOr1 = param1 || param2;
    if (!boolOr1) { // Compliant
    }

    boolean boolOr2 = true;
    boolOr2 = trueParam || param1;
    if (!boolOr2) { // Compliant
    }

    boolean boolOr3 = true;
    boolOr3 = param1 || trueParam;
    if (!boolOr3) { // Compliant
    }
  }

  public void test_merge(int a, int b, int c, int d) {
    if (a < b) {
      return;
    }
    if (a >= b) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }

    if (c < d || c <= d) {
    } else {
      return;
    }
    if (c <= d) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void relationa_le(boolean parameter1, boolean parameter2) {
    if (parameter1 <= parameter2) {
      if (parameter1 > parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 <= parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 <= parameter2) {
      if (parameter1 == parameter2) { // Compliant
      }
    }
    if (parameter1 <= parameter2) {
      if (parameter1 >= parameter2) { // Compliant
      }
    }
    if (parameter1 <= parameter2) {
      if (parameter1 < parameter2) { // Compliant
      }
    }
    if (parameter1 <= parameter2) {
      if (parameter1 != parameter2) { // Compliant
      }
    }
  }

  public void relational_l(boolean parameter1, boolean parameter2) {
    if (parameter1 < parameter2) {
      if (parameter1 == parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 >= parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 > parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 <= parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 < parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (parameter1 != parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 < parameter2) { // Compliant
    }
  }

  public void relational_ne(boolean parameter1, boolean parameter2) {
    if (parameter1 != parameter2) {
      if (parameter1 == parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 != parameter2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    if (parameter1 != parameter2) {
      if (parameter1 >= parameter2) { // Compliant
      }
    }
    if (parameter1 != parameter2) {
      if (parameter1 > parameter2) { // Compliant
      }
    }
    if (parameter1 != parameter2) {
      if (parameter1 <= parameter2) { // Compliant
      }
    }
    if (parameter1 != parameter2) {
      if (parameter1 < parameter2) { // Compliant
      }
    }
  }

  public void test_switch(int condition, boolean unknown, int var1, int var2, int var3, int var4, boolean var5, boolean var6, boolean var7) {
    if (var1 == var2 && var3 == var4) {
      var5 = false;
      var7 = false;
      switch (condition) {
        case 0:
          if (var1 == var2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
          }
          if (var5) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
          }
          var1 = 1;
          var5 = unknown;
          break;
        case 1:
          if (var1 == var2) { //False negative: Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
          }
          if (var5) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
          }
          var1 = 1;
          var5 = true;
        case 2:
          if (var1 == var2) { // Compliant
          }
          if (var5) { // compliant (fallthrough)
          }
          var1 = 1;
          var6 = false;
      }
      if (var1 == var2) { // Compliant
      }
      if (var3 == var4) { // False negative:Noncompliant
      }
      if (var5) { // Compliant
      }
      if (var6) { // Compliant
      }
      if (var7) { // Noncompliant
      }
    }
  }

  public void test_switch2(int condition, boolean unknown, int var1, int var2, int var3, int var4, boolean var5, boolean var6, boolean var7) {
    var5 = false;
    switch (condition) {
      case 0:
        var5 = true;
      default:
        var5 = true;
    }
    if (var5) { // Noncompliant
    }
  }

  public void test_switch3(int condition, boolean unknown, int var1, int var2, int var3, int var4, boolean var5, boolean var6, boolean var7) {
    switch (condition) {
      default:
        var5 = true;
        if (unknown) {
          var6 = true;
          break;
        } else {
          var6 = false;
          break;
        }
        var5 = false;
    }
    if (var5) { // Noncompliant
    }
    if (var6) { // Compliant
    }
  }

  public test_instance_fields(boolean local, boolean local1, boolean local2) {
    if (field && this.field1 == field2) {
      if (this.field) { // False negative Noncompliant
      }
      if (field1 == this.field2) { //False negative Noncompliant
      }
    }
  }

  public void test_instance_fields2(boolean local, boolean local1, boolean local2) {
    if (field && field1 == field2 && local && local1 == local2) {
      System.out.println();
      if (field) { // Noncompliant
      }
      if (field1 == field2) { // Compliant
      }
      if (local) { // Noncompliant
      }
      if (local1 == local2) { //False negative Noncompliant
      }
    }
  }

  public void test_instance_fields3(boolean local, boolean local1, boolean local2) {

    if (field && field1 == field2 && local && local1 == local2) {
      if (Integer.toString(intField).length() > 10) {
        if (field) { // Noncompliant
        }
        if (field1 == field2) { // Compliant
        }
        if (local) { // Noncompliant
        }
        if (local1 == local2) { //False negative Noncompliant
        }
      }
    }
  }

  public void test_instance_fields4(boolean local, boolean local1, boolean local2) {

    this.field1 = false;
    this.field2 = this.field1;
    if (field1 || field2) { // false negative Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }

    if (super.field && !super.field) { // false negative Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (super.field && !this.field) { // Compliant
    }

    if (super.field && super.field1 == super.field2) {
      if (super.field) { // false negative Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (super.field1 == super.field2) { // false negative Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      otherMethod();
      if (super.field) { // Compliant
      }
      if (super.field1 == super.field2) { // Compliant
      }
    }
  }

  public void test_instance_fields5(boolean local, boolean local1, boolean local2) {

    super.field1 = false;
    super.field2 = super.field1;
    if (super.field1 || super.field2) { // false negative Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }

    SuperClass instance1, instance2;
    if (instance1.field && instance1.field1 == instance2.field2) {
      if (instance1.field && instance1.field1 == instance2.field2) { // false negative Noncompliant
      }
    }
    if (instance1.field && field1 == instance2.field2) {
      if (field && field1 == instance2.field2) { // Compliant
      }
    }
  }
  public void test_instance_fields6(boolean local, boolean local1, boolean local2) {
    if (field && field1 == field2 && local && local1 == local2) {
      otherMethod();
      if (field) {
      }
      if (field1 == field2) {
      }
      if (local) { // Noncompliant
      }
      if (local1 == local2) { //False negative Noncompliant
      }
    }
  }
  public void test_instance_fields7(boolean local, boolean local1, boolean local2) {

    if (field && field1 == field2 && local && local1 == local2) {
      if (otherMethod()) {
        if (field) {
        }
        if (field1 == field2) {
        }
        if (local) { // Noncompliant
        }
        if (local1 == local2) { //False negative Noncompliant
        }
      }
    }
  }

  public void test_instance_fields6(boolean local, boolean local1, boolean local2) {
    if (field && field1 == field2 && local && local1 == local2) {
      otherMethod();
      if (field) {
      }
      if (field1 == field2) {
      }
      if (local) { // Noncompliant
      }
      if (local1 == local2) { //False negative Noncompliant
      }
    }
  }

  public void test_instance_fields7(boolean local, boolean local1, boolean local2) {

    if (field && field1 == field2 && local && local1 == local2) {
      if (otherMethod()) {
        if (field) {
        }
        if (field1 == field2) {
        }
        if (local) { // Noncompliant
        }
        if (local1 == local2) { //False negative Noncompliant
        }
      }
    }
  }

  public void test_array(boolean local1, boolean local2, int[] array) {
    local1 = false;
    array[local2 = true];
    if (local1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (local2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void test_instanceof(Object object, boolean local) {
    local = object instanceof Object; // unknown for now
    if (local) { // Compliant
    }
  }

  public void member_select2() {
    // member select
    Class instance = new Class();
    instance.field = false;
    if (instance.field) { // Compliant
    }
  }

  public void statement_for(boolean parameter1, boolean parameter2) {
    for (; ; ) {
      if (parameter1 == parameter2) {
        if (parameter1 == parameter2) { // False negative Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
        }
      }
    }
    if (parameter1 == parameter2) { // False negative
    }
  }

  public void unary_negate() {
    boolean bool = !false;
    if (bool) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void conditional_operators(boolean unknown) {
    boolean condition;
    condition = false && unknown; // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    if (condition) { // False negative (evaluate expression) Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    condition = unknown && false;
    if (condition) { // False negative (evaluate expression) Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    condition = true || unknown; // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    if (condition) { // False negative (evaluate expression) Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    condition = unknown || true;
    if (condition) { // False negative (evaluate expression) Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
  }

  public void relational_unknown(Object object) {
    boolean condition;
    condition = object != null;
    if (condition) {
      if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } else {
      if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    condition = null != object;
    if (condition != null) {
      if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } else {
      if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
  }

  public void test_switch(int condition) {
    switch (condition) {
      case 0:
        return;
    }
    if (false) { // False negative (better support of switch)  Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    switch (condition) {
      case 0:
        return;
      default:
        return;
    }
    if (false) { // Compliant, unreachable
    }
  }

  public void test_condition_array(boolean local1, boolean local2, boolean[] array) {
    local1 = false;
    if (array[local2 = true ? 1 : 0]) { // Noncompliant
      if (!local1 && local2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    } else {
      if (!local1 && local2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
  }

  public void test_condition_assignment(boolean local1, boolean local2) {
    if (local1 = false) { // False negative : evaluate assignement expression : Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      //false positive: it should not be reached
      if (false) { // Noncompliant
      }
    } else {
      if (local1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (local2 = true) { // False negative : evaluate assignement expression : Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      if (local2) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    } else {
      //false positive: it should not be reached
      if (false) { // Noncompliant
      }
    }
  }

  public abstract boolean otherMethod();

  int intField;

  public void test_integer_literals(boolean condition, int value) {
    if (3 > value && value > 3) { // False negative Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    // invalidation due to merge
    if (3 > value || value > 3) { // Compliant
    }
    // two level nesting
    if (value > 0x3) {
      if (condition || value > 0x3L) { //False negative :  Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    // invalidation due to method call
    if (intField == 3 && 3 == value) {
      if (intField == 3) { // False negative Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      if (value == 3) { // False negative Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
      otherMethod();
      if (intField == 3) { // Compliant
      }
      if (value == 3) { // False negative Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      }
    }
    // out of scope, must evaluate to unknown
    if (3 > 3) {

      if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } else {
      if (false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
  }

  public void try_catch() {
    boolean a = false, b = false, c = false, d = false;
    try {
      b = true;
      c = true;
    } catch (Exception e) {
      if (a) { //false negative : evaluate catch blocks Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (b) { // Compliant
      }
      c = true;
      d = true;
    } catch (Exception e) {
      if (a) { //false negative : evaluate catch blocks Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (c) { // Compliant
      }
      d = true;
    }
    if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (b) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (c) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    //false positive !
    if (d) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void try_finally() {
    boolean a = false;
    boolean b = false;
    boolean c = false;
    try {
      b = true;
      c = true;
    } finally {
      if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (b) { // Noncompliant
      }
      if (c) { // Noncompliant
      }
      b = true;
    }
    if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (b) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    if (c) { // Noncompliant
    }
  }

  public void finally_with_return() {
    try {
    } finally {

      return;
    }
    if (false) { // Compliant, unreachable
    }
  }

  public void ternary(boolean condition) {
    boolean result;

    result = condition ? true : false;
    if (result) { // Compliant
    }

    result = true ? condition : false; // Noncompliant
    if (result) { // Compliant
    }
    result = true ? false : condition; // Noncompliant
    if (result) { // false negative : evaluate conditional Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void ternary2(boolean condition) {
    result = false ? true : condition; // Noncompliant
    if (result) { // Compliant
    }
    result = false ? condition : true; // Noncompliant
    if (result) { //false negative : evaluate conditional Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }

    if (condition ? true : false) { // Compliant
    }
    if (condition ? false : false) { //false negative : evaluate conditional  Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void ternary3(boolean condition) {
    if (true ? condition : false) { // Noncompliant
    }
    if (true ? false : condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }

    if (false ? true : condition) { // Noncompliant
    }
    if (false ? condition : true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void ternary_with_bitwise_operators() {
    boolean b1 = false;
    boolean b2 = false;
    int value;
    value = (b1 ^ b2) ? 1 : 2; // False Negative - Not handled
    value = (b1 ^ !b2) ? 1 : 2; // False Negative - Not handled
  }

}

class SuperClass {
  boolean field, field1, field2;
}
