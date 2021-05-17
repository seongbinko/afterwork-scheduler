package site.afterworkscheduler.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)// protected로 기본생성자 생성
public class Product extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int price;

    private String priceInfo;

    private String author;

    @Column(length = 1000)
    private String imgUrl;

    private boolean isOnline;

    private boolean isOffline;

    private String location;

    private int popularity;

    private String status;

    @Column(nullable = false)
    private String siteName;

    @Column(length = 1000)
    private String siteUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product")
    @Builder.Default
    List<Collect> collects = new ArrayList<>();

    private boolean isRecommendOnline;

    private boolean isRecommendOffline;

    public void updateProduct(int popularity, int price, String priceInfo, String imgUrl, String status, String siteName) {
        this.popularity = popularity;
        this.price = price;
        this.priceInfo = priceInfo;
        this.imgUrl = imgUrl;
        this.status = status;
        this.siteName = siteName;
    }
}
