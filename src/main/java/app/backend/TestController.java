package app.backend;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.Month;

@RestController
@RequestMapping("/api")
public class TestController {
    @GetMapping("/data")
    public MyData data() {
        LocalTime currentTime = LocalTime.now();
        LocalDate currentDate = LocalDate.now();

        int hour = currentTime.getHour();
        int minute = currentTime.getMinute();
        int second = currentTime.getSecond();
        
        int day = currentDate.getDayOfMonth();
        Month month_enum = currentDate.getMonth();
        String month_low = (month_enum.toString()).toLowerCase();
        String month = Character.toUpperCase(month_low.charAt(0)) + month_low.substring(1);
        int year = currentDate.getYear();

        String tod = hour >= 12 ? "PM" : "AM";
        hour = hour > 12 ? hour - 12 : hour;
        String minbuff = minute < 10 ? "0"+minute : String.valueOf(minute);
        String secbuff = second < 10 ? "0"+second : String.valueOf(second);

        return new MyData("datetime", hour+":"+minbuff+":"+secbuff+" "+tod+" EST on "+month.toString()+" "+day+", "+year);
    }

    public record MyData(String name, String value) {}
}
