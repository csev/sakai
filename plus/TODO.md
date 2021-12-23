
Do we *need* 36 character primary keys?   Feels oracle-ish and low performance

Figure out logical keys, including composite issuer / client id

Figure out how to best epresent LEFT JOINS in the model and online

Make sure Repository is the right approach

Auto populate created and updated

Figure out JSONTrait

Figure out Settings as Map

Figure out how to construct and return a LAUNCH

Figure out how to return the first item in a list SubjectRepositoryImpl.java

Learn how to do bi-directional replactionships efficiently
What about orphan removal instead of cascade?
https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/

PagingAndSortingRepository
https://www.amitph.com/pagination-sorting-spring-data-jpa/

LIMIT 1
https://www.baeldung.com/jpa-limit-query-results

Embedded ID
https://stackoverflow.com/questions/2611619/onetomany-and-composite-primary-keys


LESSONS
-------

NaturalId Is Hibernate-Only so forget about it.

https://stackoverflow.com/questions/14254083/jpa-equivalent-to-hibernates-naturalid

No, there is not. You will have to use composite keys, so either EmbeddedId or IdClass depending what you prefer.
https://docs.oracle.com/javaee/6/api/javax/persistence/EmbeddedId.html
https://docs.oracle.com/javaee/5/api/javax/persistence/IdClass.html




