import groovy.sql.Sql

class committee_migrator {
	static void main(String[] args) {
		
 		def config = new ConfigSlurper().parse(new File('config.groovy').toURL())
		
		def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
		def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
		
		newsql.execute("delete from committee_member")
		
		
		// Metadata value 78 is contributor.committeeMember
		// Metadata value 77 is contributor.committeeChair
		
		sql.eachRow("select text_value, place, submission_id, metadata_value_id, metadata_field_id from vireosubmission, metadatavalue where metadatavalue.item_id = vireosubmission.item_id and (metadata_field_id = 78 or metadata_field_id = 77) order by submission_id, place"){ 
			
			row -> 
			println(row.text_value)
			def name = row.text_value
			def fname, lname
			
			if (name != null) {
				def name_parts = name.tokenize(",")
				println("Lastname : " + name_parts[0])
				println("Firstname : " + name_parts[1])
				fname = name_parts[1]
				lname = name_parts[0]
			} else {
				println("Name: Null")
				fname = ""
				lname = ""
			}
			
			def params = [
			row.metadata_value_id, (row.metadata_field_id == 77 ? true: false), row.place, fname, lname, '', row.submission_id
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
		
	}
}

