
public class A {
  void nonCompliant() {
    if (condition)
      firstActionInBlock();
      secondAction();  // Noncompliant
    thirdAction();

    if (condition) {
    } else if (condition) {
    } else
      firstActionInBlock();

      secondAction();  // Noncompliant

    if (condition)
      action();
    else
      firstActionInBlock();
      secondAction();  // Noncompliant

    String str = null;
    for (int i = 0; i < array.length; i++)
      str = array[i];
      doTheThing(str);  // Noncompliant

    while (true)
      firstActionInBlock();
      secondAction();  // Noncompliant

    int[] test = new int[]{1, 2};
    for (int intValue : test)
      firstActionInBlock();
      secondAction();  // Noncompliant
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
}
