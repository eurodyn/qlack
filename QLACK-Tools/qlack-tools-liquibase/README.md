# QLACK tools - Liquibase
A Docker container allowing to automatically detect changes in your database
and export detected changes to Liquibase _changesets_.

## Preface
This QLACK tool tries to provide solutions to some usual development use cases
pertaining to the evolution of your database schema in your project.

The first thing it does is allowing you to produce an initial Liquibase changeset
capturing the current state of your schema. This should be, usually, used in the
beginning of your project once you have prepared the initial version of
your schema.

The second thing it does is producing liquibase changesets capturing changes
on the above schema. This allows you to keep changing your schema using the tools
you know rather than having to perform all changes via manually-crafted
Liquibase changesets.

Please make sure you read the _Word of caution_ section before you proceed.

### Word of caution
* It is suggested to use Liquibase from the beginning of your project
and capture all database changes in manually crafted changesets. This provides
you the greatest flexibility and control in how your schema evolves.
* Make sure you **always** meticulously review the produced diff-changeset
before you include it in your project. Liquibase does a great job in detecting
changes, however there are logical changes that can not be detected. For example,
if you rename a column, Liquibase will produce _drop column_ and  _add column_
statements - most probably not what you need/expect as you will lose all
data of the original column. In such cases, delete the offending changesets
and replace them with manually-crafted ones (for the rename column example,
you can simply create a [rename column](https://www.liquibase.org/documentation/changes/rename_column.html)
changeset).
* Do not want to take our own word of caution? [Fine](http://www.liquibase.org/2007/06/the-problem-with-database-diffs.html).

## Building the container
`eurodyn/qlack-tools-liquibase` container is already available in Docker Hub,
however if you want to build it locally you can execute:

`make build`

If you do not have `make` available, you can alternatively build it with:

`docker build . -t eurodyn/qlack-tools-liquibase:1`

## Compatible databases
Currently the following databases are supported:
* MySQL
* MariaDB

Note that adding support for new databases is relatively easy, so we would
wholeheartedly welcome PRs for that.

## Prerequisites
You need to have access to the database server from the host where your
Docker Engine runs. To produce diffs, you need a user with appropriate
privileges to create new databases.

## Producing the initial changelog
To produce the initial changelog for your database you need to pass the
following parameters as environmental variables to the Docker container:

`DRIVER`: The class of the JDBC driver to connect to your database.

`DB_HOST`: The host running your database server. In case you run the
database server and Docker Engine on your own machine, you can reference
the host machine from within the container using special Docker DNS entries
such as docker.for.mac.localhost, docker.for.windows.localhost, etc.

`DB_PORT`: The port where your database server listens to.

`DB_SCHEMA`: The name of the database you are accessing.

`DB`: The type/name of the database to connect to (currently, mariadb, mysql).

`DB_USER`: The username to connect with to your database.

`DB_PASS`: The password to connect with to your database.

In addition you should mount a local directory into the Docker container, so
that the changelog can be written into. This directory should be mounted under
`/data`. The generated changelog will be named as `db.changelog.xml`.

For example:
```
docker run --rm \
-e DRIVER=org.mariadb.jdbc.Driver \
-e DB_HOST=docker.for.mac.localhost \
-e DB_PORT=45000 \
-e DB_SCHEMA=sample1 \
-e DB=mariadb \
-e DB_USER=root \
-e DB_PASS=root \
-v /Users/jdoe/tmp:/data \
eurodyn/qlack-tools-liquibase:1 \
generateChangeLog.sh
```

## Producing diffs
To capture changes in your database this script goes through a series of
steps. First, it creates a temporary database in the database server you
have indicated. The temporary database is populated using the Liquibase
scripts you are already using in your project. Finally, the temporary
database is compared with the database you are actively using (on
which you have performed changes) and a changelog is produced.
To produce the diff changelog for your database you need to pass the
following parameters as environmental variables to the Docker container:

`DRIVER`: The class of the JDBC driver to connect to your database.

`DB_HOST`: The host running your database server. In case you run the
database server and Docker Engine on your own machine, you can reference
the host machine from within the container using special Docker DNS entries
such as docker.for.mac.localhost, docker.for.windows.localhost, etc.

`DB_PORT`: The port where your database server listens to.

`DB_SCHEMA`: The name of the database you are accessing.

`DB`: The type/name of the database to connect to (currently, mariadb, mysql).

`DB_USER`: The username to connect with to your database.

`DB_PASS`: The password to connect with to your database.

`CHANGELOG`: The changelog of your project, so that Liquibase scripts
can be replied from it.

Caveat #1: In case your changelog incorporates
includes, make sure that these are referenced in a way that can be found
in all environments. For example, since Liquibase automatically looks
for entries in your classpath, you can reference all your includes using
an absolute style, e.g. _/db/changelog/changes/script0001.xml_. This will
allow Liquibase to find your entries under the _resources_ folder of your
application as well as by this script.

Caveat #2: Make sure all your changelogs have the _logicalFilePath_ attribute.
The _logicalFilePath_ can simply be the filename of your script and it is
necessary for this script to work correctly.

`DIFFLOG`: The name of the changelog that will be produced. Make sure
you provide a path to a folder which is mount in your Docker container,
so that the script is actually copied to your host running the Docker Engine.

Do not forget to mount the folder with the changesets of your progress,
so that this script can find them to use them for populating a temporary
database.

Example:
```
docker run --rm \
-e DRIVER=org.mariadb.jdbc.Driver \
-e DB_HOST=docker.for.mac.localhost \
-e DB_PORT=45000 \
-e DB_SCHEMA=sample1 \
-e DB=mariadb \
-e DB_USER=root \
-e DB_PASS=root \
-e CHANGELOG=/db/changelog/db.changelog-master.yaml \
-e DIFFLOG=/db/changelog/changes/difflog.xml \
-v /Users/jdoe/Projects/proj1/src/main/resources/db:/db \
eurodyn/qlack-tools-liquibase:1 \
diffChangeLog.sh
```