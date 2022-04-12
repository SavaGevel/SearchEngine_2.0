package main.repositories;

import main.model.Field;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, Integer> {

    Field getFieldByName(String name);

}
