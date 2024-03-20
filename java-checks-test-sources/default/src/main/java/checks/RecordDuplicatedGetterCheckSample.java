package checks;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecordDuplicatedGetterCheckSample {

  record Being(String name, int age, double size, int friends, List<Being> ancestors, boolean alive, Color color, int tentacles) {

    private static final Being CTHULU = new Being("Cthulhu", Integer.MAX_VALUE, Double.MAX_VALUE, Integer.MIN_VALUE, Collections.emptyList(), true, Color.GREEN, 0);

    public String getName() { return name.toUpperCase(Locale.ROOT); }         // Noncompliant [[sc=19;ec=26]] {{Remove this getter 'getName()' from record and override an existing one 'name()'.}}
    public boolean isAlive() { return CTHULU.alive; }                         // Noncompliant [[sc=20;ec=27]] {{Remove this getter 'isAlive()' from record and override an existing one 'alive()'.}}
    public double getSize() { return Math.random(); }                         // Noncompliant
    public int getTentacles() { return this.friends; }                        // Noncompliant
    public void getAncestors() { assert(!ancestors.isEmpty()); }              // Noncompliant
    public Color getColor() { System.out.println("yellow"); return color(); } // Noncompliant

    @Override public int age() { return age; }
    public int getAge() {        return age + 42; } // Noncompliant [[sc=16;ec=22]] {{Remove this getter 'getAge()' from record and override an existing one 'age()'.}}
  }

  record Person(String name, int age, double size, int friends, boolean alive, Color color, float power, int tentacles) {
    @Override public String name() { return name.toUpperCase(Locale.ROOT); }
    public String getName() {        return name.toUpperCase(Locale.ROOT); }

    @Override public int age() { return age; }
    public int getAge() {        return this.age; }

    @Override public double size() { return size; }
    public double getSize() {        return size(); }

    @Override public boolean alive() { return alive; }
    public boolean isAlive() { return Being.CTHULU.alive(); } // Noncompliant

    public Color getColor() { return this.color(); }

    @Override public float power() { return getPower(); }
    public float getPower() {        return power * 10; }

    @Override public int tentacles() { return getAge(); }
    public int getTentacles() {        return tentacles * 10; } // Noncompliant
  }

  enum Color { GREEN, RAINBOW; }
}
