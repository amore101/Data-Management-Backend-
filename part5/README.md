# CSE 414 Winter 2020 Homework 5: Database Application and Transaction Management

**Objectives:**
To develop a database application under concurrent access.
To interface with a relational database from a Java application via JDBC.

**Assignment tools:**
* [SQL Server](http://www.microsoft.com/sqlserver) through [SQL Azure](https://azure.microsoft.com/en-us/services/sql-database/)
* Maven (if using OSX, we recommend using Homebrew and installing with `brew install maven`)
* [Prepared Statements](https://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html)
* starter code files

Instructions assume you are on the Linux lab machines, attu, or home VM.

**Assigned date:** February 10, 2020

**Due date:** February 24, 2020, at 11:00pm. Turn in your solution by pushing to GitLab.

**WARNING:**
This hw requires writing a non-trivial amount of Java code (our solution is about 800 lines) and test cases. 
START EARLY!!!


## Assignment Details

**Read through this whole section before starting this project.** There is a lot of valuable information here, and some implementation details depend on others.

Congratulations, you are continuing to open your own flight booking service!

In this homework, you have two main tasks:
* Design a database of your customers and the flights they book
* Complete a working prototype of your flight booking application that connects to the database (in Azure) then allows customers to use a CLI to search, book, cancel, etc. flights

You will also be writing a few test cases and explaining your design in a short writeup.
We have already provided code for a UI (`FlightService.java`) and partial backend (`Query.java`).
For this homework, your task is to implement the rest of the backend.
In real life, you would develop a web-based interface instead of a CLI, but we use a CLI to simplify this homework.

For this lab, you can use any of the classes from the [Java 8 standard JDK](https://docs.oracle.com/javase/8/docs/api/).

You will need to access your Flights database on SQL Azure from HW3.
Alternatively, you may create a new database and define the Flights data as follows.


#### Connect your application to your database
To improve performance and make sure you don't alter the flights data, reference the Flights tables as `EXTERNAL TABLE`s. Every query to the Flights tables from your database will be redirected to a faster class server. We are sharing this server with this quarter's offering of CSE 344.

- Step 1: Drop all your table:
  ```sql
  DROP TABLE IF EXISTS Flights;
  DROP TABLE IF EXISTS Carriers;
  DROP TABLE IF EXISTS Weekdays;
  DROP TABLE IF EXISTS Months;
  ``` 
- Step 2: Create a credential, please enter exactly like this:
  ```sql
  CREATE MASTER KEY ENCRYPTION BY PASSWORD = 'CsE344RaNdOm$Key';

  CREATE DATABASE SCOPED
  CREDENTIAL QueryCredential
  WITH IDENTITY = 'reader2020', SECRET = '20wiUWcse()';
  ```
- Step 3: Create external data source:
  ```sql
  CREATE EXTERNAL DATA SOURCE CSE344_EXTERNAL
  WITH
  ( TYPE = RDBMS,
    LOCATION='khangishandsome.database.windows.net',
    DATABASE_NAME = 'cse344_readonly',
    CREDENTIAL = QueryCredential
  );
  ```
- Step 4: Create the external tables:
  ```sql
  CREATE EXTERNAL TABLE Flights(
    fid int,
    month_id int,
    day_of_month int,
    day_of_week_id int,
    carrier_id varchar(7),
    flight_num int,
    origin_city varchar(34),
    origin_state varchar(47),
    dest_city varchar(34),
    dest_state varchar(46),
    departure_delay int,
    taxi_out int,
    arrival_delay int,
    canceled int,
    actual_time int,
    distance int,
    capacity int,
    price int
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);

  CREATE EXTERNAL TABLE Carriers(
    cid varchar(7),
    name varchar(83)
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);

  CREATE EXTERNAL TABLE Weekdays(
    did int,
    day_of_week varchar(9)
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);

  CREATE EXTERNAL TABLE Months
  (
    mid int,
    month varchar(9)
  ) WITH (DATA_SOURCE = CSE344_EXTERNAL);
  ```
- Step 5: Check that you can query the external tables:
  ```sql
  SELECT COUNT(*) FROM Flights;  -- expect count of 1148675
  ```



##### Configure your JDBC Connection
You need to configure the appropriate information to connect `Query.java` to your new database.

In the top level directory, create a file named `dbconn.properties` with the following contents:

```
# Database connection settings

# TODO: Enter the server URL.
hw5.server_url = SERVER_URL

# TODO: Enter your database name.
hw5.database_name = DATABASE_NAME

# TODO: Enter the admin username of your server.
hw5.username = USERNAME

# TODO: Add your admin password.
hw5.password = PASSWORD
```

Do not add your `dbconn.properties` to your git repository! It contains sensitive information.

Check your Azure server and fill in the connection details:
* The server URL will be of the form `[your_server_name].database.windows.net`
* The database name, admin, and password will be whatever you specified
* If the connection isn't working for some reason, try using the fully qualified username: `hw5.username = USER_NAME@SERVER_NAME`


#### Build the application
Make sure your application can run by entering the following commands in the directory of the starter code and `pom.xml` file.
This first command will package the application files and any dependencies into a single .jar file:

```sh
$ mvn clean compile assembly:single
```

This second command will run the main method from `FlightService.java`, the interface logic for what you will implement in `Query.java`:

```sh
$ java -jar target/FlightApp-1.0-jar-with-dependencies.jar
```

If you want to run directly without creating the jar, you can run:

```sh
$ mvn compile exec:java
```

If you get our UI below, you are good to go for the rest of the lab!

```
*** Please enter one of the following commands ***
> create <username> <password> <initial amount>
> login <username> <password>
> search <origin city> <destination city> <direct> <day> <num itineraries>
> book <itinerary id>
> pay <reservation id>
> reservations
> cancel <reservation id>
> quit
```



#### Data Model
This service is based on the tables Flights, Carriers, Months, and Weekdays used in hw2 and hw3. 
Please see [hw2](https://gitlab.cs.washington.edu/cse414-20wi/source/hw2) for a reminder of their schema, or see below.
**Your flight booking service may not modify the contents of Flights, Carriers, Months, or Weekdays**,
because updates to these tables (such as scheduling new flights) occur outside of your application.

Your service follows a [client-server model](https://en.wikipedia.org/wiki/Client%E2%80%93server_model).
Users interact with your service by running your client application.
Savvy, frequent-flyer users may run several instances of your client application at once!
The client application can either 

1. store information locally (just inside the client program) and transiently (not saved after the program closes), 
in which case this information does not need to be stored in a server database.
2. store information globally (accessible to all client programs) and persistently (saved after the program closes),
in which case this information needs to be stored in a server database.

Remember that only data that falls under category #2 need be stored in your database and modeled in your entity-relationship diagram.

Here are a few logical entities you might consider using. These entities are *not necessarily database tables*.
It is up to you to decide what entities to store persistently and create a physical schema design that has the ability to run the operations below.

- **Flights / Carriers / Months / Weekdays**: modeled the same way as HW3.  
  For this application, we have very limited functionality so you shouldn't need to modify the schema from HW3 nor add any new table to reason about the data.

- **Users**: A user has a username (`varchar`), password (`varbinary`), and balance (`int`) in their account.
  All usernames should be unique in the system. Each user can have any number of reservations.
  Usernames are case insensitive (this is the default for SQL Server).
  Since we are salting and hashing our passwords through the Java application, passwords are case sensitive.
  You can assume that all usernames and passwords have at most 20 characters.

- **Itineraries**: An itinerary is either a direct flight (consisting of one flight: origin --> destination) or
  a one-hop flight (consisting of two flights: origin --> stopover city, stopover city --> destination). Itineraries are returned by the search command.

- **Reservations**: A booking for an itinerary, which may consist of one (direct) or two (one-hop) flights.
  Each reservation can either be paid or unpaid, cancelled or not, and has a unique ID.

Create other tables or indexes you need for this assignment in `createTables.sql` (see below).


#### Requirements
The following are the functional specifications for the flight service system, to be implemented in `Query.java`
(see the method stubs in the starter code for full specification as to what error message to return, etc):

- **create** takes in a new username, password, and initial account balance as input. It creates a new user account with the initial balance.
  It should return an error if negative, or if the username already exists. Usernames and passwords are checked case-insensitively.
  You can assume that all usernames and passwords have at most 20 characters.
  We will store the salted password hash and the salt itself to avoid storing passwords in plain text.
  Use the following code snippet to as a template for computing the hash given a password string:

    ```java
    // Generate a random cryptographic salt
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);

    // Specify the hash parameters
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

    // Generate the hash
    SecretKeyFactory factory = null;
    byte[] hash = null;
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
    ```

- **login** takes in a username and password, and checks that the user exists in the database and that the password matches. To compute the hash, adapt the above code.
  Within a single session (that is, a single instance of your program), only one user should be logged in. You can track this via a local variable in your program.
  If a second login attempt is made, please return "User already logged in".
  Across multiple sessions (that is, if you run your program multiple times), the same user is allowed to be logged in.
  This means that you do not need to track a user's login status inside the database.

- **search** takes as input an origin city (string), a destination city (string), a flag for only direct flights or not (0 or 1), the date (int), and the maximum number of itineraries to be returned (int).
  For the date, we only need the day of the month, since our dataset comes from July 2015. Return only flights that are not canceled, ignoring the capacity and number of seats available.
  If the user requests n itineraries to be returned, there are a number of possibilities:
    * direct=1: return up to n direct itineraries
    * direct=0: return up to n direct itineraries. If there are only k direct itineraries (where k < n), then return the k direct itineraries and up to (n-k) of the shortest indirect itineraries with the flight times. For one-hop flights, different carriers can be used for the flights. For the purpose of this assignment, an indirect itinerary means the first and second flight only must be on the same date (i.e., if flight 1 runs on the 3rd day of July, flight 2 runs on the 4th day of July, then you can't put these two flights in the same itinerary as they are not on the same day).

  <br />Sort your results. In all cases, the returned results should be primarily sorted on total actual_time (ascending). If a tie occurs, break that tie by the fid value. Use the first then the second fid for tie-breaking.

    Below is an example of a single direct flight from Seattle to Boston. Actual itinerary numbers might differ, notice that only the day is printed out since we assume all flights happen in July 2015:

    ```
    Itinerary 0: 1 flight(s), 297 minutes
    ID: 60454 Day: 1 Carrier: AS Number: 24 Origin: Seattle WA Dest: Boston MA Duration: 297 Capacity: 14 Price: 140
    ```

    Below is an example of two indirect flights from Seattle to Boston:

    ```
    Itinerary 0: 2 flight(s), 317 minutes
    ID: 704749 Day: 10 Carrier: AS Number: 16 Origin: Seattle WA Dest: Orlando FL Duration: 159 Capacity: 10 Price: 494
    ID: 726309 Day: 10 Carrier: B6 Number: 152 Origin: Orlando FL Dest: Boston MA Duration: 158 Capacity: 0 Price: 104
    Itinerary 1: 2 flight(s), 317 minutes
    ID: 704749 Day: 10 Carrier: AS Number: 16 Origin: Seattle WA Dest: Orlando FL Duration: 159 Capacity: 10 Price: 494
    ID: 726464 Day: 10 Carrier: B6 Number: 452 Origin: Orlando FL Dest: Boston MA Duration: 158 Capacity: 7 Price: 760
    ```

    Note that for one-hop flights, the results are printed in the order of the itinerary, starting from the flight leaving the origin and ending with the flight arriving at the destination.

    The returned itineraries should start from 0 and increase by 1 up to n as shown above. If no itineraries match the search query, the system should return an informative error message. See `Query.java` for the actual text.

    The user need not be logged in to search for flights.

    All flights in an indirect itinerary should be under the same itinerary ID. In other words, the user should only need to book once with the itinerary ID for direct or indirect trips.


- **book** lets a user book an itinerary by providing the itinerary number as returned by a previous search.
  The user must be logged in to book an itinerary, and must enter a valid itinerary id that was returned in the last search that was performed *within the same login session*.
  Make sure you make the corresponding changes to the tables in case of a successful booking. Once the user logs out (by quitting the application),
  logs in (if they previously were not logged in), or performs another search within the same login session,
  then all previously returned itineraries are invalidated and cannot be booked.

  A user cannot book a flight if the flight's maximum capacity would be exceeded. Each flight’s capacity is stored in the Flights table as in HW3, and you should have records as to how many seats remain on each flight based on the reservations.

  If booking is successful, then assign a new reservation ID to the booked itinerary.
  Note that 1) each reservation can contain up to 2 flights (in the case of indirect flights),
  and 2) each reservation should have a unique ID that incrementally increases by 1 for each successful booking.


- **pay** allows a user to pay for an existing unpaid reservation.
  It first checks whether the user has enough money to pay for all the flights in the given reservation. If successful, it updates the reservation to be paid.


- **reservations** lists all reservations for the currently logged-in user.
  Each reservation must have ***a unique identifier (which is different for each itinerary) in the entire system***, starting from 1 and increasing by 1 after each reservation is made.

  There are many ways to implement this. One possibility is to define a "ID" table that stores the next ID to use, and update it each time when a new reservation is made successfully.

  The user must be logged in to view reservations. The itineraries should be displayed using similar format as that used to display the search results, and they should be shown in increasing order of reservation ID under that username.
  Cancelled reservations should not be displayed.


- **cancel** lets a user to cancel an existing uncanceled reservation. The user must be logged in to cancel reservations and must provide a valid reservation ID.
  Make sure you make the corresponding changes to the tables in case of a successful cancellation (e.g., if a reservation is already paid, then the customer should be refunded).


- **quit** leaves the interactive system and logs out the current user (if logged in).


Refer to the Javadoc in `Query.java` for full specification and the expected responses of the commands above.

***CAUTION:*** Make sure your code produces outputs in the same formats as prescribed! (see test cases and Javadoc for what to expect)

#### Testing:

To test that your application works correctly, we have provided a test harness using the [JUnit framework](https://junit.org/junit4/).
Our test harness will compile your code and run all the test cases in the provided `cases/` folder. To run the harness, execute in the project directory:

```sh
$ mvn test
```

If you want to run a single test file or run files from a different directory (recursively), you can run the following command:
```sh
$ mvn test -Dtest.cases="folder_name_or_file_name_here"
```

For every test case it will either print pass or fail, and for all failed cases it will dump out what the implementation returned, and you can compare it with the expected output in the corresponding case file.

Each test case file is of the following format:

```sh
[command 1]
[command 2]
...
*
[expected output line 1]
[expected output line 2]
...
*
# everything following ‘#’ is a comment on the same line
```

While we've provided test cases for most of the methods, the testing we provide is partial (although significant).
It is **up to you** to implement your solutions so that they completely follow the provided specification.

For this homework, you're required to write two test cases (a serial one and a parallel one) for each of the commands (you don't need to test `quit`).
Separate each test case in its own file and name it `<command name>_<some descriptive name for the test case>.txt` and turn them in.
It’s fine to turn in test cases for erroneous conditions (e.g., booking on a full flight, logging in with a non-existent username).



## Milestone 1:

#### Database design
Your first task is to design and add tables to your flights database. You should decide on the relational tables given the logical data model described above. Feel free to use your E/R diagram from HW4 as a starting point. You may discover you need to adjust your design over time as you discover new requirements. You can add other tables to your database as well.

You should fill the provided `createTables.sql` file with `CREATE TABLE` and any `INSERT` statements (and optionally any `CREATE INDEX` statements) needed to implement the logical data model above.
We will test your implementation with the flights table populated with HW3 data using the schema above, and then running your `createTables.sql`.
So make sure your file is runnable on SQL Azure through SQL Server Management Studio or the Azure web interface.

Please note that due to the nature of `EXTERNAL TABLE`, you will not be able to create foreign keys that reference flights (`FOREIGN KEY REFERENCE Flights`) or `INDEX`es over the Flights table. For those foreign keys, just make them normal columns in your tables. You also do not need to keep `CREATE EXTERNAL TABLE` statements in this file.

*NOTE:*
You may want to write a separate script file with `DROP TABLE` or `DELETE FROM` statements;
it's useful to run it whenever you find a bug in your schema or data. You don't need to turn in anything for this.

#### Java customer application

Your second task is to start writing the Java application that your customers will use.
To make your life easier, we've broken down this process into 5 different steps across the two milestones (see details below).
You only need to modify `Query.java`. Do not modify `FlightService.java`.

We expect that you use [Prepared Statements](https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/java/sql/PreparedStatement.html) where applicable. Please make your code reasonably easy to read.

To keep things neat we have provided you with the `Flight` inner class that acts as a container for your flight data.
The `toString` method in the Flight class matches what is needed in methods like `search`.
We have also provided a sample helper method `checkFlightCapacity` that uses a prepared statement.
`checkFlightCapacity` outlines the way we think forming prepared statements should go for this assignment (creating a constant SQL string, preparing it in the prepareStatements method, and then finally using it).

#### Step 1: Implement clearTables

Implement the `clearTables` method in `Query.java` to clear the contents of any tables you have created for this assignment (e.g., reservations).
However, do NOT drop any of them and do NOT modify the contents or drop the `Flights` table.
**Any attempt to modify the `Flights` table will result in a harsh penalty.**

After calling this method the database should be in the same state as the beginning, i.e., with the flights table populated and `createTables.sql` called.
This method is for running the test harness where each test case is assumed to start with a clean database.
You will see how this works after running the test harness.

**`clearTables` should not take more than a minute.** Make sure your database schema is designed with this in mind.

#### Step 2: Implement create, login, and search

Implement the `create`, `login` and `search` commands in `Query.java`. Using ```mvn test```, you should now pass our provided test cases that only involve these three commands.

After implementation of these class, you should pass the following test:
```sh
mvn test -Dtest.cases="cases/no_transaction/search"
mvn test -Dtest.cases="cases/no_transaction/login"
mvn test -Dtest.cases="cases/no_transaction/create"
```
#### Step 3: Write some test cases

Write at least 1 test case for each of the three commands you just implemented. Follow the [same format](#testing) as our provided test cases.
Include your written test files in the provided `cases/mycases/` folder together with our provided test files.

Using ```mvn test -Dtest.casess="cases/mycases"```, you should now also pass your newly created test cases.



## Milestone 2:

#### Step 4: Implement book, pay, reservations, cancel, and add transactions!

Implement the `book`, `pay` , `reservations` and `cancel` commands in `Query.java`.

While implementing & trying out these commands, you'll notice that there are problems when multiple users try to use your service concurrently.
To resolve this challenge, you will need to implement transactions that ensure concurrent commands do not conflict.

Think carefully as to *which* commands need transaction handling. Do the `create`, `login` and `search` commands need transaction handling? Why or why not?

```sh
mvn test -Dtest.cases="cases/no_transaction/search"
mvn test -Dtest.cases="cases/no_transaction/pay"
mvn test -Dtest.cases="cases/no_transaction/cancel"
```
Or you can run all non transaction test:
```sh
mvn test -Dtest.cases="cases/no_transaction/"
```

##### Transaction management

You must use SQL transactions to guarantee ACID properties: we have set the isolation level for your `Connection`, and you need to define
`begin-transaction` and `end-transaction` statements and insert them in appropriate places in `Query.java`.
In particular, you must ensure that the following constraints are always satisfied, even if multiple instances of your application talk to the database at the same time:

*C1:* Each flight should have a maximum capacity that must not be exceeded. Each flight’s capacity is stored in the Flights table as in HW3, and you should have records as to how many seats remain on each flight based on the reservations.

*C2:* A customer may have at most one reservation on any given day, but they can be on more than 1 flight on the same day. (i.e., a customer can have one reservation on a given day that includes two flights, because the reservation is for a one-hop itinerary).

You must use transactions correctly such that race conditions introduced by concurrent execution cannot lead to an inconsistent state of the database.
For example, multiple customers may try to book the same flight at the same time. Your properly designed transactions should prevent that.

Design transactions correctly. Avoid including user interaction inside a SQL transaction: that is, don't begin a transaction then wait for the user to decide what she wants to do (why?).
The rule of thumb is that transactions need to be *as short as possible, but not shorter*.

Your `executeQuery` call will throw a `SQLException` when an error occurs (e.g., multiple customers try to book the same flight concurrently).
Make sure you handle the `SQLException` appropriately.
For instance, if a seat is still available, the booking should eventually go through (even though you might need to retry due to `SQLException`s being thrown).
If no seat is available, the booking should be rolled back, etc.

When one uses a DBMS, recall that by default **each statement executes in its own transaction**.
As discussed in lecture, to group multiple statements into a transaction, we use:
```java
BEGIN TRANSACTION
....
COMMIT or ROLLBACK
```
This is the same when executing transactions from Java: by default, each SQL statement will be executed as its own transaction.
To group multiple statements into one transaction in Java, you can do one of these approaches:

*Approach 1* (Approach 2 is preferred below over this approach):

Execute the SQL code for `BEGIN TRANSACTION` and friends directly, using the SQL code below (also check out SQL Azure's [transactions documentation](https://docs.microsoft.com/en-us/sql/t-sql/language-elements/transactions-transact-sql?view=sql-server-ver15)):

Please do not use `PreparedStatement`s for these queries. Since SQLServer's JDBC update, `PreparedStatement`s must fully commit to execute (i.e. have the same number of `BEGIN` and `COMMIT`).

```java
private static final String BEGIN_TRANSACTION_SQL = "BEGIN TRANSACTION;";
private static final String COMMIT_SQL = "COMMIT TRANSACTION";
private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";
private Statement generalStatement;

// When you start the database up
Connection conn = [...]
conn.setAutoCommit(true); // This is the default setting, actually
conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

generalStatement = conn.createStatement();

// Transaction begins here
generalStatement.execute(BEGIN_TRANSACTION_SQL);

// ... execute updates and queries.

generalStatement.execute(COMMIT_SQL);
// OR
generalStatement.execute(ROLLBACK_SQL);
```

*Approach 2*:
```java
// When you start the database up
Connection conn = [...]
conn.setAutoCommit(true); // This is the default setting, actually
conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

// In each operation that is to be a multi-statement SQL transaction:
// You MUST do this in order to tell JDBC that you are starting a multi-statement transaction
conn.setAutoCommit(false);

// ... execute updates and queries.

conn.commit();
// OR
conn.rollback();
// You MUST do this to make sure that future statements execute as their own transactions.
conn.setAutoCommit(true);
```

When auto-commit is set to true, each statement executes in its own transaction.
With auto-commit set to false, you can execute many statements within a single transaction.
By default, on any new connection to a DB auto-commit is set to true.

The total amount of code to add transaction handling is in fact small, but getting everything to work harmoniously may take some time.
Debugging transactions can be a pain, but print statements are your friend!

Now you should pass all the provided test in cases during `mvn test`

#### Step 5: Write more (transaction) test cases

Write at least 1 test case for each of the four commands you just implemented. Follow the [same format](#testing) as our provided test cases.

In addition, write at least 1 *parallel* test case for each of the 7 commands.
By *parallel*, we mean concurrent users interfacing with your database, with each user in a seperate application instance.

Remember that each test case file is in the following format:

```sh
[command 1]
[command 2]
...
*
[expected output line 1]
[expected output line 2]
...
*
# everything following ‘#’ is a comment on the same line
```

The `*` separates between commands and the expected output. To test with multiple concurrent users, simply add more `[command...] * [expected output...]` pairs to the file, for instance:

```sh
[command 1 for user1]
[command 2 for user1]
...
*
[expected output line 1 for user1]
[expected output line 2 for user1]
...
*
[command 1 for user2]
[command 2 for user2]
...
*
[expected output line 1 for user2]
[expected output line 2 for user2]
 ...
*
```

Each user is expected to start concurrently in the beginning. If there are multiple output possibilities due to transactional behavior, then separate each group of expected output with `|`.
See `book_2UsersSameFlight.txt` for an example.

Put your written test files in the `cases/mycases/` folder.

Using ```mvn test -Dtest.cases="cases"```, you should now also pass ALL the test cases in the `cases` folder - it will recursively run the provided test cases as well as your own.

*Congratulations!* You have now finished the entire flight booking application and are ready to launch your flight booking business :)


#### Write down your design
Please describe and/or draw your database design. This is so we can understand your implementation as close to what you were thinking.
Explain your design choices in creating new tables. Also, describe your thought process in deciding what needs to be persisted on the database
and what can be implemented in-memory (not persisted on the database). Please be concise in your writeup (< half a page).

You may include the E/R diagram you drew in hw4 here. You are free to make changes to your design since hw4.

Save this file in `writeup.md` in the same folder of `createTables.sql`. You can add images to markdown by using `![imagename](./relative/path/to/image.png)`. Make sure any images is also pushed to git.


#### What to turn in
* Customer database schema in `createTables.sql`
* Your completed version of `Query.java`
* At least 14 custom test cases (one normal & one parallel for each command) in the `cases/mycases` folder, with a descriptive name for each case
* A `writeup.md` and any images it requires


#### Grading
* `createTables.sql` and database design (10 points)
* Java customer application (50 points)
* Your custom test cases (30 points)
* Writeup (10 points)


## Submission Instructions

In order for your answers to be added to the git repo, you need to explicitly add each required file:

```sh
$ git add create_tables.sql ...
```

and push to make sure your code is uploaded to GitLab:

```sh
$ git commit
$ git push
```

As with previous assignments, make sure you check the results afterwards to make sure that your files
have been committed & uploaded to GitLab.
As a last resort, if git is not working properly, consider using the "file upload" feature on the GitLab website to add your solution to your repository.
