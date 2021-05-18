package checks;

import java.util.List;
import org.springframework.batch.item.ItemProcessor;

class ReturnEmptyArrayNotNullCheck {

  public ReturnEmptyArrayNotNullCheck() {
    return null;        
  }

  public int f11() {
    return null;        
  }
}

class ReturnEmptyArrayNotNullCheckB {
  @Unknown
  public int[] gul() {
    return null;  // Noncompliant
  }
}
