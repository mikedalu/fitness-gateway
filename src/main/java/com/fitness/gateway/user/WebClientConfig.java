package com.fitness.gateway.user;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced //allows webclient resolve the servvice name on Eureka so u don't have to harcode the ip
    public WebClient.Builder webClientBuilder (){
        return WebClient.builder();
    }

    @Bean
    public  WebClient userServiceWebClient (WebClient.Builder webClientBuilder){
        return webClientBuilder
                .baseUrl("http://USER-SERVICE")
                .build();
    }
}
