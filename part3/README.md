# CSE 414 Homework 3: Advanced SQL and Azure 

**Objectives:** To practice advanced SQL. 
To get familiar with commercial database management systems (SQL Server) and use a database management system in the cloud (Microsoft Azure). 

**Assignment tools:** SQL Server on the Microsoft Azure cloud.

**Assigned date:** Tuesday, January 21, 2020

**Due date:** Tuesday, January 28, 2020, 11:00 pm. You have 1 week for this assignment. 

**What to turn in:**
`hw3-q1.sql`, `hw3-q2.sql`, etc. (see below).

## Assignment Details

This homework is a continuation of homework 2 but with three changes:

- The queries are more challenging
- You will get to use a commercial database system (i.e., no more SQLite :). 
SQLite simply cannot execute these queries in a reasonable amount of time; 
hence, we will use SQL Server, which has one of the more advanced query optimizers.
- You will use the Microsoft Azure cloud.

In this homework, you will do three things:

1. Create a database in the SQL Server database management system running as a service on 
Microsoft Azure; import data from an Azure public blob.
2. Write and test the SQL queries below; keep in mind that the queries are challenging, 
both for you and for the database engine. 
3. Reflect on using a database management system running in a public cloud.

### A. Setting up an Azure SQL Database (0 points)

In this assignment, we want you to learn how to use an Azure SQL database from scratch. 
Your first step will thus be to setup a database in the Azure service and importing your data. 
This step may seem tedious but it is crucially important. We want you to be able to continue using Azure after the class ends. For this, you need to know how to use the system starting from nothing.

**NOTE: These steps will take some time to complete, so start early!**

#### Step 1: Create an Azure account and log in to Azure portal

Click on the "Accept lab assignment" link in the email "Action required: Accept your lab assignment", log in using your washington.edu account and password.

Afterward, you will be forwarded to the [Azure portal](https://portal.azure.com/). Don't forget to click "accept lab handout" on the front page to receive your Azure credits.


#### Step 2: Learn about Azure SQL Server

Spend some time clicking around, reading documentation, watching tutorials, and generally familiarizing yourself with Azure and SQL Server.


#### Step 3: Create a database

From the [Azure portal](https://portal.azure.com/), select "+ (Create a resource)" on the left, 
![+ button image](https://courses.cs.washington.edu/courses/cse344/19au/hw/hw3/new-button.png),
then select "Databases", then select "SQL Database". This will bring up a panel with 
configuration options for a new DB instance.

Perform the following configuration:
- Create a new resource group with a name (e.g., "myresourcegroup").

- Choose a database name (e.g., "cse414-20wi").

- Create a new server by clicking on "Server". 
  A second panel will appear to the right. Fill in the form as follows:

    - Choose a name for the server (e.g., "fooBarSqlserver"). Unlike your 
      database name, the server name must be unique across the universe of Azure SQL databases.

    - Choose an admin login and password. (You will need this when access your 
      database using tools other than the portal.)

    - Set the location to "West US" or "West US 2".

    - Click "OK".

- Make sure "Want to use SQL elastic pool?" is set to "No". 

- Under "Compute + storage", click "Configure database". A second panel will 
  open to the right. On this form,

    - Click "Looking for basic, standard, premium?"

    - Select "Standard".

    - Check that DTUs are set to 10, max data size to 250 GB (this is the current
      default setting). It should now say the monthly cost is only $15/month.

      NOTE: We will use this database for future assignments, so it will run for a couple months.
      If you choose to turn up the DTU usage, please make sure to turn it back down, or else you may run out of credits.

    - Click "Apply".

- Select "Next: Networking", which brings you to another panel.

- Select "Connectivity method" : Public endpoint

- Under Firewall rules, make sure "Allow azure services to access server" is set to "yes".

- Click "Next: Additional settings"

- Make sure that "Choose a source" is set to "Blank".

- Make sure "Advanced Threat Protection" is set to "Not now". 

- Click "Review + create"

- Double-check that your settings are correct, and click "Create". This takes a few minutes to deploy.

- Once it's created, select your new database. Select the pushpin icon to 
  "Pin to dashboard" so you can easily find it in the future.

Finally, scroll down to the "Security" section on the left side bar, click "Firewalls and Virtual networks".
You need to change this setting if you wish to access your database from an external tool 
such as JetBrains [DataGrip](https://www.jetbrains.com/datagrip/), [IntelliJ](https://www.jetbrains.com/idea/), 
[Visual Studio Code](https://docs.microsoft.com/en-us/sql/visual-studio-code/sql-server-develop-use-vscode?view=azuresqldb-current),
or [SQL Server Management Studio](https://docs.microsoft.com/en-us/sql/ssms/sql-server-management-studio-ssms?view=azuresqldb-current) (all free for students).
Using one of these tools is recommended, as your queries are no longer limited to a 5 minute timeout
(though, we hope you write queries that take shorter than 5 minutes to execute).
The easiest option is to add a rule that allows connections from any client, which you can do as follows:

<img src="https://courses.cs.washington.edu/courses/cse414/17sp/hw/hw3/firewall-rule.png" width="400"/>

Be sure to click "Save" once you have added this rule.


#### Step 4: Try out the database

The simplest way to play with the database is using the built-in Query editor in the Azure portal.
To launch this, go back to the dashboard, then click on the SQL database that you just setted. 
Enter the editor by clicking the "Query editor (preview)" on the side bar.

Enter the username and password that you chose when you created your database in Step 3. 
Enter SQL commands into the query editor. Press the "Run" button to execute them.


### B. Ingesting Data (0 points)

Next, you will import all the data from HW2. Make sure that you execute your 
`CREATE TABLE` statements first so that the tables you will add tuples to exist. 
Also, make sure that the types of the columns in the tables you created match the data.

The data used in this assignment is the same as that in hw2, in the `flights-small.csv`, `carriers.csv`, `months.csv`, and `weekdays.csv` files.
If you wish, you may import this data directly into your Azure SQL Server database.
Two tools that make this possible are `bcp` (or `freebcp` on Unix/Mac, as part of [freetds](http://www.freetds.org/userguide/)) and the SQL Server Management Studio.
However, we do not recommend you use these tools because it is easy for character or line-ending encoding issues across platforms to corrupt your data.

The easy, recommended way to ingest the data is by importing the data from a public blob storage container that we created for you.
A "public blob" is what Microsoft calls its shareable storage hosted in the Azure cloud.
Often it is easier to import data from within the same cloud (Azure), as opposed to data from outside the cloud (e.g., your local computer).

Please run the following query, which creates tables in your database and runs a bulk import to fill the tables from the csv files in the public blob:

```sql
CREATE EXTERNAL DATA SOURCE flightsblob
WITH (  TYPE = BLOB_STORAGE,
        LOCATION = 'https://cse344.blob.core.windows.net/flights'
);

BULK INSERT Carriers FROM 'carriers.csv'
WITH (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'flightsblob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1, TABLOCK); -- 1594 rows

BULK INSERT Months FROM 'months.csv'
WITH (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'flightsblob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1, TABLOCK); -- 12 rows

BULK INSERT Weekdays FROM 'weekdays.csv'
WITH (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'flightsblob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1, TABLOCK); -- 8 rows

-- Import for the large Flights table
-- This last import might take a little under 5 minutes on the provided server settings
BULK INSERT Flights FROM 'flights-small.csv'
WITH (ROWTERMINATOR = '0x0a',
DATA_SOURCE = 'flightsblob', FORMAT='CSV', CODEPAGE = 65001, --UTF-8 encoding
FIRSTROW=1, TABLOCK); -- 1148675 rows
```

Do some `SELECT count(*)` statements to check whether your imports were successful.

- Carriers has 1594 rows
- Months has 12 rows
- Weekdays has 8 rows
- Flights has 1148675 rows

### B. Ingesting Data (Alternative) (0 Points)

There's another way to upload the data with an IDE called [DataGrip](https://www.jetbrains.com/datagrip/). 
It's a useful program, and also functions as a good place to write and execute your SQL queries with the Azure database. 
You may use it for the trial period which should be plenty (and once your data is uploaded to Azure you can use the web interface afterwards if you don't want to stick with DataGrip). 
You may also use it with a [free student license](https://www.jetbrains.com/student/) if you register with your UW email address.

1. Follow the [instructions](https://www.jetbrains.com/help/datagrip/connecting-to-a-database.html#ms_azure) for connecting to Azure.

2. Run your create table statements in Azure.

3. There's an extra step here they don't mention in the connection instructions, to make your tables visible. 
When setting up your data source, you have to click the schemas tab and check the boxes for the database name, and `dbo`.

*NOTE: The picture corresponds to cse414-18au, but for us it should say cse414-20wi or whatever you choose as a name for your database instead.*

![datagrip schema](https://courses.cs.washington.edu/courses/cse414/20wi/datagrip1.png)

4. Now your tables should be visible in the drop down box.

![datagrip schema](https://courses.cs.washington.edu/courses/cse414/20wi/datagrip2.png)

5. Now you can [import the csvs](https://www.jetbrains.com/datagrip/features/importexport.html) 
by right clicking the table to want to upload and finding the .csv file.

### C. SQL Queries (90 points):

For each question below, write a single SQL query to answer that question, 
and save your submission in individual files `hw3-q1.sql`, `hw3-q2.sql`, etc.
For each query, add a comment with

* the number of rows your query returns,
* how long the query took, and 
* the first 20 rows of the result (if the result has fewer than 20 rows, output all of them). 

You can find the query time on the right side of the yellow bar at the bottom of 
the window. You can simply copy and paste the first rows into the comment. 

Note that SQL Server interprets NULL values differently than SQLite. 
Try using it in a `WHERE` predicate and you will see the difference.

Finally, make sure you know how to determine whether a flight is canceled or not.

Answer the following questions:

1. (10 points)
Between how many distinct pairs of cities (indepdendent of flight direction) do direct flights operate?
By "direct flight", we mean a flight with no intermediate stops.
By "independent of flight direction", we mean that two cities such as ('Seattle WA', 'Boise ID') 
should be counted once even if there are both flights from Seattle to Boise and from Boise to Seattle.
Consider both canceled and non-canceled flights.

    Name the output column `num_connected_cities`.

    [Output relation cardinality: 1 row]


2. (10 points)
For each origin city, find the destination city (or cities) with the shortest direct flight *for non-canceled flights*.
By direct flight, we mean a flight with no intermediate stops. 
Judge the shortest flight in time, not distance.

    Name the output columns `origin_city`, `dest_city`, and `time` representing the the flight time between them. 
    Do not include duplicates of the same origin/destination city pair. 
    Order the result by `time` ascending and then `origin_city` ascending (i.e. alphabetically).
    
    [Output relation cardinality: 339 rows]


3. (10 points)
Find origin cities that only serve flights shorter than 3 hours. 

    Name the output column `city` and sort them alphabetically. List each city only once in the result.

    [Output relation cardinality: 109]


4. (15 points) 
For each origin city, find the percentage of *non-canceled* departing flights shorter than 3 hours. 
(That is, compute `number of non-canceled departing flights shorter than 3 hours` / `number of non-canceled departing flights` * 100%, for each origin city.)

    Name the output columns `origin_city` and `percentage`
    Order by percentage value, ascending. 

    Be careful to handle cities without any flights shorter than 3 hours. 
    Please report `0` as the result for those cities.
    (Hint: if your solution returns `NULL` for those cities, find a way to replace `NULL` with `0`.
    Consider using a SQL `CASE` clause.)

    Report percentages as percentages not decimals (e.g., report 75.25 rather than 0.7525).

    [Output relation cardinality: 327]


5. (15 points)
List all cities that cannot be reached from Seattle though a direct flight 
but can be reached with one stop (i.e., with any two flights that go through an intermediate city). 
Do not include Seattle as one of these destinations (even though you could get back with two flights). 

    Name the output column `city`. Order the output ascending by city.
    
    [Output relation cardinality: 256]


6. (15 points)
List all cities that cannot be reached from Seattle through a direct flight
and cannot be reached from Seattle with one stop (i.e., with any two flights that go through an intermediate city). 
Assume all cities to be the collection of both `origin_city` and `dest_city`.

    Warning: this query might take a while to execute.
    We will learn about how to speed this up in lecture. 

    Name the output column `city`. Order the output ascending by city.

    [Output relation cardinality: 4]


7. (10 points)
List the names of carriers that operate flights from Seattle to San Francisco CA. 

    Name the output column `carrier`. 
    Return each carrier's name once.
    Order the output ascending by carrier.
    
    [Output relation cardinality: 4]

8. (5 points)
For each day of the week, find cities with the *top two* highest average number of arriving flights on that day of week.
Report the day of week, city, and the average number of arriving flights into that city on that day of the week.

    Warning: the days of the week occur a different number of times in the Flights dataset. 
    For example, Sunday occurs on 4 days while Friday occurs on 5 days.

    Name the output columns `day_of_week`, `dest_city`, `avg_flights`.
    Report day of week names (e.g. Sunday) rather than day of week numbers (e.g. 7).
    Order the days of the week from Monday to Sunday in order (e.g. Monday, Tuesday, Wednesday, ...), then by `avg_flights` descending.

    [Output relation cardinality: 14]



### D. Using a Cloud Service (10 points)

The DBMS that we use in this assignment is running somewhere in one of Microsoft's data centers. 
Comment on your experience using this DBMS cloud service. 
List a couple *advantages* and *disadvantages* to offering a DBMS as a service in a public cloud.

Save your answer in a file called `hw3-d.txt`.



## Submission Instructions
Answer each of the queries above and put your SQL query in a separate file. 
Call them `hw3-q1.sql`, `hw3-q2.sql`, etc. (and `hw3-d.txt` for the last question). 
Make sure you name the files exactly as is.

*Points may be deducted for incorrect file names, or for `.sql` files that are not executable.*

To submit, push your code to the hw3 repo for your team, `https://gitlab.cs.washington.edu/cse414-20wi/hw3/cse414-hw3-[...team usernames...]`.
The time you most recently push to your GitLab repo is considered your submission time.

To remind you, in order for your answers to be added to the git repo, you need to explicitly add each file:

```sh
$ git add hw3-q1.sql hw3-q2.sql ...
```

and push to make sure your code is uploaded to GitLab:

```sh
$ git commit
$ git push
```

**Again, just because your code has been committed on your local machine does not mean that it has been 
submitted -- it needs to be on GitLab!**

As with previous assignments, make sure you check the results afterwards to make sure that your files have been committed.
As a last resort, if git is not working properly, consider using the "file upload" feature on the GitLab website to add your solution to your repository.
