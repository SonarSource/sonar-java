import lombok.EqualsAndHashCode;

@EqualsAndHashCode(exclude = { "id" })
public class EqualsAndHashCodeExample {
  private transient int transientVar = 10;
  private String name;
  private double score;
  private String[] tags;
  private int id;

  public String getName() {
    return this.name;
  }
}