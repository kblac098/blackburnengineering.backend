package app.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IpLocationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL =
        "https://api.ip2location.io/?key=7E57A537581FA1964812418A4C816219&ip={ip}";

    public IpLocationResponse getLocation(String ip) {
        return restTemplate.getForObject(API_URL, IpLocationResponse.class, ip);
    }
}

