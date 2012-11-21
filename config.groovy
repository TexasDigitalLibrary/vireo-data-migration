

// Old dspace vireo installation information
old_db_url = "jdbc:postgresql://hostname:port/database_name"
old_db_user = "user"
old_db_pwd = "pass"
old_asset_path = "/mnt/old_vireo/assetstore/" 

// Old vireo may contain multiple instances in one vireo install. You need to
// identify which of those instances to migrate out. The first, entity_id,
// paramater is required in order to export the required settings. The later
// identifies which dspace collection that instance is associated with. You can
// find the collection_id by looking at xmlui.vireo.collection.[entity_id] or
// xmlui.vireo.default.collection in dspace.cfg.
old_entity_id = "school.edu"
old_collection_id = 1

// New vireo installation information
new_db_url = "jdbc:postgresql://hostname:port/database_name"
new_db_user = "user"
new_db_pwd = "pass"
new_asset_path = "/mnt/new_viero/attachments/"

// How should files be migrated: copy ("cp"), move ("mv"), or symlinked ("ln").
asset_command = "cp" // possible values: cp, mv, ln

// Should the netid field be translated during migration?
unscope_netid = false // Remove everything after the "@" sign.
uppercase_netid = false  // Force everything to be upper case.
lowercase_netid = false  // Force everything to be lower case.


// If you would like an admin account created then turn these settings on.
create_admin = true
admin_email = "migration@tdl.org"
admin_first = "Migration"
admin_last = "Admin"
admin_netid = null 
admin_password = "changeme"

