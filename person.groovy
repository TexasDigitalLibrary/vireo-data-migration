import groovy.sql.Sql
class person_migrator {
    static void main(String[] args) {
        
        def sql = Sql.newInstance("connect-string", "user", "password")
        def newsql = Sql.newInstance("connect-string", "user", "password")

        
        newsql.execute("delete from submission")
        newsql.execute("delete from person where id != 99999")
        
        sql.eachRow("select eperson_id, email, phone, tdlhomepostaladdress, firstname, lastname, initials, tdledupersonmajor, netid from eperson"){ 
            
            row -> 
            
            String by = getBirthYear(sql, row.eperson_id) 
            if (by == null) by = ''
                
                /*
            println row.eperson_id+ " " +  " " + by + " "  + 
            getCollege(sql, row.eperson_id) + " " +
            getDegree(sql, row.eperson_id) + " " +
            getDepartment(sql, row.eperson_id) + " " +
            row.email + " " + 
            row.tdledupersonmajor+ " " +
            getCurrentPhoneNumber(sql, row.eperson_id) + " " +
            getCurrentAddress(sql, row.eperson_id) + " " +
            row.firstname + " " + row.initials + " " + row.lastname + " " 
            row.netid+ " " +
            getPermanentEmail(sql, row.eperson_id) + " " +
            getPermanentPhone(sql, row.eperson_id) + " " +
            getPermanentAddress(sql, row.eperson_id) + " " + "1"
            
            
            println("*** BY: " + by)
            */
            
            def params = [row.eperson_id, (by == ""?null:Integer.parseInt(by)), getCollege(sql, row.eperson_id), getDegree(sql, row.eperson_id),  getDepartment(sql, row.eperson_id), row.email, row.tdledupersonmajor, getCurrentPhoneNumber(sql, row.eperson_id), getCurrentAddress(sql, row.eperson_id), row.firstname + " " + row.initials + " " + row.lastname, row.email,  row.firstname, row.lastname, row.initials, row.netid, getPermanentEmail(sql, row.eperson_id), getPermanentPhone(sql, row.eperson_id), getPermanentAddress(sql, row.eperson_id), 1]
            
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
    
    
}

