import static java.lang.Math.*; // Noncompliant {{Reduce the number of "static" imports in this class from 7 to the maximum allowed 4.}}
//^[sc=1;ec=32]
//  ^^^<
import static java.util.Collections.*;
//  ^^^<
import static com.myco.corporate.Constants.*;
//  ^^^<
import static com.myco.division.Constants.*;
//  ^^^<
import static com.myco.department.Constants.*;
//  ^^^<
import static com.myco.department.Constants.*;
//  ^^^<
import static com.myco.department.Constants.*;
//  ^^^<
