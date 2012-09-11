import groovy.sql.Sql

class person_migrator {
	static void main(String[] args) {
		
 		def config = new ConfigSlurper().parse(new File('config.groovy').toURL())
		
		def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
		def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
		
		// Delete from dependent tables - due to database constraints
		
		newsql.execute("delete from person")
		newsql.execute("delete from jpapersonimpl_affiliations")
		newsql.execute("delete from committee_member")
		newsql.execute("delete from attachment")
		newsql.execute("delete from actionlog")
		newsql.execute("delete from submission")

		// Main driving query - select all epersons from old vireo
		
		sql.eachRow("select eperson_id, email, phone, tdlhomepostaladdress, firstname, lastname, initials, tdledupersonmajor, netid from eperson"){ 
			
			row -> 
			
			// Fix birth year if null
			
			String by = getBirthYear(sql, row.eperson_id) 
			if (by == null) by = ''
				
				
			def params = [row.eperson_id, (by == ""?null:Integer.parseInt(by)), getCollege(sql, row.eperson_id), getDegree(sql, row.eperson_id),  getDepartment(sql, row.eperson_id), row.email, row.tdledupersonmajor, getCurrentPhoneNumber(sql, row.eperson_id), getCurrentAddress(sql, row.eperson_id), row.firstname + " " + row.initials + " " + row.lastname, row.email,  row.firstname, row.lastname, row.initials, row.netid, getPermanentEmail(sql, row.eperson_id), getPermanentPhone(sql, row.eperson_id), getPermanentAddress(sql, row.eperson_id), getRole(sql, row.eperson_id)]
			
			// Insert into new vireo table
			
			newsql.execute 'insert into person (id, birthyear, currentcollege, currentdegree, currentdepartment, currentemailaddress, currentmajor, currentphonenumber, currentpostaladdress, displayname, email, firstname, lastname, middlename, netid, permanentemailaddress, permanentphonenumber, permanentpostaladdress, role) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', params
			
		}

		// Update sequence counter
		
		def row = newsql.firstRow("select max(id) from person")
		newsql.execute("alter sequence seq_person restart with " + row.max + 1)
	}
	
	// Return a value for a metadata field for a given person
	
	static String getMetadataValue(Sql sql, Integer id, String mv){
		
		def rows = sql.rows("select text_value from metadatavalue, vireosubmission where vireosubmission.applicant_id = " + id + " and metadatavalue.item_id = vireosubmission.item_id and metadata_field_id = " + mv);
		
		if (rows[0] != null) {
			return rows[0].text_value
		} else
		return null
		
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


	// Get college setting - metadata value 74

	static String getCollege(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "74");
	}
	

	// Get degree setting - metadata value 72

	static String getDegree(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "72");
	}
	

	// Get current department - metadata 76

	static String getDepartment(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "76");
	}
	
	// In old vireo - students have no role
	
	static Integer getRole(Sql sql, Integer id) {
		def rows = sql.rows("select eperson_group_id from epersongroup2eperson where eperson_id = " + new Integer(id).toString() )		
		
		if (rows[0] == null) 
			return 0

		// in old vireo - 3 is administrator
		
		if (rows[0].eperson_group_id == 3) {
			return 4
		} else {
			return 0
		}
	}
}

