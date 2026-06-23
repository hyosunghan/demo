package com.example.demo.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RestUtil {

    private static final RestTemplate restTemplate = new RestTemplate();

    static {
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    public static  <T> T get(String url, Class<T> responseClass) {
        ResponseEntity<T> responseEntity = restTemplate.getForEntity(url, responseClass, setDefaultHeaders());
        checkHttpStatus(responseEntity);
        return responseEntity.getBody();
    }

    public static  <T> T post(String url, Object body, Class<T> responseClass, Map<String, Object> uriVariables) {
        ResponseEntity<T> responseEntity = restTemplate.postForEntity(url, body, responseClass, uriVariables);
        checkHttpStatus(responseEntity);
        return responseEntity.getBody();
    }

    public static  <T> T post(String url, Object request, Class<T> responseClass) {
        ResponseEntity<T> responseEntity = restTemplate.postForEntity(url, request, responseClass, setDefaultHeaders());
        checkHttpStatus(responseEntity);
        return responseEntity.getBody();
    }

    public static  <T> T post(URI url, Object body, Class<T> responseClass) {
        ResponseEntity<T> responseEntity = restTemplate.postForEntity(url, body, responseClass);
        checkHttpStatus(responseEntity);
        return responseEntity.getBody();
    }

    private static Map<String, String> setDefaultHeaders() {
        return new HashMap<>();
    }

    private static <T> void checkHttpStatus(ResponseEntity<T> responseEntity) {
        if (HttpStatus.OK != responseEntity.getStatusCode()) {
            throw new RuntimeException("rest failure: " + responseEntity.getStatusCode().getReasonPhrase());
        }
    }
}
