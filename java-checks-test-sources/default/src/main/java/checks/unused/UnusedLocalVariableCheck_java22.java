package checks.unused;

import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnusedLocalVariableCheck_java22 {
  {
    int _ = 42; // Compliant
  }


  void test() {
    record Bar(int used) { } // Compliant
    System.out.println(new Bar(42).used);
  }

  public static int count(int[] elements) {
    int count = 0;
    for (int element : elements) { // Noncompliant[[quickfixes=qf_ulv]]
//           ^^^^^^^
      // fix@qf_ulv {{Replace unused local variable with _}}
      // edit@qf_ulv [[sc=10;ec=21]]{{var _}}
      count++;
    }

    for (int a : new int[]{0, 1, 2}) { // Noncompliant[[quickfixes=qf_f1]]
//           ^
      // fix@qf_f1 {{Replace unused local variable with _}}
      // edit@qf_f1 [[sc=10;ec=15]]{{var _}}
      count++;
    }

    return count;
  }

  public static void tryWithResources() {
    try (Stream foo = Stream.of(); // Noncompliant {{Remove this unused "foo" local variable.}} [[quickfixes=qf_tr1]]
//              ^^^
//              fix@qf_tr1 {{Replace unused local variable with _}}
//              edit@qf_tr1 [[sc=10;ec=20]] {{var _}}
      Stream _ = Stream.of();
      Stream stream3 = Stream.of()) { // Noncompliant {{Remove this unused "stream3" local variable.}} [[quickfixes=qf_tr2]]
//           ^^^^^^^
//              fix@qf_tr2 {{Replace unused local variable with _}}
//              edit@qf_tr2 [[sc=7;ec=21]] {{var _}}
    } catch (Exception e) {
    }

    Stream bar = Stream.of();
    try(bar) {
    } catch (Exception e) {
    }
  }

  sealed interface Shape permits Box, Circle {}
  record Box() implements Shape { }
  record Circle() implements Shape {}

  static void switchOnSealedClass(Shape shape) {
    switch (shape) {
      case Box unused -> { } // Noncompliant
      case Circle circle -> circle.toString();
    }
  }

  static void switchWithTypePattern(Object o) {
    switch (o) {
      case Number used -> used.longValue();
      case Shape unused -> { } // Noncompliant
      default -> System.out.println();
    }
  }

  record MyRecord(int x, int y) { }

  static void switchRecordGuardedPattern(Object o) {
    switch (o) {
      case MyRecord(int x, int y) when x > 42 -> { } // Noncompliant
      //                       ^
      // fix@qfswitch1 {{Replace unused local variable with _}}
      // edit@qfswitch2 [[sc=28;ec=33]]{{var _}}
      case MyRecord(int x, int y) when y < 42 -> { } // Noncompliant
      //                ^
      // fix@qfswitch2 {{Replace unused local variable with _}}
      // edit@qfswitch2 [[sc=21;ec=26]]{{var _}}
      case MyRecord m when m.x > 42 -> { }
      case MyRecord m when o.toString().length() > 42 -> { } // Noncompliant
      case MyRecord(int x, _) -> { } // Noncompliant
      case MyRecord m -> { } // Noncompliant
      case Object object -> {
        object.toString();
        var x = 42; // Noncompliant
        System.out.println();
      }
    }
  }

  abstract class Ball {
  }

  final class RedBall extends Ball {
  }

  final class BlueBall extends Ball {
  }

  final class GreenBall extends Ball {
  }

  record BallHolder<T extends Ball>(T ball) {
  }

  record Point(int x, int y) {
  }

  record ColoredPoint(Point p, String color) {
  }


  void unnamedVariablesUseCases(Queue<Ball> queue, BallHolder<? extends Ball> ballHolder, ColoredPoint coloredPoint) {
    int total = 0;
    int _ = 1 + 1;
    java.util.function.IntUnaryOperator _ = (int _) -> 0;
    java.util.function.IntUnaryOperator _ = _ -> 0;
    java.util.function.IntBinaryOperator _ = (_, _) -> 0;
    java.util.function.IntBinaryOperator _ = (int _, int _) -> 0;
    for (Object _ : queue) { // Compliant
      total++;
    }
    System.out.println(total);
    for (int i = 0, _ = 1 + 1; i < 2; i++) {
      System.out.println(i);
    }
    while (queue.size() > 2) {
      var a = queue.remove();
      var _ = queue.remove(); // Compliant
      System.out.println(a);
    }

    try (var _ = new java.io.FileInputStream("foo.txt")) {
      queue.remove();
    } catch (Exception _) { // Compliant
      System.out.println("Exception");
    }

    queue.stream()
      .collect(Collectors.toMap(Function.identity(), _ -> 42)); // Compliant

    var ball = queue.remove();
    switch (ball) {
      case RedBall _ -> System.out.println("Red"); // Compliant
      case BlueBall _ -> System.out.println("Blue"); // Compliant
      default -> throw new IllegalStateException("Unexpected value: " + ball);
    }

    switch (ballHolder) {
      case BallHolder(RedBall _) -> System.out.println("One Red"); // Compliant
      // FIXME: the following line is commented because ECJ 3.39.0 is not able to parse it, Syntax error on the second _.
      // case BallHolder(BlueBall _), BallHolder(GreenBall _) -> System.out.println("Blue or Green Ball"); // Compliant
      case BallHolder(var _) -> System.out.println("Other"); // Compliant
    }

    switch (ballHolder) {
      // FIXME: the following line is commented because ECJ 3.39.0 is not able to parse it, Syntax error on the second _.
      // case BallHolder(RedBall _), BallHolder(BlueBall _) -> System.out.println("Red or Blue Ball"); // Compliant
      case BallHolder(_) -> System.out.println("Other Ball"); // Compliant
      default -> System.out.println("Other Ball");
    }

    if (ballHolder instanceof BallHolder(RedBall _)) { // Compliant
      System.out.println("BallHolder with RedBall");
    }

    if (coloredPoint instanceof ColoredPoint(Point(_, _), _)) { // Compliant
      System.out.println("Point (_:_) with color not important");
    }

    if (coloredPoint instanceof ColoredPoint(Point(int x, int y), _)) { // Compliant
      System.out.println("Point (" + x + ":" + y + ") with color not important");
    }

    if (coloredPoint instanceof ColoredPoint(Point(int x, int _), _)) { // Compliant
      System.out.println("Point (" + x + ":_) with color not important");
    }

    if (coloredPoint instanceof ColoredPoint(Point(_, int y), _)) { // Compliant
      System.out.println("Point (_:" + y + ") with color not important");
    }

    if (coloredPoint instanceof ColoredPoint(Point(int x, _), _)) { // Noncompliant
    }
  }

  sealed interface Tree {}
  record Node(Tree left, Tree right) implements Tree {}
  record Leaf() implements Tree {}

  void RecordPatternUnusedVars(Tree tree) {
    if (tree instanceof Node(Leaf l, _)) { // Noncompliant
    }
    if (tree instanceof Node(Node(Leaf l, _), _)) { // Noncompliant
    }
    if (tree instanceof Node(Node(Leaf l1, _), Node(_, _))) { // Noncompliant
    }
  }
}
