# CSE 414 Winter 2020 Homework 6: Parallel Data Processing and Spark

**Objectives:**  To write distributed queries. To learn about Spark and running distributed data processing in the cloud using AWS.

**Assigned date:** Wednesday, February 26, 2020.

**Due date:** Wednesday, March 4, 2020 at 11pm.

**What to turn in:**

Your written transactions answers in the `cost_estimation.md` file. Add your Spark code to the single Java file, `SparkApp.java` in the `src` directory, along with the text outputs from AWS (output for QA in QA.txt, for QB in QB.txt, for QC in QC.txt). A skeleton `SparkApp.java` has been provided for you.

**Resources:**

- [Spark programming guide](https://spark.apache.org/docs/2.4.4/rdd-programming-guide.html)
- [Spark Javadoc](https://spark.apache.org/docs/2.4.4/api/java/index.html)
- [Amazon web services EMR (Elastic MapReduce) documentation](https://aws.amazon.com/documentation/emr/)
- [Amazon S3 documentation](https://aws.amazon.com/documentation/s3/)

## Cost Estimation

(15 points) Estimate the cost of the below physical plan under pipelined execution given the following statistics and indexes. You will not need all the statistics nor indexes. You may assume uniform distribution and independence of attribute values where applicable. We have computed some of the estimated cardinalities of intermediate results already (verify them for fun!). Include your work (equations) and your complete estimate.

| R(a)        |             |             |
|-------------|-------------|-------------|
| B(R) = 1000 | T(R) = 10^5 | Clustered index on R(a) |
| min(a, R) = 150 | max(a, R) = 250 |  |

| S(a, b, c)  |             |             |
|-------------|-------------|-------------|
| B(S) = 2000 | T(S) = 4*10^4 | Unclustered index on S(a) |
| min(a, S) = 0 | max(a, S) = 250 | Unclustered index on S(b) |
| V(b, S) =  1000 | V(c, S) =  10 | Clustered index on S(c) |

| U(b, d)     |             |             |
|-------------|-------------|-------------|
| B(U) = 500 | T(U) = 10^4 | Unclustered index on U(b) |
| V(b, U) =  250 | | 
| min(d, U) = 1 | max(d, U) = 1000 | |

<img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/costest.jpg" width="700"/>

For the written part of this assignment, put your answers in the `cost_estimation.md` file in [markdown format](https://www.markdownguide.org/basic-syntax).

## Spark Programming Assignment

In this homework, you will be writing Spark and Spark SQL code, to be executed both locally on your machine and also using Amazon Web Services.

We will be using a similar flights dataset used in previous homeworks. This time, however, we will be using the *entire* data dump from the [US Bereau of Transportation Statistics](https://www.transtats.bts.gov/DL_SelectFields.asp?Table_ID=236&DB_Short_Name=On-Time), which consists of information about all domestic US flights from 1987 to 2011 or so. The data is in [Parquet](https://parquet.apache.org/) format. Your local runs/tests will use a subset of the data (in the `flights_small` directory) and your cloud jobs will use the full data (stored on Amazon [S3](https://aws.amazon.com/s3/)).

Here is a rough outline on how to do the HW:

A. Sign up for AWS and apply for credit for AWS

B. Complete the HW locally

C. Run your solutions on AWS Elastic MapReduce one at a time when you are fairly confident with your solutions

### A. Sign up on Amazon Web Services

Follow these steps to set up your Amazon Web Services account.

1. If you do not already have an Amazon Web Services account, go to [AWS Educate](https://aws.amazon.com/education/awseducate/) and sign up with your @uw email.
1. Click "Join AWS Educate", and choose the student account. Then complete the necessary forms, and wait until your account is approved. **This may take a while, so do this early.**
2. To get $$$ to use Amazon AWS, sign in to your AWS Educate account, click "AWS Account", and create a starter account. **This will only work if you have not applied for a starter account in the past.**
3. Follow the steps to finalize your credits. If everything works, you should have $100 credit in your account.

**IMPORTANT: If you run AWS in any other way rather than how we instruct you to do so below, you must remember to manually terminate the AWS clusters when you are done. While the credit that you receive should be more than enough for this homework assignment, you will be responsible for paying the extra bill should your credits be exhausted.**

Now you are ready to run applications using Amazon cloud. But before you do that let's write some code and run it locally.

### B. Get Code Working Locally

We have created empty method bodies for each of the questions below (QA, QB, and QC). *Do not change any of the method signatures*. You are free to define extra methods and classes if you need to. We have also provided a warmup method that shows fully-functional examples of three ways that the same query could be solved using Spark's different APIs.

There are many ways to write the code for this assignment. Here are some documentation links that we think would get you started up about what is available in the Spark functional APIs:
* [Spark 2.4.5 Manual](https://spark.apache.org/docs/2.4.5/)
* [Spark 2.4.5 Javadocs](https://spark.apache.org/docs/2.4.5/api/java/index.html)
* [Dataset](https://spark.apache.org/docs/2.4.5/api/java/org/apache/spark/sql/Dataset.html)
* [Row](https://spark.apache.org/docs/2.4.5/api/java/org/apache/spark/sql/Row.html) (see also RowFactory)
* [JavaRDD](https://spark.apache.org/docs/2.4.5/api/java/index.html?org/apache/spark/api/java/JavaRDD.html) (see also JavaPairRDD)
* [Tuple2](https://www.scala-lang.org/api/2.9.1/scala/Tuple2.html)

The quickstart documentation also more depth and examples of using [RDDs](https://spark.apache.org/docs/2.4.5/rdd-programming-guide.html) and [Datasets](https://spark.apache.org/docs/2.4.5/sql-getting-started.html).

For questions a, b, and c, you will get the points for writing a correct query.

(a) (15 points) Complete the method QA in SparkApp.java. Use the Spark functional APIs or SparkSQL. Select all flights that leave from 'Seattle, WA', and return the destination city names. Only return each destination city name once. Return the results in an RDD where the Row is a single column for the destination city name.

(b) (30 points) Complete the method QB in SparkApp.java. Only use the Spark functional APIs. Find the number of non-canceled (!= 1) flights per month-origin city pair. Return the results in an RDD where the row has three columns that are the origin city name, month, and count, in that order.

(c) (30 points) Complete the method QC in SparkApp.java.  Only use the Spark functional APIs. Compute the average delay from all departing flights for each city. Flights with NULL delay values should not be counted, and canceled flights should not be counted. Return the results in an RDD where the row has two columns that are the origin city name and average, in that order.

#### Testing Locally
We provide cardinality testing when you run

`$ mvn test`

You are responsible for verifying you have the correct format and contents in your results.

#### Running Local Jobs
To actually execute the main method, toggle the SparkSession initialization on lines 147 and 148 of SparkApp.java to allow it to run on locally (local SparkSession, not cluster). Run from the top level
 directory (with pom.xml in it):

```
$ mvn clean compile assembly:single
$ java -jar target/sparkapp-1.0-jar-with-dependencies.jar \
      flights_small output
```

Note that, on Windows, this should be executed in the root directory of this repo, so that the program can find `bin/winutils.exe`. (The code uses the directory of execution `.`; you can change this in the `createLocalSession()` method if you must.) For reference, `winutils.exe` was obtained for this Hadoop version from a [Github repo](https://github.com/cdarlint/winutils).

For this quarter, we added code to add compatibility with Java 9+. It should work fine. In case there is a problem, you can force a Java 8 execution by downloading a Java 8 JRE and setting your JAVA_HOME variable to your Java 8 runtime.


### C. Run Code on Elastic Map Reduce (EMR)
(10 points) 

Run your jobs on Elastic Map Reduce (EMR) as described below, and copy the resulting output from EMR to QA.txt, QB.txt, and QC.txt, respectively. For this part you will get full credit only if both your queries are correct and you have the correct output from running the full dataset on AWS in your QA.txt, QB.txt, and QC.txt files.

Running all jobs at the same time with the provided configuration took less than 30 min for the solutions.

We will use Amazon's [Elastic Map Reduce](https://aws.amazon.com/emr/) (EMR) to deploy our code on AWS. Follow these steps to do so after you have set up your account, received credits as mentioned above, and have tested your solution locally. **Read this carefully!**

1. Toggle the `SparkSession` initialization of `SparkApp.java` to allow it to run on AWS (cluster SparkSession; comment out local). Then create a jar file from the top level directory that packages everything needed to run the Spark application. The following command creates the jar file in the `targets` folder: 

    ```sh
    $ mvn clean compile assembly:single
    ```

2. Login to [S3](https://s3.console.aws.amazon.com/s3/home) and create a "bucket". S3 is Amazon's cloud storage service, and a bucket is similar to a folder. Give your bucket a meaningful name and select **US East (N. Virginia)**, and leave the other settings as default. Upload the jar file that you created in Step 1 to that bucket by selecting that file from your local drive and click "Upload" once you have selected the file.

3. Login to [EMR](https://console.aws.amazon.com/elasticmapreduce/home?region=us-east-1). Make sure you select `US East (N. Virginia)` on the upper right. **This is the only region supported by the starter account.**

4. We will first configure cluster software. **Click on the “Create Cluster" link, then click "Go to advanced options”** in the Amazon EMR console. 
    * **Check the boxes for Spark and Hadoop** so that your screen looks like this:
      <img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/hw6-createCluster-5.29.0.png" width="700"/>

    * Next, scroll to the Steps section at the bottom of the page and **create a Spark application step**. A "step" is a single job to be executed. You could specify multiple Spark jobs to be executed one after another in a cluster. Fill out the Spark application step details by filling in the boxes so that your screen looks like this (with SparkApp instead of HW6):
      <img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/SparkAddStep.JPG" width="700"/>

        The --class option under "Spark-submit options" tells Spark where your main method lives. (--class edu.uw.cs.SparkApp)

        The "Application location" should just point to where your uploaded jar file is. You can use the folder button to navigate.

        The full flights data location is the first argument: s3://us-east-1.elasticmapreduce.samples/flightdata/input

        The output destination is the second argument. Use can use the bucket that holds your jar. You can modify the “output” folder name prefix to be something different if you like. 

        Make sure you fill out the correct bucket names. There are two arguments listed (and separated by white space, as if you were running the program locally):

        **Change “Action on failure” to “Terminate cluster”** (or else you will need to terminate the cluster manually).

    * **Click Add.**

    * Back to the main screen, now on the **After last step completes:** option at the bottom of the page, select **Cluster auto-terminates** so the cluster will shut down once your Spark application is finished. **Click Next.**

    * On the next screen, we will now configure the hardware for a five-node EMR cluster to execute the code. We recommend using the “m4.large” "instance type", which is analogous to some set of allocated resources on a server (in AWS terminology, “m” stands for high memory, “4” represents the generation of servers, and “large” is the relative size of allocated resources). You get to choose how many machines you want in your cluster. For this assignment 1 master instance and 4 core (i.e., worker) instances of m4.large should be good. You are free to add more or pick other types, but make sure you think about the price tag first... Grabbing 100 machines at once will probably drain your credit in a snap :( If m4.large is not available, choose another instance with a similar name (m4.xlarge, m5.large, etc.). **Click Next.**
      <img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/SparkAddCluster.JPG" width="700"/>

    * Under “General Options” *uncheck the “Termination protection” option*. We recommend that you allow the default logging information in case you need to debug a failure. **Click Next.**

    * Click *Create cluster* once you are done and your cluster will start spinning up!

It will take a bit for AWS to both provision the machines and run your Spark job. As a reference, it took about 10 mins to run the warmup job on EMR. You can monitor the status of your cluster on the EMR homepage.

To rerun a similar job (maybe you want to try a different jar), use the "Clone" cluster button to copy the settings into a new job when you run your actual HW problems.

**Make sure your cluster is terminated!** It should do so if you selected the options above. You should check this each time you look at the HW, just to make sure you don't get charged for leaving a cluster running. It's fine if you see warning (or even occasional error) messages in the logs. If your EMR job finishes successfully, you should see something similar to the below in the main EMR console screen:

<img src="https://courses.cs.washington.edu/courses/cse344/17au/assets/hw6-success.png" width="700"/>

#### Debugging AWS jobs

Debugging AWS jobs is not easy for beginners. Besides making sure your program works locally before running on AWS, here are some general tips:

- Make sure that you set ALL the job details (i.e., options, arguments, bucket names, etc) correctly!
- Make sure you switched the two lines of SparkSession code mentioned to run your job on AWS instead of locally.
- Make sure you freshly compile your solution and replace your jar to test a new version of code!
- **99% of cluster failures or errors are due to the first three points!**
- The easiest way to debug is to look at the output/logging files. Spark generates a lot of log files, the most useful ones are probably the `stderr.gz` files listed under `containers/application.../container/stderr.gz`. You will have one `container` folder per machine instance. So make sure you check all folders. You should also check out the log files that you have specified when you created the job in Step 8 above. You can also see the names of those files listed as "Log File" under "Steps":
    <img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/debug.png" width="700"/>
- It is rare that your HW solution is fine but the cluster fails. This is usually due to AWS not being able to grab your machines due to the demand for the instance type saturating the supply available. If you can't find available instances in a region, try changing to a different **EC2 subnet**, like so:

    <img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/subnet.png" width="700"/>
- Spark has a web UI that you can set up to check on job progress etc. You can check out [their webpage](http://docs.aws.amazon.com/emr/latest/ManagementGuide/emr-web-interfaces.html) for details. But these are more involved so you are probably better off to first try examining the logs.  Specifically, try the "Application History" tab and the dropdown.

#### IMPORTANT: Cleanup after completing the HW

Double check that the clusters you have created are all terminated.

S3 charges by [downloading/uploading data from/to the buckets](https://aws.amazon.com/s3/pricing/). So once you are done with the assignment you might want to delete all the buckets that you have created (in addition to shutting down any EMR clusters that you have created).

The amount you are allocated from Amazon should be more than enough to complete the assignment. And every year we have students forgetting to shut down their cluster/clean up their buckets and that can result in substantial charges that they need to pay out of pocket. So be warned!

## Submission Instructions

Turn in your `cost_estimation.md`, `QA.txt`, `QB.txt`, ..., `SparkApp.java` and any other Java files that you created by pushing them to your git repository.

Like previous assignments, make sure you check Gitlab afterwards to make sure that your file(s) have been committed.
