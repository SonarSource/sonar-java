package javax.annotation;

import java.util.List;

import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.LightweightPeer;
import java.awt.peer.WindowPeer;
import java.beans.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.logging.*;
import sun.awt.AppContext;
import sun.awt.DebugHelper;
import sun.awt.HeadlessToolkit;
import sun.awt.SunToolkit;
import sun.awt.CausedFocusEvent;
import static java.lang.Boolean.TRUE;

@interface CheckForNull {}
@interface Nullable {}

public class Class extends SuperClass {

  private static class Class1 {
    Object field;

    Object method() {
      if (field != null) {
        return field;
      }
      return null;
    }
  }

  private Object instanceVariable;
  private boolean field, field1, field2;
  private Boolean preAssignedBoolean = true;

  public void assign(boolean parameter) {
    parameter = false;
    if (parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      if (parameter) { // Compliant, unreachable
      }
    }
    if (!parameter) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      if (!parameter) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
  }

  public void conditional_and(boolean parameter1, boolean parameter2, boolean parameter3) {
    if (false && false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false && true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false && parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    // Noncompliant@+1
    if (true && false) { // Noncompliant
    }
    // Noncompliant@+1 {{Remove this expression which always evaluates to "true"}}
    if (true && true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true && parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 && false) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (parameter1 && true) { // Noncompliant [[sc=23;ec=27]] {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 && parameter2) { // Compliant, unknown
      if(parameter3 || (!parameter3)){} // Noncompliant [[sc=25;ec=36]] {{Remove this expression which always evaluates to "true"}}
    }

  }

  void precise_issue_location(int max, int min, int R, boolean param) {

    if (max != R && (min == R || min > max)) {
    } else if (min < R || max < R) { // Noncompliant [[sc=27;ec=34]] {{Remove this expression which always evaluates to "false"}}
    }

    if ((min == R || min > max)) {
      if (max != R && (min == R || min > max)) { // Noncompliant [[sc=36;ec=45]] {{Remove this expression which always evaluates to "true"}}

      }
    }
    while (param && true) { // Noncompliant [[sc=21;ec=25]] {{Remove this expression which always evaluates to "true"}}
      break;
    }
    do{}while (parameter1 && false); // Noncompliant [[sc=30;ec=35]] {{Remove this expression which always evaluates to "false"}}
  }

  public void bitwise_and(boolean parameter1, boolean parameter2) {
    if (false & false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false & true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false & parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true & false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (true & true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true & parameter2) { // Compliant, unknown
    }
    if (parameter1 & false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter1 & true) { // Compliant, unknown
    }
    if (parameter1 & parameter2) { // Compliant, unknown
    }
  }

  public void conditional_or(boolean parameter1, boolean parameter2) {
    // Noncompliant@+1
    if (false || false) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    // Noncompliant@+1
    if (false || true) { // Noncompliant
    }
    if (false || parameter2) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (true || false) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true || true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true || parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 || false) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (parameter1 || true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 || parameter2) { // Compliant, unknown
    }
  }

  public void bitwise_or(boolean parameter1, boolean parameter2) {
    if (false | false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false | true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (false | parameter2) { // Compliant, unknown
    }
    if (true | false) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true | true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true | parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 | false) { // Compliant, unknown
    }
    if (parameter1 | true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 | parameter2) { // Compliant, unknown
    }
  }

  public void conditional_bitwise_xor(boolean parameter1, boolean parameter2) {
    if (false ^ false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (false ^ true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (false ^ parameter2) { // Compliant, unknown
    }
    if (true ^ false) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (true ^ true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
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
    if (field == false && field == true) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (field == false || field == true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void identifier_local() {
    // local variables
    boolean localFalse = false;
    if (localFalse) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    boolean localTrue = true;
    if (localTrue) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void identifier_parameter(boolean parameter) {
    if (parameter) { // Compliant
    }
    if (parameter && !parameter) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (parameter & !parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter || !parameter) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter | !parameter) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter ^ parameter) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (parameter ^ !parameter) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
    if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (!false) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void member_select() {
    // member select
    Class instance = new Class();
    if (instance.field != null && instance.field == null) { // Compliant
    }
  }

  public void method_invocation() {
    Class1 instance = new Class1();
    if (instance.method() != null && instance.method() == null) { // Compliant
    }
  }

  public void unary_logical_complement() {
    // unary logical complement
    if (!false) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void relational_equal(boolean parameter1, boolean parameter2, boolean condition) {
    if (parameter1 == parameter2) {
      if (parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 >= parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 > parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 <= parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 < parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 != parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (parameter1 == parameter2) { // Compliant
    }
  }

  public void relational_ge(boolean parameter1, boolean parameter2) {
    if (parameter1 >= parameter2) {
      if (parameter1 >= parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 < parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
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
      if (parameter1 == parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 >= parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 > parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 <= parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 < parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 != parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
      if (j < k) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (k > j) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
    if (condition1) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    condition2 = true;
    if (condition2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void statement_control_flow(boolean condition1, boolean condition2, boolean condition3, boolean condition4) {
    for (; ; ) {
      if (condition1) {
        if (condition1) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        }
        break;
        if (condition1) { // Compliant, unreachable
        }
      }
      if (condition2) {
        if (condition2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        }
        continue;
        if (condition2) { // Compliant, unreachable
        }
      }
      if (condition3) {
        if (condition3) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        }
        return;
        if (condition3) { // Compliant, unreachable
        }
      }
      if (condition4) {
        if (condition4) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        }
        throw new RuntimeException("");
        if (condition4) { // Compliant, unreachable
        }
      }
    }
  }

  public void statement_do_while_if_after(boolean parameter1, boolean parameter2) {
    do {
    } while (parameter1 == parameter2);
    if (parameter1 == parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void statement_do_while_in_if(boolean parameter1, boolean parameter2) {
    if (parameter1 == parameter2) {
      do {
      } while (parameter1 == parameter2); // Noncompliant
    }
  }

  public void statement_for(boolean parameter1, boolean parameter2) {
    for (; parameter1 == parameter2; ) {
      if (parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
    if (parameter1 == parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void statement_if(boolean parameter1, boolean parameter2) {
    if (parameter1 == parameter2) {
      if (parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
    if (parameter1 == parameter2) { // Compliant
    }
  }

  public void statement_switch(boolean condition) {
    switch (expression) {
      case 1:
      case 2:
      case 3:
        ;
    }
    condition = true;
    if (condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void statement_synchronized(boolean condition) {
    synchronized (condition = true) {
      if (condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
  }

  public void statement_while(boolean parameter1, boolean parameter2) {
    while (parameter1 == parameter2) {
      if (parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
    if (parameter1 == parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void tests(boolean parameter1, boolean parameter2, boolean condition) {
    if (parameter1 == parameter2) { // Compliant
    }
    if (parameter1 == parameter2 && parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (parameter1 == parameter2 || parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    // Noncompliant@+1 {{Remove this expression which always evaluates to "false"}}
    if (parameter1 == parameter2 && parameter1 != parameter2) {
    }
    // Noncompliant@+1 {{Remove this expression which always evaluates to "false"}}
    if (parameter1 == parameter2 && parameter1 > parameter2) {
    }
    // Noncompliant@+1 {{Remove this expression which always evaluates to "false"}}
    if (parameter1 == parameter2 && parameter1 < parameter2) {
    }
  }
  public void tests2(boolean parameter1, boolean parameter2, boolean condition) {
    // Noncompliant@+1 {{Remove this expression which always evaluates to "true"}}
    if (parameter1 == parameter2 || parameter1 != parameter2) {
    }
    if (condition && !condition) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (condition || !condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    // Noncompliant@+1
    if ((parameter1 == parameter2 || condition) && !(parameter1 == parameter2 || condition)) {
    }
    // Noncompliant@+1
    if ((parameter1 == parameter2 || condition) || !(parameter1 == parameter2 || condition)) { // Noncompliant
    }
    // Noncompliant@+1
    if (!(parameter1 == parameter2 || condition) && (parameter1 == parameter2 || condition)) { // Noncompliant
    }
    //Noncompliant@+1
    if (!(parameter1 == parameter2 || condition) || (parameter1 == parameter2 || condition)) { // Noncompliant
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
    if (local2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void test_label() {
    label:
    while (true) { // compliant excluded from check as it is a common construction.
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
    if (a >= b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }

    if (c < d || c <= d) {
    } else {
      return;
    }
    if (c <= d) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void relationa_le(boolean parameter1, boolean parameter2) {
    if (parameter1 <= parameter2) {
      if (parameter1 > parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 <= parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
      if (parameter1 == parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 >= parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 > parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 <= parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 < parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (parameter1 != parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
    if (parameter1 < parameter2) { // Compliant
    }
  }

  public void relational_ne(boolean parameter1, boolean parameter2) {
    if (parameter1 != parameter2) {
     if (parameter1 == parameter2) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (parameter1 != parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
          if (var1 == var2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
          }
          if (var5) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
          }
          var1 = 1;
          var5 = unknown;
          break;
        case 1:
          if (var1 == var2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
      if (var3 == var4) { // Noncompliant
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
      if (this.field) { // Noncompliant
      }
      if (field1 == this.field2) { // Noncompliant
      }
    }
  }

  public void test_instance_fields2(boolean local, boolean local1, boolean local2) {
    if (field && field1 == field2 && local && local1 == local2) {
      System.out.println();
      if (field) { // Noncompliant
      }
      if (field1 == field2) { // Noncompliant
      }
      if (local) { // Noncompliant
      }
      if (local1 == local2) { // Noncompliant
      }
    }
  }

  public void test_instance_fields3(boolean local, boolean local1, boolean local2) {

    if (field && field1 == field2 && local && local1 == local2) {
      if (Integer.toString(intField).length() > 10) {
        if (field) { // Noncompliant
        }
        if (field1 == field2) { // Noncompliant
        }
        if (local) { // Noncompliant
        }
        if (local1 == local2) { // Noncompliant
        }
      }
    }
  }

  public void test_instance_fields4(boolean local, boolean local1, boolean local2) {

    this.field1 = false;
    this.field2 = this.field1;
    // Noncompliant@+1 {{Remove this expression which always evaluates to "false"}}
    if (field1 || field2) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (field1 && !field1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (this.field1 && !this.field1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }

    if (super.field && !super.field) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (super.field && !this.field) { // Compliant
    }

    if (super.field && super.field1 == super.field2) {
      if (super.field) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
      if (super.field1 == super.field2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
    // Noncompliant@+1 {{Remove this expression which always evaluates to "false"}}
    if (super.field1 || super.field2) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
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
      if (local1 == local2) { // Noncompliant
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
        if (local1 == local2) { // Noncompliant
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
      if (local1 == local2) { // Noncompliant
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
        if (local1 == local2) { // Noncompliant
        }
      }
    }
  }

  public void test_instance_fields8(boolean local, boolean local1, boolean local2) {
    if (field && field1 == field2 && local && local1 == local2) {
      instanceVariable.otherMethod();
      if (field) { // Noncompliant
      }
      if (field1 == field2) { // Noncompliant
      }
      if (local) { // Noncompliant
      }
      if (local1 == local2) { // Noncompliant
      }
    }
  }

  public void test_instance_fields9(boolean local, boolean local1, boolean local2) {

    if (field && field1 == field2 && local && local1 == local2) {
      if (instanceVariable.otherMethod()) {
        if (field) { // Noncompliant
        }
        if (field1 == field2) { // Noncompliant
        }
        if (local) { // Noncompliant
        }
        if (local1 == local2) { // Noncompliant
        }
      }
    }
  }

  public void test_array(boolean local1, boolean local2, int[] array) {
    local1 = false;
    array[(local2 = true) ? 0 : 1] = 42; // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    if (local1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (local2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
        if (parameter1 == parameter2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        }
      }
    }
    if (parameter1 == parameter2) { // False negative
    }
  }

  public void unary_negate() {
    boolean bool = !false;
    if (bool) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void conditional_operators(boolean unknown) {
    boolean condition;
    condition = false && unknown; // Noncompliant {{Remove this expression which always evaluates to "false"}}
    if (condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    condition = unknown && false;
    if (condition) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    condition = true || unknown; // Noncompliant {{Remove this expression which always evaluates to "true"}}
    if (condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    condition = unknown || true;
    if (condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void relational_unknown(Object object) {
    boolean condition;
    condition = object != null;
    if (condition) {
      if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } else {
      if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    condition = null != object;
    if (condition != null) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } else {
      if (false) { // unreachable statement
      }
    }
  }

  public void test_switch(int condition) {
    switch (condition) {
      case 0:
        return;
    }
    if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
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
      if (!local1 && local2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    } else {
      if (!local1 && local2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    }
  }

  public void test_condition_assignment(boolean local1, boolean local2) {
    if (local1 = false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      if (false) { // compliant, unreachable
      }
    } else {
      if (local1) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
    if (local2 = true) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      if (local2) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      }
    } else {
      if (false) { // compliant unreachable
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
  }

  public void test_integer_literals_split(boolean condition, int value) {
    // out of scope, must evaluate to unknown
    if (3 > 3) {

      if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    } else {
      if (!true) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
  }

  public void try_catch() {
    boolean a = false, b = false, c = false, d = false;
    try {
      foo();
      b = true;
      foo();
      c = true;
      foo();
    } catch (IllegalArgumentException e) {
      if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (b) {
      }
      c = true;
      d = true;
    } catch (Exception e) {
      if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (c) {
      }
      d = true;
    }
    if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (b) {
    }
    if (c) {
    }
    if (d) {
    }
  }

  public void try_finally() {
    boolean a = false;
    boolean b = false;
    boolean c = false;
    try {
      foo();
      b = true;
      c = true;
    } finally {
      if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
      if (b) {
      }
      if (c) {
      }
      b = true;
    }
    if (a) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (c) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
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
    if (result) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
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
    if (condition ? false : false) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }

  public void ternary3(boolean condition) {
    if (true ? condition : false) { // Noncompliant
    }
    // Noncompliant@+1
    if (true ? false : condition) { // Noncompliant
    }

    if (false ? true : condition) { // Noncompliant
    }
    // Noncompliant@+1
    if (false ? condition : true) { // Noncompliant
    }
  }

  public void ternary_with_bitwise_operators() {
    boolean b1 = false;
    boolean b2 = false;
    int value;
    value = (b1 ^ b2) ? 1 : 2; // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    value = (b1 ^ !b2) ? 1 : 2; // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
  }

  public void handlePreAssignedBoolean() {
    if (preAssignedBoolean) { // Compliant because it is a field
      System.out.print("Was true");
    }
    System.out.println();
  }

  public void handlePreAssignedBoolean() {
    Boolean preAssignedBoolean = true;
    if (preAssignedBoolean) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      System.out.print("Was true");
    }
    System.out.println();
  }

  public void handlePreAssignedBoolean() {
    Boolean preAssignedBoolean = Boolean.TRUE;
    if (preAssignedBoolean) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      System.out.print("Was true");
    }
    System.out.println();
  }

  public void constantTests() {
    String value = "default";
    if (value == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      raiseError();
    }
  }
}

class SuperClass {
  boolean field, field1, field2;
  private static final String ACCEPT_ENCODING = "";
  private static final String GZIP = "";

  Env env;
  Request request;

  class Request {
    String header(String name, String foo) {
      return null;
    }
  }
  class Env {
    Object gzip;
    Object prodMode;

    boolean gzip() {
      return gzip == null;
    }

    boolean prodMode() {
      return prodMode == null;
    }
  }

  protected boolean shouldGzip() {
    return env.gzip()
        && env.prodMode()
        && request.header(ACCEPT_ENCODING, "").contains(GZIP);
  }

  private Object mutex;
  public boolean doubleMutexCondition() {
    if (mutex == null) {
      synchronized (this) {
        if (mutex == null) {
          mutex = new Mutex();
          return true;
        }
      }
    }
    return false;
  }

  private boolean initialized;
  public boolean doubleBooleanMutexCondition() {
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          initialized = true;
          return true;
        }
      }
    }
    return false;
  }

  private void nullableMethodReturn() {
    Object foo = nullableMethod();
    if(foo == null) { // compliant, both path are possible.

    }
  }

  @CheckForNull
  private Object nullableMethod() {
    return new Class1().method();
  }

  private void annotationWithNullableAndNonnullReturn() {
    Object foo = annotationWithNullableAndNonnull();
    if(foo == null) { // compliant, nullable wins
    }
  }

  @javax.annotation.Nullable
  @javax.annotation.Nonnull
  private Object annotationWithNullableAndNonnull() {
    return hashCode() == 0 ? this : null;
  }

  static void fromEntryArray(boolean foo) {
    Entry entry = new Object();
    printState();
    boolean reusable = entry instanceof ImmutableMapEntry
        && entry.isReusable();
    printState();
    Object o = reusable ? entry : new Object(); // compliant both path are explored.
    return;
  }

  private void castNumbers(long n, long m) {
    long product = n * m;
    int truncatedProduct = (int) product;
    if (product == truncatedProduct) {
      handleProper(truncatedProduct);
    }
  }

  private void orEqualAssignement(boolean a) {
    boolean foo = false;
    foo |= a;
    if(foo) {}
    foo = false;
    foo &= a;
    if(foo) {} // Noncompliant
  }

  public void sonarJava_1391(boolean b1, boolean b2) {
    b1 &= !b2;
    if (b1) {
      log("b1 true");
    } else {
      if (b2) {  // Compliant (fixed false positive)
        log("b2 true");
      } else {
        log("b2 false");
      }
    }
  }

  public void booleanObjectAssignment() {
    boolean b = Boolean.FALSE;
    if (b) {   // Noncompliant
      log("B true");
    }
  }

  public void staticBooleanObjectAssignment() {
    boolean b = TRUE;
    if (b) {   // Noncompliant
      log("B true");
    }
  }

  public void repeatedConditions(Object a, Object b) {
    if (a == b) {
      if ( a == b) {   // Noncompliant
        log("Are same!");
      } else {
        log("Not same!");
      }
    }
  }

  public void invertedConditions(Object a, Object b) {
    if (a == b) {
      if ( a != b) {   // Noncompliant
        log("Not same!");
      } else {
        log("Are same!");
      }
    }
  }

  public void negatedConditions(Object a, Object b) {
    if (!(a == b)) {
      if ( a != b) {   // Noncompliant
        log("Not same!");
      } else {
        log("Are same!");
      }
    }
  }

  public void invertedConditionsNotFirst(Object a, Object b) {
    if (a != b) {
      if ( a == b) {   // Noncompliant
        log("Are same!");
      } else {
        log("Not same!");
      }
    }
  }

  public void transitiveConditions(Object a, Object b, Object c) {
    if (a == b) {
      if (b == c) {
        if ( a == c) {   // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
          log("Are same!");
        } else {
          log("Not same!");
        }
      }
    }
  }

  public void nonTransitiveConditions(Object a, Object b, Object c) {
    if (a != b) {
      if (b != c) {
        if ( a != c) {
          log("Are same!");
        } else {
          log("Not same!");
        }
      }
    }
  }

  void foo(Object a, Object b, Object c) {
    if(a!=c) {
      if(b!=c) {
        if(a==b) { // nothing to say

        }
      }
    }

    if(a==c) {
      if(b!=c) {
        if(a==b) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

        }
      }
    }
  }

  public void useEquals(Object a, Object b) {
    if (a.equals(b)) {
      log("Are equal!");
      if (!a.equals(b)) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
        log("Not equal!");
      }
    }
  }

  public void negateEquals(Object a, Object b) {
    if (!a.equals(b)) {
      log("Not equal!");
      if (a.equals(b)) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
        log("Are equal!");
      }
    }
  }

  private void disableRulesDebt(List<RuleDto> ruleDtos, Integer a) {
    for (RuleDto ruleDto : ruleDtos) {
      if (a.equals(ruleDto.getSubCharacteristicId())) {
        ruleDto.setSubCharacteristicId(RuleDto.DISABLED_CHARACTERISTIC_ID);
      }
      if (a.equals(ruleDto.getDefaultSubCharacteristicId())) {
        ruleDto.setDefaultSubCharacteristicId(null);
      }
    }
  }

  public void equalsAfterEqual(Object a, Object b) {
    // Same as expression in method tests
    if (a == b || a.equals(b)) { // Compliant "!=" does not imply "equals"
    }
  }

  public void equalsBeforeEqual(Object a, Object b) {
    if (a.equals(b) || a == b) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
  }

  public void notNullAfterCall(Object a) {
    a.toString();
    if (a == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      log("Error");
    }
  }

  void SONARJAVA_1485(boolean condition) {
    boolean still = false;
    for (Foo foo : foos) {
      for (Foo foo2 : foos) {
        if (condition) {
          still = true;
        }
      }
    }
    if (still) {
    }
  }

  public void incrementChange(int n, int m) {
    int i = n;
    if( i == m) {
      ++i;
      if ( i == m) {
        log("equality");
      }
    }
  }

  public void decrementChange(int n, int m) {
    int i = n;
    if( i == m) {
      --i;
      if ( i == m) {
        log("equality");
      }
    }
  }

  void equalsDoesNotImpliesNull(Object o, Object v) {
    if(o.equals(v) || v==null) {

    }

  }

  void conjunctionEqual(Integer a, Integer b) {
    if( a <= b) {
      if( a >= b) {
        if(a == b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

        }
        if(a.equals(b)) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

        }
      }
    }
  }

  void conjunctionLessThan(Integer a, Integer b) {
    if( a <= b) {
      if(a < b) {

      }
      if( a != b) {
        if(a < b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

        }
      }
      if( !a.equals(b)) {
        if(a < b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

        }
      }
    }
  }

  void conjunctionGreaterThan(Integer a, Integer b) {
    if( a >= b) {
      if(a > b) {

      }
      if( a != b) {
        if(a > b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

        }
      }
      if( !a.equals(b)) {
        if(a > b) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

        }
      }
    }
  }

  void foo(int x, int y) {
    x = y;
    if(x<=y) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

    }
  }

  void testAfterAddAssignment(int y) {
    int x = y;
    if(x == y) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

    }
    x += y;
    if(x == y) { // Compliant (unless y==0!)

    }
  }
}

public class TryCatchCFG {

  private Object monitor;
  private boolean shutdown;

  private void doSomething() {
  }

  void fun(boolean abort) {
    while (!abort) {
      try {
        synchronized (monitor) {
          long delay = 1000L;
          while (!shutdown && delay > 0) {
            long now = System.currentTimeMillis();
            monitor.wait(delay);
            delay -= (System.currentTimeMillis() - now);
          }
          if (shutdown) {
            abort = true;
          }
          doSomething(); // may throw an exception
        }

      } catch (RuntimeException e) {
        if (abort) {
          System.out.println("Abort");
        } else {
          System.out.println("Retry");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}

public class MultiThread {

  private final Object monitor = new Object();
  private boolean shutdown;

  public void run() {
    shutdown = false;
    long delay = 1000L;
    monitor.wait(delay);
    if (shutdown) { // Compliant since shutdown could have been modified during wait()
      System.out.println("Shutdown");
    }
  }
}

class BooleanWrapper {
  void test1(Boolean condition) {
    if (Boolean.FALSE.equals(condition)) {
    } else if (Boolean.TRUE.equals(condition)) {
    } else if (condition == null) { // Noncompliant
    }
  }

  void test2(Boolean condition) {
    if (Boolean.TRUE.equals(condition)) {
    } else if (condition == null) {
    } else if (Boolean.FALSE.equals(condition)) { // Noncompliant
    }
  }

  void test3(Boolean condition) {
    if (condition == null) {
    } else if (Boolean.FALSE.equals(condition)) {
    } else if (Boolean.TRUE.equals(condition)) { // Noncompliant
    }
  }

  void test4(Boolean condition) {
    if (Boolean.TRUE.equals(condition)) {
      if (Boolean.FALSE.equals(condition)) {} // Noncompliant
      if (null == condition) {} // Noncompliant
    }
  }

  void test5(Boolean condition) {
    if (null != condition) {
      if (Boolean.FALSE.equals(condition)) {
      } else if (Boolean.TRUE.equals(condition)) {} // Noncompliant
    }
  }
}
class UtilObjects {
  void fun(Object a, Object b) {
    if(java.util.Objects.equals(a, b)) {
      if(a.equals(b)) // Noncompliant
      {}
    }
  }
}

class VolatileFields {
  private volatile boolean volatileField = false;

  void bar() {
    boolean a = volatileField;
    if (volatileField) {
      return;
    }
    while (true) {
      if (a) {} // Compliant as we don't known the state of volatileField prior to the method
    }
  }

  void qix() {
    boolean a = volatileField;
    if (a) {
      return;
    }
    while (true) {
      if (a) {} // Noncompliant
    }
  }

  void foo() {
    if (volatileField) {
      return;
    }
    while (true) {
      if (volatileField) {} // Compliant as this field is volatile, it can be modified by another thread
    }
  }
class DITO {
  DITO() throws MyExceptionFoo {}
}
  class MyExceptionFoo {}
  void plop(Object result) {
    if (result == null) {
      try {
        result = new DITO();
      } catch (final MyExceptionFoo ie) {
      }
      if (result != null) { // compliant : constructor can throw an exception and so result is null
        System.out.println("");;
      }
    }
  }

  public void reschedule() {
    Throwable scheduleFailure = null;
    try {
      executor.schedule(this, schedule.delay, schedule.unit);
    } catch (java.lang.Throwable e) {
      scheduleFailure = e;
    } finally {
      System.out.println("");
    }
    if (scheduleFailure != null) {
    }
  }
}

class UsingLong {
  Long woo(boolean b) {
    Long myLong = null;
    if (b) {
      myLong = 0L;
    }
    if (myLong != null) { // Compliant
    }
    return myLong;
  }
}
class KeyboardFocusManager {

  // This huge method requires more than 10_000 steps to be analyzed after introduction of try catch flow modelization.
  static void processCurrentLightweightRequests() {
    KeyboardFocusManager manager = getCurrentKeyboardFocusManager();
    LinkedList localLightweightRequests = null;

    Component globalFocusOwner = manager.getGlobalFocusOwner();
    if ((globalFocusOwner != null) &&
      (globalFocusOwner.appContext != AppContext.getAppContext())) {
      return;
    }

    synchronized (heavyweightRequests) {
      if (currentLightweightRequests != null) {
        clearingCurrentLightweightRequests = true;
        disableRestoreFocus = true;
        localLightweightRequests = currentLightweightRequests;
        allowSyncFocusRequests = (localLightweightRequests.size() < 2);
        currentLightweightRequests = null;
      } else {
        return;
      }
    }

    Throwable caughtEx = null;
    try {
      if (localLightweightRequests != null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        Component lastFocusOwner = null;
        Component currentFocusOwner = null;

        for (Iterator iter = localLightweightRequests.iterator(); iter.hasNext(); ) {
          currentFocusOwner = manager.getGlobalFocusOwner();
          LightweightFocusRequest lwFocusRequest = (LightweightFocusRequest) iter.next();
          if (!iter.hasNext()) {
            disableRestoreFocus = false;
          }

          FocusEvent currentFocusOwnerEvent = null;
          if (currentFocusOwner != null) {
            currentFocusOwnerEvent = new CausedFocusEvent(currentFocusOwner,
              FocusEvent.FOCUS_LOST,
              lwFocusRequest.temporary,
              lwFocusRequest.component, lwFocusRequest.cause);
          }

          FocusEvent newFocusOwnerEvent =
            new CausedFocusEvent(lwFocusRequest.component,
              FocusEvent.FOCUS_GAINED,
              lwFocusRequest.temporary,
              currentFocusOwner == null ? lastFocusOwner : currentFocusOwner, lwFocusRequest.cause);

          if (currentFocusOwner != null) {
            ((AWTEvent) currentFocusOwnerEvent).isPosted = true;
            caughtEx = dispatchAndCatchException(caughtEx, currentFocusOwner, currentFocusOwnerEvent);
          }
          ((AWTEvent) newFocusOwnerEvent).isPosted = true;
          caughtEx = dispatchAndCatchException(caughtEx, lwFocusRequest.component, newFocusOwnerEvent);

          if (manager.getGlobalFocusOwner() == lwFocusRequest.component) {
            lastFocusOwner = lwFocusRequest.component;
          }
        }
      }
    } finally {
      clearingCurrentLightweightRequests = false;
      disableRestoreFocus = false;
      localLightweightRequests = null;
      allowSyncFocusRequests = true;
    }

    if (caughtEx instanceof RuntimeException) {
      throw (RuntimeException) caughtEx;
    } else if (caughtEx instanceof Error) {
      throw (Error) caughtEx;
    }

  }
}

public class MyConstantsTestClass {

  private final Object finalObject = new Object();

  private final Object finalNullObject = null;

  private final Object myUncertainObject;

  public MyConstantsTestClass() {
    myUncertainObject = new Object();
  }

  public MyConstantsTestClass(int a) {
    myUncertainObject = null;
  }

  public void constant(boolean parameter) {
    if (finalObject != null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (finalObject == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (finalNullObject != null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    if (finalNullObject == null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
    if (myUncertainObject == null) { // Compliant we can't be sure how we reached this path
    }
    if (myUncertainObject != null) { // Compliant we can't be sure how we reached this path
    }
  }
}

public class Squid2583 {
    private final transient ByteArrayOutputStream trasientBaos = new ByteArrayOutputStream();

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void raiseIssue() {
        if (trasientBaos != null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
            trasientBaos.reset();
        }

        if (baos != null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
            baos.reset();
        }
    }
}

class NestedMax {
  boolean a,b,c,d,e,f,g,h;
  void foo() {
    plop(new Object());
    if(a) {

    } else {

    }
  }
  // Method wich requires more than the max step for the first path of execution
  void plop(@Nullable Object param) {
    if(param == null) { // should not raise an issue.

    }
    if(a) {
      System.out.println("");
    } else {
      System.out.println("");
    }
    if (b) {
      System.out.println("");
    }else {
      System.out.println("");
    }
    if(c) {
      System.out.println("");
    }else {
      System.out.println("");
    }
    if(d) {
      System.out.println("");
    } else {
      System.out.println("");
    }
    if(e) {
      System.out.println("");
    } else {
      System.out.println("");
    }
    if (f) {
      System.out.println("");
    }else {
      System.out.println("");
    }
    if(g) {
      System.out.println("");
    }else {
      System.out.println("");
    }
    if(h) {
      System.out.println("");
    } else {
      System.out.println("");
    }
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
    System.out.println("");
  }
  boolean foo(Object o1, Object o2) {
    if(o1 == null && o2 == null)
      return false;
    if((o1 != null && o2 == null) || (o1 == null && o2 != null)) { // Noncompliant [[sc=53;ec=63]] {{Remove this expression which always evaluates to "true"}}
      return false;
    }
    return true;
  }

}

class CheckingLoops {
  void foo(java.util.List<String> words) {
    if (GoodOldForLoop.count(words)) {} // Compliant
    if (ForEachLoop.count(words)) {} // Compliant
  }

  static class GoodOldForLoop {
    private static boolean count(java.util.List<String> words) {
      boolean result = false;
      for (int i = 0; i < words.size(); i++) {
        String word = words.get(i);
        if (isWord(word)) {
          result = true;
        }
      }
      return result;
    }

    private static boolean isWord(String word) {
      return word.startsWith("hello") && word.endsWith("word");
    }
  }

  static class ForEachLoop {
    private static boolean count(java.util.List<String> words) {
      boolean result = false;
      for (String word : words) {
        if (isWord(word)) {
          result = true;
        }
      }
      return result;
    }

    private static boolean isWord(String word) {
      return word.startsWith("hello") && word.endsWith("word");
    }
  }

  private class AssertLearning {
    void method(boolean x) {
      assert x;
      if(x) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
        System.out.println("");
      }
    }
  }
}

class SimpleAssignments {
  Object myField;

  void foo() {
    this.myField = null;
    if (this.myField == null) { // Noncompliant
    }
  }

  void foobarbar() {
    myField = null;
    if (myField == null) { // Noncompliant
    }
  }

  void bar() {
    this.myField = null;
    if (myField == null) { // Noncompliant
    }
  }

  void foobar() {
    myField = null;
    if (this.myField == null) { // Noncompliant
    }
  }

  void foofoo() {
    if (myField == this.myField) { // Noncompliant
    }
    if (this.myField == myField) { // Noncompliant
    }
  }

  void foofoobar() {
    Object myField = null;
    if (myField == this.myField) { // Compliant
    }
    if (this.myField == myField) { // Compliant
    }
  }

  void foobarfoo() {
    SimpleAssignments a = new SimpleAssignments();
    if (a.myField == myField) { // Compliant
    }
    if (a.myField == this.myField) { // Compliant
    }
  }

}

class Test {
  void less_than_method_equals(int a, int b) {
    if (a < b) {
      if (java.util.Objects.equals(a, b)) {  // Noncompliant

      }
    }
  }
}

class ResetFieldWhenThisUsedAsParameter {
  static class A {
    boolean value;
    B b;

    void foo() {
      this.value = true;
      b.bar(this);
      if (value) { // Compliant
        // do something
      }
    }
  }

  static class B {
    void bar(A a) {
      // do something to 'a'
    }
  }
}

class OptionalEmptyNotPresent {
  private Optional fun() {
    java.util.Optional<String> op = java.util.Optional.empty();
    if (op.isPresent()) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      // unreachable by definition of Optional.empty()
    }
    return op;
  }
  private void orElse() {
    java.util.Optional<String> op = java.util.Optional.empty();
    String val = op.orElse(null);
    if(val == null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}

    }
  }
}

abstract class SubtypeOfErrorCaught {

  public void foo() {
    boolean fail;
    try {
      doSomething();
      fail = true;
    } catch (java.lang.AssertionError e) {
      fail = false;
    }

    if (fail) { // Compliant
      doSomethingElse();
    }
  }

  abstract void doSomething();
  abstract void doSomethingElse();
}

class OptionalWrappedValue {

  void test() {
    java.util.Optional<String> op1 = java.util.Optional.of("someString");
    String val = op1.orElse(null);
    if(val == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }

    java.util.Optional<String> op2 = java.util.Optional.ofNullable("someString");
    val = op2.orElse(null);
    if(val == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }

    java.util.Optional<String> op3 = java.util.Optional.ofNullable(null);
    val = op3.orElse("");
    if(val == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
  }
}

abstract class UnaryOperators {

  private long x;

  public void bar() {
    if (x > 0) {
      x--;
      if (x == 0) { // Compliant
        doSomething();
      }
    }
  }

  public void foo() {
    if (this.x > 0) {
      this.x--;
      if (this.x == 0) { // Compliant
        doSomething();
      }
    }
  }

  public void qix() {
    if (this.x > 0) {
      if (this.x == 0) { // Noncompliant
        doSomething();
      }
    }
  }

  public void gul(UnaryOperators a) {
    if (a.x > 0) {
      a.x--;
      if (a.x == 0) { // Compliant
        doSomething();
      }
    }
  }

  abstract void doSomething();
}

class ExcludedCornerCases {
  void test() {
    final boolean debug = false;
    //...
    if (debug) {
      // Print something
    }
    if (true) {
      // do something
    }
  }
}
