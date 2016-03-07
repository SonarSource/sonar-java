public enum Continent {

  NORTH_AMERICA(23, 24709000),
  EUROPE(50, 39310000);

  public int countryCount;  // Noncompliant [[sc=3;ec=9]] {{Lower the visibility of this field.}}
  public static int countryCount2;  // compliant, static field
  private int landMass;

  Continent(int countryCount, int landMass) {
  }

  public void setLandMass(int landMass) {  // Noncompliant [[sc=3;ec=9]] {{Lower the visibility of this setter or remove it altogether.}}
    this.landMass = landMass;
  }
  public void setLandMass(int landMass) { // compliant: empty setter is harmless : this can be a pattern used when implementing singleton enums.
  }
  void setLandMass(int landMass) {

  }
  public void setLandMass() {
    //do something.
  }
  public int landMassCompute(int landMass) {
    //do something.
  }
}
public enum Continent2 {
  NORTH_AMERICA (23, 24709000),
  EUROPE (50, 39310000);

  private int countryCount;
  private int landMass;

  Continent(int countryCount, int landMass) {

  }
  public abstract void setLandMass(int landMass); // Noncompliant [[sc=3;ec=9]] {{Lower the visibility of this setter or remove it altogether.}}
  public abstract void getLandMass(int landMass);
}