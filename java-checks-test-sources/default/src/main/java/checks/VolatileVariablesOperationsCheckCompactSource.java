volatile int count1 = 0;
int nonVolatileCount1 = 0;
volatile boolean boo1 = false;

void main() {
  count1++; // Noncompliant {{Use an "AtomicInteger" for this field; its operations are atomic.}}
  nonVolatileCount1++;
  boo1 = !boo1; // Noncompliant {{Use an "AtomicBoolean" for this field; its operations are atomic.}}
}
