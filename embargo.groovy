import groovy.sql.Sql;
import au.com.bytecode.opencsv.CSVReader;

class EmbargoMigrator {


  static void main(String[] args) {

    def config = new ConfigSlurper().parse(new File('config.groovy').toURL())

    def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
    def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)

    newsql.execute("truncate attachment cascade")
    newsql.execute("truncate actionlog cascade")
    newsql.execute("truncate committee_member cascade")
    newsql.execute("truncate submission cascade")
    newsql.execute("truncate embargo_type cascade")


    // Main driver query - for each embargo type in old vireo
    int i = 0;
    sql.eachRow("select preference from vireoadminpreference where preference_type = 'AVAILABLE_EMBARGO_TYPES' order by preference_order asc"){

      row ->
      CSVReader reader = new CSVReader(new StringReader(row.preference));

      String[] fields = reader.readNext();
      
      
      
      
      int displayOrder = i++;
      String name = fields[0];
      String description = fields[1];
      Integer duration = null;
      if (!"indefinite".equals(fields[2]))
        duration = Integer.valueOf(fields[2]);
      
      boolean active = false;
      if ("true".equals(fields[3]))
        active = true;
    
      def params = [displayOrder, displayOrder, name, description, duration, active];
      
      newsql.execute '''insert into embargo_type 
        (id, displayOrder, name, description, duration, active)
        values (?,?,?,?,?,?)''',params
      


    }


    def row = newsql.firstRow("select (max(id) + 1) max  from embargo_type")
    newsql.execute("alter sequence seq_embargo_type restart with " + row.max )

  }
}













