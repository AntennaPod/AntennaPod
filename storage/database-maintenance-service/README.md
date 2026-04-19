# :storage:database-maintenance-service

Periodic tasks to clean up the database, such as clearing old download logs.
Should never be directly triggered by users.
Scheduled via WorkManager; tasks run in the background automatically.

