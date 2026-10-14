package com.crosscheck.bootstrap.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    OpenAPI analysisOpenApi() {
        return new OpenAPI().info(new Info().title("CrossCheck API").version("1.0")
                .description("Asynchronous analysis jobs. The bearer is a client-generated job access credential, not user authentication. "
                        + "Reuse the same credential when polling or retrying a job. In the openai profile, submitting a query incurs provider usage."))
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes("JobCredential",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .description("Client-generated 32-byte random credential encoded as Base64url without padding. Enter the credential without the Bearer prefix.")));
    }
}
