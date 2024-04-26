import java.util.stream.IntStream;

class TestSwitch {

  public record QonId(String value) {
      public QonId { // Noncompliant
      Objects.requireNonNull(value, "QonId value cannot be null");
    }
  }

  public record QonId_(String value) {
    public QonId_ { // compliant
      Objects.requireNonNull(value, "QonId value cannot be null");
    }
  }
  enum Color {
    RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW, BLACK, WHITE,
    ORANGE, BROWN, LIME, PURPLE, GREY, CRIMSON, NAVY, OLIVE,
    FUCHSIA, MAROON, AQUA, TEAL, SILVER, AZURE, BEIGE
  }

  public static void testArrowCases(Color color) {
    switch(color) {
      case RED -> { System.out.println("red"); } // Compliant
      case GREEN -> { // Compliant
        System.out.println("green");
      }
      case BLUE ->  { // Noncompliant
        System.out.println("blue");
      }
      case CYAN -> System.out.println("cyan"); // Compliant
      case MAGENTA ->System.out.println("magenta"); // Noncompliant
      case YELLOW ->  System.out.println("yellow"); // Noncompliant
      case BLACK ->
        System.out.println("black"); // Compliant
      case WHITE ->
          System.out.println("white"); // Noncompliant
    }
  }

  public static void testBlockIndentation(Color color) {
    switch(color) {
      case RED: { System.out.println("red"); } // Compliant
      case GREEN: { // Compliant
        System.out.println("green");
      }
      case BLUE:  { // Noncompliant
        System.out.println("blue");
      }
      case CYAN: {} // Compliant
      case MAGENTA: { // Compliant
      }
      case YELLOW: {
          System.out.println("yellow"); // Compliant
        } // Compliant
      case BLACK: {
          System.out.println("black");
      } // Noncompliant
      case WHITE: {
        System.out.println("white"); // Compliant
      }
      case ORANGE: {} // Compliant
      case BROWN: {
      } // Compliant
      case LIME: {
        } // Compliant
      case PURPLE: {
          } // Noncompliant {{Make this line start after 6 or 8 spaces instead of 10 in order to indent the code consistently. (Indentation level is at 2.)}}
      case GREY:
      { // Compliant
        System.out.println("grey");
      }
      case CRIMSON:
      {
        System.out.println("crimson"); // Compliant
          System.out.println("not red"); // Noncompliant
        System.out.println("not blue"); // Compliant
      }
      case NAVY:
      {
          System.out.println("navy"); // Noncompliant
      }
      case OLIVE:
      {
          System.out.println("olive"); // Noncompliant
        }
      case FUCHSIA:
        { // Compliant
          System.out.println("fuchsia");
        }
      case MAROON:
      {
        System.out.println("maroon");
        } // Noncompliant
      case AQUA:
      {
      } // Compliant
      case TEAL:
      {
        } // Noncompliant
      case SILVER:
        {
        } // Compliant
      case AZURE:
        {
          } // Noncompliant
      case BEIGE:
          { // Noncompliant
          }
    }
  }

  public static void testBlockIndentationArrowCases(Color color) {
    switch(color) {
      case RED -> { System.out.println("red"); } // Compliant
      case GREEN -> { // Compliant
        System.out.println("green");
      }
      case BLUE ->  { // Noncompliant
        System.out.println("blue");
      }
      case CYAN -> {} // Compliant
      case MAGENTA -> { // Compliant
      }
      case YELLOW -> {
          System.out.println("yellow"); // Compliant
        } // Compliant
      case BLACK -> {
          System.out.println("black");
      } // Noncompliant
      case WHITE -> {
        System.out.println("white"); // Compliant
      }
      case ORANGE -> {} // Compliant
      case BROWN -> {
      } // Compliant
      case LIME -> {
        } // Compliant
      case PURPLE -> {
          } // Noncompliant
      case GREY ->
      { // Compliant
        System.out.println("grey");
      }
      case CRIMSON ->
      {
        System.out.println("crimson"); // Compliant
      }
      case NAVY ->
      {
          System.out.println("navy"); // Noncompliant
      }
      case OLIVE ->
      {
          System.out.println("olive"); // Noncompliant
        }
      case FUCHSIA ->
        { // Compliant
          System.out.println("fuchsia");
        }
      case MAROON ->
      {
        System.out.println("maroon");
        } // Noncompliant
      case AQUA ->
      {
      } // Compliant
      case TEAL ->
      {
        } // Noncompliant
      case SILVER ->
        {
        } // Compliant
      case AZURE ->
        {
          } // Noncompliant
      case BEIGE ->
          { // Noncompliant
          }
    }
  }
}

// TODO: check empty brackets

class Foo {
  int a;                          // Compliant
   int b; // Noncompliant {{Make this line start after 2 spaces instead of 3 in order to indent the code consistently. (Indentation level is at 2.)}}
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
    System.out.println();         // Compliant
    }                             // Compliant

 public void foo2() {             // Compliant
   System.out.println("hehe"); // Noncompliant
    System.out.println();         // Compliant - already reported
  }

  public void foo3() {            // Compliant
System.out.println(); // Noncompliant
System.out.println();             // Compliant - already reported
System.out.println();             // Compliant - already reported

if (true) {                       // Compliant - already reported
  System.out.println(); // Noncompliant
  if (true) {                     // Compliant - already reported
        System.out.println();     // Compliant
    System.out.println(); // Noncompliant {{Make this line start after 8 spaces instead of 4 in order to indent the code consistently. (Indentation level is at 2.)}}
  }

      ; System.out.println();     // Compliant
}
}

  class Foo {

    int a;                        // Compliant

  int b; // Noncompliant

  }

  void foo() {
    IntStream
      .range(1, 5)
      .map((a -> {
        return a + 1;
      }));

    IntStream.range(1, 5).map(
      a -> {
        return a + 1; // Compliant
      });

    IntStream.range(1, 5)
      .map(
        a -> {
          return a + 1;
        })
      .close();

    IntStream.range(1, 5)
      .map(
        a -> {
                return a + 1; // Noncompliant
        })
      .close();

    IntStream
      .range(1, 5)
      .map(a -> {
        return a + 1; // Compliant
      }).close();

    IntStream
      .range(1, 5)
      .map(a -> {
         return a + 1; // Noncompliant
      }).close();

    IntStream
      .range(1, 5)
      .map(a ->
        a + 1
      ).close();

    IntStream
      .range(1, 5)
      .map(a -> a + 1) // Compliant
      .close();

    IntStream.range(1, 5).map(a -> a + 1);

    IntStream
      .range(1, 5)
      .map(
        a ->
          a + 1); // Compliant

    IntStream
      .range(1, 5)
      .map((
        a -> {
          return a + 1;
        }));

    IntStream
      .range(1, 5)
      .map((
        (int a) -> {
          return a + 1;
        }));

    IntStream
      .range(1, 5)
      .map(a -> {
        if (a == 5) {
          a--;
        }
         a += 1; // Noncompliant {{Make this line start after 8 spaces instead of 9 in order to indent the code consistently. (Indentation level is at 2.)}}
        return a + 1;
      });
    IntStream.range(1, 5).map(a -> {
      return a + 1;
    });

    IntStream.range(1, 2).forEach(x -> { System.out.println(x); });

    IntStream.range(1, 2).forEach(x -> {
            System.out.println(x);
          });

    Math.abs(
      IntStream.range(1, 2)
        .map(a -> {
          return a + 1;
        }).sum()
    );
  }
}

enum Bar {
  A,
 B,
   C;

  public void foo1() {            // Compliant
  }

 public void foo2() { // Noncompliant
 }
}

interface Qix {

 void foo1(); // Noncompliant

  void foo2();                    // Compliant

}

class Baz {

  void foo() {
    new MyInterface() {
      public void foo() {         // Compliant
        System.out.println();     // Compliant
          System.out.println(); // Noncompliant
      }
        public void bar() { // Noncompliant
        }
    };
  }

  int[] foo = new int[] {
    0,
    new Foo()
  };

}

 class Qiz { // Noncompliant
  public void foo() {
    switch (0) {
      case 0:
        System.out.println(); System.out.println(); // Compliant
        break;
    }

    System.out.println( // Compliant
        ); System.out.println(); // Compliant

    switch (foo) { // Compliant
    }

    switch (foo) { // Compliant
      case 0:
    }

    switch (foo) {
      case 0:
      case 1:
      case 2: {
        new Object().toString();
         new Object().toString(); // Noncompliant {{Make this line start after 8 spaces instead of 9 in order to indent the code consistently. (Indentation level is at 2.)}}
        break;
      }
      case 3: {
        new Object().toString();
      }
      new Object().toString();
      break;
      case 4:
      { // Compliant
        new Object().toString();
        break;
      }
      case 5:
        if (abs(x - z) == 0.5) {
          return x + copySign(0.5, x);
        } else {
          return z;
        }
    }

    switch (foo) {
      case 1: break; // Noncompliant
      case 2
        : case 3: break; // Compliant
    }
  };
  static {
    try{
       while (keys.hasMoreElements()) { // Noncompliant {{Make this line start after 6 spaces instead of 7 in order to indent the code consistently. (Indentation level is at 2.)}}
        s = keys.nextElement();
        rId = (String) s;
        cName = (String) exceptionClassNames.get(rId);
        exceptionRepositoryIds.put (cName, rId);
      }
    } catch (NoSuchElementException e) { }
  }
}
@interface Example {
  public static class Inner {
    public static final String FOO = "foo";
  }
}

class IndentFoo {
  public static boolean showJobDifferences(final String name, final JobsDifference.JobDifference diff, boolean onlyOkButton) {
    FutureTask<Boolean> task = new FutureTask<>(() -> {
      Optional<ButtonType> result = new DifferenceDialog(name, diff, onlyOkButton).showAndWait();
      if (result.isPresent() && (result.get() == DifferenceDialog.ACTION_YES_ALL || result.get() == DifferenceDialog.ACTION_OK_ALL)) {
        skipDialog = true;
      }
      return result.get() == DifferenceDialog.ACTION_YES || result.get() == DifferenceDialog.ACTION_YES_ALL;
    });
  }

  @Override
  protected void append(final ILoggingEvent event) {
    synchronized (lock) {
      Platform.runLater(() -> {
        Text text = new Text();
        text.setText(patternLayout.doLayout(event));
        switch (event.getLevel().levelInt) {
          case Level.DEBUG_INT:
            text.setFill(Color.BLACK);
            break;
          default:
            text.setFill(Color.BLACK);
            break;
        }
        listView.getItems().add(text);
      });
    }
  }

}

interface plop {
   @Foo
   public static class Inner { // Noncompliant
    public static final String FOO = "foo";
   }
  private <T extends Serializable> void saveMetricOnFile(Metric metric, T value) {
    sensorContext.<T>newMeasure()
      .withValue(value)
      .forMetric(metric)
      .on(inputFile)
      .save();
    this.inclusionPredicates = new Predicate[] {s -> true};
  }

  default void bar() {
    map(p -> {
      String s;
    }).foo().bar();
  }

}
