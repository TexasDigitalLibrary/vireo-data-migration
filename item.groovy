import groovy.sql.Sql

class item_migrator {
	static void main(String[] args) {
		
 		def config = new ConfigSlurper().parse(new File('config.groovy').toURL())

		def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
		def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
		
    // check that we have paths defined.
    if (!config.old_asset_path)
      throw new RuntimeException("old_asset_path is not defined, unable to migrate assets.");
    if (!config.new_asset_path)
      throw new RuntimeException("new_asset_path is not defined, unable to migrate assets.");


    // Clear out any old attachments, both on disk and in the database.
		newsql.execute("delete from attachment")
    "rm -rf ${config.new_asset_path}/*".execute().waitFor();



		sql.eachRow("""select mimetype, submission_id, applicant_id, item.last_modified, vireosubmission.item_id, bitstream.bitstream_id, internal_id, name 
		from item, vireosubmission, item2bundle,  bundle2bitstream, bitstream, bitstreamformatregistry
		where vireosubmission.item_id = item.item_id and vireosubmission.item_id = item2bundle. item_id and item2bundle.bundle_id = bundle2bitstream.bundle_id and bitstream.bitstream_id =  bundle2bitstream.bitstream_id and bitstreamformatregistry.bitstream_format_id = bitstream.bitstream_format_id order by submission_id asc """) { 
			
			row -> 
			
			// if row.name - ends with pdf.txt then skip
			
			//public enum AttachmentType {
			// UNKNOWN,
			// PRIMARY,
			// SUPPLEMENTAL,
			// LICENSE,
			// ARCHIVED,
			// FEEDBACK
			
			def file_path = name_to_path(row.internal_id)
			
			// Make a link from the archive into the attachments directory
			// We should probably create a UUID and 'mv' the asset into the attachments directory - with that nam
			
			try {
				def cmd =  "cp ${config.old_asset_path}/" + file_path + "/" + row.internal_id + " ${config.new_asset_path}/" + row.internal_id
				def proc = cmd.execute()
				proc.waitFor()
			} catch (Exception ex) {
				println("Exception " + ex);
			}
			
			// If name is not unique add on a number
			
			def subname = row.name  
			def count = 1
			
			// This has a problem where if there is more than two submissions with the same name
			// they get the suffix of '12' and then '123' - unique - yes - but we should probably 
			// shave off the trailing number before adding the new one on
			
			while(!is_name_unique(newsql, subname, row.submission_id)) {
				println("Fixing Name: " + subname)
				def name_parts = subname.tokenize(".")
				subname = name_parts[0] + count + "." + name_parts[1]
				println "\n\n** New Name: " + subname
				count++
			}
	
			// Need to figure out which is the primary document. Currently default to 1 (all are primary)
			
			def params = [
			row.bitstream_id,                       
			row.internal_id + "|" + row.mimetype,                        
			row.last_modified,                      
			subname,                       
			1,
			row.applicant_id,
			row.submission_id
			]       
			
			
			if (row.name != null)  {
				newsql.execute '''insert into attachment (
				id,                 
				data,                       
				date,                       
				name,                       
				type,                       
				person_id,                  
				submission_id                       
				)
				values (
				?,?,?,?,?,?,?
				)''', params
				
			}
		}
		
                // Update sequence counter

                def row = newsql.firstRow("select (max(id) + 1) max from attachment")
                newsql.execute("alter sequence seq_attachment restart with " + row.max)
 
	}
	
	// Compute the path in the dspace asset directory given the long numeric name
	
        static String  name_to_path(String name) {
        	def p1 = name.substring(0,2)
        	def p2 = name.substring(2,4)
        	def p3 = name.substring(4,6)
        	
        	def path = p1 + "/" + p2 + "/" + p3 
        	return path
        }
        
        // Determine if a given filename is already used within this submission
        
        static Boolean is_name_unique(Sql sql, String name, Integer submission_id) {
        	def params = [submission_id, name]
        	
        	def row = sql.rows("select name from attachment where submission_id = ? and name = ?", params) 
        	
        	if (row.size ==0 ) {
        		return true
        		
        	} else {
        		return false
        	}
        }
}

