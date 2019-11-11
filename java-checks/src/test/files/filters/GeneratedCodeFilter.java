package foo;

import javax.annotation.processing.Generated;

class this_is_a_bad_class_name { // WithIssue

  Object This_Is_A_Bad_Field_Name; // WithIssue

  @Generated("value")
  void Bad_Generated_Method_Name() {  // NoIssue
    Object Bad_Variable_Name; // NoIssue
  }

  void Bad_Method_Name() { // WithIssue
    Object Bad_Variable_Name; // WithIssue
  }
}

/** this is an alpha */ // NoIssue
/**
 * Other comment
 */
@Generated("value")
class this_is_a_bad_generated_class_name { // NoIssue

  Object This_Is_A_Bad_Field_Name; // NoIssue

  void Bad_Method_Name() { // NoIssue
    Object Bad_Variable_Name; // NoIssue
  }
}

@javax.annotation.Generated("value")
class bad_generated_name { //NoIssue

  Object Bad_Field_Name; // NoIssue

  void Bad_Method_Name() { // NoIssue
    Object Bad_Variable_Name; // NoIssue
  }
}

@unrelated.Generated
class unrelated_bad_name { //WithIssue

  Object Bad_Field_Name; // WithIssue

  void Bad_Method_Name() { // WithIssue
    Object Bad_Variable_Name; // WithIssue
  }
}
