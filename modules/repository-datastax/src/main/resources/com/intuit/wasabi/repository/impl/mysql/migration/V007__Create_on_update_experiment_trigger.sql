--
-- Auto-versioning of deleted experiments.
--

DELIMITER ;;

CREATE TRIGGER on_update_experiment BEFORE UPDATE ON experiment
for each row begin
  if NEW.state = 'DELETED' and OLD.state != 'DELETED' and OLD.version = 0 then
    set NEW.version = (SELECT MAX(version)+1 FROM experiment
            WHERE app_name=NEW.app_name AND label=NEW.label);
  end if;
end ;;

DELIMITER ;
