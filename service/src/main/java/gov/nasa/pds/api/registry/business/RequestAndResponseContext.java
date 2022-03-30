package gov.nasa.pds.api.registry.business;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.util.StringTokenizer;

import gov.nasa.pds.api.registry.controllers.URIParameters;
import gov.nasa.pds.api.registry.exceptions.ApplicationTypeException;
import gov.nasa.pds.api.registry.exceptions.NothingFoundException;
import gov.nasa.pds.api.registry.search.HitIterator;
import gov.nasa.pds.api.registry.search.SearchUtil;
import gov.nasa.pds.model.Summary;

public class RequestAndResponseContext
{
    private static final Logger log = LoggerFactory.getLogger(RequestAndResponseContext.class);

    final private long begin_processing = System.currentTimeMillis();
	final private String queryString;
	final private List<String> keywords;
	final private String lidvid;
    final private List<String> fields;
    final private List<String> sort;
    final private int start;
    final private int limit;
    final private Map<String, String> presetCriteria;
    final private boolean onlySummary;
    final private ProductVersionSelector selector;
    final private String format;
    final private Map<String, ProductBusinessLogic> formatters;
    final private ObjectMapper om;
    final private URL baseURL;

    static public RequestAndResponseContext buildRequestAndResponseContext(
    		ObjectMapper om, URL base, // webby criteria
    		URIParameters parameters,
    		Map<String,String> preset, // default criteria when nothing else is defined
    		String output_format // the accept statement of the request that informs the output type
    		) throws ApplicationTypeException,LidVidNotFoundException
    { return new RequestAndResponseContext(om, base, parameters, preset, output_format); }
    
    private RequestAndResponseContext(
    		ObjectMapper om, URL base, // webby criteria
    		URIParameters parameters,
    		Map<String,String> preset, // default criteria when nothing else is defined
    		String output_format // the accept statement of the request that informs the output type
    		) throws ApplicationTypeException,LidVidNotFoundException
    {
    	Map<String, ProductBusinessLogic> formatters = new HashMap<String, ProductBusinessLogic>();
    	formatters.put("*/*", new PdsProductBusinessObject());
    	formatters.put("application/csv", new WyriwygBusinessObject());
    	formatters.put("application/json", new PdsProductBusinessObject());
    	formatters.put("application/kvp+json", new WyriwygBusinessObject());
    	formatters.put("application/vnd.nasa.pds.pds4+json", new Pds4ProductBusinessObject(true));
    	formatters.put("application/vnd.nasa.pds.pds4+xml", new Pds4ProductBusinessObject(false));
    	formatters.put("application/xml", new PdsProductBusinessObject());
    	formatters.put("text/csv", new WyriwygBusinessObject());
    	formatters.put("text/html", new PdsProductBusinessObject());
    	formatters.put("text/xml", new PdsProductBusinessObject());
    	this.formatters = formatters;
    	this.format = this.find_match(output_format);
    	this.baseURL = base;
    	this.om = om;
    	this.queryString = parameters.getQuery();
    	this.keywords = parameters.getKeywords();
    	this.lidvid = parameters.getIdentifier() == null ? null : LidVidUtils.extractLidFromLidVid(parameters.getIdentifier());
    	this.fields = new ArrayList<String>();
    	this.fields.addAll(this.add_output_needs (parameters.getFields()));
    	this.sort = parameters.getSort();
    	this.start = parameters.getStart();
    	this.limit = parameters.getLimit();
    	this.onlySummary = parameters.getSummanryOnly();
    	this.presetCriteria = preset;
    	this.selector = parameters.getSelector();
    }
    
    public List<String> getKeywords() { return this.keywords; }
    public String getLIDVID() { return this.lidvid; }
	public final List<String> getFields() { return this.fields; }
	public final List<String> getSort() { return this.sort; }
	public int getStart() { return this.start; }
	public int getLimit() { return this.limit; }
	public boolean isOnlySummary() { return this.onlySummary; }
	public String getQueryString() { return this.queryString; }
	public final Map<String, String> getPresetCriteria() { return this.presetCriteria; };
	public ProductVersionSelector getSelector() { return this.selector; }

	private List<String> add_output_needs (List<String> given) throws ApplicationTypeException
	{
		List<String> complete = new ArrayList<String>();
		String max_needs[] = {}, min_needs[] = {};
		given = SearchUtil.jsonPropertyToOpenProperty(given);

		if (this.formatters.containsKey(this.format))
		{ 
			this.formatters.get(this.format).setBaseURL(this.baseURL);
			this.formatters.get(this.format).setObjectMapper(this.om);
			max_needs = SearchUtil.jsonPropertyToOpenProperty(this.formatters.get(this.format).getMaximallyRequiredFields());
			min_needs = SearchUtil.jsonPropertyToOpenProperty(this.formatters.get(this.format).getMinimallyRequiredFields());
		}
		else
		{
                    String known = String.join(", ", this.formatters.keySet());
			log.warn("The Accept header value " + String.valueOf(this.format) + " is not supported, supported values are " + known);
			throw new ApplicationTypeException("The Accept header value " + String.valueOf(this.format) + " is not supported, supported values are " + known);
		}

		/* if the URL contains fields, then make sure the minimum was included too OR there is maximum set. */
		if ((given != null && 0 < given.size()) || 0 < max_needs.length)
		{
			if (given != null) complete.addAll(given);

			for (int index=0 ; index < min_needs.length ; index++)
			{ if (!complete.contains(min_needs[index])) complete.add(min_needs[index]); }
		}
		
		if (0 < max_needs.length)
		{
			List<String> allowed = Arrays.asList(max_needs);
			List<String> filtered = new ArrayList<String>();

			for (String field : complete)
			{
				if (allowed.contains(field)) { filtered.add(field); }
			}
			complete = filtered;
		}

		return complete;
	}
	
	private String find_match (String from_user)
	{
		String match=from_user;
		StringTokenizer mimes = new StringTokenizer(from_user, ",");
		
		while (mimes.hasMoreTokens())
		{
			/* separate the mime_type/mime_subtype from ;* stuff */
			String mime = mimes.nextToken();
			if (mime.contains(";")) mime = mime.substring(0, mime.indexOf(";"));
			
			if (this.formatters.keySet().contains(mime))
			{
				match = mime;
				break;
			}
		}
		log.info("Matched output type as '" + match + "' from '" + from_user + "'.");
		return match;
	}

	public Object getResponse() throws NothingFoundException
	{
		Object response = this.formatters.get(this.format).getResponse();
		
		if (response == null)
		{
			log.warn("Could not find any data given these conditions");
			log.warn("   fields: " + String.valueOf(this.getFields().size()));
			for (String field : this.getFields()) log.warn("      " + field);
			log.warn("   keyword: " + String.valueOf(this.getKeywords().size()));
			for (String keyword : this.getKeywords()) log.warn("    " + keyword);
			log.warn("   lidvid: " + this.getLIDVID());
			log.warn("   limit: " + String.valueOf(this.getLimit()));
			log.warn("   query string: " + String.valueOf(this.getQueryString()));
			log.warn("   selector: " + String.valueOf(this.getSelector()));
			log.warn("   sorting: " + String.valueOf(this.getSort().size()));
			for (String sort : this.getSort()) log.warn("      " + sort);
			log.warn("   start: " + String.valueOf(this.getStart()));
			log.warn("   summary: " + String.valueOf(this.isOnlySummary()));
			throw new NothingFoundException();
		}
		return response;
	}
	
	public void setResponse(HitIterator hits, int real_total)
	{ 
		Summary summary = new Summary();
		summary.setQ(this.getQueryString());
		summary.setStart(this.getStart());
		summary.setLimit(this.getLimit());
		summary.setSort(this.getSort());
		summary.setHits(this.formatters.get(this.format).setResponse(hits, summary, this.fields, this.onlySummary));
		summary.setProperties(new ArrayList<String>());
		
		if (0 < real_total) summary.setHits(real_total);

		summary.setTook((int)(System.currentTimeMillis() - this.begin_processing));
	}

	public void setResponse(SearchHits hits)
	{ this.setResponse(hits, null); }

	public void setResponse(SearchHits hits, List<String> uniqueProperties)
	{ if (hits != null) this.setResponse(hits, uniqueProperties, (int)hits.getTotalHits().value); }

	public void setResponse(SearchHits hits, List<String> uniqueProperties, int total_hits)
	{
		if (hits != null)
		{
			Summary summary = new Summary();
			summary.setQ(this.getQueryString());
			summary.setStart(this.getStart());
			summary.setLimit(this.getLimit());
			summary.setSort(this.getSort());
			summary.setHits(total_hits);

			if (uniqueProperties != null) summary.setProperties(uniqueProperties);
			this.formatters.get(this.format).setResponse(hits, summary, this.fields, this.onlySummary);

			summary.setTook((int)(System.currentTimeMillis() - this.begin_processing));
		}
	}
	
	public void setResponse(RestHighLevelClient client, SearchRequest request) throws IOException
	{
        request.source().size(this.getLimit());
        request.source().from(this.getStart());
        this.setResponse(client.search(request, RequestOptions.DEFAULT).getHits());
	}

	public void setSingularResponse(RestHighLevelClient client, SearchRequest request) throws IOException
	{
        request.source().size(this.getLimit());
        request.source().from(this.getStart());
        SearchHits hits = client.search(request, RequestOptions.DEFAULT).getHits();
        
        if (hits != null && hits.getTotalHits().value == 1L) this.formatters.get(this.format).setResponse(hits.getAt(0), this.fields);
        else
        {
        	log.error("Too many or too few lidvids which is just wrong.");
        	throw new IOException("Too many or too few lidvids matched the request when it should have just been 1.");
        }
	}
}
