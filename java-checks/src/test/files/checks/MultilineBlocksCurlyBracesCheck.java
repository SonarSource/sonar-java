
public class A {
  void nonCompliant() {
    if (condition)
      firstActionInBlock();
      secondAction();  // Noncompliant {{This line will not be executed conditionally; only the first line of this 2-line block will be. The rest will execute unconditionally.}}
    thirdAction();

    if (condition) {
    } else if (condition) {
    } else
      firstActionInBlock();

      secondAction();  // Compliant because vertical whitespace

    if (condition)
      action();
    else
      firstActionInBlock();
      secondAction();  // Noncompliant {{This line will not be executed conditionally; only the first line of this 2-line block will be. The rest will execute unconditionally.}}

    String str = null;
    for (int i = 0; i < array.length; i++)
      str = array[i];
      doTheThing(str);  // Noncompliant {{This line will not be executed in a loop; only the first line of this 2-line block will be. The rest will execute only once.}}

    while (true)
      firstActionInBlock();
      secondAction();  // Noncompliant {{This line will not be executed in a loop; only the first line of this 2-line block will be. The rest will execute only once.}}

    int[] test = new int[]{1, 2};
    for (int intValue : test)
      firstActionInBlock();
      // comment
      // bla bla bla
      secondAction(); // Compliant because vertical whitespace
  }

  void compliant() {
    if (condition)
      action();
    outerAction();

    if (condition) {
      firstActionInBlock();
      secondAction();
    } else if (condition) {
    } else {
      other();
    }
    thirdAction();

    String str = null;
    for (int i = 0; i < array.length; i++) {
      str = array[i];
      doTheThing(str);
    }
  }

  void expansion() {
    if (condition); secondAction(); // Noncompliant
    if (condition) firstActionInBlock(); secondAction();  // Noncompliant; secondAction executed unconditionally

    if (condition) firstActionInBlock();
       secondAction();  // Noncompliant {{This line will not be executed conditionally; only the first line of this 2-line block will be. The rest will execute unconditionally.}}

    if (condition) firstActionInBlock();
    secondAction();

    if (condition)
      firstActionInBlock();
    secondAction();

    for (int i = 0;i<10;i++); secondAction(); // Noncompliant
    for (int i = 0;i<10;i++) firstActionInBlock(); secondAction();  // Noncompliant; secondAction executed unconditionally

    for (int i = 0;i<10;i++) firstActionInBlock();
      secondAction();  // Noncompliant

    for (int i = 0;i<10;i++) firstActionInBlock();
    secondAction(); // compliant : indentation is not confusing
    for (int i = 0;i<10;i++)
      firstActionInBlock();
    secondAction();
  }

  void negativeIndent(boolean cond) {
      if (cond) throw new NullPointerException();
    if (cond) throw new NullPointerException();

    if (cb.position() != 0)
    cb = cb.slice();

    boolean eof = false; // Compliant, because additional vertical whitespace
  }

  void oneLiner() {
    if (context.toString().contains("pippo")) scanTree(context.getTopTree());scanTree(context.getTopTree()); // Noncompliant {{This statement will not be executed conditionally; only the first statement will be. The rest will execute unconditionally.}}
    for (String s : myList) scanTree(context.getTopTree());scanTree(context.getTopTree()); // Noncompliant {{This statement will not be executed in a loop; only the first statement will be. The rest will execute only once.}}
  }
}
