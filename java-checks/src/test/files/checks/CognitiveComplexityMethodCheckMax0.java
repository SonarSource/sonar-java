class CognitiveComplexityCheck {


  public int ternaryOp(int a, int b) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}

    int c = a>b?b:a;

    return c>20?4:7;

  }

  public void extraConditions(){ // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 21 to the 0 allowed.}}

    if (a && b || c || d) {      // +3
    }
    if (a && b || c && d || e) { // +5
    }
    if (a || b && c || d && e) { // +5
    }
    if (a && b && c || d || e) { // +3
    }

    if (a) {                     // +1
    }
    if (a && b && c && d && e) { // +2
    }
    if (a || b || c || d || e) { // +2
    }
  }


  public void switch2(){ // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 14 to the 0 allowed.}}

    switch(foo){                              //+1
      case 1:
        break;
      case ASSIGNMENT:
        if (lhs.is(Tree.Kind.IDENTIFIER)) {   //+2 (nesting=1)
          if (a && b && c || d) {             //+5 (nesting=2)

          }

          if(element.is(Tree.Kind.ASSIGNMENT)) { //+3 (nesting=2)
            out.remove(symbol);
          } else {                               //+3 (nesting=2)
            out.add(symbol);
          }
        }
        break;
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


  static boolean enforceLimits(BoundTransportAddress boundTransportAddress) {
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

  private static String getValueToEval( List<String> strings ) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 6 to the 0 allowed.}}

    if (Measure.Level.ERROR.equals(alertLevel) && foo = YELLOW) {   // 1
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

  boolean isPalindrome(char [] s, int len) { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 2 to the 0 allowed.}}

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


}

