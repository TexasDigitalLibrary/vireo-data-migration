import groovy.sql.Sql
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class committee_migrator {

  
    // Regex taken from old vireo: org.dspace.app.xmlui.aspect.vireo.model

    static String REGEX_FOR_NAME_TOKEN = "(?:\\p{L}|[-'])+";
    static String REGEX_FOR_INTEGRATED_NAME_PART = "((?:" + REGEX_FOR_NAME_TOKEN + "\\s*)+)";
    static String REGEX_FOR_STANDALONE_NAME_PART = "((?:\\s*" + REGEX_FOR_NAME_TOKEN + "\\s*)+)";
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
		
		newsql.execute("delete from committee_member")
		
		
		// Metadata value 78 is contributor.committeeMember
		// Metadata value 77 is contributor.committeeChair
    int memberField = Integer.valueOf(getMetadataFieldId(sql, "dc", "contributor", "committeeMember"));
    int chairField  = Integer.valueOf(getMetadataFieldId(sql, "dc", "contributor", "committeechair"));	

    int nameExceptions = 0;	
		sql.eachRow("select text_value, place, submission_id, metadata_value_id, metadata_field_id from vireosubmission, metadatavalue where metadatavalue.item_id = vireosubmission.item_id and (metadata_field_id = ${memberField} or metadata_field_id = ${chairField}) order by submission_id, place"){ 
			
			row -> 
			def name = row.text_value


      def lname = null;
      def fname = null;
      def mname = null;

      if (name != null && !",".equals(name)) {
        name = name.trim();

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
            
            if (name_parts.size() != 2)
              throw new RuntimeException("Unable to parce name");

            fname = name_parts[1].trim();
            lname = name_parts[0].trim();
          } catch (RuntimeException re) {
            lname = name;
          }

          nameExceptions++;
          println("["+row.submission_id+"] Unable to parse committee member '"+name+"', using alternative parsing: l='"+lname+"', f='"+fname+"', m='"+mname+"'");
        }
      } else {
        println("["+row.submission_id+"] Has a blank committee member.");
      }

			def params = [
			row.metadata_value_id, (row.metadata_field_id == chairField ? true: false), row.place, fname, lname, mname, row.submission_id
			]	
			
			newsql.execute '''insert into committee_member (
			id,
			chair,			
			displayorder,			
			firstname,			
			lastname,			
			middlename,			
			submission_id
			)
			values (
			?,?,?,?,?,?,?
			)''', params
		}

    println("Committee name exceptions= "+nameExceptions);
                // Update sequence counter

                def row = newsql.firstRow("select (max(id) + 1) max from committee_member")
                newsql.execute("alter sequence seq_committee_member restart with " + row.max)

		
	}

  static String getMetadataFieldId(Sql sql, String schema, String element, String qualifier) {
    
    def params = [schema, element, qualifier];
   
    if (qualifier == null) params = [schema,element];

    def row = sql.firstRow("select f.metadata_field_id from metadatafieldregistry f, metadataschemaregistry s where f.metadata_schema_id = s.metadata_schema_id AND s.short_id = ? AND f.element = ? AND f.qualifier " + ( qualifier == null ? " IS NULL " : " = ? "), params);

    return row.metadata_field_id;
  }

}

