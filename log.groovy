import groovy.sql.Sql

class log_migrator {
  static void main(String[] args) {

    def sql = Sql.newInstance("jdbc:postgresql://ec2-50-16-34-97.compute-1.amazonaws.com:5432/dspace-ut-etd-stage-1.2", "ut", "VXP6gpevbY")
    def newsql = Sql.newInstance("jdbc:postgresql://localhost:5432/vireo", "postgres", "tdl1402")

    newsql.execute("delete from actionlog")

    sql.eachRow("select * from vireolog order by submission_id asc "){ 

    row -> 
	println(row.log_id)

        if (submissionExists(sql, row.submission_id)) {

	def params = [
            row.log_id, row.date, row.log_entry, (row.private==null?false:row.private), getSubStatus(row.submission_status), null, row.eperson_id, row.submission_id
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
  }

 static Boolean submissionExists(Sql sql, Integer subId) {
     def rows = sql.rows("select submission_id from vireosubmission where submission_id = " + subId)

     if (rows[0] != null) {
          return true
      } else
          return false
 
  }

static String getSubStatus(Integer stat) {

    def state = [10:'InProgress', 20:'Submitted', 30:'InReview', 40:'NeedsCorrection', 50:'WaitingOnRequirements', 60:'Approved', 
	    70:'PendingPublication', 80:'Published', 90:'OnHold', 100:'Withdrawn', 110:'Cancelled']

    println("getSubStatus: " + state[stat])
    return state[stat]
}
}

