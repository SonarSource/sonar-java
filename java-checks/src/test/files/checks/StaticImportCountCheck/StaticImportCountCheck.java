import static java.lang.Math.*; // Noncompliant {{Reduce the number of "static" imports in this class from 7 to the maximum allowed 4.}}
//^[sc=1;ec=31]
//^[sc=1;ec=31]@-1<
import static java.util.Collections.*;
//^[sc=1;ec=38]<
import static com.myco.corporate.Constants.*;
//^[sc=1;ec=45]<
import static com.myco.division.Constants.*;
//^[sc=1;ec=44]<
import static com.myco.department.Constants.*;
//^[sc=1;ec=46]<
import static com.myco.department.Constants.*;
//^[sc=1;ec=46]<
import static com.myco.department.Constants.*;
//^[sc=1;ec=46]<
