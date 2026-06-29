package checks;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.List;

public class OneToManyMappingCheckSample {

  @Entity
  class Author {
    @OneToMany // Noncompliant {{Add "mappedBy" or "@JoinColumn" to this "@OneToMany" relationship.}}
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
}
