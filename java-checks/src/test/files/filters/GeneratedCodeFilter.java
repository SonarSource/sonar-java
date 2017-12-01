import javax.annotation.Generated;

class this_is_a_bad_class_name { // WithIssue

  Object This_Is_A_Bad_Field_Name; // WithIssue

  @Generated
  void Bad_Generated_Method_Name() {  // NoIssue
    Objet Bad_Variable_Name; // NoIssue
  }

  void Bad_Method_Name() { // WithIssue
    Objet Bad_Variable_Name; // WithIssue
  }
}

/** this is an alpha */ // NoIssue
/**
 * Other comment
 */
@Generated
class this_is_a_bad_generated_class_name { // NoIssue

  Object This_Is_A_Bad_Field_Name; // NoIssue

  void Bad_Method_Name() { // NoIssue
    Objet Bad_Variable_Name; // NoIssue
  }
}
