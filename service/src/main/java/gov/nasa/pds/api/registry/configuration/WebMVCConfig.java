package gov.nasa.pds.api.registry.configuration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import gov.nasa.pds.api.registry.view.CsvErrorMessageSerializer;
import gov.nasa.pds.api.registry.view.CsvPluralSerializer;
import gov.nasa.pds.api.registry.view.CsvSingularSerializer;
import gov.nasa.pds.api.registry.view.HtmlErrorMessageSerializer;
import gov.nasa.pds.api.registry.view.JsonErrorMessageSerializer;
import gov.nasa.pds.api.registry.view.JsonPluralSerializer;
import gov.nasa.pds.api.registry.view.JsonProductSerializer;
import gov.nasa.pds.api.registry.view.JsonSingularSerializer;
import gov.nasa.pds.api.registry.view.Pds4JsonProductSerializer;
import gov.nasa.pds.api.registry.view.Pds4JsonProductsSerializer;
import gov.nasa.pds.api.registry.view.Pds4XmlProductSerializer;
import gov.nasa.pds.api.registry.view.Pds4XmlProductsSerializer;
import gov.nasa.pds.api.registry.view.PdsProductTextHtmlSerializer;
import gov.nasa.pds.api.registry.view.PdsProductXMLSerializer;
import gov.nasa.pds.api.registry.view.PdsProductsTextHtmlSerializer;
import gov.nasa.pds.api.registry.view.PdsProductsXMLSerializer;
import gov.nasa.pds.api.registry.view.XmlErrorMessageSerializer;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = { "gov.nasa.pds.api.registry.configuration ",  "gov.nasa.pds.api.registry.controller", "gov.nasa.pds.api.registry.search"})
public class WebMVCConfig implements WebMvcConfigurer
{   
	private static final Logger log = LoggerFactory.getLogger(WebMVCConfig.class);
 
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
	{
		registry.addResourceHandler("/webjars/**")
		.addResourceLocations("classpath:/META-INF/resources/webjars/");
		registry.addResourceHandler("/swagger-ui/**")
				.addResourceLocations("classpath:/swagger-ui/");
    }

	@Override
	@SuppressWarnings("deprecation")
	public void configurePathMatch(PathMatchConfigurer configurer)
	{
		// this is important to avoid that parameters (e.g lidvid) are truncated after .
		configurer.setUseSuffixPatternMatch(false);
	}

	/**
	 * Setup a simple strategy: use all the defaults and return JSON by default when not sure. 
	 */
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer)
	{
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters)
	{
		WebMVCConfig.log.info("Number of converters available " + Integer.toString(converters.size()));
		converters.add(new CsvErrorMessageSerializer());
		converters.add(new CsvPluralSerializer());
		converters.add(new CsvSingularSerializer());
		converters.add(new HtmlErrorMessageSerializer());
		converters.add(new JsonErrorMessageSerializer());
		converters.add(new JsonPluralSerializer());
		converters.add(new JsonSingularSerializer());
		converters.add(new JsonProductSerializer());
		converters.add(new Pds4JsonProductSerializer());
		converters.add(new Pds4JsonProductsSerializer());
		converters.add(new Pds4XmlProductSerializer());
		converters.add(new Pds4XmlProductsSerializer());
		converters.add(new PdsProductTextHtmlSerializer());
		converters.add(new PdsProductXMLSerializer());
		converters.add(new PdsProductsTextHtmlSerializer());
		converters.add(new PdsProductsXMLSerializer());
		converters.add(new XmlErrorMessageSerializer());
	}
}
