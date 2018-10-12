package repro;

public class Repro<E extends Enum<E>> {

  private final Class<E> appleTypeEnum;

  public Repro(Class<E> appleTypeEnum) {
    this.appleTypeEnum = appleTypeEnum;
  }

  public Apple<E> createApple() {
    return baseBuilder(new Apple.Builder<>(), appleTypeEnum)
      .build();
  }

  private static <T extends Fruit<E>, B extends Fruit.Builder<T, B, E>, E extends Enum<E>>
  B baseBuilder(B builder, Class<E> typeEnum) {
    return builder;
  }

}
public abstract class Apple<E extends Enum<E>> extends Fruit<E> {

  public static class Builder<E extends Enum<E>> implements Fruit.Builder<Apple<E>, Apple.Builder<E>, E> {}

}

public abstract class Fruit<E extends Enum<E>> {

  public interface Builder<W extends Fruit<X>, Y extends Fruit.Builder<W, Y, X>, X extends Enum<X>> {
    default W build() {
      return null;
    }
  }

}

