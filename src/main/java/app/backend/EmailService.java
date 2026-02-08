package app.backend;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.Month;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import app.backend.IpLocationService;

@Service
public class EmailService {
    int day;
    String month;
    int year;
    String logArchivePath = "hidden_key/archives";

    // 5:30 AM every day (server local time)
    @Scheduled(cron = "0 21 18 * * *")
    public void sendDailyEmail() {
        final String username = "hidden_key";
        final String appPassword = "hidden_key"; // Gmail app password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse("hidden_key")
            );
            message.setFrom(new InternetAddress(username));
            message.setSubject("Automated: BlackburnEngineering Daily Site Logs");

            // Body part - plain text
            MimeBodyPart textPart = new MimeBodyPart();

            LocalDate currentDate = LocalDate.now();
            
            //day = currentDate.getDayOfMonth() - 1;
            day = 14;
            Month month_enum = currentDate.getMonth();
            String month_low = (month_enum.toString()).toLowerCase();
            month = Character.toUpperCase(month_low.charAt(0)) + month_low.substring(1);
            year = currentDate.getYear();

            String mostservedip = getMostServedIp().getKey(); // calls cleanLogs
            int mostservedrequests = getMostServedIp().getValue();

            //IpLocationResponse response = (new IpLocationService()).getLocation(mostservedip);
            String country =        "na";               //response.country_name;
            String region =         "na";               //response.region_name;
            String city =           "na";               //response.city_name;
            String organization =   "na";               //response.as;
            String location = city+", "+region+", "+country+" and it is affiliated with "+organization;
            
            textPart.setText("Hello,\n\nAttached are the access and error logs for blackburnengineering.site for "+month+" "+day+", "
            +year+".\n\nThe most served client, with "+mostservedrequests+" requests, was "+mostservedip+
            ". This address is from "+location+".\n\n\nHave a great day!");

            // Attachment part
            MimeBodyPart attachmentPart1 = new MimeBodyPart();
            attachmentPart1.attachFile("hidden_key/"+year+"/"+month+"/access_"+month+"_"+day+".log");  // <- path to the file you want to append
            
            MimeBodyPart attachmentPart2 = new MimeBodyPart();
            attachmentPart2.attachFile("hidden_key/" + year + "/" + month + "/error_" + month + "_" + day + ".log");  // <- path to the file you want to append

            // Combine body + attachment
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart1);
            multipart.addBodyPart(attachmentPart2);

            // Set content of the message to the multipart
            message.setContent(multipart);

            Transport.send(message);
            System.out.println("Daily Site Logs Email sent successfully for "+month+", "+day+", "+year);

        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void createArchive() {
        Path yearFolder = Path.of(logArchivePath, String.valueOf(year));
        Path monthFolder = Path.of(logArchivePath+"/"+year, String.valueOf(month));
        try {
            // Create year folder if it doesn't exist
            if (!Files.exists(yearFolder)) {
                Files.createDirectory(yearFolder);
                System.out.println("Created folder: " + yearFolder);
            }
            // Create month folder if it doesn't exist
            if (!Files.exists(monthFolder)) {
                Files.createDirectory(monthFolder);
                System.out.println("Created folder: " + monthFolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void cleanAccessLogs() {
        Path accessLog = Paths.get("hidden_key/access.log");
        Path dayLog = Paths.get("hidden_key/"+year+"/"+month+"/access_"+month+"_"+day+".log");
        // Date pattern in logs: dd/MMM/yyyy
        String datePrefix = String.format("%02d/%s/%d", day, month.substring(0,3), year);

        try (BufferedReader reader = Files.newBufferedReader(accessLog);
             BufferedWriter writer = Files.newBufferedWriter(dayLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Extract the date inside brackets
                int start = line.indexOf('[') + 1;
                int end = line.indexOf(':');
                if (start > 0 && end > start) {
                    String logDate = line.substring(start, end);
                    if (logDate.equals(datePrefix)) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            System.out.println("Filtered log saved to: " + dayLog);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void cleanErrorLogs() {
        Path errorLog = Paths.get("hidden_key/error.log");
        Path dayLog = Paths.get("hidden_key/" + year + "/" + month + "/error_" + month + "_" + day + ".log");

        // Date pattern in logs: yyyy/MM/dd
        String monthPadded = String.format("%02d", java.time.Month.valueOf(month.toUpperCase()).getValue());
        String datePrefix = String.format("%d/%s/%02d", year, monthPadded, day); // e.g., "2025/08/14"

        try (BufferedReader reader = Files.newBufferedReader(errorLog);
             BufferedWriter writer = Files.newBufferedWriter(dayLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Extract the date at the start of the line
                if (line.length() >= 10) { // first 10 chars contain yyyy/MM/dd
                    String logDate = line.substring(0, 10);
                    if (logDate.equals(datePrefix)) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            System.out.println("Filtered error log saved to: " + dayLog);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void cleanLogs() {
        createArchive();
        cleanAccessLogs();
        cleanErrorLogs();
    }
    private Map.Entry<String, Integer> getMostServedIp() {
        cleanLogs();
            Path dayLog = Paths.get("hidden_key/archives/"
            + year + "/" + month + "/access_" + month + "_" + day + ".log");

        Map<String, Integer> ipCount = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(dayLog)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Access log format: "127.0.0.1 - - [date] "GET ..."
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String ip = parts[0];
                    ipCount.put(ip, ipCount.getOrDefault(ip, 0) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Find the IP with the max requests
        String mostServedIp = null;
        int maxRequests = 0;
        for (Map.Entry<String, Integer> entry : ipCount.entrySet()) {
            if (entry.getValue() > maxRequests) {
                mostServedIp = entry.getKey();
                maxRequests = entry.getValue();
            }
        }
        System.out.println("Most served IP: " + mostServedIp + " with " + maxRequests + " requests.");
        return new AbstractMap.SimpleEntry<>(mostServedIp, maxRequests);
    }
}
