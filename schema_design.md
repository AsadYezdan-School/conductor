GOAL

this is a repo for a job scheduling service, it uses the Submitter to add jobs to a database, the jobs are http requests for now (refer to db-migrations/changelog/v001_initial_schema.sql for how thet are defined)(job definitions are immutable, editing a job def should result in a new version of the job being created, mark this using a version number on the job, not that we always execute based on the latest version of a job). each job has a cron expression, and the scheduler reads the db and evaluates when a particular cron expression matches,
when it does it happens the job definition as a message to SQS, a plane of Workers poll the SQS queue, execute the http jobs and persist the status, of the jobs to the db.
We need to come up with a schema that supports changes in job status (waiting, queued,executed, succeeded, failed etc.). I also need  a way to log previous runs of a job so that i can see run history and stats about the previous runsof a job, i need to be able to support retries, and keep the schema open to extension
so that we can add other kinds of jobs in the future, ie shell scripts, python scripts, etc.(but those are out of scope for now, as long as they are considered when designing the schema nothing further is needed).


REQUIREMENTS
The design must encompass the folowing requirements, ensure that your work maps concretely to these ideas key deliverables:

1. Data Model
Based on your scenario, develop a Data Model (S10) using appropriate visual
documentation methods (e.g., an Entity Relationship Diagram) which should include
evidence of:
a) A minimum of 5 entities.
b) Attributes and their data types.
c) Primary Keys (PK) and Foreign Keys (FK).
d) Relationships between entities and their cardinalities.
e) Justifications of any noteworthy design decisions.
f) Explanation of how the model satisfies 3NF normalisation.



2. Database Implementation
Using an appropriate relational database platform, build a prototype database (S10) based
on your Data Model.
Your database implementation should include:
a) The full database schema (all tables) as defined by SQL queries.
b) Sample data populated within each table, of a quantity and type appropriate to the
chosen scenario. You may generate or source synthetic data, or use real, publicly
available data, subject to its terms of use.
c) An explanation of your implementation process in terms of any supportive tools
you used to implement the database e.g., visual database design tools, Generative
AI for SQL generation or troubleshooting.
Evidence your database implementation and sample data collection or generation using
screenshots, references, and SQL queries (as text) in your report.

3. Data Reporting and Visualisation
Following the implementation of your database, you should demonstrate its Business
Intelligence value through appropriate data queries and visualisations which would support
data-driven decision-making in the context of your real-world scenario.
To do this, complete the following tasks to deliver a publicly available dashboard / data
analysis resource:
a) Develop at least 3 custom SQL queries which draw on data from more than 1
table and which could be used to support decision-making in the context of the
chosen real-world scenario.
b) Interact with your database programmatically using code or visually using a GUI-
based tool to generate interactive tables and data visualisations. Possible
approaches may include, but are not limited to, the following:
• An online Python notebook (e.g., Jupyter, Google Colab, Kaggle) and the
Matplotlib library. (Ideal for Data Analysts)
• A hosted dashboard tool e.g., PowerBI or Tableau or other no-code tools. (Ideal
for Data / Business Analysts)
• A simple web application (e.g., a React or NextJS app published on Vercel)
combined with a JavaScript visualisation library such as GraphJS. (Ideal for
Software Engineers)
c) Generate at least two data visualisations (S13) appropriate to the data analysis
techniques being adopted to help support data-driven decision making within the
context of your chosen scenario e.g., a word cloud for text analysis, line graph for
trend / predictive analysis, etc… (S11)
As well as showcasing the final solution, which should be publicly accessible, explain
and evidence your implementation process and justify your choice of data
visualisations, referring back to the real-world scenario and relevant supporting
literature. For this deliverable, the plan is to create a Raact based web application, that allows for new jobs to be submitted, job status to be viewed, allow modificationss to job defs, jobs to be parked (ie they are submitted but we want the scheduler, not to execute them until they are unparked), and allow jobs to be deleted


