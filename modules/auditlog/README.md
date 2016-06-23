#The AuditLog module

The AuditLog module allows to store and retrieve several user interactions with the application:
experiment creations, bucket creations, ..., 

It makes use of the EventLog module to intercept all events and store them. 

The main purpose is to leverage the /logs endpoint.
