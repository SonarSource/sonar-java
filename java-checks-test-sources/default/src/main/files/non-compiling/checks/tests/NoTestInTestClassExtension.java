
public class ParentTest extends UnknowParentTest {
  protected Object setup;

  void createSetup() {
    setup = new Object();
  }
}

class ChildTest extends ParentTest {
  @Override
  void createSetup() {
    setup = new Object();
  }
}
