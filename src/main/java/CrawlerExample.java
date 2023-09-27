import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

class Disaster {
    String area;
    String text;
    String type;

    public Disaster(String area, String text, String type) {
        this.area = area;
        this.text = text;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Disaster{" +
                "area='" + area + '\'' +
                ", text='" + text + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}

public class CrawlerExample {
    public static void main(String[] args) {
        // ChromeDriver의 경로 설정
        System.setProperty("webdriver.chrome.driver", "C:/Users/Youngjin/chromedriver-win64/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=%EB%84%A4%EC%9D%B4%EB%B2%84+%EC%9E%AC%EB%82%9C%EB%AC%B8%EC%9E%90");

            List<Disaster> disasters = new ArrayList<>();

            WebElement totalElement = driver.findElement(By.cssSelector(".npgs ._total"));
            int page = Integer.parseInt(totalElement.getText());

            for (int p = 0; p < page; p++) {
                // `disaster_list` 클래스의 내용 가져오기
                List<WebElement> disasterListElements = driver.findElements(By.cssSelector(".disaster_list"));
                WebElement disasterListElement = disasterListElements.get(p);
                // disasterListElement 내에서 `area` 클래스의 내용 크롤링
                List<WebElement> areaElements = disasterListElement.findElements(By.cssSelector(".area"));
                List<WebElement> disasterTextElements = disasterListElement.findElements(By.cssSelector(".disaster_text"));
                List<WebElement> typeTextElements = disasterListElement.findElements(By.cssSelector(".disaster_type .text"));

                for (int i = 0; i < areaElements.size(); i++) {
                    String areaText = areaElements.get(i).getText();
                    String disasterText = disasterTextElements.get(i).getText();
                    String typeText = typeTextElements.get(i).getText().replace("\n","");
                    disasters.add(new Disaster(areaText, disasterText, typeText));
                }

                WebElement nextButton = driver.findElement(By.cssSelector(".pg_next"));
                nextButton.click();
                Thread.sleep(3000);
            }

            for (Disaster disaster : disasters)
                System.out.println(disaster);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
