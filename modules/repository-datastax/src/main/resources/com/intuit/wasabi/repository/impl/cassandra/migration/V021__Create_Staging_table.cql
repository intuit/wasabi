create table if not exists staging(
  time timeuuid,
  type text,
  msg text,
  exep text,
  primary key(time)
);
