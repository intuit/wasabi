-- Altering experiment table to include hypothesis check and results fields

alter TABLE experiment ADD hypothesisIsCorrect varchar;
alter TABLE experiment ADD results varchar;