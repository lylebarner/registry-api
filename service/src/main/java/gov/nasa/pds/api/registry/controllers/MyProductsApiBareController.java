package gov.nasa.pds.api.registry.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.opensearch.action.search.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nasa.pds.api.registry.business.ErrorFactory;
import gov.nasa.pds.api.registry.business.LidVidNotFoundException;
import gov.nasa.pds.api.registry.business.ProductBusinessObject;
import gov.nasa.pds.api.registry.business.RequestAndResponseContext;
import gov.nasa.pds.api.registry.opensearch.OpenSearchRegistryConnection;
import gov.nasa.pds.api.registry.exceptions.ApplicationTypeException;
import gov.nasa.pds.api.registry.exceptions.NothingFoundException;
import gov.nasa.pds.api.registry.search.HitIterator;
import gov.nasa.pds.api.registry.search.RegistrySearchRequestBuilder;

@Component
public class MyProductsApiBareController {
    
    private static final Logger log = LoggerFactory.getLogger(MyProductsApiBareController.class);  
    
    protected final ObjectMapper objectMapper;

    protected final HttpServletRequest request;   

    protected Map<String, String> presetCriteria = new HashMap<String, String>();
    
    @Value("${server.contextPath}")
    protected String contextPath;
    
    @Autowired
    protected HttpServletRequest context;
    
    // TODO remove and replace by BusinessObjects 
    @Autowired
    OpenSearchRegistryConnection esRegistryConnection;
    
    @Autowired
    protected ProductBusinessObject productBO;
    
    @Autowired
    RegistrySearchRequestBuilder searchRequestBuilder;
    

    public MyProductsApiBareController(ObjectMapper objectMapper, HttpServletRequest context) {
        this.objectMapper = objectMapper;
        this.request = context;
    }

    protected void fillProductsFromLidvids (RequestAndResponseContext context, List<String> lidvids, int real_total) throws IOException
    {
    	context.setResponse(new HitIterator(lidvids.size(), this.esRegistryConnection.getRestHighLevelClient(),
                RegistrySearchRequestBuilder.getQueryFieldsFromKVP("lidvid",
                        lidvids, context.getFields(), this.esRegistryConnection.getRegistryIndex())), real_total);
    }

    
    protected void getProducts(RequestAndResponseContext context) throws IOException
    {
        SearchRequest searchRequest = this.searchRequestBuilder.getSearchProductsRequest(
        		context.getQueryString(),
        		context.getKeywords(),
        		context.getFields(), context.getStart(), context.getLimit(), this.presetCriteria);
        context.setResponse(this.esRegistryConnection.getRestHighLevelClient(), searchRequest);
    }
 

    protected ResponseEntity<Object> getProductsResponseEntity(URIParameters parameters)
    {
        String accept = this.request.getHeader("Accept");
        log.debug("accept value is " + accept);

        try
        {
        	RequestAndResponseContext context = RequestAndResponseContext.buildRequestAndResponseContext(this.objectMapper, this.getBaseURL(), parameters, this.presetCriteria, accept);
        	this.getProducts(context);                
        	return new ResponseEntity<Object>(context.getResponse(), HttpStatus.OK);
        }
        catch (ApplicationTypeException e)
        {
        	log.error("Application type not implemented", e);
        	return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_ACCEPTABLE);
        }
        catch (IOException e)
        {
            log.error("Couldn't serialize response for content type " + accept, e);
            return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (LidVidNotFoundException e)
        {
            log.warn("Could not find lid(vid) in database: " + parameters.getIdentifier());
            return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_FOUND);
        }
        catch (NothingFoundException e)
        {
        	log.warn("Could not find any matching reference(s) in database.");
        	return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_FOUND);
        }
        catch (ParseCancellationException pce)
        {
            log.error("Could not parse the query string: " + parameters.getQuery(), pce);
            return new ResponseEntity<Object>(ErrorFactory.build(pce, this.request), HttpStatus.BAD_REQUEST);
        }
    }    
    
    
    protected ResponseEntity<Object> getAllProductsResponseEntity(URIParameters parameters)
    {
        String accept = this.request.getHeader("Accept");
        log.debug("accept value is " + accept);

        try
        {            
            RequestAndResponseContext context = RequestAndResponseContext.buildRequestAndResponseContext(this.objectMapper, this.getBaseURL(), parameters, this.presetCriteria, accept);
            this.getProductsByLid(context);
            return new ResponseEntity<Object>(context.getResponse(), HttpStatus.OK);
        }
        catch (ApplicationTypeException e)
        {
        	log.error("Application type not implemented", e);
        	return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_ACCEPTABLE);
        }
        catch (IOException e)
        {
            log.error("Couldn't serialize response for content type " + accept, e);
            return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (LidVidNotFoundException e)
        {
            log.warn("Could not find lid(vid) in database: " + parameters.getIdentifier());
            return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_FOUND);
        }
        catch (NothingFoundException e)
        {
        	log.warn("Could not find any matching reference(s) in database.");
        	return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_FOUND);
        }
        catch (ParseCancellationException pce)
        {
            log.error("", pce);
            return new ResponseEntity<Object>(ErrorFactory.build(pce, this.request), HttpStatus.BAD_REQUEST);
        }
    }    
    
    
    public void getProductsByLid(RequestAndResponseContext context) throws IOException 
    {
        SearchRequest req = searchRequestBuilder.getSearchProductsByLid(context.getLIDVID(), context.getStart(), context.getLimit());
        context.setSingularResponse(this.esRegistryConnection.getRestHighLevelClient(), req);
    }

    
    protected ResponseEntity<Object> getLatestProductResponseEntity(URIParameters parameters)
    {
        String accept = request.getHeader("Accept");
        
        try 
        {
            RequestAndResponseContext context = RequestAndResponseContext.buildRequestAndResponseContext(this.objectMapper, this.getBaseURL(), parameters, this.presetCriteria, accept);
            SearchRequest request = RegistrySearchRequestBuilder.getQueryFieldsFromKVP("lidvid", context.getLIDVID(), context.getFields(),this.esRegistryConnection.getRegistryIndex(), false);
            context.setSingularResponse(this.esRegistryConnection.getRestHighLevelClient(), request);            
            context.setResponse(esRegistryConnection.getRestHighLevelClient(), request);

            if (context.getResponse() == null)
            { 
            	log.warn("Could not find any matches for LIDVID: " + context.getLIDVID());
            	return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<Object>(context.getResponse(), HttpStatus.OK);
        } 
        catch (ApplicationTypeException e)
        {
        	log.error("Application type not implemented", e);
        	return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_ACCEPTABLE);
        }
        catch (IOException e) 
        {
            log.error("Couldn't get or serialize response for content type " + accept, e);
            return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (LidVidNotFoundException e)
        {
            log.warn("Could not find lid(vid) in database: " + parameters.getIdentifier());
            return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_FOUND);
        }
        catch (NothingFoundException e)
        {
        	log.warn("Could not find any matching reference(s) in database.");
        	return new ResponseEntity<Object>(ErrorFactory.build(e, this.request), HttpStatus.NOT_FOUND);
        }
    }

    
    private boolean proxyRunsOnDefaultPort() {
        return (((this.context.getScheme() == "https")  && (this.context.getServerPort() == 443)) 
                || ((this.context.getScheme() == "http")  && (this.context.getServerPort() == 80)));
    }
 
    protected URL getBaseURL() {
        try {
            MyProductsApiBareController.log.debug("contextPath is: " + this.contextPath);
            
            URL baseURL;
            if (this.proxyRunsOnDefaultPort()) {
                baseURL = new URL(this.context.getScheme(), this.context.getServerName(), this.contextPath);
            } 
            else {
                baseURL = new URL(this.context.getScheme(), this.context.getServerName(), this.context.getServerPort(), this.contextPath);
            }
            
            log.debug("baseUrl is " + baseURL.toString());
            return baseURL;
            
        } catch (MalformedURLException e) {
            log.error("Server URL was not retrieved");
            return null;
        }
    }
}
