package com.example.contentservice.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {
    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(
          ConnectionFactoryOptions.builder()
                  .option(DRIVER, "postgresql")
                  .option(HOST, "localhost")
                  .option(PORT, 5432)
                  .option(USER, "postgres")
                  .option(PASSWORD, "1234")
                  .option(DATABASE, "webflux")
                  .build()
        );
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory){
        return new R2dbcEntityTemplate(connectionFactory);
    }
}