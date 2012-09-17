import groovy.sql.Sql
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SubmissionMigrator {

  // Regex taken from old vireo: org.dspace.app.xmlui.aspect.vireo.model

  static String REGEX_FOR_NAME_TOKEN = "(?:\\p{L}|[-'])+";
  static String REGEX_FOR_INTEGRATED_NAME_PART = "((?:" + REGEX_FOR_NAME_TOKEN + "\\s*)+)";
  static String REGEX_FOR_STANDALONE_NAME_PART = "((?:\\s*" + REGEX_FOR_NAME_TOKEN  + "\\s*)+)";
  static String REGEX_FOR_INTEGRATED_MIDDLE_INITIAL = "(?:\\s+(\\p{L})[.])?";
  static String REGEX_FOR_STANDALONE_MIDDLE_INITIAL = "(?:\\s*(\\p{L})[.]?\\s*)?";

  static Pattern PATTERN_FOR_AUTHORITATIVE_NAME = Pattern.compile( //
  REGEX_FOR_INTEGRATED_NAME_PART // last name
  + "(?:\\s*,\\s*" // delimiter between first and last names
  + REGEX_FOR_INTEGRATED_NAME_PART // first name
  + REGEX_FOR_INTEGRATED_MIDDLE_INITIAL + ")?" // middle
  // initial
  + "(?:\\s*,\\s*(?:\\d+)[-]?)?"); // birth year


  static void main(String[] args) {

    def config = new ConfigSlurper().parse(new File('config.groovy').toURL())

    def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
    def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)

    // These can take a really long time if there is a lot of data - so log when we start and stop

    newsql.execute("truncate attachment cascade")
    newsql.execute("truncate actionlog cascade")
    newsql.execute("truncate submission cascade")
    newsql.execute("truncate custom_action_value cascade")


    // Main driver query - for each submission in old vireo
    int nameExceptions = 0;
    int customActions = 0;
    sql.eachRow("select * from vireosubmission order by submission_id asc"){

      row ->

      def name = getName(sql, row.item_id);

      def lname = null;
      def fname = null;
      def mname = null;

      if (name != null && !",".equals(name)) {
        name = name.trim();

        // This should be made more robust
        Matcher m = PATTERN_FOR_AUTHORITATIVE_NAME.matcher(name);
        if (m.matches()) {
          // Yay, it's a well formed name.

          lname = m.group(1);
          fname = m.group(2);
          mname = m.group(3);
        } else {
          // We have a badly formatted name so fallback to simple parsing

          try {
            def name_parts = name.tokenize(",");
            fname = name_parts[1].trim();
            lname = name_parts[0].trim();
          } catch (RuntimeException re) {
            lname = name;
          }

          nameExceptions++;
          println("["+row.submission_id+"] Unable to parse student '"+name+"', using alternative parsing: l='"+lname+"', f='"+fname+"', m='"+mname+"'");
        }
      } else {
        println("["+row.submission_id+"] Has a blank student name.");
      }

      def committeeApprovalDate = null;
      if (row.committee_approval != null)
        committeeApprovalDate = new java.sql.Date(System.currentTimeMillis());

      def params = [row.submission_id, getUmiRelease(row.umi), row.approval_date, row.college, committeeApprovalDate, row.committee_email_address,
        row.email_hash, null, getDegree(sql, row.item_id),
        getDegreeLevel(sql, row.item_id), getDepartment(sql, row.item_id), getDepositId(sql, row.item_id), getAbstract(sql, row.item_id),
        getKeywords(sql, row.item_id), getDocumentTitle(sql, row.item_id), getType(sql, row.item_id), getGraduationMonth(sql, row.item_id),
        getGraduationYear(sql, row.item_id), new java.sql.Date(System.currentTimeMillis()), "Migrated from old vireo system.",
        row.license_agreement_date, getMajor(sql, row.item_id), getSubStatus(row.status),
        row.year_of_birth, fname, lname, mname, row.submission_date, (row.assigned_to == -1 ?null:row.assigned_to), getEmbargoType(newsql, row.embargo_name), row.applicant_id]

      // Fix lastactionlog and lastactionlogentry

      newsql.execute '''insert into submission (
                        id,                     
                        umirelease,                     
                        approvaldate,                   
                        college,                        
                        committeeapprovaldate,                  
                        committeecontactemail,                  
                        committeeemailhash,                     
                        committeeembargoapprovaldate,                   
                        degree,                 
                        degreelevel,                    
                        department,                     
                        depositid,                      
                        documentabstract,                       
                        documentkeywords,                       
                        documenttitle,                  
                        documenttype,                   
                        graduationmonth,                        
                        graduationyear,                 
                        lastactionlogdate, 
                        lastactionlogentry,                     
                        licenseagreementdate,                   
                        major,                  
                        statename,                      
                        studentbirthyear,                       
                        studentfirstname,                       
                        studentlastname,                        
                        studentmiddlename,                      
                        submissiondate,                 
                        assignee_id,                    
                        embargotype_id,                 
                        submitter_id)
                        values (
                        ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?
                        )''', params

      // Migrate custom action definitions.
      if (row.custom_actions_completed != null) {
        def actions = row.custom_actions_completed.tokenize(",");
        for (String action : actions) {

          action = action.trim();

          // check if the definition exists
          def definition = newsql.firstRow "select id from custom_action_definition where id = ?", [Integer.valueOf(action)];

          if (definition != null) {

            def actionParams = [customActions++, true, Integer.valueOf(action),row.submission_id]
            newsql.execute '''insert into custom_action_value (id, value, definition_id, submission_id) values (?,?,?,?)''', actionParams;

          } else {
            println "Skipping definitio: ${action}";
          }
        }
      }

    }

    println("Student name exceptions: "+nameExceptions);
    // Update sequence counter

    def row1 = newsql.firstRow("select (max(id) + 1) max  from submission")
    newsql.execute("alter sequence seq_submission restart with " + row1.max )

    def row2 = newsql.firstRow("select (max(id) + 1) max  from custom_action_value")
    newsql.execute("alter sequence seq_custom_action_value restart with " + row2.max )

  }

  // Set up value for UMI Release

  static boolean getUmiRelease(Integer val) {
    if (val == null || val == 0)
      return false
    else
      return true
  }

  // Get metadata value for degree

  static String getDegree(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"thesis","degree","name"));
  }

  // Get and translate the metadata value for degree level

  static Integer getDegreeLevel(Sql sql, Integer id) {
    def degree = getMetadataValue(sql, id, getMetadataFieldId(sql,"thesis","degree","level"))

    if (degree == null) return null
    if (degree.equals("Doctoral")) return 3
    if (degree.equals("Masters")) return 2
  }

  // Get value for department from metadata table

  static String getDepartment(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"thesis","degree","department"));
  }

  // Get value for submission abstract from metadata table

  static String getAbstract(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"dc","description","abstract"));
  }


  // Get type/genre - such as thesis

  static String getType(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"dc","type","genre"));
  }


  // Get submission title

  static String getDocumentTitle(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"dc","title",null));
  }


  // Get creator name

  static String getName(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"dc","creator",null));
  }

  // Get identifier URI

  static String getDepositId(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"dc","identifier","uri"));
  }

  // Get major - degree/discipline

  static String getMajor(Sql sql, Integer id) {
    return getMetadataValue(sql, id, getMetadataFieldId(sql,"thesis","degree","discipline"));
  }


  // Get Keywords

  static String getKeywords(Sql sql, Integer id) {
    def ret = ""


    def fieldId = getMetadataFieldId(sql, "dc","subject",null);


    sql.eachRow("select text_value from metadatavalue where  metadatavalue.item_id = " + id + " and metadata_field_id = "+fieldId){

      row ->
      ret = ret + row.text_value + "; "
    }

    if (ret.length() > 0)
      return ret[0..-2] // remove trailing semicolon
  }


  static Integer getGraduationMonth(Sql sql, Integer id) {

    def field_id = getMetadataFieldId(sql,"dc","date","submitted");

    def row = sql.firstRow("select text_value from metadatavalue where metadatavalue.item_id = " + id + " and metadata_field_id = " + field_id);

    if (row == null) return null;

    def parts = row.text_value.split(" ");
    if ("January".equals(parts[0])) return 0;
    if ("Feburary".equals(parts[0])) return 1;
    if ("March".equals(parts[0])) return 2;
    if ("April".equals(parts[0])) return 3;
    if ("May".equals(parts[0])) return 4;
    if ("June".equals(parts[0])) return 5;
    if ("July".equals(parts[0])) return 6;
    if ("August".equals(parts[0])) return 7;
    if ("September".equals(parts[0])) return 8;
    if ("October".equals(parts[0])) return 9;
    if ("November".equals(parts[0])) return 10;
    if ("December".equals(parts[0])) return 11;

    println("Error: Unknown month, "+parts[0]);
    return null;
  }

  static Integer getGraduationYear(Sql sql, Integer id) {

    def field_id = getMetadataFieldId(sql,"dc","date","submitted");

    def row = sql.firstRow("select text_value from metadatavalue where metadatavalue.item_id = " + id + " and metadata_field_id = " + field_id);

    if (row == null) return null;

    def parts = row.text_value.split(" ");

    return Integer.valueOf(parts[1]);
  }

  // Utility function to return a value from the metadata table

  static String getMetadataValue(Sql sql, Integer id, String mv){

    def rows = sql.rows("select text_value from metadatavalue where metadatavalue.item_id = " + id + " and metadata_field_id = " + mv)

    if (rows[0] != null) {
      return rows[0].text_value
    } else
      return null
  }

  static String getMetadataFieldId(Sql sql, String schema, String element, String qualifier) {

    def params = [schema, element, qualifier];

    if (qualifier == null) params = [schema,element];

    def row = sql.firstRow("select f.metadata_field_id from metadatafieldregistry f, metadataschemaregistry s where f.metadata_schema_id = s.metadata_schema_id AND s.short_id = ? AND f.element = ? AND f.qualifier " + ( qualifier == null ? " IS NULL " : " = ? "), params);

    return row.metadata_field_id;
  }

  static Integer getEmbargoType(Sql sql, String name) {

    def row = sql.firstRow("select id from embargo_type where name = ?",[name]);

    if (row == null)
      return null;
    else
      return row.id;
  }


  // Translate submission status

  static String getSubStatus(Integer stat) {

    def state = [10:'InProgress', 20:'Submitted', 30:'InReview', 40:'NeedsCorrection', 50:'WaitingOnRequirements', 60:'Approved',
          70:'PendingPublication', 80:'Published', 90:'OnHold', 100:'Withdrawn', 110:'Cancelled']

    return state[stat]
  }
}




