### Pulse Service

Primary goal of this application is to facilitates Dependency Management for Application Server: pulse service

###  Assumptions:

1. Minimal service/server interface to show the design.
2. The services assembled will not have cyclic dependencies but only hierarchical dependency is assumed.
3. All services are user level artifacts, no special provision is made to kernel/core services.
4. Minimal to none external libraries to be used.

####  Frameworks and Technologies used

1. Gradle  is used as a build automation tool. Gradle 2.9
2. Directed graph data structure with DFS traversal is used to build the in/out degree dependencies of service.
3. Java concurrency utilities, ExecutorService, CountDownLatch are used to orchestrate the service rollout.
4. Automated code check by checkstyle, findbugs and pmd.
5. YAML configuration library (snakeyaml) is used to read the service configuration.
6. JUnit and Jacoco used for code unit test and code coverage.
7. JDK 1.8: java version "1.8.0_66" Java(TM) SE Runtime Environment (build 1.8.0_66-b18)
8. Slf4j for logger is kept as a placeholder.
9. {and more needs to be added}

#### How to Run:  you need JDK 1.8 and gradle.
##### Using the command line (Option 1)

You could run it via the command line

    	./gradlew build
    	./gradlew test

NOTE: Use `./gradlew.bat` when running on DOS/windows

##### Running from within your IDE (Option 2)

This option lets you start and run the project just like you would start any java main program in the comfort of your IDE.

This option also enables you to effortlessly work on development.

###  What to look for?

Access the reports in a browser at: [SERVICE_MODULE]/build/reports/

Following reports are available to review:

1. [SERVICE_MODULE_PATH]/build/reports/checkstyle/index.html
2. [SERVICE_MODULE_PATH]/build/reports/checkstyle/main.html
3. [SERVICE_MODULE_PATH]/build/reports/findbugs/main.html
4. [SERVICE_MODULE_PATH]/build/reports/pmd/main.html
5. [SERVICE_MODULE_PATH]/build/reports/tests/index.html
6. [SERVICE_MODULE_PATH]/build/reports/jacoco/test/html/index.html

###  TODO:
1. Finding the Cycle within the dependency graph to abort the service startup to avoid cyclic deadlock.
2. Integrate the exceptions mechanism to the use case to make it robust.

###  Change Log:
1. Adding Junits.
2. Adding CycleFinder for the service configurations with ConfigCycleDetectedException.
