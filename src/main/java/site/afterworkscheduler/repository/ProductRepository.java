package site.afterworkscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import site.afterworkscheduler.entity.Category;
import site.afterworkscheduler.entity.Product;

import java.util.List;
import java.util.Optional;

@Transactional
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByTitleLikeAndCategory(String title, Category category);
    List<Product>findAllBySiteName(String siteName);

    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.status = 'N' where p.status = 'Y' and p.siteName = :siteName")
    int bulkStatusNWithSiteName(@Param("siteName") String siteName);

    @Query("update Product p set p.status = 'N' where p.status = 'Y'")
    int bulkStatusN();
}
