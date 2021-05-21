package site.afterworkscheduler.scheduler.source;

public enum ChromeDriverPath {
    KNS("chromedriver.exe"),
    CJS("C:\\Users\\Jason\\Downloads\\chromedriver.exe"),
    KSB("/Users/ko/projects/chromedriver"),
    EC2("/usr/local/bin/chromedriver");

    final private String path;

    public String getPath(){
        return path;
    }

    ChromeDriverPath(String path) {
        this.path = path;
    }
}
