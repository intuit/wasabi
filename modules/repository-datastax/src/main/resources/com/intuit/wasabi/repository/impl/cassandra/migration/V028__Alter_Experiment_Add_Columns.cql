-- Altering experiment table to include hypothesis check and results fields

alter TABLE experiment ADD hypothesis_is_correct varchar;
alter TABLE experiment ADD results varchar;
alter TABLE experiment ADD experiment_type text;
alter TABLE experiment ADD source_url text;