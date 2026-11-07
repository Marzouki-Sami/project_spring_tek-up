package com.marzouki.shop.geo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeoProperties.class)
class GeoConfig {
}