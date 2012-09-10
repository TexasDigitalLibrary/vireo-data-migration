import groovy.sql.Sql

class SubmissionMigrator {
	static void main(String[] args) {
				
 		def config = new ConfigSlurper().parse(new File('config.groovy').toURL())
		
		def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
		def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
		
		println("Deleteing")
		
		newsql.execute("truncate attachment cascade")		
		newsql.execute("truncate actionlog cascade")		
		newsql.execute("truncate submission cascade")
		
		println("Done deleting")
		
		sql.eachRow("select * from vireosubmission order by submission_id asc"){ 
			
			row -> 
			println ("title: " + getDocumentTitle(sql, row.submission_id))
			
			def name = getName(sql, row.submission_id)
			def name_parts = null
			def fname, lname
			
			// This should be made more robust
			
			if (name != null) {
				name_parts = name.tokenize(",")
				println("Lastname : " + name_parts[0])
				println("Firstname : " + name_parts[1])
				fname = name_parts[1]
				lname = name_parts[0]
			} else {
				println("Name: Null")
				fname = ""
				lname = ""
			}
			
			// Compute embargotype_id
			
			def et = 1
			if (row.embargo_duration == 12)
				et = 2
			else if (row.embargo_duration == 24)
				et = 3
			else if (row.embargo_name.equals("None"))
				et =  1
			else if (row.embargo_duration == -1)
				et = 4
			
			def params = [row.submission_id, getUmiRelease(row.umi), row.approval_date, row.college, null, row.committee_email_address, 
			row.email_hash, null, getDegree(sql, row.item_id), 
			getDegreeLevel(sql, row.item_id), getDepartment(sql, row.item_id), getDepositId(sql, row.item_id), getAbstract(sql, row.item_id),
			getKeywords(sql, row.item_id), getDocumentTitle(sql, row.item_id), getType(sql, row.item_id), 1, 12, null, null,
			row.license_agreement_date, getMajor(sql, row.item_id), getSubStatus(row.status),
			row.year_of_birth, fname, lname, "", row.submission_date, (row.assigned_to == -1 ?null:row.assigned_to), et, row.applicant_id]
			
			// Fix lastactionlog and lastactionlogentry
			// Fix embargotypeid 
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
			
		}
	}
	
	static boolean getUmiRelease(Integer val) {
		if (val == null || val == 0) 
			return false
		else
			return true 
	}
	
	static String getDegree(Sql sql, Integer id) {
		return getMetadataValue(sql, id, "72");
	}
	
	static Integer getDegreeLevel(Sql sql, Integer id) {
		def degree = getMetadataValue(sql, id, "73")
		
		if (degree == null) return null
			if (degree.equals("Doctoral")) return 3
			if (degree.equals("Masters")) return 2
	}

static String getDepartment(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "76");
}

static String getAbstract(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "27");
}

static String getType(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "79");
}

static String getDocumentTitle(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "64");
}

static String getName(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "9");
}

static String getDepositId(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "25");
}

static String getMajor(Sql sql, Integer id) {
	return getMetadataValue(sql, id, "74");
}


static String getKeywords(Sql sql, Integer id) {
	def ret = ""
	
	sql.eachRow("select text_value from metadatavalue where  metadatavalue.item_id = " + id + " and metadata_field_id = 57"){
		
		row ->
		println(row.text_value)
		ret = ret + row.text_value + ";"
	}       
	
	if (ret.length() > 0)
		return ret[0..-2] // remove trailing semicolon
}

static String getMetadataValue(Sql sql, Integer id, String mv){
	
	def rows = sql.rows("select text_value from metadatavalue where metadatavalue.item_id = " + id + " and metadata_field_id = " + mv)
	
	if (rows[0] != null) {
		return rows[0].text_value
	} else
	return null
}


static String getSubStatus(Integer stat) {
	
	def state = [10:'InProgress', 20:'Submitted', 30:'InReview', 40:'NeedsCorrection', 50:'WaitingOnRequirements', 60:'Approved',
	70:'PendingPublication', 80:'Published', 90:'OnHold', 100:'Withdrawn', 110:'Cancelled']
	
	return state[stat]
}
}


