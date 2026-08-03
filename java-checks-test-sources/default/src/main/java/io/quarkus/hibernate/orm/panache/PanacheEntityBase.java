package io.quarkus.hibernate.orm.panache;

import java.util.List;

public abstract class PanacheEntityBase {
  public static <T extends PanacheEntityBase> List<T> listAll() {
    return null;
  }

  public static long count() {
    return 0;
  }
}
