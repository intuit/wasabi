--
-- Cascading deletes from experiment rows to the other tables.
--

DELIMITER ;;

CREATE TRIGGER on_delete_experiment BEFORE DELETE ON experiment
for each row begin
  DELETE FROM experiment_rollup WHERE experiment_id=OLD.id;
  DELETE FROM event_impression WHERE experiment_id=OLD.id;
  DELETE FROM event_action WHERE experiment_id=OLD.id;
  DELETE FROM bucket WHERE experiment_id=OLD.id;
end ;;

DELIMITER ;
