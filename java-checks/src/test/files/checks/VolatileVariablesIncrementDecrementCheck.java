class A {
  private volatile int count1 = 0;
  private volatile long count2 = 0L;
  private volatile Integer count3 = 0;
  private volatile Long count4 = 0L;
  private volatile double noAtomicCounterPart = 0.0;
  private int nonVolatileCount1 = 0;
  private float nonVolatileCount2 = 0f;

  private volatile boolean boo1 = false;
  private boolean boo2 = false;

  public void incrementCounts() {
    count1++; // Noncompliant {{Use an "AtomicInteger" for this field; its increments are atomic.}}
    ++this.count1; // Noncompliant {{Use an "AtomicInteger" for this field; its increments are atomic.}}
    (count2)++; // Noncompliant {{Use an "AtomicLong" for this field; its increments are atomic.}}
    (++count2); // Noncompliant {{Use an "AtomicLong" for this field; its increments are atomic.}}
    count3++; // Noncompliant {{Use an "AtomicInteger" for this field; its increments are atomic.}}
    ++count3; // Noncompliant
    count4++; // Noncompliant {{Use an "AtomicLong" for this field; its increments are atomic.}}
    ++count4; // Noncompliant
    nonVolatileCount1++;
    ++nonVolatileCount1;
    nonVolatileCount2++;
    ++nonVolatileCount2;
    noAtomicCounterPart++;
    ++noAtomicCounterPart;
  }

  public void decrementCounts() {
    count1--; // Noncompliant {{Use an "AtomicInteger" for this field; its decrements are atomic.}}
    --count1; // Noncompliant
    (count2)--; // Noncompliant {{Use an "AtomicLong" for this field; its decrements are atomic.}}
    (--count2); // Noncompliant
    count3--; // Noncompliant
    --count3; // Noncompliant
    count4--; // Noncompliant
    --count4; // Noncompliant
    nonVolatileCount1--;
    --nonVolatileCount1;
    nonVolatileCount2--;
    --nonVolatileCount2;
    noAtomicCounterPart--;
    --noAtomicCounterPart;
  }

  public boolean toggleBooleans(){
    boo1 = !boo1;  // Noncompliant {{Use an "AtomicBoolean" for this field}}
    boo1 = (!boo1);  // Noncompliant
    boo1 = !(boo1);  // Noncompliant
    this.boo1 = (!this.boo1);  // Noncompliant
    boo2 = !boo2;
    boo2 = !boo1;
    this.boo2 = (!this.boo1);
    boo1 = !boo2;
    boo1 = !true;
    bool1 = boo2 = !false;
    return !boo1;
  }
}
