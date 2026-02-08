package app.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IpLocationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL =
        "hidden_key";

    public IpLocationResponse getLocation(String ip) {
        return restTemplate.getForObject(API_URL, IpLocationResponse.class, ip);
    }
}

