import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ApiExplorer {
    static class ApiResponse {
        public List<Message> DisasterMsg;

        static class Message {
            public List<Row> row;
        }

        static class Row {
            public String create_date;
            public String location_id;
            public Long md101_sn;
            public String msg;
            public List<String> locations;
            public String category;

            @Override
            public String toString() {
                return "{\n" +
                        "재난문자 ID\n\t" + md101_sn + "\n" +
                        "발생 시각\n\t" + create_date + "\n" +
                        "위치\n\t" + locations + "\n" +
                        "카테고리\n\t" + category + "\n" +
                        "내용\n\t" + msg + "\n" +
                        "}\n";
            }
        }
    }

    static WebDriver driver;

    static String getJsonFromUrl(String urlString) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    static List<String> getLocations(String location_id) {
        List<String> codes = new ArrayList<>();
        List<String> ret = new ArrayList<>();
        List<String> ids= Arrays.stream(location_id.split(",")).collect(Collectors.toList());
        try (CSVParser parser = new CSVParser(new FileReader("location.csv"), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                for(String id:ids){
                    if (record.get(0).equals(id)) {
                        codes.add(record.get(4));//법정동코드 가져오기
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            //법정동코드를 실제주소로 변환

            for(String code:codes) {
                String urlString = "https://grpc-proxy-server-mkvo6j4wsq-du.a.run.app/v1/regcodes?regcode_pattern="+code;
                String jsonString = getJsonFromUrl(urlString);

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode rootNode = objectMapper.readTree(jsonString);
                    JsonNode regcodesNode = rootNode.get("regcodes");

                    for (JsonNode regcode : regcodesNode) {
                        String name = regcode.get("name").textValue();
                        ret.add(name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return ret;
        }
    }

    static String getCategory(String message) {
        try{
            driver.get("https://www.safekorea.go.kr/idsiSFK/neo/sfk/cs/sfc/dis/disasterMsgList.jsp?menuSeq=679");
            WebElement searchElem=driver.findElement(By.cssSelector(".search03_input"));
            searchElem.sendKeys(message.split("\n")[0]);

            driver.findElement(By.cssSelector("#datepicker1")).click();
            driver.findElement(By.cssSelector(".ui-state-default[href='#']")).click();
            driver.findElement(By.cssSelector(".search_btn")).click();
            Thread.sleep(1000);
            List<WebElement> categoryElem=driver.findElements(By.cssSelector("#disasterSms_tr_0_DSSTR_SE_NM"));
            if(categoryElem.isEmpty())
                return "오래된 데이터라 알 수 없음";
            else
                return categoryElem.get(0).getText();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("webdriver.chrome.driver", "C:/Users/Youngjin/chromedriver-win64/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);


        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1741000/DisasterMsg3/getDisasterMsg1List"); /*URL*/
        urlBuilder.append("?serviceKey=f2xSRtrfY%2FED5KxxxOCc10yA7pqyOZ0j1e6yBHTcPgeXaID%2BMycImbjVevXRo0VK3DmvsKd%2BPStbS1Yh0mMWfg%3D%3D"); /*Service Key*/
        urlBuilder.append("&pageNo=1"); /*페이지번호*/
        urlBuilder.append("&numOfRows=500"); /*한 페이지 결과 수*/
        urlBuilder.append("&type=json"); /*호출문서 형식*/
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        //System.out.println("Response code: " + conn.getResponseCode());
        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ApiResponse apiResponse = objectMapper.readValue(sb.toString(), ApiResponse.class);
        for (ApiResponse.Row disaster : apiResponse.DisasterMsg.get(1).row) {
            disaster.locations = getLocations(disaster.location_id);
            disaster.category = getCategory(disaster.msg);
            if(disaster.category.equals("기타")||disaster.category.equals("교통통제")||disaster.category.equals("교통사고"))
                continue;
            System.out.println(disaster);
        }
    }
}