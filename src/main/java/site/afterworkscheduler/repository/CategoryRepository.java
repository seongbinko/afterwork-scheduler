package site.afterworkscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import site.afterworkscheduler.entity.Category;

import java.util.Optional;

@Transactional(readOnly = true)
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
}
