public enum Continent {

  NORTH_AMERICA(23, 24709000),
  EUROPE(50, 39310000);

  public int countryCount; // Noncompliant {{Lower the visibility of this field.}}
//^^^^^^
  public static int countryCount2;  // compliant, static field
  private int landMass;
  public final com.google.common.collect.ImmutableList regions; // Compliant - immutable
  private final java.util.Date date;

  Continent(int countryCount, int landMass) {
  }

  public void setLandMass(int landMass) { // Noncompliant {{Lower the visibility of this setter or remove it altogether.}}
//^^^^^^
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
  public final java.util.List regions; // Noncompliant {{Lower the visibility of this field.}}

  Continent2(int countryCount, int landMass) {

  }
  public abstract void setLandMass(int landMass); // Noncompliant {{Lower the visibility of this setter or remove it altogether.}}
//^^^^^^
  public abstract void getLandMass(int landMass);
}

public enum Continent3 {
  NORTH_AMERICA (23, 24709000),
  EUROPE (50, 39310000);

  public final int countryCount; // Compliant - final
  private int landMass;
  public final String[] regions; // Noncompliant {{Lower the visibility of this field.}}
  public final java.util.Date date; // Noncompliant {{Lower the visibility of this field.}}
  public static final String[] regions2;

  Continent3(int countryCount, int landMass) {
    this.countryCount = countryCount;
    this.landMass = landMass;
    this.regions = new String[] {"Grazelands", "Molag Amur", "Sheogorad", "West Gash"};
  }
}
