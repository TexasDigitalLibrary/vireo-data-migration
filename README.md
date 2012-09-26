# Vireo Data Migration #

Scripts for migrating from Vireo 1.x to the new [Vireo 1.8](https://github.com/TexasDigitalLibrary/Vireo). 
The scripts are not well tested in various environment and you are advised to 
throughly test the output of these migration scripts at your institution before
deploying in a production environment. Remember, it is always a good practice
to have a backup of your data. 

These upgrade scripts only work for a postgresql to postgresql migration.

## Instructions ##

1. **Install Groovy**

  These installation script are written in a java-like scripting language
  called Groovy and thus require that Groovy interpreter be installed. 
  [Download the groovy binary](http://groovy.codehaus.org/Download) and
  install the binary in your path.

2. **Setup new database**

  Create a new database and define the vireo schema. It is also advise that you use the predefined indexes.
  * Create a DB user: `createuser -dSRP vireo2`
  * Create the database: `createdb -U vireo2 -E UNICODE -h localhost vireo2`
  * Create the schema: `psql -U vireo2 vireo2 < [vireo2]/conf/sql/postgresql/schema.sql`
  * Create the indexes: `psql -U vireo2 vireo2 < [vireo2]/conf/sql/postgresql/indexes.sql`

3. **Configure the migration**

  Edit the config.groovy configuration file for you're installation. Here are the important parameters that *must* be set.

  **For your old dspace-based vireo installation**
  - `old_db_url`: The JDBC URL for the database
  - `old_db_user`:  Database username
  - `old_db_pwd`:  Database password
  - `old_asset_path`: The path to DSpace's assetstore (with a trailing slash)
  - `old_entity_id`: The vireo instance id, typically this is your schools DNS name (i.e. tamu.edu, ut.edu)
  - `old_collection_id`: The dspace collection id which contains submissions. You can find this by looking in your dspace.cfg's `xmlui.vireo.collection.[entity-id]` or `xmlui.vireo.default.collection`  

  **For your new vireo 1.8 installation**
  - `new_db_url`: The JDBC URL for the new database
  - `new_db_user`: Database username
  - `new_db_pwd`: Database password
  - `new_asset_path`: The path to Vireo2's assetstore. This directory must exist, and the path should end with a trailing slash.
  - `link_assets`: true or false. Whether you want the files copied into the new asset store or just symlinked. Linking is useful while testing because it is quicker, however for a production deployment you should always set this to false.

  **Migration Admin**
  - `create_admin`: true or false. Whethey you would like a new administrative user to be created during the installation. This can be helpfull during testing to have an easy account to login with on the new system. If this flag is true, then set the following parameters.
  - `admin_email`: The email address of the new administrative user.
  - `admin_first`: The first name of the new administrative user.
  - `admin_last`: The last name of the new administrative user.
  - `admin_netid`: The netid of the new administrative user (may be null).
  - `admin_password`: The password of the new administrative user.

4. **Run the migration & pray**
  
  `./migration.sh`
   
  Good Luck**!** The script may not be able to parse all names in the old vireo into their components (first, middle, last, birth). When these names are found the migration scripts will do a fall back parsing and report the results.
