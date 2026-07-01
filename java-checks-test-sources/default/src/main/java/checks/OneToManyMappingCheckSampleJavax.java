package checks;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

public class OneToManyMappingCheckSampleJavax {

  @Entity
  class Author {
    @OneToMany // Noncompliant {{Add "mappedBy" or "@JoinColumn" to this "@OneToMany" relationship.}}
//  ^^^^^^^^^^
    private List<Book> books;
  }

  @Entity
  class Book {
    @ManyToOne
    private Author author;
  }

  // Compliant: uses mappedBy
  @Entity
  class AuthorWithMappedBy {
    @OneToMany(mappedBy = "author")
    private List<BookWithAuthor> books;
  }

  @Entity
  class BookWithAuthor {
    @ManyToOne
    @JoinColumn(name = "author_id")
    private AuthorWithMappedBy author;
  }

  // Compliant: uses @JoinColumn on the @OneToMany field
  @Entity
  class AuthorWithJoinColumn {
    @OneToMany
    @JoinColumn(name = "author_id")
    private List<BookNoRef> books;
  }

  @Entity
  class BookNoRef {
    // No reference back to Author
  }

  @Entity
  class AuthorUnidirectional {
    @OneToMany // Noncompliant
    private List<AnotherBook> books;
  }

  @Entity
  class AnotherBook {
    // No reference back
  }

  // Compliant: uses @JoinTable (explicit join table mapping)
  @Entity
  class AuthorWithJoinTable {
    @OneToMany
    @JoinTable(name = "author_books")
    private List<BookWithJoinTable> books;
  }

  @Entity
  class BookWithJoinTable {
    // No reference back
  }

  // Property-access: noncompliant getter
  @Entity
  class AuthorPropertyAccess {
    private List<BookPropertyAccess> books;

    @OneToMany // Noncompliant
    public List<BookPropertyAccess> getBooks() {
      return books;
    }
  }

  @Entity
  class BookPropertyAccess {
    // No reference back
  }

  // Property-access: compliant with mappedBy
  @Entity
  class AuthorPropertyAccessMappedBy {
    private List<BookPropertyAccessMappedBy> books;

    @OneToMany(mappedBy = "author")
    public List<BookPropertyAccessMappedBy> getBooks() {
      return books;
    }
  }

  @Entity
  class BookPropertyAccessMappedBy {
    @ManyToOne
    private AuthorPropertyAccessMappedBy author;
  }

  // Property-access: compliant with @JoinColumn
  @Entity
  class AuthorPropertyAccessJoinColumn {
    private List<BookPropertyAccessJoinColumn> books;

    @OneToMany
    @JoinColumn(name = "author_id")
    public List<BookPropertyAccessJoinColumn> getBooks() {
      return books;
    }
  }

  @Entity
  class BookPropertyAccessJoinColumn {
    // No reference back
  }

  // Property-access: compliant with @JoinTable
  @Entity
  class AuthorPropertyAccessJoinTable {
    private List<BookPropertyAccessJoinTable> books;

    @OneToMany
    @JoinTable(name = "author_books2")
    public List<BookPropertyAccessJoinTable> getBooks() {
      return books;
    }
  }

  @Entity
  class BookPropertyAccessJoinTable {
    // No reference back
  }
}
