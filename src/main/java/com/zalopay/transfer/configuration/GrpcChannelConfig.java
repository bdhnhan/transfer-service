package com.zalopay.transfer.configuration;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ManagedChannel.class})
@AutoConfigureAfter({ExternalServiceProperties.class})
@EnableConfigurationProperties({ExternalServiceProperties.class})
public class GrpcChannelConfig {

    @Autowired
    private ExternalServiceProperties externalServiceProperties;

    private  ManagedChannel managedChannel(ExternalServiceProperties.ServiceProperties props) {
        ManagedChannel channel = null;
        if (props != null) {
            channel = ManagedChannelBuilder.forAddress(props.getHost(), props.getPort())
                    .usePlaintext().build();
        }
        return channel;
    }

    @Bean
    public ManagedChannel zaloWalletChannel() {
        ExternalServiceProperties.ServiceProperties props = externalServiceProperties.getZaloWallet();
        return managedChannel(props);
    }

    @Bean
    public ManagedChannel bankChannel() {
        ExternalServiceProperties.ServiceProperties props = externalServiceProperties.getBank();
        return managedChannel(props);
    }
}
