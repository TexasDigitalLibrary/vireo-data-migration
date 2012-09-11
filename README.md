# Vireo Data Migration #

Scripts for migrating from Vireo 1.x to the new [Vireo 1.8](https://github.com/TexasDigitalLibrary/Vireo).

Because of the database dependencies, the scripts MUST be run in this order:

1. person 
2. submission
3. item
4. log
5. committee

The scripts assume that a copy of the asset directory from old-Vireo is accessible on the same volume where they will reside in the new attachments directory.
