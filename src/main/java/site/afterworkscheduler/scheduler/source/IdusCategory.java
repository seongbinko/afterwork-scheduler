package site.afterworkscheduler.scheduler.source;

import lombok.Getter;

@Getter
public enum IdusCategory {
    food("요리",2),
    craft("공예",3),
    art("미술",4),
    flower("플라워",5),
    beauty("뷰티",6);

    private final String krCategory;
    private final int num;

    IdusCategory(String krCategory, int num){
        this.krCategory = krCategory;
        this.num = num;
    }

}

