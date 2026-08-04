package io.quarkus.mongodb.panache;

import java.util.List;

public abstract class PanacheMongoEntityBase {
  public static <T extends PanacheMongoEntityBase> List<T> listAll() {
    return null;
  }

  public static long count() {
    return 0;
  }
}
