class CognitiveComplexityCheck {


  public int ternaryOp(int a, int b) { // Noncompliant [[sc=14;ec=23;secondary=6,8]] {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}

    int c = a>b?b:a;

    return c>20?4:7;

  }

  public boolean extraConditions() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 3 to the 0 allowed.}}
    return a && b || foo(b && c);
  }
  public boolean extraConditions2() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}
    return a && (b || c) || d;
  }
  public void extraConditions3() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 3 to the 0 allowed.}}
    if (a && b || c || d) {}
  }
  public void extraConditions4() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 5 to the 0 allowed.}}
    if (a && b || c && d || e) {}
  }
  public void extraConditions5() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 5 to the 0 allowed.}}
    if (a || b && c || d && e) {}
  }
  public void extraConditions6() {// Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 3 to the 0 allowed.}}
    if (a && b && c || d || e) {}
  }
  public void extraConditions7() {// Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 1 to the 0 allowed.}}
    if (a) {}
  }
  public void extraConditions8() {// Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}
    if (a && b && c && d && e) {}
  }
  public void extraConditions9() {// Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}
      if (a || b || c || d || e) {}
  }
  public void extraCondition10() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 4 to the 0 allowed.}}
    if (a && b && c || d || e && f){}
  }


  public void switch2(){ // Noncompliant [[sc=15;ec=22;secondary=46,50,51,51,51,55,57]] {{Refactor this method to reduce its Cognitive Complexity from 12 to the 0 allowed.}}

    switch(foo){                              //+1
      case 1:
        break;
      case ASSIGNMENT:
        if (lhs.is(Tree.Kind.IDENTIFIER)) {   //+2 (nesting=1)
          if (a && b && c || d) {             //+5 (nesting=2)

          }

          if(element.is(Tree.Kind.ASSIGNMENT)) { //+3 (nesting=2)
            out.remove(symbol);
          } else {                               //+1
            out.add(symbol);
          }
        }
        break;
    }
  }

  public void extraCondition11() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}
    if (a || (b || c)) {}
  }

  public void extraConditions12() { // Noncompliant [[secondary=70,72,74,76,78,80,81]] {{Refactor this method to reduce its Cognitive Complexity from 7 to the 0 allowed.}}
    if (     // +1
      a
      && b   // +1 - secondary on each first operator of a new sequence
      && c
      || d   // +1
      || e
      && f   // +1
      && g
      || (h  // +1
      || (i
      && j   // +1
      || k)) // +1 - parentheses completely ignored
      || l
      || m
      ){}
  }

  public void breakWithLabel(java.util.Collection<Boolean> objects) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}
    doABarrelRoll:
    for(Object o : objects) { // +1
      break doABarrelRoll;    // +1
    }
  }

  public void doFilter(ServletRequest servletRequest) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 13 to the 0 allowed.}}

    if (consumedByStaticFile) {                             // 1
      return;
    }

    try {

    } catch (HaltException halt) {                          // 1

    } catch (Exception generalException) {                  // 1

    }

    if (body.notSet() && responseWrapper.isRedirected()) {  // 2
      body.set("");
    }

    if (body.notSet() && hasOtherHandlers) {                // 2
      if (servletRequest instanceof HttpRequestWrapper) {   // 2 (nesting=1)
        ((HttpRequestWrapper) servletRequest).notConsumed(true);
        return;
      }
    }

    if (body.notSet() && !externalContainer) {               // 2
      LOG.info("The requested route [" + uri + "] has not been mapped in Spark");
    }

    if (body.isSet()) {                                      // 1
      body.serializeTo(httpResponse, serializerChain, httpRequest);
    } else if (chain != null) {                              // 1
      chain.doFilter(httpRequest, httpResponse);
    }
  }


  public final T to(U u) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 7 to the 0 allowed.}}

    for (int ctr=0; ctr<args.length; ctr++)
      if (args[ctr].equals("-debug"))
        debug = true ;

    for (int i = chain.length - 1; i >= 0; i--)
      result = chain[i].to(result);

    if (foo)
      for (int i = 0; i < 10; i++)
        doTheThing();

    return (T) result;
  }


  static boolean enforceLimits(BoundTransportAddress boundTransportAddress) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 1 to the 0 allowed.}}
    Iterable<JoinTuple> itr = () -> new JoinTupleIterator(tuples.tuples(), parentIndex, parentReference);

    Predicate<TransportAddress> isLoopbackOrLinkLocalAddress = t -> t.address().getAddress().isLinkLocalAddress()
            || t.address().getAddress().isLoopbackAddress();

  }

  String bulkActivate(Iterator<String> rules) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 6 to the 0 allowed.}}

    try {
      while (rules.hasNext()) {  // +1
        try {
          if (!changes.isEmpty()) {  }  // +2, nesting 1
        } catch (BadRequestException e) { }  // +2, nesting 1
      }
    } finally {
      if (condition) {  // +1
        doTheThing();
      }
    }
    return result;
  }

  private static String getValueToEval( Measure.Level alertLevel, Color foo ) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 6 to the 0 allowed.}}

    if (Measure.Level.ERROR.equals(alertLevel) // +1
      && foo == YELLOW) {   // +1
      return condition.getErrorThreshold();
    } else if (Measure.Level.WARN.equals(alertLevel)) {             // 1
      return condition.getWarningThreshold();
    } else {                                                        // 1
      while (true) {                                                // 2 (nesting = 1)
        doTheThing();
      }
      throw new IllegalStateException(alertLevel.toString());
    }
  }

  boolean isPalindrome(char [] s, int len) { // Noncompliant Refactor this method to reduce its Cognitive Complexity from 3 to the 0 allowed.

    if(len < 2)
      return true;
    else
      return s[0] == s[len-1] && isPalindrome(s[1], len-2); // TODO find recursion
  }

  void extraConditions() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 10 to the 0 allowed.}}

    if (a < b) {                // 1
      doTheThing();
    }

    if (a == b || c > 3 || b-7 == c) {  // 2
      while (a-- > 0 && b++ < 10) {     // 3 (nesting = 1)
        doTheOtherThing();
      }
    }

    do {                                // 1

    } while (a-- > 0 || b != YELLOW);   // 1 (for ||)

    for (int i = 0; i < 10 && j > 20; i++) {  // 2
      doSomethingElse();
    }
  }

  public static void main (String [] args) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 4 to the 0 allowed.}}

    Runnable r = () -> {
      if (condition) {
        System.out.println("Hello world!");
      }
    };

    r = new MyRunnable();

    r = new Runnable () {
      public void run(){
        if (condition) {
          System.out.println("Well, hello again");
        }
      }
    };
  }

  int sumOfNonPrimes(int limit) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 9 to the 0 allowed.}}

    int sum = 0;
    OUTER: for (int i = 0; i < limit; ++i) {
      if (i <= 2) {
        continue;
      }
      for (int j = 2; j < 1; ++j) {
        if (i % j == 0) {
          continue OUTER;
        }
      }
      sum += i;
    }
    return sum;
  }

  String getWeight(int i){ // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 4 to the 0 allowed.}}

    if (i <=0) {
      return "no weight";
    }
    if (i < 10) {
      return "light";
    }
    if (i < 20) {
      return "medium";
    }
    if (i < 30) {
      return "heavy";
    }
    return "very heavy";
  }

  public static HighlightingType toProtocolType(TypeOfText textType) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 1 to the 0 allowed.}}

    switch (textType) {
      case ANNOTATION: {
        return HighlightingType.ANNOTATION;
      }
      case CONSTANT:
        return HighlightingType.CONSTANT;
      case CPP_DOC:
        return HighlightingType.CPP_DOC;
      default:
        throw new IllegalArgumentException(textType.toString());
    }
  }

  public String getSpecifiedByKeysAsCommaList() {
    return getRuleKeysAsString(specifiedBy);
  }

  void localClasses() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 3 to the 0 allowed.}}
    class local {
      boolean plop() { // compliant : will be counted in the enclosing method
        return a && b || c && d;
      }
    }
  }

  void noNestingForIfElseIf() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 21 to the 0 allowed.}}
    while (true) { // +1
      if (true) { // +2 (nesting=1)
        for (;;) { // +3 (nesting=2)
          if (true) { // +4 (nesting=3)
          } else if (true) { // +1
          } else { // +1
            if (true) {
            } // +5 (nesting=4)
          }

          if (true) {} // +4 (nesting=3)
        }
      }
    }
  }


}

