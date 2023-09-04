package com.zalopay.transfer.configuration;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("external")
public class ExternalServiceProperties {
    private ServiceProperties zaloWallet;
    private ServiceProperties bank;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceProperties {
        private String host;
        private int port;
    }
}
