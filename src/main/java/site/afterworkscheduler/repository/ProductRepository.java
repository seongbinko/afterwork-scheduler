package site.afterworkscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import site.afterworkscheduler.entity.Category;
import site.afterworkscheduler.entity.Product;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByTitleLikeAndCategory(String title, Category category);
    Optional<Product> findByTitleLikeAndCategoryAndLocation(String title, Category category, String location);
    List<Product>findAllBySiteName(String siteName);
}
