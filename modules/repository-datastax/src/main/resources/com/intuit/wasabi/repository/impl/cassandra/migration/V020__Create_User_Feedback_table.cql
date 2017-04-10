CREATE TABLE user_feedback (
  user_id text,
  submitted timestamp,
  score int,
  comments text,
  contact_okay boolean,
  user_email text,

  PRIMARY KEY (user_id, submitted)
);

create index on user_feedback(user_email);
create index on user_feedback(contact_okay);


