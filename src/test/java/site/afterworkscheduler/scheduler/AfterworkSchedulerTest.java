package site.afterworkscheduler.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.afterworkscheduler.entity.Category;
import site.afterworkscheduler.entity.Product;
import site.afterworkscheduler.repository.CategoryRepository;
import site.afterworkscheduler.repository.ProductRepository;
import site.afterworkscheduler.scheduler.source.ChromeDriverPath;
import site.afterworkscheduler.scheduler.source.HobyintheboxCategory;

import java.util.Arrays;
import java.util.List;


@SpringBootTest
class AfterworkSchedulerTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    static ChromeDriverPath chromeDriverPath = ChromeDriverPath.KNS;

    public static final String WEB_DRIVER_ID = "webdriver.chrome.driver"; // 드라이버 ID
    public static final String WEB_DRIVER_PATH = chromeDriverPath.getPath(); // 드라이버 경로

    @Test
    @DisplayName("DB 최신화 테스트_(테스트케이스:하비인더박스)")
    // test가 끝나면 롤백을 한다.
    void start_afterwork_scheduler() {
        // 프러덕트 테이블의 모든 컬럼을 N으로 바꾼다.
        String siteName = "하비인더박스";
        //productRepository.bulkStatusNWithSiteName(siteName);

        try {
            System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        } catch (Exception e){
            e.printStackTrace();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);//

        List<HobyintheboxCategory> enumValues = Arrays.asList(HobyintheboxCategory.values());

        int newProductCount = 0;
        int updateProductCount = 0;

        for (int i = 0; i < 1; i++) {
            int pageNum = 1;
            while (true) {
                String krCategory = enumValues.get(i).getKrCategory();
                int numCategory = enumValues.get(i).getNum();

                String strUrl = "https://hobbyinthebox.co.kr/category/" + krCategory + "/" + numCategory + "/?page=" + pageNum;

                //webDriver를 해당 url로 이동한다.
                driver.get(strUrl);

                //브라우저 이동시 생기는 로드시간을 기다린다.
                //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                List<WebElement> elList = driver.findElements(By.className("sp-product-item"));

                if (elList.isEmpty()) {
                    break;
                }

                for (int j = 0; j < elList.size(); j++) {
                    String strTitle = null;
                    String strAuthor = null;
                    int intPrice = 0;
                    String strPrice = null;
                    String strPriceInfo = null;
                    String strImgUrl = null;
                    String strSiteUrl = null;
                    String strSiteName = siteName;
                    String strCategory = null;
                    String strStatus = "Y";
                    int intPopularity = 0;
                    boolean isOnline = true;
                    boolean isOffline = false;

                    strTitle = elList.get(j).findElement(By.className("sp-product-item-thumb-origin")).findElement(By.tagName("img")).getAttribute("alt");

                    // 가격이 없는 경우 예외처리
                    try {
                        List<WebElement> desc = elList.get(j).findElement(By.className("sp-product-description")).findElements(By.tagName("div"));

                        // 사이즈가 3일경우 즉 할인가격이 없을 경우
                        if (desc.size() == 3) {
                            strPrice = desc.get(0).getText();
                            strPriceInfo = desc.get(0).getText();
                            strAuthor = desc.get(1).getText();
                        }
                        //설명사이즈가 3이상인 경우 즉 할인 가격인 경우
                        else {
                            strPrice = desc.get(1).getText();
                            strPriceInfo = desc.get(1).getText();
                            strAuthor = desc.get(2).getText();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    intPrice = PriceStringToInt(strPrice);

                    strImgUrl = elList.get(j).findElement(By.className("sp-product-item-thumb-origin")).findElement(By.tagName("img")).getAttribute("src");

                    // 연결 사이트 mybiskit에 맞춰넣음
                    strSiteUrl = elList.get(j).findElement(By.className("sp-product-item-thumb")).findElement(By.tagName("a")).getAttribute("href");

                    // 카테고리 변환
                    if (krCategory.contains("운동")) {
                        strCategory = "운동/건강";
                    } else if (krCategory.contains("자수")
                            || krCategory.contains("비누")
                            || krCategory.contains("공예")
                            || krCategory.contains("가드닝")
                            || krCategory.contains("바늘")) {
                        strCategory = "공예";
                    } else if (krCategory.contains("디지털디자인")
                            || krCategory.contains("캘리")
                            || krCategory.contains("사진")
                            || krCategory.contains("아트")
                            || krCategory.contains("라이프")
                            || krCategory.contains("미술")
                            || krCategory.contains("퍼즐")) {
                        strCategory = "아트";
                    } else if (krCategory.contains("밀키트")
                            || krCategory.contains("베이킹")) {
                        strCategory = "요리";
                    } else {
                        //Todo: 테스트 코드에서는 로그를 못찍으니 실제어플리케이션에서는 @sl4j를 활용한 log.info를 찍으십시오
                        System.out.println("No Category");
                    }
                    System.out.println("category = " + strCategory);

                    Category category = categoryRepository.findByName(strCategory).orElse(null);

                    Product product = productRepository.findByTitleLikeAndCategory(strTitle,category).orElse(null);

                    if(product == null) {
                        product = Product.builder()
                                .title(strTitle)
                                .author(strAuthor)
                                .popularity(intPopularity)
                                .price(intPrice)
                                .priceInfo(strPriceInfo)
                                .imgUrl(strImgUrl)
                                .isOnline(isOnline)
                                .isOffline(isOffline)
                                .siteUrl(strSiteUrl)
                                .siteName(strSiteName)
                                .status(strStatus)
                                .category(category)
                                .build();
                        newProductCount ++;
                        productRepository.save(product);
                    } else {
                        product.updateProduct(intPopularity, intPrice, strPriceInfo,strImgUrl, strStatus, strSiteName);
                        updateProductCount ++;
                    }
                }
                pageNum++;
            }
        }
        System.out.println("총 save하는 product size: "+ newProductCount);
        System.out.println("총 update하는 product size: "+ updateProductCount);
    }

    public int PriceStringToInt(String price){
        price = price.replace(" ", "");
        price = price.replace(",", "");
        price = price.replace("원", "");

        return Integer.valueOf(price);
    }
}