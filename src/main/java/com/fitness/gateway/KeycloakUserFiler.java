package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserFiler implements WebFilter {

    private final UserService userService;

    @Override
    public Mono<Void> filter (ServerWebExchange exchange, WebFilterChain chain){
    String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
    String token = exchange.getRequest().getHeaders().getFirst("Authorization");
    RegisterRequest registerRequest = getUserDetails(token);

    if(userId == null){
        userId = registerRequest.getKeCloakId();
    }
    if(userId != null && token != null){
        String finalUserid = userId;
        return userService.validateUsesrId(userId)
                .flatMap(exist -> {
                    if(!exist) {
                        //Register user

//                        RegisterRequest  registerRequest = getUserDetails(token);
                        if(registerRequest != null){
                            return userService.registerUser(registerRequest)
                                    .then(Mono.empty());
                        } else{

                        return Mono.empty();
                        }

                    } else{
                        log.info("User already exist, skipping sync: ");
                        return Mono.empty();
                    }
                })
                .then(Mono.defer(()->{
                    ServerHttpRequest mutateRequest = exchange.getRequest().mutate()
                            .header("X-User-ID", finalUserid)
                            .build();
                    return chain.filter(exchange.mutate().request(mutateRequest).build());
                }));
    }
    return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {
        try{
            String tokenWithoutBearer = token.replace("Bearer", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(claims.getStringClaim("email"));
            registerRequest.setKeCloakId(claims.getStringClaim("sub"));
            registerRequest.setPassword("dummy@123");
            registerRequest.setFirstName(claims.getStringClaim("given_name"));
            registerRequest.setLastName(claims.getStringClaim("family_name"));
            return  registerRequest;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}

