package edu.pitt.dbmi.nlp.noble.terminology.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Term;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

/**
 * The Class LexEVSRestTerminology.
 */
public class LexEVSRestTerminology extends AbstractTerminology {
	private static String TAG_QUERY_RESPONSE = "queryResponse";
	private static String TAG_CLASS = "class";
	private static String TAG_FIELD = "field";
	
	private String location;
	private String scheme;
	
	/**
	 * Instantiates a new lex EVS rest terminology.
	 *
	 * @param server the server
	 */
	public LexEVSRestTerminology(String server){
		this.location = server;
		// check if url contains schema name
		int x = server.indexOf("#");
		if(x > -1){
			String s = server;
			location  = s.substring(0,x);
			scheme = s.substring(x+1);
		}
		
		// select default
		if(scheme == null){
			scheme = "NCI MetaThesaurus";
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#convertConcept(java.lang.Object)
	 */
	protected Concept convertConcept(Object obj) {
		if(obj instanceof Element){
			Element element = (Element) obj;
			String name = element.getAttribute("name");
			
			// class node
			if(TAG_CLASS.equals(element.getTagName())){
				// we have a concept class
				if("org.LexGrid.concepts.Concept".equals(name)){
					Map<String,Object> content = processElement(element);
					
					// setup some usefull values
					String cui   = ""+content.get("_entityCode");
					Concept concept = new Concept(cui);
					String preferredName = null;
					
					// set synonyms and terms
					List<Term> terms = new ArrayList<Term>();
					List<String> synonyms = new ArrayList<String>();
					Set<Source> sources = new HashSet<Source>();
					if(content.containsKey("_presentationList")){
						for(Map m: (List<Map>) content.get("_presentationList")){
							if("org.LexGrid.concepts.Presentation".equals(m.get("name"))){
								String pt = ""+m.get("_isPreferred");
								String text = null;
								String lang = (String) m.get("_language");
								String form = (String) m.get("_representationalForm");
								Source src = null;
								
								// get content
								Map tm = ((List<Map>) m.get("_value")).get(0);
								if("org.LexGrid.commonTypes.Text".equals(tm.get("name"))){
									text = ""+tm.get("_content");
									synonyms.add(text.trim());
								}
								
								// get source
								List<Map> sl = (List<Map>) m.get("_sourceList");
								if(sl != null && !sl.isEmpty()){
									Map sm = sl.get(0);
									if("org.LexGrid.commonTypes.Source".equals(sm.get("name"))){
										String s = (String) sm.get("_content");
										if(s != null){
											src = new Source(s);
											sources.add(src);
										}
									}
								}
								
								if(text != null){
									Term term = new Term(text);
									term.setPreferred(Boolean.parseBoolean(pt));
									term.setLanguage(lang);
									term.setForm(form);
									if(src != null)
										term.setSource(src);
									terms.add(term);
								}
							}
						}
					}
					
					// set definitions and terms
					List<Definition> defs = new ArrayList<Definition>();
					if(content.containsKey("_definitionList")){
						for(Map m: (List<Map>) content.get("_definitionList")){
							if("org.LexGrid.concepts.Definition".equals(m.get("name"))){
								String text = null;
								Source src = null;
								
								Map tm = ((List<Map>) m.get("_value")).get(0);
								if("org.LexGrid.commonTypes.Text".equals(tm.get("name"))){
									text = ""+tm.get("_content");
								}
								// get source
								List<Map> sl = (List<Map>) m.get("_sourceList");
								if(sl != null && !sl.isEmpty()){
									Map sm = sl.get(0);
									if("org.LexGrid.commonTypes.Source".equals(sm.get("name"))){
										String s = (String) sm.get("_content");
										if(s != null){
											src = new Source(s);
										}
									}
								}
								
								if(text != null){
									Definition d = new Definition(text);
									if(src != null)
										d.setSource(src);
									defs.add(d);
								}
							}
						}
					}
					
					// set preferred name
					if(content.containsKey("_entityDescription")){
						for(Map m: (List<Map>) content.get("_entityDescription")){
							if("org.LexGrid.commonTypes.EntityDescription" .equals(m.get("name"))){
								preferredName = ""+m.get("_content");
							}
						}
					}
					
					// get properties
					// set definitions and terms
					Properties props = new Properties();
					if(content.containsKey("_propertyList")){
						for(Map m: (List<Map>) content.get("_propertyList")){
							if("org.LexGrid.commonTypes.Property".equals(m.get("name"))){
								String value = null;
								String prop = (String) m.get("_propertyName");
								
								Map tm = ((List<Map>) m.get("_value")).get(0);
								if("org.LexGrid.commonTypes.Text".equals(tm.get("name"))){
									value =  (String) tm.get("_content");
								}
							
								if(prop != null && value != null){
									props.setProperty(prop, value);
								}
							}
						}
					}
						
					
					// set name
					concept.setName(preferredName);
					concept.setTerminology(this);
					concept.setSynonyms(synonyms.toArray(new String [0]));
					concept.setTerms(terms.toArray(new Term [0]));
					concept.setDefinitions(defs.toArray(new Definition[0]));
					concept.setSources(sources.toArray(new Source [0]));
					concept.setProperties(props);
					if(props.containsKey("Semantic_Type")){
						concept.setSemanticTypes(new SemanticType []{SemanticType.getSemanticType(props.getProperty("Semantic_Type"))});
					}
					
					concept.setInitialized(true);
					
					return concept;
				}
			}
			
		}
		return null;
	}
	
	/**
	 * Process response.
	 *
	 * @param doc the doc
	 * @return the list
	 */
	private List<Concept> processResponse(Document doc){
		Element root = doc.getDocumentElement();
		NodeList list = root.getElementsByTagName(TAG_QUERY_RESPONSE);
		List<Concept> response = new ArrayList<Concept>();
		if(list.getLength() > 0){
			NodeList children = ((Element)list.item(0)).getChildNodes();
			for(int i=0;i<children.getLength();i++){
				if(children.item(i) instanceof Element && TAG_CLASS.equals(children.item(i).getNodeName())){
					Element e = (Element) children.item(i);
					response.add(convertConcept(e));
				}
			}
		}
		return response;
	}
	
	
	/**
	 * convert element to a map.
	 *
	 * @param element the element
	 * @return the map
	 */
	private Map processElement(Element element){
		Map<String,Object> content = new HashMap<String, Object>();
		content.put("name",element.getAttribute("name"));
		
		// go over fields and save content in the map
		NodeList ch = element.getChildNodes();
		for(int i=0;i<ch.getLength();i++){
			if(TAG_FIELD.equals(ch.item(i).getNodeName())){
				Element e = (Element) ch.item(i);
				String nm = e.getAttribute("name");
				
				// does this field has classes or text
				NodeList l = e.getElementsByTagName(TAG_CLASS);
				if(l.getLength() > 0){
					List<Map> clist = new ArrayList<Map>();
					for(int j=0;j<l.getLength();j++){
						clist.add(processElement((Element)l.item(j)));
					}
					content.put(nm,clist);
				}else if(!content.containsKey(nm)){
					content.put(nm,e.getTextContent().trim());
				}
			}
		}
		return content;
	}
	
	

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSourceFilter()
	 */
	public Source[] getSourceFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#getRelatedConcepts(edu.pitt.dbmi.nlp.noble.terminology.Concept, edu.pitt.dbmi.nlp.noble.terminology.Relation)
	 */
	public Concept[] getRelatedConcepts(Concept c, Relation r) throws TerminologyException {
		// TODO Auto-generated method stub
		return null;
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#getRelatedConcepts(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public Map getRelatedConcepts(Concept c) throws TerminologyException {
		// TODO Auto-generated method stub
		return null;
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#getSources()
	 */
	public Source[] getSources() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * lookup concept by code.
	 *
	 * @param cui the cui
	 * @return the concept
	 * @throws TerminologyException the terminology exception
	 */
	public Concept lookupConcept(String cui) throws TerminologyException {
		try{
			URL u = new URL(location+"/GetXML?query=org.LexGrid.concepts.Concept&" +
							"org.LexGrid.concepts.Concept[@_entityCode="+filter(cui)+
							"]&codingSchemeName="+filter(scheme));
			List<Concept> list = processResponse(parseXML(u.openStream()));
			return list.isEmpty()?null:list.get(0);
		}catch(Exception ex){
			throw new TerminologyException("Problem w/ lookup",ex);
		}
	}

	
	/**
	 * do search.
	 *
	 * @param text the text
	 * @return the concept[]
	 * @throws TerminologyException the terminology exception
	 */
	public Concept[] search(String text) throws TerminologyException {
		
		try{
			URL u = new URL(location+"/GetXML?query=Concept,Presentation,Text&Text[@_content=" +
					filter(text)+"]&codingSchemeName="+filter(scheme));
			List<Concept> list = processResponse(parseXML(u.openStream()));
			for(Concept c: list){
				c.setSearchString(text);
			}
			return list.toArray(new Concept [0]);
		}catch(Exception ex){
			throw new TerminologyException("Problem w/ lookup",ex);
		}
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#setSourceFilter(edu.pitt.dbmi.nlp.noble.terminology.Source[])
	 */
	public void setSourceFilter(Source[] srcs) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getDescription()
	 */
	public String getDescription() {
		return "Provides access to LexEVS instances through REST service calls";
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getFormat()
	 */
	public String getFormat() {
	return "REST";
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getLocation()
	 */
	public String getLocation() {
		return location;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getName()
	 */
	public String getName() {
		return "LexEVS REST";
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getURI()
	 */
	public URI getURI() {
		return URI.create(getLocation());
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getVersion()
	 */
	public String getVersion() {
		return "1.0";
	}

	/**
	 * Filter.
	 *
	 * @param str the str
	 * @return the string
	 */
	private String filter(String str){
		return str.replaceAll(" ","%20");
	}
	
	/**
	 * parse XML document.
	 *
	 * @param in the in
	 * @return the document
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private Document parseXML(InputStream in) throws IOException {
		Document document = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		//factory.setNamespaceAware(true);

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			//builder.setErrorHandler(new XmlErrorHandler());
			//builder.setEntityResolver(new XmlEntityResolver());
			document = builder.parse(in);
			
			// close input stream
			in.close();
		}catch(Exception ex){
			throw new IOException(ex.getMessage());
		}
		return document;
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception{
		//String server = "http://lexevsapi.nci.nih.gov/lexevsapi50/";
		String server = "http://lexevsapi60.nci.nih.gov/lexevsapi60/";
		//String server = "http://lexevsapi51.nci.nih.gov/lexevsapi51#NCI MetaThesaurus";
		Terminology term = new LexEVSRestTerminology(server);
		long time = System.currentTimeMillis();
		// ZFA_0001234 | C0025202
		System.out.println("--- lookup ---");
		Concept c = term.lookupConcept("C0025202");
		if(c != null){
			c.printInfo(System.out);
		}
		System.out.println("lookup time "+(System.currentTimeMillis()-time));
		
		System.out.println("--- search ---");
		
		time = System.currentTimeMillis();
		Concept [] cs = term.search("melanoma");
		for(Concept i: cs){
			i.printInfo(System.out);
		}
		System.out.println("lookup time "+(System.currentTimeMillis()-time));
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSemanticTypeFilter()
	 */
	public SemanticType[] getSemanticTypeFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#setSemanticTypeFilter(edu.pitt.dbmi.nlp.noble.terminology.SemanticType[])
	 */
	public void setSemanticTypeFilter(SemanticType[] srcs) {
		// TODO Auto-generated method stub
		
	}

}
