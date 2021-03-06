package site.afterworkscheduler.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.afterworkscheduler.entity.Category;
import site.afterworkscheduler.entity.Product;
import site.afterworkscheduler.repository.CategoryRepository;
import site.afterworkscheduler.repository.ProductRepository;
import site.afterworkscheduler.scheduler.source.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class AfterworkScheduler {

    @PersistenceContext
    EntityManager em;

    // KNS, KSB, CJS 만 변경 시 위에 이넘값으로 변경
    static ChromeDriverPath chromeDriverPath = ChromeDriverPath.KNS;

    public static final String WEB_DRIVER_ID = "webdriver.chrome.driver"; // 드라이버 ID
    public static final String WEB_DRIVER_PATH = chromeDriverPath.getPath(); // 드라이버 경로

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TalingMacro talingMacro;

    public static ExecutorService executorService = Executors.newCachedThreadPool();

    @Scheduled(cron = "0 0 4 * * *")
    public void task() throws InterruptedException {
        try {
            System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--headless"); //should be enabled for Jenkins
        options.addArguments("--disable-dev-shm-usage"); //should be enabled for Jenkins
        options.addArguments("--window-size=1920x1080"); //should be enabled for Jenkins

        // 소스 실행전 시간 취득
        long start = System.currentTimeMillis();

        List<Future> futureList = new ArrayList<>();

        futureList.add(crawlClass101(options));

        futureList.add(crawlHobby(options));

        futureList.add(crawlMocha(options));

        futureList.addAll(talingThread(options));

        futureList.add(crawlHobbyInTheBox(options));

        futureList.add(crawlMybiskit(options));

        futureList.addAll(crawlIdus(options));

        boolean isDoingTask = true;
        while(isDoingTask) {
            isDoingTask = false;
            for (Future future : futureList) {
                log.info(String.valueOf(future.isDone()));
                if (!future.isDone()) {
                    isDoingTask = true;
                    break;
                }
            }

            Thread.sleep(1000 * 60);
        }

        log.info("모든 Task 완료");
        executorService.shutdown();

        setRecommendOnline();

        setRecommendOffline();

        bulkDeleteByStatusN();

        long end = System.currentTimeMillis();
        log.info("스케줄러 실행 시간 : " + (end - start) / 1000.0 + "초");
    }

    @Transactional
    public Future crawlMybiskit(ChromeOptions options) {
        Runnable runnable = () -> {
            String siteName = "마이비스킷";
            productRepository.bulkStatusNWithSiteName(siteName);

            WebDriver driver = new ChromeDriver(options);

            String strUrl = "https://www.mybiskit.com/lecture";

            //webDriver를 해당 url로 이동한다.
            driver.get(strUrl);

            //브라우저 이동시 생기는 로드시간을 기다린다.
            //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 무한 스크롤
            InfiniteScroll(driver);

            List<WebElement> elList = driver.findElements(By.className("class_summary"));
            List<Product> updateProducts = new ArrayList<>();

            for (WebElement webElement : elList) {
                String strTitle = null;
                String strAuthor = null;
                int intPrice = 0;
                String strPrice = null;
                String strPriceInfo = "사전예약";
                String strImgUrl = null;
                String strSiteUrl = null;
                String strSiteName = "마이비스킷";
                String strCategory = null;
                String strStatus = "Y";
                int intPopularity = 0;
                boolean isOnline = true;
                boolean isOffline = false;

                strTitle = webElement.findElement(By.className("class_tit")).getText();
                intPopularity = Integer.parseInt(webElement.findElement(By.className("cnt")).getText());

                // 가격이 없는 경우 예외처리
                try {
                    strPrice = webElement.findElement(By.className("real_price")).findElement(By.className("num")).getText();
                    strPriceInfo = strPrice + "원";
                    intPrice = PriceStringToInt(strPrice);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    strPriceInfo += "/월 x " + webElement.findElement(By.className("installment")).findElement(By.className("num")).getText();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                strImgUrl = webElement.findElement(By.className("fixed")).findElement(By.tagName("img")).getAttribute("data-src");

                // 연결 사이트 mybiskit에 맞춰넣음
                strSiteUrl = "https://www.mybiskit.com/lecture/" + strTitle.replace(" ", "-") + "-" + strImgUrl.split("/")[4];
                strCategory = webElement.findElement(By.className("ctag")).getText();

                //카테고리 변환
                if (strCategory.contains("운동")) {
                    strCategory = "운동/건강";
                } else if (strCategory.contains("부업")) {
                    strCategory = "교육";
                } else if (strCategory.contains("자수")
                        || strCategory.contains("비누")
                        || strCategory.contains("원데이")) {
                    strCategory = "공예";
                } else if (strCategory.contains("디지털디자인")
                        || strCategory.contains("캘리")
                        || strCategory.contains("사진")) {
                    strCategory = "아트";
                } else if (strCategory.contains("음악")) {
                    strCategory = "음악";
                } else if (strCategory.contains("요리")) {
                    strCategory = "요리";
                } else {
                    System.out.println("No Category");
                }

                Category category = categoryRepository.findByName(strCategory).orElse(null);

                Product product = productRepository.findByTitleLikeAndCategory(strTitle, category).orElse(null);
                log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$ 마이비스킷 $$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                if (product == null) {
                    product = Product.builder()
                            .title(strTitle)
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

                    productRepository.save(product);
                } else {
                    product.setPopularity(intPopularity);
                    product.setPrice(intPrice);
                    product.setPriceInfo(strPriceInfo);
                    product.setImgUrl(strImgUrl);
                    product.setStatus(strStatus);
                    product.setSiteName(strSiteName);

                    updateProducts.add(product);
                }
            }
            productRepository.saveAll(updateProducts);
            log.info("총 update하는 product size: " + updateProducts.size());

            // 크롤링이 끝났을 경우 driver 종료
            try {
                // 드라이버 연결 종료
                driver.close(); // 드라이버 연결해제
                // 프로세스 종료
                driver.quit();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        };

        return executorService.submit(runnable);

    }

    @Transactional
    public Future crawlHobbyInTheBox(ChromeOptions options) {
        Runnable runnable = () -> {
            // 프러덕트 테이블의 모든 컬럼을 N으로 바꾼다.
            String siteName = "하비인더박스";
            productRepository.bulkStatusNWithSiteName(siteName);

            WebDriver driver = new ChromeDriver(options);

            try {
                System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
            } catch (Exception e) {
                e.printStackTrace();
            }

            HobyintheboxCategory[] enumValues = HobyintheboxCategory.values();
            List<Product> updateProducts = new ArrayList<>();
            for (HobyintheboxCategory enumValue : enumValues) {
                //for (int i = 0; i < 3; i++) {
                int pageNum = 1;
                while (true) {
                    String krCategory = enumValue.getKrCategory();
                    int numCategory = enumValue.getNum();

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

                    for (WebElement webElement : elList) {
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

                        webElement.findElement(By.className("gdARiY")).getText();

                        strTitle = webElement.findElement(By.className("sp-product-item-thumb-origin")).findElement(By.tagName("img")).getAttribute("alt");

                        // 가격이 없는 경우 예외처리
                        try {
                            List<WebElement> desc = webElement.findElement(By.className("sp-product-description")).findElements(By.tagName("div"));

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

                        strImgUrl = webElement.findElement(By.className("sp-product-item-thumb-origin")).findElement(By.tagName("img")).getAttribute("src");

                        // 연결 사이트 mybiskit에 맞춰넣음
                        strSiteUrl = webElement.findElement(By.className("sp-product-item-thumb")).findElement(By.tagName("a")).getAttribute("href");

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
                            log.info("No Category");
                        }
                        log.info("category = " + strCategory);

                        Category category = categoryRepository.findByName(strCategory).orElse(null);

                        Product product = productRepository.findByTitleLikeAndCategory(strTitle, category).orElse(null);
                        log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 하비인더박스 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                        if (product == null) {
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
                            productRepository.save(product);
                        } else {
                            product.setPopularity(intPopularity);
                            product.setPrice(intPrice);
                            product.setPriceInfo(strPriceInfo);
                            product.setImgUrl(strImgUrl);
                            product.setStatus(strStatus);
                            product.setSiteName(strSiteName);
                            updateProducts.add(product);
                        }
                    }
                    pageNum++;
                }
            }
            productRepository.saveAll(updateProducts);
            log.info("총 update하는 product size: " + updateProducts.size());

            // 크롤링이 끝났을 경우 driver 종료
            try {
                // 드라이버 연결 종료
                driver.close(); // 드라이버 연결해제
                // 프로세스 종료
                driver.quit();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        };

        return executorService.submit(runnable);
    }

    @Transactional
    public Future crawlClass101(ChromeOptions options) {
        Runnable runnable = () -> {
            String siteName = "클래스101";
            productRepository.bulkStatusNWithSiteName(siteName);

            WebDriver driver = new ChromeDriver(options);

            Class101Category[] enumValues = Class101Category.values();
            for (Class101Category enumValue : enumValues) {

                String krCategory = enumValue.getKrCategory();
                String categoryCode = enumValue.getCategoryCode();

                String url = "https://class101.net/search?category=" + categoryCode + "&sort=likedOrder&state=sales";

                //webDriver를 해당 url로 이동한다.
                driver.get(url);

                boolean isLastPage = false;
                List<Product> updateProducts = new ArrayList<>();
                while(!isLastPage){
                    //브라우저 이동시 생기는 로드시간을 기다린다.
                    //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다.
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 이미지 로딩을 위한 스크롤 다운
                    InfiniteScrollForClass101(driver);

                    List<WebElement> elList = driver.findElements(By.xpath("//*[@id=\"wrapper\"]/div[1]/main/div/section/section/section[2]/div/div/div[3]/div[1]/ul/li"));
                    elList.addAll(driver.findElements(By.xpath("//*[@id=\"wrapper\"]/div[1]/main/div/section/section/section[2]/div/div/div[3]/div[3]/div/div[1]/ul/li")));

                    List<WebElement> pageElementList = driver.findElements(By.xpath("//*[@id=\"wrapper\"]/div[1]/main/div/section/section/section[2]/div/div/div[3]/section/div/button"));

                    for (WebElement webElement : elList) {
                        String strTitle = null;
                        String strAuthor = null;
                        int intPrice = 0;
                        String strPrice = null;
                        String strPriceInfo = null;
                        String strImgUrl = null;
                        String strSiteUrl = null;
                        String strSiteName = "클래스101";
                        String strCategory = null;
                        String strStatus = "Y";
                        int intPopularity = 0;
                        boolean isOnline = true;
                        boolean isOffline = false;

                        try {
                            //제목
                            strTitle = webElement.findElement(By.xpath("a/div/div[2]/div[2]")).getText();
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                        strAuthor = webElement.findElement(By.xpath("a/div/div[2]/div[1]/div")).getText().split("・")[1];

                        String strPopularity = webElement.findElement(By.xpath("a/div/div[2]/div[3]/div/div/div[1]")).getText();
                        intPopularity = PriceStringToIntForClass101(strPopularity);

                        strSiteUrl = webElement.findElement(By.xpath("a")).getAttribute("href");

                        try {
                            //int가격을 위한 사전작업
                            int length = webElement.findElements(By.xpath("a/div/div")).size();
                            if (length >= 5){
                                strPrice = webElement.findElement(By.xpath("a/div/div[" + (length - 1)+ "]")).getText();
                                strPriceInfo = strPrice;
                                intPrice = PriceStringToIntForClass101(strPrice.split("원")[0]);
                            }

                            //할인가 적용 가격
                            if (strPrice.contains("월")) {
                                strPrice = strPrice.replace(" ", "");
                                strPrice = strPrice.replace("월", "");
                                strPrice = strPrice.replace("개", "");
                                strPrice = strPrice.replace("(", "");
                                strPrice = strPrice.replace(")", "");

                                strPriceInfo = strPrice.split("원")[0] + "원/월 x " + strPrice.split("원")[1];
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            //이미지 경로
                            strImgUrl = webElement.findElement(By.tagName("picture")).findElement(By.tagName("img")).getAttribute("src");
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }

                        //카테고리 변환
                        if (krCategory.contains("운동")) {
                            strCategory = "운동/건강";
                        } else if (krCategory.contains("직무교육")
                                || krCategory.contains("개발")
                                || krCategory.contains("수익")
                                || krCategory.contains("언어")) {
                            strCategory = "교육";
                        } else if (krCategory.contains("공예")
                                || krCategory.contains("글쓰기")
                                || krCategory.contains("드로잉")) {
                            strCategory = "공예";
                        } else if (krCategory.contains("라이프")
                                || krCategory.contains("디자인")
                                || krCategory.contains("사진")) {
                            strCategory = "아트";
                        } else if (krCategory.contains("음악")) {
                            strCategory = "음악";
                        } else if (krCategory.contains("요리")
                                || krCategory.contains("베이킹")) {
                            strCategory = "요리";
                        } else {
                            log.info("No Category");
                        }

                        Category category = categoryRepository.findByName(strCategory).orElse(null);

                        Product product = productRepository.findByTitleLikeAndCategory(strTitle, category).orElse(null);
                        log.info("&&&&&&&&&&&&&&&&&&&&&&&& 클래스101 &&&&&&&&&&&&&&&&&&&&&&&&");
                        if (product == null) {
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

                            productRepository.save(product);
                        } else {
                            product.setPopularity(intPopularity);
                            product.setPrice(intPrice);
                            product.setPriceInfo(strPriceInfo);
                            product.setImgUrl(strImgUrl);
                            product.setStatus(strStatus);
                            product.setSiteName(strSiteName);

                            updateProducts.add(product);
                        }
                    }
                    if (!pageElementList.isEmpty() && pageElementList.get(pageElementList.size() - 1).isEnabled()){
                        pageElementList.get(pageElementList.size() - 1).sendKeys("\n");
                    }
                    else{
                        isLastPage = true;
                    }
                }

                productRepository.saveAll(updateProducts);
                log.info("총 update하는 product size: " + updateProducts.size());
            }

            // 크롤링이 끝났을 경우 driver 종료
            try {
                // 드라이버 연결 종료
                driver.close(); // 드라이버 연결해제
                // 프로세스 종료
                driver.quit();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        };
        return executorService.submit(runnable);
    }

    public List<Future> crawlIdus(ChromeOptions options) {
        List<Future> futureList = new ArrayList<>();
        IdusCategory[] enumValues = IdusCategory.values();
        String siteName = "아이디어스";
        productRepository.bulkStatusNWithSiteName(siteName);

        Runnable runnableOnline = () -> {
            crawlIdusOnline(siteName, options);
        };

        futureList.add(executorService.submit(runnableOnline));

        for (IdusCategory enumValue : enumValues) {
            Runnable runnableOffline = () -> {
                crawlIdusOffline(siteName, options, enumValue);
            };

            futureList.add(executorService.submit(runnableOffline));
        }

        return futureList;
    }

    @Transactional
    public void crawlIdusOnline(String strSiteName, ChromeOptions options) {

        WebDriver driver = new ChromeDriver(options);
        WebDriver driverDetail = new ChromeDriver(options);

        //온라인 클래스!
        String strUrl = "https://www.idus.com/oc";

        //webDriver를 해당 url로 이동한다.
        driver.get(strUrl);

        //브라우저 이동시 생기는 로드시간을 기다린다.
        //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 무한 스크롤
        InfiniteScroll(driver);

        WebElement webElementMain = driver.findElement(By.className("class-list"));

        List<WebElement> webElementList = webElementMain.findElements(By.className("ui_grid__item"));

        List<Product> updateProducts = new ArrayList<>();

        for (WebElement webElement : webElementList) {
            String strTitle = null;
            String strAuthor = null;
            int intPrice = 0;
            String strPrice = null;
            String strPriceInfo = "0원";
            String strImgUrl = null;
            String strSiteUrl = null;
            String strCategory = null;
            String strStatus = "Y";
            int intPopularity = 0;
            String strPopularity = null;
            boolean isOnline = true;
            boolean isOffline = false;

            strTitle = webElement.findElement(By.className("class-name")).getText();

            // 가격이 없는 경우 예외처리
            strPrice = webElement.findElement(By.className("sale")).getText();
            strPriceInfo = strPrice.replace(" ", "");
            intPrice = PriceStringToInt(strPrice);

            strImgUrl = webElement.findElement(By.className("thumb-img")).getAttribute("style").split("\"")[1];

            // 연결 사이트 Idus에 맞춰넣음
            strSiteUrl = webElement.findElement(By.tagName("a")).getAttribute("href");
            strAuthor = webElement.findElement(By.className("label")).getText().split("·")[1];
            strCategory = webElement.findElement(By.className("label")).getText().split("·")[0];

            try {
                driverDetail.get(strSiteUrl);

                strPopularity = driverDetail.findElement(By.className("oc-star-icon-count")).getText();
                intPopularity = PriceStringToInt(strPopularity);
            } catch (Exception e) {
                continue;
            }

            //브라우저 이동시 생기는 로드시간을 기다린다.
            //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 0.1초를 대기한다.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //카테고리 변환
            if (strCategory.contains("유리")
                    || strCategory.contains("플라워")
                    || strCategory.contains("우드")
                    || strCategory.contains("도자")
                    || strCategory.contains("캔들")
                    || strCategory.contains("비누")
                    || strCategory.contains("페이퍼아트")
                    || strCategory.contains("금속")
                    || strCategory.contains("가죽")
                    || strCategory.contains("자개")
                    || strCategory.contains("레진")
                    || strCategory.contains("점토")
                    || strCategory.contains("바느질")
                    || strCategory.contains("뜨개")
                    || strCategory.contains("위빙")) {
                strCategory = "공예";
            } else if (strCategory.contains("디지털")
                    || strCategory.contains("드로잉")
                    || strCategory.contains("유화")
                    || strCategory.contains("수채화")
                    || strCategory.contains("색연필")
                    || strCategory.contains("캘리그래피")
                    || strCategory.contains("실크스크린")
                    || strCategory.contains("뷰티")) {
                strCategory = "아트";
            } else if (strCategory.contains("쿠킹")
                    || strCategory.contains("디저트")) {
                strCategory = "요리";
            } else {
                log.info("No Category");
                continue;
            }

            Category category = categoryRepository.findByName(strCategory).orElse(null);

            Product product = productRepository.findByTitleLikeAndCategory(strTitle, category).orElse(null);
            log.info("!!!!!!!!!!!!!!!!!!!!!!!! 온라인 아이디어스 !!!!!!!!!!!!!!!!!!!!!!!!");

            if (product == null) {
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

                productRepository.save(product);
            } else {
                product.setPopularity(intPopularity);
                product.setPrice(intPrice);
                product.setPriceInfo(strPriceInfo);
                product.setImgUrl(strImgUrl);
                product.setStatus(strStatus);
                product.setSiteName(strSiteName);

                updateProducts.add(product);
            }
        }
        productRepository.saveAll(updateProducts);
        log.info("Idus Online 총 update하는 product size: " + updateProducts.size());

        // 크롤링이 끝났을 경우 driver 종료
        try {
            // 드라이버 연결 종료
            driver.close();
            driverDetail.close(); // 드라이버 연결해제
            // 프로세스 종료
            driver.quit();
            driverDetail.quit();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public void crawlIdusOffline(String strSiteName, ChromeOptions options, IdusCategory idusCategory) {

        WebDriver driver = new ChromeDriver(options);
        WebDriver driverDetail = new ChromeDriver(options);

        List<Product> updateProducts = new ArrayList<>();

        String krCategory = idusCategory.getKrCategory();
        int numCategory = idusCategory.getNum();

        String strUrl = "https://www.idus.com/c/category/" + numCategory;

        //webDriver를 해당 url로 이동한다.
        driver.get(strUrl);

        //브라우저 이동시 생기는 로드시간을 기다린다.
        //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 1초를 대기한다.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 무한 스크롤
        InfiniteScroll(driver);

        List<WebElement> webElementList = driver.findElements(By.className("ui_grid__item"));

        for (WebElement webElement : webElementList) {
            String strTitle = null;
            String strAuthor = null;
            int intPrice = 0;
            String strPrice = null;
            String strPriceInfo = null;
            String strImgUrl = null;
            String strSiteUrl = null;
            String strCategory = krCategory;
            String strStatus = "Y";
            int intPopularity = 0;
            String strPopularity = null;
            String strLocation = null;
            boolean isOnline = false;
            boolean isOffline = true;

            strTitle = webElement.findElement(By.className("ui_card__title")).getText();

            try {
                strPopularity = webElement.findElement(By.className("ui_rating__label")).getText();

                strPopularity = strPopularity.replace("(", "");
                strPopularity = strPopularity.replace(")", "");

                intPopularity = Integer.parseInt(strPopularity);
            } catch (Exception ignore) {

            }

            strLocation = webElement.findElement(By.className("ui_card__overlay--label")).getText();

            strImgUrl = webElement.findElement(By.className("ui_card__imgcover")).findElement(By.tagName("a")).getAttribute("data-lazy-img");

            // 연결 사이트 mybiskit에 맞춰넣음
            strSiteUrl = webElement.findElement(By.className("ui_card__imgcover")).findElement(By.tagName("a")).getAttribute("href");

            driverDetail.get(strSiteUrl);

            //브라우저 이동시 생기는 로드시간을 기다린다.
            //HTTP 응답속도보다 자바의 컴파일 속도가 더 빠르기 때문에 임의적으로 0.1초를 대기한다.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                strPrice = driverDetail.findElement(By.className("price_tag__strong")).getText();
                intPrice = PriceStringToInt(strPrice);
                strPriceInfo = strPrice;

                strAuthor = driverDetail.findElement(By.className("artist_card__label")).getText();
            }
            catch (Exception ignore){

            }

            //카테고리 변환
            if (strCategory.contains("공예")) {
                strCategory = "공예";
            } else if (strCategory.contains("미술")
                    || strCategory.contains("플라워")
                    || strCategory.contains("뷰티")) {
                strCategory = "아트";
            } else if (strCategory.contains("요리")) {
                strCategory = "요리";
            } else {
                log.info("No Category");
                continue;
            }

            Category category = categoryRepository.findByName(strCategory).orElse(null);

            Product product = productRepository.findByTitleLikeAndCategory(strTitle, category).orElse(null);

            log.info("!!!!!!!!!!!!!!!!!!!!!!!! 오프라인 아이디어스 !!!!!!!!!!!!!!!!!!!!!!!!");

            if (product == null) {
                product = Product.builder()
                        .title(strTitle)
                        .author(strAuthor)
                        .popularity(intPopularity)
                        .price(intPrice)
                        .priceInfo(strPriceInfo)
                        .imgUrl(strImgUrl)
                        .isOnline(isOnline)
                        .isOffline(isOffline)
                        .location(strLocation)
                        .siteUrl(strSiteUrl)
                        .siteName(strSiteName)
                        .status(strStatus)
                        .category(category)
                        .build();

                productRepository.save(product);
            } else {
                product.setPopularity(intPopularity);
                product.setPrice(intPrice);
                product.setPriceInfo(strPriceInfo);
                product.setImgUrl(strImgUrl);
                product.setStatus(strStatus);
                product.setSiteName(strSiteName);

                updateProducts.add(product);
            }
        }

        productRepository.saveAll(updateProducts);
        log.info("Idus Offline 총 update하는 product size: " + updateProducts.size());

        // 크롤링이 끝났을 경우 driver 종료
        try {
            // 드라이버 연결 종료
            driver.close();
            driverDetail.close(); // 드라이버 연결해제
            // 프로세스 종료
            driver.quit();
            driverDetail.quit();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    //Hobbyful update
    @Transactional
    public Future crawlHobby(ChromeOptions options){
        Runnable runnable = () -> {

            List<Product> updateProducts = new ArrayList<>();
            String siteName = "하비풀";
            productRepository.bulkStatusNWithSiteName(siteName);
            WebDriver driver = new ChromeDriver(options);
            String[] moveCategoryName = {"/embroidery", "/macrame", "/drawing", "/digital-drawing", "/knitting", "/ratan", "/leather"
                    , "/soap-candle", "/jewelry-neonsign", "/calligraphy", "/kids"};
            int moveCategory = 0;
            while(moveCategory < moveCategoryName.length) {
                String url = "https://hobbyful.co.kr/list/class" + moveCategoryName[moveCategory];
                driver.get(url);
                String category_temp = null;

                if (moveCategory == 0) category_temp = "공예";
                else if (moveCategory == 1) category_temp = "공예";
                else if (moveCategory == 2) category_temp = "아트";
                else if (moveCategory == 3) category_temp = "아트";
                else if (moveCategory == 4) category_temp = "공예";
                else if (moveCategory == 5) category_temp = "공예";
                else if (moveCategory == 6) category_temp = "공예";
                else if (moveCategory == 7) category_temp = "공예";
                else if (moveCategory == 8) category_temp = "공예";
                else if (moveCategory == 9) category_temp = "아트";
                else if (moveCategory == 10) category_temp = "아트";

                final List<WebElement> base = driver.findElements(By.className("class-list"));
                int size = base.size();

                for (int i = 0; i < size; i++) {
                    final List<WebElement> img = driver.findElements(By.className("class-list-thumb"));
                    final List<WebElement> cont = driver.findElements(By.className("class-list-cont"));
                    final List<WebElement> base2 = driver.findElements(By.className("class-list"));
                    String location = null;
                    String imgUrl = img.get(i).findElement(By.tagName("img")).getAttribute("src");
                    String title = cont.get(i).findElement(By.className("class-list-name")).getText();
                    String author = cont.get(i).findElement(By.className("class-list-lecturer-name")).getText();
                    String price_temp = cont.get(i).findElement(By.className("class-list-price")).getText();
                    String price_info = price_temp;
                    if (price_temp.contains("월")) {
                        price_temp = price_temp.replace("월", "");
                        int index = price_temp.indexOf("원");
                        price_temp = price_temp.substring(0, index);
                        price_temp = price_temp.replace(",", "");
                    } else {
                        price_temp = price_temp.replace(" ", "");
                        price_temp = price_temp.replace(",", "");
                        price_temp = price_temp.replace("원", "");
                    }
                    int price = Integer.parseInt(price_temp);

                    if (price_info.contains("개월")) {
                        price_info = price_info.replace("월", "");
                        int monthly_digit = (price_info.indexOf("개")) - 1;
                        price_info = price_info.replace(" ", "");
                        int indexOfWon = price_info.indexOf("원");
                        price_info = price_info.substring(0, indexOfWon + 1);
                        String additionTo = "/월 x ";
                        price_info = price_info + additionTo + monthly_digit;
                    }

                    boolean isOnline = true;
                    boolean isOffline = false;
                    String status = "Y";
                    String siteUrl = base2.get(i).findElement(By.tagName("a")).getAttribute("href");
                    try{
                        siteUrl = URLDecoder.decode(siteUrl, "UTF-8");
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    Category category = categoryRepository.findByName(category_temp).orElse(null);

                    Product product = productRepository.findByTitleLikeAndCategory(title, category).orElse(null);
                    log.info("############################# 하비풀 #########################################");
                    if(product == null){
                        product = Product.builder()
                                .title(title)
                                .author(author)
                                .price(price)
                                .priceInfo(price_info)
                                .imgUrl(imgUrl)
                                .isOnline(isOnline)
                                .isOffline(isOffline)
                                .status(status)
                                .siteName(siteName)
                                .siteUrl(siteUrl)
                                .category(category)
                                .build();
                        productRepository.save(product);
                    }else{
                        product.setTitle(title);
                        product.setAuthor(author);
                        product.setPrice(price);
                        product.setPriceInfo(price_info);
                        product.setImgUrl(imgUrl);
                        product.setOnline(isOnline);
                        product.setOffline(isOffline);
                        product.setSiteUrl(siteUrl);
                        product.setSiteName(siteName);
                        product.setStatus(status);
                        product.setCategory(category);
                        updateProducts.add(product);
                    }
                }
                moveCategory++;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            productRepository.saveAll(updateProducts);
            log.info("총 update하는 product size: "+ updateProducts.size());
            try {
                //드라이버가 null이 아니라면
                if (driver != null) {
                    // 드라이버 연결 종료
                    driver.close(); // 드라이버 연결해제
                    // 프로세스 종료
                    driver.quit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        };
        return executorService.submit(runnable);
    }

    //MochaClass Update
    @Transactional
    public Future crawlMocha(ChromeOptions options){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                List<Product> updateProducts = new ArrayList<>();
                String siteName = "모카클래스";
                productRepository.bulkStatusNWithSiteName(siteName);
                WebDriver driver = new ChromeDriver(options);
                String[] moveCategoryName = {"핸드메이드·수공예", "쿠킹+클래스", "플라워+레슨", "드로잉", "음악", "요가·필라테스", "레져·스포츠", "자기계발", "Live+클래스"};
                int moveCategory = 0;
                String[] moveIsOnlineName = {"false", "true"};
                int moveIsOnline = 0;
                while(moveCategory < moveCategoryName.length) {
                    String category_temp = null;
                    if(moveCategory == 0) category_temp = "공예";
                    else if(moveCategory == 1) category_temp = "요리";
                    else if(moveCategory == 2) category_temp = "아트";
                    else if(moveCategory == 3) category_temp = "아트";
                    else if(moveCategory == 4) category_temp = "음악";
                    else if(moveCategory == 5) category_temp = "운동/건강";
                    else if(moveCategory == 6) category_temp = "운동/건강";
                    else if(moveCategory == 7) category_temp = "교육";
                    else if(moveCategory == 8) category_temp = "교육";

                    String url = "https://mochaclass.com/Search?isOnlineClass="+moveIsOnlineName[moveIsOnline]+"&where=list&category="+moveCategoryName[moveCategory];
                    driver.get(url);

                    while (true) {
                        try{
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        WebElement base = null;
                        try{
                            base = driver.findElement(By.className("MuiGrid-root"));
                        }catch(Exception e){
                            moveIsOnline = 0;
                            break;
                        }
                        final List<WebElement> base2 = base.findElements(By.tagName("a"));
                        final WebElement multiPage_base = driver.findElement(By.className("MuiPagination-ul"));
                        final List<WebElement> multiPage = multiPage_base.findElements(By.tagName("li"));
                        final String nextPage = multiPage.get(multiPage.size() - 1).findElement(By.tagName("button")).getAttribute("class");
                        int size = base2.size();
                        for (int i = 0; i < size; i++) {
                            boolean isOnline = false;
                            boolean isOffline = true;
                            final List<WebElement> desc = base2.get(i).findElements(By.tagName("p"));
                            String imgUrl = base2.get(i).findElement(By.tagName("img")).getAttribute("src");
                            String title = desc.get(1).getText();
                            if(title.contains("온라인")){
                                isOnline = true;
                                isOffline = false;
                            }
                            if(moveIsOnline == 0){
                                isOffline = true;
                                isOnline = false;
                            }else if(moveIsOnline == 1){
                                isOffline = false;
                                isOnline = true;
                            }
                            String location = desc.get(2).getText();
                            String price_temp = desc.get(3).getText();
                            String price_info = price_temp;
                            String author = null;
                            int price = 0;
                            if(price_temp.contains("문의")){
                                price = 0;
                            }else{
                                if(price_temp.contains("%")){
                                    price_info = desc.get(5).getText();
                                    price_temp = desc.get(5).getText();
                                    price_temp = price_temp.replace(",", "");
                                    price_temp = price_temp.replace("원", "");
                                    price = Integer.parseInt(price_temp);
                                } else if(!price_temp.contains("%")){
                                    price_temp = price_temp.replace(",", "");
                                    price_temp = price_temp.replace("원", "");
                                    price = Integer.parseInt(price_temp);
                                }
                            }
                            if(moveCategory == moveCategoryName.length-1 || moveCategory == moveCategoryName.length-2){
                                isOnline = true;
                            }
                            String status = "Y";
                            String siteUrl = base2.get(i).getAttribute("href");
                            Category category = categoryRepository.findByName(category_temp).orElse(null);

                            Product product = productRepository.findByTitleLikeAndCategory(title, category).orElse(null);
                            log.info("************************* 모카클래스 ****************************");
                            if(product == null){
                                product = Product.builder()
                                        .title(title)
                                        .author(author)
                                        .price(price)
                                        .priceInfo(price_info)
                                        .imgUrl(imgUrl)
                                        .isOnline(isOnline)
                                        .isOffline(isOffline)
                                        .location(location)
                                        .status(status)
                                        .siteName(siteName)
                                        .siteUrl(siteUrl)
                                        .category(category)
                                        .build();
                                productRepository.save(product);
                            }else{
                                product.setTitle(title);
                                product.setAuthor(author);
                                product.setPrice(price);
                                product.setPriceInfo(price_info);
                                product.setImgUrl(imgUrl);
                                product.setOnline(isOnline);
                                product.setOffline(isOffline);
                                product.setLocation(location);
                                product.setSiteUrl(siteUrl);
                                product.setSiteName(siteName);
                                product.setStatus(status);
                                product.setCategory(category);
                                updateProducts.add(product);
                            }
                        }
                        if (nextPage.contains("disabled")) {
                            if(moveIsOnline == 0){
                                moveIsOnline = 1;
                            } else if(moveIsOnline == 1){
                                moveIsOnline = 0;
                            }
                            break;
                        } else {
                            multiPage.get(multiPage.size() - 1).click();
                        }
                    }

                    if(moveIsOnline == 0){
                        moveCategory++;
                    }
                }
                productRepository.saveAll(updateProducts);
                log.info("총 update하는 product size: "+ updateProducts.size());
                try {
                    //드라이버가 null이 아니라면
                    if (driver != null) {
                        // 드라이버 연결 종료
                        driver.close(); // 드라이버 연결해제
                        // 프로세스 종료
                        driver.quit();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        };

        return executorService.submit(runnable);
    }

    public List<Future> talingThread(ChromeOptions options){
        List<Future> futureList = new ArrayList<>();
        String siteName = "탈잉";
        //N처리 과정
        productRepository.bulkStatusNWithSiteName(siteName);

        SeleniumListResponse infoList = talingMacro.sorted();
        List<MainRegionSort> mainRegionList = infoList.getMainRegionList();
        List<CategorySort> cateList = infoList.getCateList();

        //리스트의 리스트 담는 새로운 변수
        List<ArrayList> listOflist = new ArrayList<>();

        //스레드 개수
        int threadCnt = 6;
        //나눌 개수
        int divideCnt = 10;
        //나누기
        int totElement = cateList.size();
        for(int i = 0; i < threadCnt; i++){
            ArrayList<CategorySort> temp = new ArrayList<>();
            if((totElement - divideCnt) > 0){
                for(int j = i*divideCnt; j < i*divideCnt+divideCnt; j++){
                    temp.add(cateList.get(j));
                }
                listOflist.add(temp);
                totElement -= divideCnt;
            }else{
                for(int j = i*divideCnt; j < cateList.size(); j++){
                    temp.add(cateList.get(j));
                }
                listOflist.add(temp);
            }
        }

        //스레드 실행
        for(int i = 0; i < listOflist.size(); i++){
            List<CategorySort> realCateList = listOflist.get(i);
            Runnable runnable = () -> crawlTaling(options, mainRegionList, realCateList, siteName);

            futureList.add(executorService.submit(runnable));
        }

        return futureList;
    }

    @Transactional
    public void crawlTaling(ChromeOptions options, List<MainRegionSort> mainRegionList, List<CategorySort> cateList, String siteName){

        WebDriver driver = new ChromeDriver(options);
        //업데이트 담을 리스트
        List<Product> updateProducts = new ArrayList<>();

        //이동 해야할 카테고리 수
        int categoryCnt = 0;
        while(true) {//카테고리 만큼 이동하는 while loop
            //지역 코드 및 지역 Url (지역코{들}) 크롤링
            String url = "https://taling.me/Home/Search/?page=1&cateMain=&cateSub="
                    + cateList.get(categoryCnt).getCategoryNum() + "&region=&orderIdx=&query=&code=&org=&day=&time=&tType=&region=&regionMain=";
            driver.get(url);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WebElement right = driver.findElement(By.className("right")); //지역 클라스 베이스
            List<WebElement> select = right.findElements(By.tagName("select")); //1. 지역명 리스트 2~alpha 는 지역 url

            //지역 코드
            List<WebElement> mainRegionEList = select.get(0).findElements(By.tagName("option")); //지역 코드 element list
            List<Integer> mainRegionCodeList = new ArrayList<>(); //지역 코드 문자열 list
            List<String> mainRegionCodeName = new ArrayList<>();
            for(int i = 1; i < mainRegionEList.size(); i++){ //0번 text가 "지역"이기 때문에 이것을 거르고 나머지 가지고오기
                String mainRegionName = mainRegionEList.get(i).getText();
                mainRegionCodeName.add(mainRegionName);
            }
            for(int i = 0; i < mainRegionCodeName.size(); i++){ //해당 지역명이 어떤 코드인지 확인하고 코드 저장
                for(int j = 0; j < mainRegionList.size(); j++){
                    if(mainRegionCodeName.get(i).equals(mainRegionList.get(j).getMainRegionLabel())){
                        mainRegionCodeList.add(mainRegionList.get(j).getMainRegionNum());
                    }
                }
            }
            //지역 코드 카운드
            int mainRegionCodeListCnt = 0;

            //지역 Url (지역코{들})
            List<String> mainRegionCodesList = new ArrayList<>(); //지역 코드 '모음' 문자열 list (regionUrl)
            for(int i = 1; i < select.size(); i++){
                List<WebElement> mainRegionEList2 = select.get(i).findElements(By.tagName("option"));
                String mainRegionCodes = mainRegionEList2.get(0).getAttribute("value");
                mainRegionCodesList.add(mainRegionCodes);
            }
            //지역 코드(들)의 카운트
            int mainRegionCodesListCnt = 0;

            while (true) {//지역만큼 이동하는 while loop
                //이동해야 할 페이지 수
                int pageCount = 1;

                //저장할 지역 찾기
                StringBuilder sb = new StringBuilder();
                String mainRegion = null;
                for (int j = 0; j < mainRegionList.size(); j++) {
                    int mainRegionCode = mainRegionCodeList.get(mainRegionCodeListCnt);
                    if (mainRegionCode == mainRegionList.get(j).getMainRegionNum()) {
                        mainRegion = mainRegionList.get(j).getMainRegionLabel();
                        break;
                    }
                }

                while (true) {//페이지만 이동하는 while loop
                    url = "https://taling.me/Home/Search/?page=" + pageCount + "&cateMain=&cateSub="
                            + cateList.get(categoryCnt).getCategoryNum() + "&region=&orderIdx=&query=&code=&org=&day=&time=&tType=&region="
                            + mainRegionCodesList.get(mainRegionCodesListCnt) + "&regionMain=" + mainRegionCodeList.get(mainRegionCodeListCnt);
                    driver.get(url);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //상품 찾기
                    WebElement base = driver.findElement(By.className("cont2"));
                    List<WebElement> product_base = base.findElements(By.className("cont2_class"));
                    //상품을 찾지 못했을시 다음 지역으로 이동
                    if(product_base.size() == 0){
                        mainRegionCodeListCnt += 1;
                        mainRegionCodesListCnt += 1;
                        break;
                    }

//상품 상세 정보 크롤링
                    for(int i = 0; i < product_base.size(); i++){
                        //카테고리
                        String category_temp = cateList.get(categoryCnt).getCategoryLabel();
                        //이미지 URL
                        int imgUrlChk = 0;
                        String imgUrl_temp = product_base.get(i).findElement(By.className("img")).getAttribute("style");
                        String found = "";
                        if(imgUrl_temp.contains("s3.")){
                            found = "s3.";
                            imgUrlChk = 1;
                        }else if(imgUrl_temp.contains("img.")){
                            found = "img.";
                            imgUrlChk = 1;
                        }
                        String imgUrl = null;
                        if(imgUrlChk == 0) continue;
                        if(imgUrlChk == 1){
                            int imgUrl_index = imgUrl_temp.indexOf(found);
                            String http = "https://";
                            imgUrl_temp = imgUrl_temp.substring(imgUrl_index, imgUrl_temp.length()-3);
                            imgUrl = http + imgUrl_temp;
                        }
                        //저자
                        String author = product_base.get(i).findElement(By.className("name")).getText();
                        //제목
                        String title = product_base.get(i).findElement(By.className("title")).getText();
                        //지역
                        String location_temp = product_base.get(i).findElement(By.className("location")).getText();
                        String location = null;
                        boolean isOnline = false;
                        boolean isOffline = false;
                        String[] arr = null;
                        sb.append(mainRegion);
                        sb.append(",");
                        if (location_temp.contains("온라인") || location_temp.contains("온/오프라인") || location_temp.contains("Live") || location_temp.contains("live")) {
                            arr = location_temp.split("온라인 Live|온/오프라인|지역없음|지역 없음|,");
                            int cnt = 0;
                            for (int j = 0; j < arr.length; j++) {
                                if (!(arr[j].equals("") || arr[j].equals(" ") || arr[j].equals("  "))) {
                                    if (cnt > 0) {
                                        sb.append(",");
                                    }
                                    sb.append(arr[j]);
                                    cnt++;
                                }
                            }
                            String convertSb = sb.toString();
                            char replace = ',';
                            char sb_last = convertSb.charAt(sb.length()-1);
                            if(sb_last == replace){
                                convertSb = convertSb.substring(0, convertSb.length()-1);
                            }
                            location = convertSb;
                        }else if (location_temp.contains("지역 없음") || location_temp.contains("지억없음")) {
                            arr = location_temp.split("지역없음|지역 없음|,");
                            int cnt = 0;
                            for (int j = 0; j < arr.length; j++) {
                                if (arr[j].equals("") || arr[j].equals(" ") || arr[j].equals("  ")) continue;
                                else {
                                    if (cnt > 0) {
                                        sb.append(",");
                                    }
                                    sb.append(arr[j]);
                                    cnt++;
                                }
                            }
                            if (sb.length() == 3) {
                                location = sb.substring(0, sb.length() - 1);
                            } else {
                                location = sb.toString();
                            }
                        } else {
                            sb.append(location_temp);
                            location = sb.toString();
                        }

                        //온라인 유무
                        if(location.contains("온라인") || title.contains("온라인") || location.contains("녹화영상") || location.contains("튜터전자책")){
                            isOnline = true;
                            String[] check = location.split(",");
                            for(int j = 0; j < check.length; j++){
                                if(!(check[j].equals("온라인") || check[j].equals("녹화영상") || check[j].equals("튜터전자책"))){
                                    isOffline = true;
                                    break;
                                }
                            }
                        }else{
                            isOffline = true;
                        }

                        //가격
                        String price_temp = product_base.get(i).findElement(By.className("price2")).getText();
                        String price_info = price_temp;
                        int price = 0;

                        if (price_temp.contains("시간")) {
                            int dash_pos = 0;
                            price_info = price_info.replace("￦", "");
                            dash_pos = price_info.indexOf("/");
                            price_info = price_info.substring(0, dash_pos);
                            price_info += "원/시간";

                            price_temp = price_temp.replace("￦", "");
                            price_temp = price_temp.replace(",", "");
                            price_temp = price_temp.replace("/시간", "");
                        } else {
                            price_info = price_info.replace("￦", "");
                            price_info += "원";

                            price_temp = price_temp.replace("￦", "");
                            price_temp = price_temp.replace(",", "");
                        }
                        price = Integer.parseInt(price_temp);

                        //인기도
                        String popularity_temp = product_base.get(i).findElement(By.className("d_day")).getText();
                        int popularity = 0;
                        if (popularity_temp.contains("명")) {
                            popularity = Integer.parseInt(popularity_temp.substring(0, popularity_temp.indexOf("명")));
                        } else if (popularity_temp.contains("D")) {
                            try{
                                popularity_temp = product_base.get(i).findElement(By.className("review")).getText();
                                popularity = Integer.parseInt(popularity_temp.substring(1, popularity_temp.length() - 1));
                            }catch(Exception e){
                                popularity = 0;
                            }
                        }
                        //사이트명
                        String siteUrl = product_base.get(i).findElement(By.tagName("a")).getAttribute("href");
                        //상태
                        String status = null;
                        try {
                            WebElement find = product_base.get(i).findElement(By.className("soldoutbox"));
                            status = "N";
                        } catch (Exception e) {
                            status = "Y";
                        }

                        Category category = categoryRepository.findByName(category_temp).orElse(null);
                        Product product = productRepository.findByTitleLikeAndCategory(title, category).orElse(null);
                        log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@ 탈잉 @@@@@@@@@@@@@@@@@@@@@@@@@@");
                        if(product == null){
                            product = Product.builder()
                                    .title(title)
                                    .author(author)
                                    .price(price)
                                    .priceInfo(price_info)
                                    .imgUrl(imgUrl)
                                    .isOnline(isOnline)
                                    .isOffline(isOffline)
                                    .popularity(popularity)
                                    .location(location)
                                    .status(status)
                                    .siteName(siteName)
                                    .siteUrl(siteUrl)
                                    .category(category)
                                    .build();
                            productRepository.save(product);
                        }else{
                            product.setTitle(title);
                            product.setAuthor(author);
                            product.setPrice(price);
                            product.setPriceInfo(price_info);
                            product.setImgUrl(imgUrl);
                            product.setOnline(isOnline);
                            product.setOffline(isOffline);
                            product.setLocation(location);
                            product.setPopularity(popularity);
                            product.setSiteUrl(siteUrl);
                            product.setSiteName(siteName);
                            product.setStatus(status);
                            product.setCategory(category);
                            updateProducts.add(product);
                        }
                        //지역명 초기화
                        sb.setLength(0);
                    }
//상품 상세 정보 크롤링
                    pageCount += 1;
                }//페이지만 이동하는 while loop

                //해당 카테고리에 지역만큼 다 이동했으면 다음 카테고리로 이동
                if(mainRegionCodeListCnt == mainRegionCodeList.size()){
                    categoryCnt += 1;
                    break;
                }

            }//지역만큼 이동하는 while loop

            //TalingMacro에 지정해둔 cateList 만큼 다 이동했을시 종료
            if(categoryCnt == cateList.size()){
                break;
            }

        }//카테고리 만큼 이동하는 while loop

        //모든 While 작업이 끝났으면 한번에 업데이트
        productRepository.saveAll(updateProducts);
        log.info("총 update하는 product size: "+ updateProducts.size());
        try {
            //드라이버가 null이 아니라면
            if (driver != null) {
                // 드라이버 연결 종료
                driver.close(); // 드라이버 연결해제
                // 프로세스 종료
                driver.quit();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public void setRecommendOnline(){

        productRepository.bulkIsRecommendOnlineFalse();

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM Product p WHERE p.isOnline = true and p.status = 'Y' GROUP BY p.title ORDER BY rand()");
        Query query = em.createQuery(String.valueOf(jpql));
        query.setMaxResults(12);

        List<Product> productList = query.getResultList();

        List<Product> updateProducts = new ArrayList<>();
        for (Product product : productList){
            product.setRecommendOnline(true);
            updateProducts.add(product);
        }

        productRepository.saveAll(updateProducts);
    }

    @Transactional
    public void setRecommendOffline(){

        productRepository.bulkIsRecommendOfflineFalse();

        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM Product p WHERE p.isOffline = true and p.status = 'Y' GROUP BY p.title ORDER BY rand()");
        Query query = em.createQuery(String.valueOf(jpql));
        query.setMaxResults(12);

        List<Product> productList = query.getResultList();

        List<Product> updateProducts = new ArrayList<>();
        for (Product product : productList){
            product.setRecommendOffline(true);
            updateProducts.add(product);
        }

        productRepository.saveAll(updateProducts);
    }

    @Transactional
    public void bulkDeleteByStatusN(){
        productRepository.bulkDeleteByStatusN();
    }

    public void InfiniteScroll(WebDriver driver){
        // 현재 켜져있는 drvier 무한스크롤 제일 밑으로 내려가기 위한 코드
        JavascriptExecutor jse = (JavascriptExecutor) driver;

        // 현재 스크롤 높이
        Object last_height = jse.executeScript("return document.body.scrollHeight");
        while (true) {
            jse.executeScript("window.scrollTo(0,document.body.scrollHeight)");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jse.executeScript("window.scrollTo(0,document.body.scrollHeight - 50)");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Object new_height = jse.executeScript("return document.body.scrollHeight");

            log.info("last_height = " + last_height);
            log.info("new_height = " + new_height);

            // 스크롤을 내렸음에도 불구하고 이전과 같다면 제일 밑으로 확인된다.
            if (new_height.toString().equals(last_height.toString())) {
                break;
            }

            // 현재 스크롤 높이
            last_height = new_height;
        }
    }

    public void InfiniteScrollForClass101(WebDriver driver){
        // 현재 켜져있는 drvier 무한스크롤 제일 밑으로 내려가기 위한 코드
        JavascriptExecutor jse = (JavascriptExecutor) driver;

        // 현재 스크롤 높이
        Object last_height = jse.executeScript("return document.body.scrollHeight");
        while (true) {

            jse.executeScript("window.scrollTo({top:document.body.scrollHeight, left:0, behavior:'smooth'})");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            jse.executeScript("window.scrollTo({top:0, left:0, behavior:'smooth'})");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Object new_height = jse.executeScript("return document.body.scrollHeight");

            log.info("last_height = " + last_height);
            log.info("new_height = " + new_height);

            // 스크롤을 내렸음에도 불구하고 이전과 같다면 제일 밑으로 확인된다.
            if (new_height.toString().equals(last_height.toString())) {
                break;
            }

            // 현재 스크롤 높이
            last_height = new_height;
        }
    }

    public int PriceStringToInt(String price){
        price = price.replace(" ", "");
        price = price.replace(",", "");
        price = price.replace("원", "");
        price = price.replace("월", "");
        price = price.replace("/", "");

        return Integer.valueOf(price);
    }

    public int PriceStringToIntForClass101(String price){
        price = price.replace(" ", "");
        price = price.replace(",", "");
        price = price.replace("원", "");
        price = price.replace("월", "");
        price = price.replace("(", "");
        price = price.replace(")", "");
        price = price.replace("개", "");

        return Integer.valueOf(price);
    }
}

