import groovy.sql.Sql

class SubmissionMigrator {
    static void main(String[] args) {
        
        def sql = Sql.newInstance("connect-string", "user", "password")
        def newsql = Sql.newInstance("connect-string", "user", "password")
        
        newsql.execute("delete from actionlog")
        newsql.execute("delete from submission")
        
        sql.eachRow("select * from vireosubmission order by submission_id"){ 
            
            row -> 
            println("----")
            println ("title: " + getMetadataValue(sql, row.submission_id, "64"))
            
            println("UMI: " + getUmiRelease(row.umi));
            println("Degree: " + getDegree(sql, row.submission_id))
            println("Degree Level: " + getDegreeLevel(sql, row.submission_id))
            println("Department: " + getDepartment(sql, row.submission_id))
            println("Abstract: " + getAbstract(sql, row.submission_id))
            println("Keywords: " + getKeywords(sql, row.submission_id))
            println("Title: " + getDocumentTitle(sql, row.submission_id))
            println ("type : " + getType(sql, row.submission_id))
            println ("major : " + getMajor(sql, row.item_id, row.applicant_id))
            println ("name : " + getName(sql, row.submission_id))
            
            def name = getName(sql, row.submission_id)
            def name_parts = null
            def fname, lname
            
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
            
            def params = [row.submission_id, getUmiRelease(row.umi), row.approval_date, row.college, null, row.committee_email_address, 
            "Committee Dispposition", row.email_hash, null, getDegree(sql, row.item_id), 
            getDegreeLevel(sql, row.item_id), getDepartment(sql, row.item_id), "Deposit Id", getAbstract(sql, row.item_id),
            getKeywords(sql, row.item_id), getDocumentTitle(sql, row.item_id), getType(sql, row.item_id), 1, 12, null, null,
            row.license_agreement_date, getMajor(sql, row.item_id, row.applicant_id), getSubStatus(row.status),
            row.year_of_birth, fname, lname, "", row.submission_date, (row.assigned_to == -1 ?null:row.assigned_to), 226, row.applicant_id]
            
            // Fix lastactionlog and lastactionlogentry
            
            newsql.execute '''insert into submission (
            id,                     
            umirelease,                     
            approvaldate,                   
            college,                        
            committeeapprovaldate,                  
            committeecontactemail,                  
            committeedisposition,                   
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
            ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?
            )''', params
            
        }
    }
    
    static boolean getUmiRelease(Integer val) {
        if (val == null || val == 0) 
            return false
        else
            return true 
    }
    
    static String getDocumentFilename(Sql sql, int id) {
        
        println "item_id " + id
        
        def rows = sql.rows("select bitstream.name from item2bundle, bundle, bitstream where item2bundle.item_id = " + id + " and bundle.bundle_id = item2bundle.bundle_id and bundle.primary_bitstream_id = bitstream.bitstream_id")
        
        if (rows[0] != null) {
            return rows[0].name
        } else
        return null
        
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

static String getMajor(Sql sql, Integer id, Integer applicant_id) {
    def rows = sql.rows("select tdledupersonmajor from eperson where eperson.eperson_id= " + applicant_id)
    
    if (rows[0] != null) {
        return rows[0].tdledupersonmajor
    } else
    return null
}


def getFirstName(Sql sql, Integer id, Integer applicant_id) {
    def rows = sql.rows("select firstname, lastname, initials from eperson where eperson.eperson_id= " + applicant_id)
    
    
    
    
}

static String getSubStatus(Integer stat) {
    
    def state = [10:'InProgress', 20:'Submitted', 30:'InReview', 40:'NeedsCorrection', 50:'WaitingOnRequirements', 60:'Approved',
    70:'PendingPublication', 80:'Published', 90:'OnHold', 100:'Withdrawn', 110:'Cancelled']
    
    return state[stat]
}
}

