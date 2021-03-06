import groovy.sql.Sql
import java.security.MessageDigest

class person_migrator {
  static void main(String[] args) {
    
 		def config = new ConfigSlurper().parse(new File('config.groovy').toURL())
		
    def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
    def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
		
		// Delete from dependent tables - due to database constraints
    newsql.execute("truncate attachment cascade")
    newsql.execute("truncate actionlog cascade")
    newsql.execute("truncate committee_member cascade")
    newsql.execute("truncate submission cascade")
    newsql.execute("truncate person cascade")

    
		// Main driving query - select all epersons from old vireo
		sql.eachRow("select eperson_id, email, phone, tdlhomepostaladdress, firstname, lastname, initials, tdledupersonmajor, netid from eperson"){ 
			
			row -> 
			
      // Fix birth year if null
			
			String by = getBirthYear(sql, row.eperson_id) 
			if (by == null) by = ''
			
      
      def netid = row.netid;
      if (netid != null) {
        if (config.unscope_netid)
          netid = netid.substring(0,netid.indexOf("@"));

        if (config.uppercase_netid)
          netid = netid.toUpperCase();

        if (config.lowercase_netid)
          netid = netid.toLowerCase();
      }

				
			def params = [row.eperson_id, (by == ""?null:Integer.parseInt(by)), getCollege(sql, row.eperson_id), getDegree(sql, row.eperson_id),  getDepartment(sql, row.eperson_id), row.email, row.tdledupersonmajor, getCurrentPhoneNumber(sql, row.eperson_id), getCurrentAddress(sql, row.eperson_id), null, row.email,  row.firstname, row.lastname, row.initials, netid, getPermanentEmail(sql, row.eperson_id), getPermanentPhone(sql, row.eperson_id), getPermanentAddress(sql, row.eperson_id), getRole(sql, row.eperson_id)]
			
			// Insert into new vireo table
			
			newsql.execute 'insert into person (id, birthyear, currentcollege, currentdegree, currentdepartment, currentemailaddress, currentmajor, currentphonenumber, currentpostaladdress, displayname, email, firstname, lastname, middlename, netid, permanentemailaddress, permanentphonenumber, permanentpostaladdress, role) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', params
			
		}

    // Create a migration admin
    if (config.create_admin) {
      def id = newsql.firstRow("select max(id) from person").max
      if (id == null)
        id = 1;
      else
        id++;

      String email = config.admin_email;
      String first = config.admin_first;
      String last = config.admin_last;
      String displayName = first + " " + last;
      String netid = config.admin_netid;
      String passwordHash = getPasswordHash(id,config.admin_password);
      int role = 4;

      def params = [id, email, netid, passwordHash, first, last, displayName, role]
      newsql.execute 'insert into person (id, email, netid, passwordhash, firstname, lastname, displayname, role) values(?, ?, ?, ?, ?, ?, ?, ?)', params
    } 

		// Update sequence counter
		def row = newsql.firstRow("select (max(id) + 1) max from person")
		newsql.execute("alter sequence seq_person restart with " + row.max)
	}
	
	// Return a value for a metadata field for a given person
	
	static String getMetadataValue(Sql sql, Integer id, String mv){
		
		def rows = sql.rows("select text_value from metadatavalue, vireosubmission where vireosubmission.applicant_id = " + id + " and metadatavalue.item_id = vireosubmission.item_id and metadata_field_id = " + mv);
		
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
	
	// Get birth year
	
	static String getBirthYear(Sql sql, Integer id) {
		def rows = sql.rows("select year_of_birth from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].year_of_birth
		else
			return null
	}

	// Get current phone number 
	
	static String getCurrentPhoneNumber(Sql sql, Integer id) {
		def rows = sql.rows("select current_phone from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].current_phone
		else
			return null
	}
	
	// Get current address
	
	static String getCurrentAddress(Sql sql, Integer id) {
		def rows = sql.rows("select current_address from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].current_address
		else
			return null
	}
	
	// Get current email address

	static String getEmailAddress(Sql sql, Integer id) {
		def rows = sql.rows("select email_address from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].email_address
		else
			return null
	}

	// Get permanent email address
	
	static String getPermanentEmail(Sql sql, Integer id) {
		def rows = sql.rows("select permanent_email from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].permanent_email
		else
			return null
	}

	// Get permanent phone
	
	static String getPermanentPhone(Sql sql, Integer id) {
		def rows = sql.rows("select permanent_phone from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].permanent_phone
		else
			return null
	}

	// Get permanent  address
	
	static String getPermanentAddress(Sql sql, Integer id) {
		def rows = sql.rows("select permanent_address from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].permanent_address
		else
			return null
	}


	// Get college setting   
	static String getCollege(Sql sql, Integer id) {

    def rows = sql.rows("select college from vireosubmission where applicant_id = " + id);

    if (rows[0] != null)
      return rows[0].college
    else
      return null
	}
	

	// Get thesis.degree.discipline
	static String getDegree(Sql sql, Integer id) {
		return getMetadataValue(sql, id, getMetadataFieldId(sql, "thesis","degree","discipline"));
	}
	

	// Get thesis.degree.department
	static String getDepartment(Sql sql, Integer id) {
		return getMetadataValue(sql, id, getMetadataFieldId(sql, "thesis","degree","department"));
	}
	
	// In old vireo - students have no role
	
	static Integer getRole(Sql sql, Integer id) {
		def rows = sql.rows("select eperson_group_id from epersongroup2eperson where eperson_id = " + new Integer(id).toString() )		
		
		if (rows[0] == null) 
			return 1

		// in old vireo - 3 is administrator
		
		if (rows[0].eperson_group_id == 3) {
			return 4
		} else {
			return 1
		}
	}


  static String getPasswordHash(Long id, String raw) {
  
      // Get the password salt
      byte[] salt = new byte[24];
      Random random = new Random(id);
      random.nextBytes(salt);
      
      
      // Generate the hash using the pre-defined algorithm.
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt);
      byte[] byteHash = md.digest(raw.getBytes());

      // Convert the hash to hex values for easy storage and portability.
      StringBuffer hash = new StringBuffer();
      for (int i = 0; i < byteHash.length; i++) {
        hash.append(Integer.toString((byteHash[i] & 0xff) + 0x100, 16).substring(1));
      }
      
      // Retun the hash.
      return hash.toString();
  }
}

