import groovy.sql.Sql;
import au.com.bytecode.opencsv.CSVReader;
import java.text.DateFormatSymbols;

class EmbargoMigrator {


  static void main(String[] args) {

    def config = new ConfigSlurper().parse(new File('config.groovy').toURL())

    def entityId = config.old_entity_id;

    def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
    def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)

    newsql.execute("truncate embargo_type cascade")
    newsql.execute("truncate custom_action_value cascade")
    newsql.execute("truncate custom_action_definition cascade")
    newsql.execute("delete from email_template where systemrequired = false")
    newsql.execute("truncate college cascade")
    newsql.execute("truncate department cascade")
    newsql.execute("truncate major cascade")
    newsql.execute("truncate degree cascade")
    newsql.execute("truncate document_type cascade")
    newsql.execute("truncate graduation_month cascade")
    


    embargos(sql,newsql,entityId);
    customActions(sql,newsql,entityId);
    emailTemplates(sql,newsql,entityId);
    graduationMonths(sql,newsql,entityId);
    basicList(sql,newsql,'AVAILABLE_COLLEGES','college',entityId);
    basicList(sql,newsql,'AVAILABLE_DEPARTMENTS','department',entityId);
    basicList(sql,newsql,'AVAILABLE_MAJORS','major',entityId);
    levelList(sql,newsql,'AVAILABLE_DEGREES','degree',entityId);
    levelList(sql,newsql,'AVAILABLE_DOCUMENT_TYPES','document_type',entityId);


  }


  static void levelList(Sql sql, Sql newsql, String type, String table, String entityId) {

    int i = 0;
    sql.eachRow("select preference from vireoadminpreference where preference_type = ? and entity_id = ? order by preference_order asc",[type,entityId]) {
      row ->

      def partsList = row.preference.tokenize("..:..");

      def level = 0;
      if ("Undergraduate".equals(partsList[1]))
        level = 1;
      if ("Masters".equals(partsList[1]))
        level = 2;
      if ("Doctoral".equals(partsList[1]))
        level = 3;

      i++;
      def params = [i, i, partsList[0], level];
      newsql.execute "insert into "+table+" (id, displayorder, name, level) values (?,?,?, ?)",params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from "+table)
    newsql.execute("alter sequence seq_"+table+" restart with " + row.max )
  }

  static void basicList(Sql sql, Sql newsql, String type, String table, String entityId) {

    int i = 0;
    sql.eachRow("select preference from vireoadminpreference where preference_type = ? and entity_id = ? order by preference_order asc",[type,entityId]) {
      row ->

      i++;
      def params = [i, i, row.preference];
      newsql.execute "insert into "+table+" (id, displayorder, name) values (?,?,?)",params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from "+table)
    newsql.execute("alter sequence seq_"+table+" restart with " + row.max )
  }

  static void graduationMonths(Sql sql, Sql newsql, String entityId) {

    // Main driver query - for each month in old vireo
    int i = 0;
    sql.eachRow("select preference from vireoadminpreference where preference_type = 'AVAILABLE_GRADUATION_MONTHS' and entity_id = ? order by preference_order asc",[entityId]) {
      row ->

      int month = 0;
      for (int m = 0; m <= 11; m++) {
        if (new DateFormatSymbols().getMonths()[m].equals(row.preference))
          month = m;
      }

      i++;
      def params = [i,i,month];
      newsql.execute '''insert into graduation_month (id, displayorder, month) values (?,?,?)''',params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from graduation_month")
    newsql.execute("alter sequence seq_graduation_month restart with " + row.max )
  }

  static void emailTemplates(Sql sql, Sql newsql, String entityId) {

    // Main driver query - for each email template in old vireo
    int i = 100;
    sql.eachRow("select preference from vireoadminpreference where preference_type = 'EMAIL_TEMPLATES' and entity_id = ? order by preference_order asc",[entityId]) {
      row ->

      def partsList = row.preference.tokenize("..:..");

      i++;
      def params = [i,i, partsList[0], "[ETD] {FULL_NAME}'s {DOCUMENT_TYPE}",partsList[1]];
      newsql.execute '''insert into email_template (id, displayorder, name, subject, message, systemrequired) values (?,?,?,?,?,false)''',params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from email_template")
    newsql.execute("alter sequence seq_email_template restart with " + row.max )
  }

  static void customActions(Sql sql, Sql newsql, String entityId) {

    // Main driver query - for each custom action in old vireo
    int i = 0;
    sql.eachRow("select preference, pref_id from vireoadminpreference where preference_type = 'ACTION_CHECKLIST' and entity_id = ? order by preference_order asc",[entityId]) {
      row ->

      def params = [row.pref_id, i++, row.preference];
      newsql.execute '''insert into custom_action_definition (id, displayorder, label) values (?,?,?)''',params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from custom_action_definition")
    newsql.execute("alter sequence seq_custom_action_definition restart with " + row.max )
  }

  static void embargos(Sql sql, Sql newsql, String entityId) {
    // Main driver query - for each embargo type in old vireo
    int i = 0;
    sql.eachRow("select preference from vireoadminpreference where preference_type = 'AVAILABLE_EMBARGO_TYPES' and entity_id = ? order by preference_order asc",[entityId]) {
      row ->
      CSVReader reader = new CSVReader(new StringReader(row.preference));

      // Parse out the embargo fields
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

      newsql.execute '''insert into embargo_type (id, displayOrder, name, description, duration, active) values (?,?,?,?,?,?)''',params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from embargo_type")
    newsql.execute("alter sequence seq_embargo_type restart with " + row.max )

  }

}

