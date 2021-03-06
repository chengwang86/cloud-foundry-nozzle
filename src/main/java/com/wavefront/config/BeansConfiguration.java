package com.wavefront.config;

import com.wavefront.props.AppInfoProperties;
import com.wavefront.props.FirehoseProperties;
import com.wavefront.props.PcfProperties;
import com.wavefront.props.WavefrontProxyProperties;
import com.wavefront.service.FirehoseToWavefrontProxyConnector;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

/**
 * Configuration class responsible for instantiating all the Spring Beans
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Configuration
@EnableConfigurationProperties({PcfProperties.class, WavefrontProxyProperties.class,
    FirehoseProperties.class, AppInfoProperties.class})
public class BeansConfiguration {

  private static final Logger logger = Logger.getLogger(
      BeansConfiguration.class.getCanonicalName());
  private static final String NETTY_MAX_CONNECTIONS_PROP = "reactor.ipc.netty.pool.maxConnections";

  @Autowired
  private FirehoseToWavefrontProxyConnector firehoseToWavefrontProxyConnector;

  @Bean
  public CommandLineRunner commandLineRunner() {
    return args -> firehoseToWavefrontProxyConnector.connect();
  }

  @Bean
  public DefaultConnectionContext connectionContext(PcfProperties pcfProperties) {
    // Set property if not already set
    System.setProperty(NETTY_MAX_CONNECTIONS_PROP,
        System.getProperty(NETTY_MAX_CONNECTIONS_PROP, "32"));
    logger.info("Using reactor.ipc.netty.pool.maxConnections: " +
        System.getProperty("reactor.ipc.netty.pool.maxConnections"));
    return DefaultConnectionContext.builder().apiHost(pcfProperties.getHost()).
        skipSslValidation(pcfProperties.isSkipSslValidation()).build();
  }

  @Bean
  public PasswordGrantTokenProvider tokenProvider(PcfProperties pcfProperties) {
    logger.info(String.format("Using PCF properties, host: %s and user: %s",
        pcfProperties.getHost(), pcfProperties.getUser()));
    return PasswordGrantTokenProvider.builder().username(pcfProperties.getUser()).
        password(pcfProperties.getPassword()).build();
  }

  @Bean
  public ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext,
                                                      TokenProvider tokenProvider) {
    return ReactorCloudFoundryClient.builder().connectionContext(connectionContext).
        tokenProvider(tokenProvider).build();
  }

  @Bean
  public ReactorDopplerClient dopplerClient(ConnectionContext connectionContext,
                                            TokenProvider tokenProvider) {
    return ReactorDopplerClient.builder().connectionContext(connectionContext).
        tokenProvider(tokenProvider).build();
  }
}
