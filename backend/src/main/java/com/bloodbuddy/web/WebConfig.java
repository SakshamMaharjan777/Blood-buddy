package com.bloodbuddy.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers the web-layer plumbing. Currently one thing: the {@code ActorContext}
 * argument resolver, so controller signatures can read
 * {@code create(RequestCreateRequest body, ActorContext actor)} instead of
 * digging the session out of the request themselves.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ActorContextArgumentResolver actorContextArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(actorContextArgumentResolver);
    }
}
