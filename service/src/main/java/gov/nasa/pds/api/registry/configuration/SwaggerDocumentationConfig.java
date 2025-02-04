package gov.nasa.pds.api.registry.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-07-24T09:48:37.812-07:00[America/Los_Angeles]")
@Configuration
public class SwaggerDocumentationConfig {

	@Value("${registry.service.version:undefined}")
	private String version;

	ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("PDS Federated API")
            .description("Federated PDS API which provides actionable end points standardized between the different nodes. ")
            .license("Apache 2.0")
            .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
            .termsOfServiceUrl("")
            .version(this.version)
            .contact(new Contact("","", "pds_operator@jpl.nasa.gov"))
            .build();
    }

    @Bean
    public Docket customImplementation(){
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                    .apis(RequestHandlerSelectors.basePackage("gov.nasa.pds.api.registry"))
                    .build()
                .directModelSubstitute(org.joda.time.LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(org.joda.time.DateTime.class, java.util.Date.class)
                .apiInfo(apiInfo());
    }
    
   

}
