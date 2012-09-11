# Vireo Data Migration #

Scripts for migrating from Vireo 1.x to the new [Vireo 1.8](https://github.com/TexasDigitalLibrary/Vireo).

Instructions

1. Install Groovy

  These installation script are written in a java-like scripting language called Groovy and thus require that Groovy interpreter be installed. [Download the groovy binary](http://groovy.codehaus.org/Download) and install the binary in your path.

2. Setup new database

  **For PostgreSQL**
  - Create a DB user: `createuser -dSRP vireo2`
  - Create the database: `createdb -U vireo2 -E UNICODE -h localhost vireo2`

2. Configure the migration

  Edit the config.groovy configuration file for you're installation. Here are the important parameters that *must* be set.

  **For your old dspace-based vireo installation**
  - old_db_url: The JDBC URL for the database
  - old_db_user:  Database username
  - old_db_pwd:  Database password

  **For your new vireo 1.8 installation**
  - new_db_url: The JDBC URL for the new database
  - new_db_user: Database username
  - new_db_pwd: Database password

3. Run the migration & pray
  
  `./migration.sh`
   
  You should be finished now. Good luck.
