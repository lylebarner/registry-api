package gov.nasa.pds.api.registry.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nasa.pds.api.registry.exceptions.UnsupportedSearchProperty;
import gov.nasa.pds.api.registry.search.HitIterator;
import gov.nasa.pds.model.Summary;
import gov.nasa.pds.model.WyriwygProduct;
import gov.nasa.pds.model.WyriwygProductKeyValuePairs;
import gov.nasa.pds.model.WyriwygProducts;

public class WyriwygBusinessObject implements ProductBusinessLogic
{
	private static final Logger log = LoggerFactory.getLogger(WyriwygBusinessObject.class);

	@SuppressWarnings("unused")
	private ObjectMapper om;
	@SuppressWarnings("unused")
	private URL baseURL;
	private WyriwygProduct product = null;
	private WyriwygProducts products = null;
	
	@Override
	public String[] getMaximallyRequiredFields() { return new String[0]; }
	@Override
	public String[] getMinimallyRequiredFields() { return new String[0]; }

	@Override
	public Object getResponse() { return this.product == null ? this.products : this.product; }

	@Override
	public void setBaseURL(URL baseURL) { this.baseURL = baseURL; }

	@Override
	public void setObjectMapper(ObjectMapper om) { this.om = om; }

	@Override
	public void setResponse (SearchHit hit, List<String> fields)
	{
    	WyriwygProduct product = new WyriwygProduct();
    	for (Entry<String, Object> pair : hit.getSourceAsMap().entrySet())
    	{
    		WyriwygProductKeyValuePairs kvp = new WyriwygProductKeyValuePairs();
    		try
    		{
    			kvp.setKey(SearchUtil.openPropertyToJsonProperty(pair.getKey()));
    			kvp.setValue(String.valueOf(pair.getValue()));
    			product.addKeyValuePairsItem(kvp);
    		}
    		catch (UnsupportedSearchProperty e) { log.warn("openSearch property " + pair.getKey() + " is not supported, ignored"); }
    	}
    	this.product = product;
	}

	@Override
	public int setResponse(HitIterator hits, Summary summary, List<String> fields, boolean onlySummary)
	{
		Set<String> uniqueProperties = new TreeSet<String>();
		WyriwygProducts products = new WyriwygProducts();

		for (Map<String,Object> kvps : hits)
        {
            uniqueProperties.addAll(ProductBusinessObject.getFilteredProperties(kvps, fields, null).keySet());

            if (!onlySummary)
            {
            	WyriwygProduct product = new WyriwygProduct();
            	for (Entry<String, Object> pair : kvps.entrySet())
            	{
            		WyriwygProductKeyValuePairs kvp = new WyriwygProductKeyValuePairs();
            		try
            		{
            			kvp.setKey(SearchUtil.openPropertyToJsonProperty(pair.getKey()));
            			kvp.setValue(String.valueOf(pair.getValue()));
            			product.addKeyValuePairsItem(kvp);
            		}
            		catch (UnsupportedSearchProperty e) { log.warn("openSearch property " + pair.getKey() + " is not supported, ignored"); }
            	}
            	products.addDataItem(product);
            }
        }
		summary.setProperties(new ArrayList<String>(uniqueProperties));
		products.setSummary(summary);
		this.products = products;
		return products.getData().size();
	}

	@Override
	public int setResponse(SearchHits hits, Summary summary, List<String> fields, boolean onlySummary)
	{
		Set<String> uniqueProperties = new TreeSet<String>();
		WyriwygProducts products = new WyriwygProducts();

		for (SearchHit hit : hits.getHits())
        {
			Map<String, Object> kvps = hit.getSourceAsMap();
            uniqueProperties.addAll(ProductBusinessObject.getFilteredProperties(kvps, fields, null).keySet());

            if (!onlySummary)
            {
            	WyriwygProduct product = new WyriwygProduct();
            	for (Entry<String, Object> pair : kvps.entrySet())
            	{
            		WyriwygProductKeyValuePairs kvp = new WyriwygProductKeyValuePairs();
            		try
            		{
            			kvp.setKey(SearchUtil.openPropertyToJsonProperty(pair.getKey()));
            			kvp.setValue(String.valueOf(pair.getValue()));
            			product.addKeyValuePairsItem(kvp);
            		}
            		catch (UnsupportedSearchProperty e) { log.warn("openSearch property " + pair.getKey() + " is not supported, ignored"); }
            	}
            	products.addDataItem(product);
            }
        }
		
		summary.setProperties(new ArrayList<String>(uniqueProperties));
		products.setSummary(summary);
		this.products = products;
		return (int)(hits.getTotalHits().value);
	}
}
