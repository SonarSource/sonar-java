package symbolicexecution.checks;

class ConditionAlwaysTrueOrFalseCheckMaxReturnedFlows {

  void only_20_flows_instead_of_32() {
    // each "try" multiply the number of flows by 2 with an element "'IllegalArgumentException' is caught." and an element "'Throwable' is caught."
    try{ throw new IllegalArgumentException(""); } catch (Throwable e) {}
    try{ throw new IllegalArgumentException(""); } catch (Throwable e) {}
    try{ throw new IllegalArgumentException(""); } catch (Throwable e) {}
    try{ throw new IllegalArgumentException(""); } catch (Throwable e) {}
    try{ throw new IllegalArgumentException(""); } catch (Throwable e) {}
    boolean condition = true;
    if (condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  void only_20_flows_instead_of_24() {
    boolean condition;
    switch ((int)(Math.random() * 32)) {
      case 0: condition = true; break;
      case 1: condition = true; break;
      case 2: condition = true; break;
      case 3: condition = true; break;
      case 4: condition = true; break;
      case 5: condition = true; break;
      case 6: condition = true; break;
      case 7: condition = true; break;
      case 8: condition = true; break;
      case 9: condition = true; break;
      case 10: condition = true; break;
      case 11: condition = true; break;
      case 12: condition = true; break;
      case 13: condition = true; break;
      case 14: condition = true; break;
      case 15: condition = true; break;
      case 16: condition = true; break;
      case 17: condition = true; break;
      case 18: condition = true; break;
      case 19: condition = true; break;
      case 20: condition = true; break;
      case 21: condition = true; break;
      case 22: condition = true; break;
      default: condition = true;
    }
    if (condition) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

}
