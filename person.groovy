class person_migrator {
	static void main(String[] args) {
		
 		def config = new ConfigSlurper().parse(new File('config.groovy').toURL())
		
		def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
		def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
		
		
		newsql.execute("delete from jpapersonimpl_affiliations")
		newsql.execute("delete from committee_member")
		newsql.execute("delete from attachment")
		newsql.execute("delete from actionlog")
		newsql.execute("delete from submission")
		newsql.execute("delete from person where id != 99999")
		
		sql.eachRow("select eperson_id, email, phone, tdlhomepostaladdress, firstname, lastname, initials, tdledupersonmajor, netid from eperson"){ 
			
			row -> 
			
			String by = getBirthYear(sql, row.eperson_id) 
			if (by == null) by = ''
				
				
				def params = [row.eperson_id, (by == ""?null:Integer.parseInt(by)), getCollege(sql, row.eperson_id), getDegree(sql, row.eperson_id),  getDepartment(sql, row.eperson_id), row.email, row.tdledupersonmajor, getCurrentPhoneNumber(sql, row.eperson_id), getCurrentAddress(sql, row.eperson_id), row.firstname + " " + row.initials + " " + row.lastname, row.email,  row.firstname, row.lastname, row.initials, row.netid, getPermanentEmail(sql, row.eperson_id), getPermanentPhone(sql, row.eperson_id), getPermanentAddress(sql, row.eperson_id), getRole(sql, row.eperson_id)]
			
			newsql.execute 'insert into person (id, birthyear, currentcollege, currentdegree, currentdepartment, currentemailaddress, currentmajor, currentphonenumber, currentpostaladdress, displayname, email, firstname, lastname, middlename, netid, permanentemailaddress, permanentphonenumber, permanentpostaladdress, role) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', params
			
		}
		
	}
	
	
	static String getMetadataValue(Sql sql, Integer id, String mv){
		
		def rows = sql.rows("select text_value from metadatavalue, vireosubmission where vireosubmission.applicant_id = " + id + " and metadatavalue.item_id = vireosubmission.item_id and metadata_field_id = " + mv);
		
		if (rows[0] != null) {
			return rows[0].text_value
		} else
		return null
		
	}
	
	static String getBirthYear(Sql sql, Integer id) {
		def rows = sql.rows("select year_of_birth from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].year_of_birth
		else
			return null
	}
	
	static String getCurrentPhoneNumber(Sql sql, Integer id) {
		def rows = sql.rows("select current_phone from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].current_phone
		else
			return null
	}
	
	
	static String getCurrentAddress(Sql sql, Integer id) {
		def rows = sql.rows("select current_address from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].current_address
		else
			return null
	}
	
	static String getEmailAddress(Sql sql, Integer id) {
		def rows = sql.rows("select email_address from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].email_address
		else
			return null
	}
	
	static String getPermanentEmail(Sql sql, Integer id) {
		def rows = sql.rows("select permanent_email from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].permanent_email
		else
			return null
	}
	
	static String getPermanentPhone(Sql sql, Integer id) {
		def rows = sql.rows("select permanent_phone from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].permanent_phone
		else
			return null
	}
	
	static String getPermanentAddress(Sql sql, Integer id) {
		def rows = sql.rows("select permanent_address from vireosubmission where applicant_id = " + id)
		
		if (rows[0] != null)
			return rows[0].permanent_address
		else
			return null
	}
	static String getCollege(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "74");
	}
	
	static String getDegree(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "72");
	}
	
	static String getDepartment(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "76");
	}
	
	// In old vireo - students have no role
	
	static Integer getRole(Sql sql, Integer id) {
		def rows = sql.rows("select eperson_group_id from epersongroup2eperson where eperson_id = " + new Integer(id).toString() )		
		
		if (rows[0] == null) 
			return 0
		
		if (rows[0].eperson_group_id == 3) {
			println "role: " + rows[0].eperson_group_id
			return 4
		} else {
			println "getRole returning zero"
			return 0
		}
		
	}
	
}

