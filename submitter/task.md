Goal

Create a REST service using Javax RS WS, which provide endpoints to do the following
1) create and submit a new http job that can be added to the db
2) Edit a job definition, remember job defs are immutable to we'd create a new version
3) Park a particular job, which means it wont be scheduled to run until its unparked.
4) Unpark a particular job.

Leverage virtual threads, this REST service does a lot of IO bound work, so virtual threads will help to improve the scalability of the service. The service should be able to handle a large number of concurrent requests without running into thread exhaustion issues.
Remember to add some basic validation to the endpoints, and to return appropriate status codes. Also remember to add some basic logging to the service.
Additionally, add some tests for the endpoints, use JUnit 5 for unit tests, and a suitable framework for integration tests, where we spin up an instance of the service locally.

Tech Stack
- Java 25
- JAX-RS (Jersey)
- PostGres (this is the db where will be making changes)
- Dagger for DI
