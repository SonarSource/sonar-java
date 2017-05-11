
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

      secondAction();  // Noncompliant {{This line will not be executed conditionally; only the first line of this 3-line block will be. The rest will execute unconditionally.}}

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
      secondAction();  // Noncompliant {{This line will not be executed in a loop; only the first line of this 4-line block will be. The rest will execute only once.}}
  }

  void compliant() {
    if (condition)
      action();
    outerAction;

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
}
