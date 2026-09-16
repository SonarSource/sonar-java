package checks;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

class UselessIncrementCheck {
  public static int var;

  public int pickNumber() {
    int i = 0;
    int j = 0;
    if (i == 1) {
      return var++;
    } else if (i == 2) {
      return UselessIncrementCheck.var++;
    }
    i = i++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
    UselessIncrementCheck.var = UselessIncrementCheck.var++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                                       ^^
    UselessIncrementCheck.var = i++;
    return j++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//          ^^
  }

  public int pickNumber2() {
    int i = 0;
    int j = 0;
    i++; //Compliant
    UselessIncrementCheck.var = ++var;
    return ++j; //Compliant
  }

  public void lambdas() {
    int[] array = new int[5];

    IntUnaryOperator increment = value -> value++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                             ^^
    IntUnaryOperator decrement = value -> value--; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                             ^^
    UnaryOperator<Integer> inc = x -> x++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                     ^^
    UnaryOperator<Integer> dec = x -> x--; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                     ^^
    Function<Integer, Integer> f = x -> x++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                       ^^
    BiFunction<Integer, Integer, Integer> bf1 = (x, y) -> x++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                                         ^^
    BiFunction<Integer, Integer, Integer> bf2 = (x, y) -> y--; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                                         ^^
    UnaryOperator<Integer> paren = x -> (x)++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//                                         ^^

    IntUnaryOperator prefixInc = value -> ++value;
    IntUnaryOperator prefixDec = value -> --value;
    IntUnaryOperator addition = value -> value + 1;
    Consumer<Integer> consumer = value -> value++;
    Consumer<Integer> consumerDec = value -> value--;
    IntConsumer intConsumer = value -> value++;
    UnaryOperator<Integer> fieldThis = x -> this.var++;
    UnaryOperator<Integer> fieldAccess = x -> var++;
    IntUnaryOperator arrayAccess = x -> array[x]++;
    Consumer<Integer> blockBody = value -> { value++; };
  }

  public void run() {
    return;
  }
}
