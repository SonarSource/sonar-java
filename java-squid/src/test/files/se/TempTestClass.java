package javax.annotation;

@interface CheckForNull {}
public class TempTestClass {

  public static void castedComparison(String string, int count) {
    final int len = string.length();
    final long longSize = (long) len * (long) count;
    final int size = (int) longSize;
    //FP because of casting and type difference, we can actually end up with diff value but we'll live with it for now.
    if (size != longSize) { // Noncompliant
      throw new ArrayIndexOutOfBoundsException("Required array size too large: " + longSize);
    }
  }

  private transient BiEntry[] hashTableVToK;

  static void fromEntryArray(int n, Entry[] entryArray) {
    checkPositionIndex(n, entryArray.length);
    int tableSize = Hashing.closedTableSize(n, MAX_LOAD_FACTOR);
    int mask = tableSize - 1;
    ImmutableMapEntry[] keyTable = createEntryArray(tableSize);
    ImmutableMapEntry[] valueTable = createEntryArray(tableSize);
    Entry[] entries;
    if (n == entryArray.length) {
      entries = entryArray;
    } else {
      entries = createEntryArray(n);
    }
    int hashCode = 0;

    for (int i = 0; i < n; i++) {
      @SuppressWarnings("unchecked")
      Entry entry = entryArray[i];
      checkEntryNotNull(key, value);
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      int keyBucket = Hashing.smear(keyHash) & mask;
      int valueBucket = Hashing.smear(valueHash) & mask;

      ImmutableMapEntry nextInKeyBucket = keyTable[keyBucket];
      checkNoConflictInKeyBucket(key, entry, nextInKeyBucket);
      ImmutableMapEntry nextInValueBucket = valueTable[valueBucket];
      checkNoConflictInValueBucket(value, entry, nextInValueBucket);
      ImmutableMapEntry newEntry;
      if (nextInValueBucket == null && nextInKeyBucket == null) {
        boolean reusable = entry instanceof ImmutableMapEntry
            && ((ImmutableMapEntry) entry).isReusable();
        newEntry =
            reusable ? (ImmutableMapEntry) entry : new ImmutableMapEntry(key, value); // Issue is detected here in ruling...
      } else {
        newEntry = new NonTerminalImmutableBiMapEntry(
            key, value, nextInKeyBucket, nextInValueBucket);
      }
    }
  }

  private void bigToDouble(BigInteger x) {
    BigInteger absX = x.abs();
    int shift = 1;
    long twiceSignifFloor = absX.shiftRight(shift).longValue();
    long signifFloor = twiceSignifFloor >> 1;
    signifFloor &= SIGNIFICAND_MASK; // remove the implied bit
    boolean increment = (twiceSignifFloor & 1) != 0
        && ((signifFloor & 1) != 0 || absX.getLowestSetBit() < shift);
    //FP - SONARJAVA-???
    long signifRounded = increment ? signifFloor + 1 : signifFloor; // Noncompliant
  }

}
