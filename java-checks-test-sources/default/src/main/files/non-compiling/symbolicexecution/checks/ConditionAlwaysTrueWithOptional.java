import java.util.Optional;

public class Squid2583 {

  private boolean isSpecial(MyObject obj) {
    return obj.getMyString().contains("B") && noEmptyNodeNames(obj) && foo(); // FP might be raised here because of optional in noEmptyNodeName method
  }

  void foo() {
  }

  private boolean noEmptyNodeNames(MyObject obj) {
    return obj.getValueOne().isPresent() && obj.getValueTwo().isPresent(); // constraint cleanup can cause absence of yields which can lead to FP.
  }

  public static final class MyObject {
    private String myString;
    private final Optional<String> valueOne;
    private final Optional<String> valueTwo;

    public MyObject() {
    }

    public String getMyString() {
      return myString;
    }

    public Optional<String> getValueOne() {
      return valueOne;
    }

    public Optional<String> getValueTwo() {
      return valueTwo;
    }
  }

}
