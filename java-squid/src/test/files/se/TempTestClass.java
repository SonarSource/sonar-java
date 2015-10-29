package org.sonar.plugins.java.api.tree;

import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;

public class TempTestClass {

  public static boolean indirectNull(CharSequence s1, CharSequence s2) {
    int length = s1.length();
    if (s1 == s2) {
      return true;
    }
    if (length != s2.length()) {
      return false;
    }
    return true;
  }

  public static void castedComparison(String string, int count) {
    final int len = string.length();
    final long longSize = (long) len * (long) count;
    final int size = (int) longSize;
    if (size != longSize) {
      throw new ArrayIndexOutOfBoundsException("Required array size too large: " + longSize);
    }
  }

  private boolean initialized;

  public boolean doubleMutexCondition() {
    // A 2-field variant of Double Checked Locking.
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          initialized = true;
          return true;
        }
      }
    }
    return false;
  }

  ReferenceEntry removeEntryFromChain(ReferenceEntry first, ReferenceEntry entry) {
    int newCount = count;
    ReferenceEntry newFirst = entry.getNext();
    for (ReferenceEntry e = first; e != entry; e = e.getNext()) {
      ReferenceEntry next = copyEntry(e, newFirst);
    }
    return newFirst;
  }

  final ReferenceEntry head = new AbstractReferenceEntry();
  public int size() {
    int size = 0;
    for (ReferenceEntry e = head.getNextInAccessQueue(); e != head;
        e = e.getNextInAccessQueue()) {
      size++;
    }
    return size;
  }

  public void add(long x) {
    Cell[] as; long b, v; int[] hc; Cell a; int n;
    if ((as = cells) != null || !casBase(b = base, b + x)) {
        boolean uncontended = true;
        if ((hc = threadHashCode.get()) == null ||
            as == null || (n = as.length) < 1 ||
            (a = as[(n - 1) & hc[0]]) == null ||
            !(uncontended = a.cas(v = a.value, v + x)))
            retryUpdate(x, hc, uncontended);
    }
  }
  
  public long sum() {
    Cell[] as = cells;
    if (as != null) {
      Cell a = as[0];
      if (a != null)
        sum += a.value;
    }
    return sum;
  }
  
  private void delete(BiEntry entry) {
    int keyBucket = entry.keyHash & mask;
    BiEntry prevBucketEntry = null;
    for (BiEntry bucketEntry = hashTableKToV[keyBucket];
        true;
        bucketEntry = bucketEntry.nextInKToVBucket) {
      if (bucketEntry == entry) {
        if (prevBucketEntry == null) {
          hashTableKToV[keyBucket] = entry.nextInKToVBucket;
        } else {
          prevBucketEntry.nextInKToVBucket = entry.nextInKToVBucket;
        }
        break;
      }
      prevBucketEntry = bucketEntry;
    }
  }
  
  private void arrayReference() {
    Object[] table = new Object[tableSize];
    int index = 12;
    Object value = table[index];
    if (value != null) {
      index = 13;
    }
  }
  
  private transient BiEntry[] hashTableVToK;
  private void delete(BiEntry entry) {
    int keyBucket = entry.keyHash & mask;
    BiEntry prevBucketEntry = null;
    for (BiEntry bucketEntry = hashTableKToV[keyBucket];
        true;
        bucketEntry = bucketEntry.nextInKToVBucket) {
      if (bucketEntry == entry) {
        if (prevBucketEntry == null) {
          hashTableKToV[keyBucket] = entry.nextInKToVBucket;
        } else {
          prevBucketEntry.nextInKToVBucket = entry.nextInKToVBucket;
        }
        break;
      }
      prevBucketEntry = bucketEntry;
    }

    int valueBucket = entry.valueHash & mask;
    prevBucketEntry = null;
    for (BiEntry bucketEntry = hashTableVToK[valueBucket];
        true;
        bucketEntry = bucketEntry.nextInVToKBucket) {
      if (bucketEntry == entry) {
        if (prevBucketEntry == null) {
          hashTableVToK[valueBucket] = entry.nextInVToKBucket;
        } else {
          prevBucketEntry.nextInVToKBucket = entry.nextInVToKBucket;
        }
        break;
      }
      prevBucketEntry = bucketEntry;
    }
    
    if (entry.prevInKeyInsertionOrder == null) {
      firstInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
    } else {
      entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
    }
    
    if (entry.nextInKeyInsertionOrder == null) {
      lastInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
    } else {
      entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
    }
    
    size--;
    modCount++;
  }
  
  private ValueSetLink firstEntry;
  private void rehashIfNecessary() {
    ValueEntry[] hashTable = new ValueEntry[this.hashTable.length * 2];
    int mask = hashTable.length - 1;
    for (ValueSetLink entry = firstEntry;
        entry != this; entry = entry.getSuccessorInValueSet()) {
      ValueEntry valueEntry = (ValueEntry) entry;
      int bucket = valueEntry.smearedValueHash & mask;
    }
  }

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
            reusable ? (ImmutableMapEntry) entry : new ImmutableMapEntry(key, value);
      } else {
        newEntry = new NonTerminalImmutableBiMapEntry(
            key, value, nextInKeyBucket, nextInValueBucket);
      }
    }
  }
  
  private final transient AvlNode header;
  private AvlNode firstNode() {
    AvlNode node;
    if (range.hasLowerBound()) {
      node = rootReference.get().ceiling(comparator(), endpoint);
    } else {
      node = header.succ;
    }
    return (node == header || !range.contains(node.getElement())) ? null : node;
  }
  
  private final char[][] replacements;
  public String escape(String s, int index) {
    int slen = s.length();
    char c = s.charAt(index);
    if (c < replacements.length && replacements[c] != null) {
      return escapeSlow(s, index);
    }
    return s;
  }
  
  private void bigToDouble(BigInteger x) {
    BigInteger absX = x.abs();
    int shift = 1;
    long twiceSignifFloor = absX.shiftRight(shift).longValue();
    long signifFloor = twiceSignifFloor >> 1;
    signifFloor &= SIGNIFICAND_MASK; // remove the implied bit
    boolean increment = (twiceSignifFloor & 1) != 0
        && ((signifFloor & 1) != 0 || absX.getLowestSetBit() < shift);
    long signifRounded = increment ? signifFloor + 1 : signifFloor;
  }

  public final void invoke(Object[] args)
    throws Throwable {
    if (args.length == 1) {
      Object arg = args[0];
      if (arg == null) {
        return false;
      }
    }
  }
  
  private Guard activeGuards = null;
  private void endWaitingFor(Guard guard) {
    int waiters = --guard.waiterCount;
    if (waiters == 0) {
      // unlink guard from activeGuards
      for (Guard p = activeGuards, pred = null;; pred = p, p = p.next) {
        if (p == guard) {
          if (pred == null) {
            activeGuards = p.next;
          } else {
            pred.next = p.next;
          }
          p.next = null;  // help GC
          break;
        }
      }
    }
  }
  
  private Boolean nonNullReassignment(Object a, Object b) {
    if (a == null) {
      return null;
    }
    if (a == b) {
      return true;
    } else {
      return b.flags();
    }
  }
  
  boolean isSubClass(JavaSymbol.TypeJavaSymbol c, JavaSymbol base) {
    if (c == null) {
      return false;
    }
    if (c == base) {
      return true;
    } else if ((base.flags() & Flags.INTERFACE) != 0) {
      return isSubClass(superclassSymbol(c), base);
    } else {
      return isSubClass(superclassSymbol(c), base);
    }
  }
}
