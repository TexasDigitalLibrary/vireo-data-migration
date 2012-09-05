import groovy.sql.Sql

class item_migrator {
	static void main(String[] args) {
		
		def sql = Sql.newInstance("jdbc:postgresql://ec2-50-16-34-97.compute-1.amazonaws.com:5432/dspace-ut-etd-stage-1.2", "ut", "PASSWORD")
		def newsql = Sql.newInstance("jdbc:postgresql://localhost:5432/vireo", "postgres", "PASSWORD")
		
		newsql.execute("delete from attachment")
		
		sql.eachRow("""select mimetype, submission_id, applicant_id, item.last_modified, vireosubmission.item_id, bitstream.bitstream_id, internal_id, name 
		from item, vireosubmission, item2bundle,  bundle2bitstream, bitstream, bitstreamformatregistry
		where vireosubmission.item_id = item.item_id and vireosubmission.item_id = item2bundle. item_id and item2bundle.bundle_id = bundle2bitstream.bundle_id and bitstream.bitstream_id =  bundle2bitstream.bitstream_id and bitstreamformatregistry.bitstream_format_id = bitstream.bitstream_format_id order by submission_id asc  """) { 
			
			row -> 
			println("Name: " + row.name)
			println("format: " + row.mimetype)
			
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
				def cmd =  "ln /mnt/ut-etd/" + file_path + "/" + row.internal_id + " /mnt/attachments/" + row.internal_id
				println(cmd);
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
		
		
	}
	
	// Compute the path in the dspace asset directory given the long numeric name
	
        static String  name_to_path(String name) {
        	def p1 = name.substring(0,2)
        	def p2 = name.substring(2,4)
        	def p3 = name.substring(4,6)
        	
        	def path = p1 + "/" + p2 + "/" + p3 
        	println("Name: " + name)
        	println("Path: " + path)
        	return path
        }
        
        // Determine if a given filename is already used within this submission
        
        static Boolean is_name_unique(Sql sql, String name, Integer submission_id) {
        	def params = [submission_id, name]
        	
        	println("is unique " + name + " id " + submission_id);
        	
        	def row = sql.rows("select name from attachment where submission_id = ? and name = ?", params) 
        	
        	println("row size " + row.size)
        	
        	if (row.size ==0 ) {
        		println("size " + row.size + " returning true ")
        		return true
        		
        	} else {
        		println("size " + row.size + " returning  false")
        		return false
        	}
        }
}
