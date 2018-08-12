CREATE TABLE IF NOT EXISTS user_feedback (
  user_id text,
  submitted timestamp,
  score int,
  comments text,
  contact_okay boolean,
  user_email text,

  PRIMARY KEY (user_id, submitted)
);

create index if not exists on user_feedback(user_email);
create index if not exists on user_feedback(contact_okay);


