public abstract class Animal {

  abstract void move();
  abstract void feed();

}

public abstract class AbstractColor {
  private int red = 0;
  private int green = 0;
  private int blue = 0;

  public int getRed()
  {
    return red;
  }
}

public interface AnimalInterface {

  void move();
  void feed();

}

public class Color {
  private int red = 0;
  private int green = 0;
  private int blue = 0;

  private Color ()
  {}

  public int getRed()
  {
    return red;
  }
}

public abstract class Lamp {

  private boolean switchLamp=false;

  public abstract void glow();

  public void flipSwitch()
  {
    switchLamp = !switchLamp;
    if (switchLamp)
    {
      glow();
    }
  }
}

public abstract class Empty {

}