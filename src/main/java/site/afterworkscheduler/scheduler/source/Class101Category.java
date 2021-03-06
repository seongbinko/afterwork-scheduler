package site.afterworkscheduler.scheduler.source;

import lombok.Getter;

@Getter
public enum Class101Category {
//    health("운동/건강","5ea2b372fdaf6841ea893f6a"),
//    life("라이프스타일","5c7821a235b52893ce37e3b0"),
//    food("요리/음료","5c5d5bfc74eabcfdafc3a2da"),
//    art("미술", "5c5d5bfc74eabcfdafc3a2e7"),
//    carrer("커리어/기타","5f59ca5715e71a1fa2e797f2"),
//    craft("공예", "5c7821a235b52893ce37e39a"),
//    digitalDrawing("디지털드로잉", "5c7821a235b52893ce37e39c"),
//    picture("사진/영상","5c5d5bfc74eabcfdafc3a2e2"),
//    music("음악","5c5d5bfc74eabcfdafc3a2de"),
//    flanguage("어학/외국어","5ee0c56d0909112e12d82809"),
//    startup("창업","5f59ca5715e71a1fa2e797ed"),
//    business("비즈니스/생산성","5f59ca5715e71a1fa2e797ee"),
//    sns("SNS/콘텐츠","5f59ca5715e71a1fa2e797eb"),
//    mind("마인드/자기계발", "5f59ca5715e71a1fa2e797e9"),
//    onlineshopingmall("온라인쇼핑몰","5f59ca5715e71a1fa2e797ec"),
//    stock("부동산/주식/재테크","5f59ca5715e71a1fa2e797ea"),
//    writing("글쓰기/콘텐츠","5f59ca5715e71a1fa2e797f1"),
//    video("영상/디자인","5f59ca5715e71a1fa2e797ef"),
//    data("데이터/개발","5f59ca5715e71a1fa2e797f0");

    craft("공예", "604f1c9756c3676f1ed00317"),
    drawing("드로잉","604f1c9756c3676f1ed00304"),
    food("요리/음료","604f1c9756c3676f1ed0034f"),
    baking("베이킹","604f1c9756c3676f1ed0035e"),
    music("음악","604f1c9756c3676f1ed00365"),
    health("운동","604f1c9756c3676f1ed00373"),
    life("라이프","604f1c9756c3676f1ed0037e"),
    picture("사진/영상","604f1c9756c3676f1ed00389"),
    design("디자인","604f1c9756c3676f1ed0038e"),
    develop("개발","604f1c9756c3676f1ed00397"),
    education("직무교육","604f1c9756c3676f1ed003a2"),
    stock("수익창출","604f1c9756c3676f1ed003b2"),
    writing("글쓰기","604f1c9756c3676f1ed003c4"),
    flanguage("언어","604f1c9756c3676f1ed003cd");


    private final String krCategory;
    private final String categoryCode;

    Class101Category(String krCategory, String categoryCode){
        this.krCategory = krCategory;
        this.categoryCode = categoryCode;
    }
}
