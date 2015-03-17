import lombok.ToString;

@ToString(exclude = "id")
public class ToStringExample {
  private static final int STATIC_VAR = 10;
  private String name;
  private String[] tags;
  private int id;

  public String getName() {
    return this.getName();
  }
}