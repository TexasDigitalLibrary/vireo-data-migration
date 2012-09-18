import groovy.sql.Sql;
import au.com.bytecode.opencsv.CSVReader;
import java.text.DateFormatSymbols;

class SettingsMigrator {

  // Migrate all the settings
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
    newsql.execute("truncate configuration cascade");



    embargos(sql,newsql,entityId);
    customActions(sql,newsql,entityId);
    emailTemplates(sql,newsql,entityId);
    graduationMonths(sql,newsql,entityId);
    basicList(sql,newsql,'AVAILABLE_COLLEGES','college',entityId);
    basicList(sql,newsql,'AVAILABLE_DEPARTMENTS','department',entityId);
    basicList(sql,newsql,'AVAILABLE_MAJORS','major',entityId);
    levelList(sql,newsql,'AVAILABLE_DEGREES','degree',entityId);
    levelList(sql,newsql,'AVAILABLE_DOCUMENT_TYPES','document_type',entityId);
    
    def row = sql.firstRow("select preference from vireoadminpreference where preference_type = 'SUBMISSION_INSTRUCTIONS' and entity_id = ?",[entityId])
    
    def instructions = DEFAULT_SUBMIT_INSTRUCTIONS
    if (row != null && row.preference != null)
      instructions = row.preference;
    
    configuration(newsql,'submit_instructions',instructions);
    configuration(newsql,'submit_license',DEFAULT_SUBMIT_LICENSE);


  }

  // Migrate lists which have degree level
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

  // Migrate generic lists
  static void basicList(Sql sql, Sql newsql, String type, String table, String entityId) {

    int i = 0;
    sql.eachRow("select preference from vireoadminpreference where preference_type = ? and entity_id = ? order by preference_order asc",[type,entityId]) {
      row ->

      i++;
      def params = [i, i, row.preference];
      newsql.execute "insert into "+table+" (id, displayorder, name) values (?,?,?)",params
    }

    def row = newsql.firstRow("select (max(id) + 1) max  from "+table)

    if (row.max != null)
        newsql.execute("alter sequence seq_"+table+" restart with " + row.max )
  }

  // Set a particular configuration value on the new vireo.
  static void configuration(Sql newsql, String name, String value) {
    def row1 = newsql.firstRow("select (max(id) + 1) max  from configuration")
    def id;
    if (row1 != null && row1.max != null)
      id = row1.max + 1;
    else
      id = 1;

    def params = [id, name, value];
    newsql.execute '''insert into configuration (id, name, value) values (?,?,?)''',params

    def row2 = newsql.firstRow("select (max(id) + 1) max  from configuration")
    newsql.execute("alter sequence seq_configuration restart with " + row2.max )
  }

  // Migrate graduation months
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

  // Migrate email templates
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

  // Migrate custom actions
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

  // Migrate embargos
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


  public final static String DEFAULT_SUBMIT_INSTRUCTIONS =
  "The Thesis Office has received your electronic submittal. You will also receive an email confirmation. We will check your records as soon as possible to determine whether or not we have the signed Approval Form on file. Please be aware that your file is not complete and cannot be reviewed until we have both the electronic manuscript and the signed Approval Form. \n"+
  "\n" +
  "As soon as both items have been received, your manuscript will be placed in the queue and will be processed along with all other submissions for the semester in the order in which your completed file (manuscript and Approval Form) was received.\n"+
  "\n" +
  "The following are approximate turn-around times after the manuscript and the signed approval form have been submitted to the Thesis Office. Manuscripts are reviewed in the order received.\n"+
  "\n" +
  "Early in semester â 5 working days\n" +
  "Week before Deadline Day â 10 working days\n" +
  "Deadline Day â 15 working days\n" +
  "\n"+
  "If you have any questions about your submittal, feel free to contact our office. \n" +
  "\n" +
  "Thank you,\n" +
  "\n" +
  "Thesis Office\n";

  public final static String DEFAULT_SUBMIT_LICENSE =
  "I grant the Texas Digital Library (hereafter called \"TDL\"), my home institution (hereafter called \"Institution\"), and my academic department (hereafter called \"Department\") the non-exclusive rights to copy, display, perform, distribute and publish the content I submit to this repository (hereafter called \"Work\") and to make the Work available in any format in perpetuity as part of a TDL, Institution or Department repository communication or distribution effort.\n" +
  "\n" +
  "I understand that once the Work is submitted, a bibliographic citation to the Work can remain visible in perpetuity, even if the Work is updated or removed.\n" +
  "\n" +
  "I understand that the Work's copyright owner(s) will continue to own copyright outside these non-exclusive granted rights.\n" +
  "\n" +
  "I warrant that:\n" +
  "\n" +
  "    1) I am the copyright owner of the Work, or\n" +
  "    2) I am one of the copyright owners and have permission from the other owners to submit the Work, or\n" +
  "    3) My Institution or Department is the copyright owner and I have permission to submit the Work, or\n" +
  "    4) Another party is the copyright owner and I have permission to submit the Work.\n" +
  "\n" +
  "Based on this, I further warrant to my knowledge:\n" +
  "\n" +
  "    1) The Work does not infringe any copyright, patent, or trade secrets of any third party,\n" +
  "    2) The Work does not contain any libelous matter, nor invade the privacy of any person or third party, and\n" +
  "    3) That no right in the Work has been sold, mortgaged, or otherwise disposed of, and is free from all claims.\n" +
  "\n" +
  "I agree to hold TDL, Institution, Department, and their agents harmless for any liability arising from any breach of the above warranties or any claim of intellectual property infringement arising from the exercise of these non-exclusive granted rights.\n"+
  "\n";
}

