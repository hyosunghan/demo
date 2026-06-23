package com.example.demo.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchClientConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.scheme}")
    private String scheme;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = getRestClient();
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        ElasticsearchTransport elasticsearchTransport = new RestClientTransport(restClient, mapper);
        return new ElasticsearchClient(elasticsearchTransport);
    }

    @Bean
    public RestClient restClient() {
        return getRestClient();
    }

    private RestClient getRestClient() {
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(host, port, scheme));
        restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
            BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
            basicCredentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            httpClientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
            return httpClientBuilder;
        });
        return restClientBuilder.build();
    }
}

