import groovy.sql.Sql

class log_migrator {
    static void main(String[] args) {
        
        def config = new ConfigSlurper().parse(new File('config.groovy').toURL())
        
        def sql = Sql.newInstance(config.old_db_url,config.old_db_user, config.old_db_pwd)
        def newsql = Sql.newInstance(config.new_db_url,config.new_db_user, config.new_db_pwd)
        
        newsql.execute("delete from actionlog")
        
        // Main driving SQL query

        sql.eachRow("select * from vireolog order by submission_id asc "){ 
            
            row -> 
            
            // Attachment ID ??

            if (submissionExists(newsql, row.submission_id)) {

            def person_id = null;
            if (personExists(newsql,row.eperson_id))
                person_id = row.eperson_id;


                def params = [
                row.log_id, row.date, row.log_entry, (row.private==null?false:row.private), getSubStatus(row.submission_status), null, person_id, row.submission_id
                ]       
                
                newsql.execute '''insert into actionlog (
                id,                     
                actiondate,                     
                entry,                  
                privateflag,                    
                submissionstate,                        
                attachment_id,                  
                person_id,                      
                submission_id                   
                )
                values (
                ?,?,?,?,?,?,?,?
                )''', params
            }
            
        }

        // Update sequence counter

        def count = newsql.firstRow("select (max(id) + 1) max from actionlog")
        newsql.execute("alter sequence seq_actionlog restart with " + count.max)

  // Now put in a log row for each submission - indicating that the submission was migrated from Vireo 
        // Note - use of nextval for the id - since we have already updated the sequence number

        sql.eachRow("select submission_id, status from vireosubmission order by submission_id asc "){
            row->   

              if (submissionExists(newsql, row.submission_id)) {

                newsql.execute('''insert into actionlog(
                                id,
                                actiondate,                     
                                entry,                  
                                privateflag,                    
                                submissionstate,                        
                                attachment_id,                  
                                person_id,                      
                                submission_id) values (
                                        nextval('seq_actionlog'),
                                        localtimestamp,
                                        ?,
                                        false, 
                                        ?, 
                                        null, 
                                        null,
                                        ?)
                                ''', ["Submission imported into Vireo 1.8.", getSubStatus(row.status), row.submission_id])
              }

        }


    }

    // Determine if a submission exists
    
    static Boolean submissionExists(Sql sql, Integer subId) {
        def rows = sql.rows("select id from submission where id = " + subId)
        
        if (rows[0] != null) {
            return true
        } else
        return false
        
    }
    
    // Return status of a submission
    
    static String getSubStatus(Integer stat) {
        
        def state = [10:'InProgress', 20:'Submitted', 30:'InReview', 40:'NeedsCorrection', 50:'WaitingOnRequirements', 60:'Approved', 
        70:'PendingPublication', 80:'Published', 90:'OnHold', 100:'Withdrawn', 110:'Cancelled']
        
        return state[stat]
    }

  static boolean personExists(Sql sql, Integer personId) {
    def rows = sql.rows("select id from person where id = "+personId);

    if (rows[0] != null) return true;
    return false;
  }

}


