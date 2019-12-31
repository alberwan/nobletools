package edu.pitt.dbmi.nlp.noble.terminology.impl;

import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.EXACT_MATCH;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.MAX_SEARCH_HITS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.PROPERTIES_MATCH;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.SEARCH;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.getElementByTagName;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.getElementsByTagName;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.openURL;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.parseXML;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BClass;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BOntology;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalRepository;
import edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

/**
 * The Class BioPortalTerminology.
 */
public class BioPortalTerminology extends AbstractTerminology {
	private BOntology ontology;
	private BioPortalRepository repository = new BioPortalRepository();

	/**
	 * Instantiates a new bio portal terminology.
	 */
	public BioPortalTerminology(){
		super();
	}
	
	/**
	 * Instantiates a new bio portal terminology.
	 *
	 * @param ontology the ontology
	 */
	public BioPortalTerminology(BOntology ontology){
		super();
		this.ontology = ontology;
	}
	
	
	/**
	 * Sets the ontology.
	 *
	 * @param ontology the new ontology
	 */
	public void setOntology(BOntology ontology) {
		this.ontology = ontology;
	}

	/**
	 * Sets the ontology.
	 *
	 * @param name the new ontology
	 */
	public void setOntology(String name) {
		// repository.getOntology("NCI_Thesaurus");
		IOntology [] ont = repository.getOntologies(name);
		if(ont.length > 0)
			setOntology((BOntology) ont[0]);
	}

	/**
	 * converts IClass to Concept.
	 *
	 * @param obj the obj
	 * @return the concept
	 */
	protected Concept convertConcept(Object obj) {
		if (obj instanceof IClass)
			return ((IClass)obj).getConcept();
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSourceFilter()
	 */
	public Source[] getSourceFilter() {
		return getSources();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#getRelatedConcepts(edu.pitt.dbmi.nlp.noble.terminology.Concept, edu.pitt.dbmi.nlp.noble.terminology.Relation)
	 */
	public Concept[] getRelatedConcepts(Concept c, Relation r) throws TerminologyException {
		IClass cls = c.getConceptClass();
		if(r == Relation.BROADER){
			//IClass cls = ontology.getClass(c.getCode());
			if(cls != null){
				return convertConcepts(cls.getDirectSuperClasses());
			}
		}else if(r == Relation.NARROWER){
			//IClass cls = ontology.getClass(c.getCode());
			if(cls != null){
				return convertConcepts(cls.getDirectSubClasses());
			}
		}else if(r == Relation.SIMILAR){
			//IClass cls = ontology.getClass(c.getCode());
			if(cls != null){
				List<IClass> clses = new ArrayList<IClass>();
				for(IClass eq: cls.getEquivalentClasses()){
					if(!eq.isAnonymous()){
						clses.add(eq);
					}
				}
				return convertConcepts(clses);
			}
		}
		return new Concept [0];
		
		
		
		/*
		String n = c.getName().replaceAll(" ", "_");
		IClass cls = ontology.createClass(n);

		IClass[] clsResult = null;
		try {
			// get parents
			if (r.equals(Relation.BROADER))
				clsResult = cls.getDirectSuperClasses();
			// get children
			else if (r.equals(Relation.NARROWER))
				clsResult = cls.getDirectSubClasses();

		} catch (Exception ex) {
			throw new TerminologyException(ex.getMessage());
		}

		if (clsResult != null) {
			ArrayList<Concept> concepts = new ArrayList<Concept>();
			for (int i = 0; i < clsResult.length; i++) {
				concepts.add(convertConcept(clsResult[i]));
			}
			return concepts.toArray(new Concept[] {});
		} else
			return null;
		*/	
	}

	/**
	 * Convert concepts.
	 *
	 * @param clses the clses
	 * @return the concept[]
	 */
	private Concept [] convertConcepts(IClass [] clses){
		Concept [] concepts = new Concept[clses.length];
		for(int i=0;i<concepts.length;i++)
			concepts[i] = clses[i].getConcept();
		return concepts;
	}
	
	/**
	 * Convert concepts.
	 *
	 * @param clses the clses
	 * @return the concept[]
	 */
	private Concept [] convertConcepts(Collection<IClass> clses){
		Concept [] concepts = new Concept[clses.size()];
		int i=0;
		for(IClass cls: clses)
			concepts[i++] = cls.getConcept();
		return concepts;
	}
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#getRelatedConcepts(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public Map getRelatedConcepts(Concept c) throws TerminologyException {
		Map<Relation,List<Concept>> map = new HashMap<Relation,List<Concept>>();
		map.put(Relation.BROADER,Arrays.asList(getRelatedConcepts(c,Relation.BROADER)));
		map.put(Relation.NARROWER,Arrays.asList(getRelatedConcepts(c,Relation.NARROWER)));
		map.put(Relation.SIMILAR,Arrays.asList(getRelatedConcepts(c,Relation.SIMILAR)));
		return map;
	}

	/**
	 * get all ontologies in bioportal.
	 *
	 * @return the sources
	 */
	public Source[] getSources() {
		return iOntologiesToSource(repository.getOntologies());
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#lookupConcept(java.lang.String)
	 */
	public Concept lookupConcept(String cui) throws TerminologyException {
		if(ontology == null)
			return null;
		IResource cls = ontology.getResource(cui);
		return (cls != null && cls instanceof IClass)?((IClass)cls).getConcept():null;
	}

	/**
	 * According to the Bioportal 2.0 docs there should be "exactmatch" and
	 * "contains" only.
	 *
	 * @param text the text
	 * @param method the method
	 * @return the concept[]
	 * @throws TerminologyException the terminology exception
	 */
	public Concept[] search(String text, String method) throws TerminologyException {
		return (ontology != null)?ontology.search(text, method):searchAll(text, method);
	}

	/**
	 * The search result should be light- not BClass, but Concept. We can create
	 * BClasses later if needed
	 *
	 * @param text the text
	 * @param method the method
	 * @return the concept[]
	 * @throws TerminologyException the terminology exception
	 */
	public Concept[] searchAll(String text, String method) throws TerminologyException {
	    /*
		* ontologyids=<ontologyid>,<ontologyid>… - limits the search to specific ontologies (default: all ontologies)
	    * isexactmatch=[1/0] – match the entire concept name (default: 0)
	    * includeproperties=[1/0] – include attributes in the search (default: 0)
	    * pagesize=<pagesize> - the number of results to display in a single request (default: all)
	    * pagenum=<pagenum> - the page number to display (pages are calculated using <total results>/<pagesize>) (default: 1)
	    * maxnumhits=<maxnumhits> - the maximum number of top matching results to return (default: 1000)
	    * subtreerootconceptid=<uri-encoded conceptid> - narrow the search to concepts residing in a sub-tree, 
	    *  where the "subtreerootconceptid" is the root node. This feature requires a SINGLE <ontologyid> passed in using the "onotlogyids" parameter. 
		*/
		
		// create a URL
		String url = repository.getURL()+SEARCH+text+"/";
		
		// is it exact match or not
		url += "&isexactmatch="+((method.equalsIgnoreCase(EXACT_MATCH))?1:0);
		
		// include attributess
		url += "&includeproperties="+((method.equalsIgnoreCase(PROPERTIES_MATCH))?1:0);
		
		// set number of results
		url += "&maxnumhits="+MAX_SEARCH_HITS;
		
		// required field
		url += "&"+repository.getAPIKey();
		
		Document doc = parseXML(openURL(url));
		if(doc != null){
			Element results = getElementByTagName(doc.getDocumentElement(),"searchResultList");
			List<Concept> list = new ArrayList<Concept>();
			if(results != null){
				// get concept name from each search bean, rest of info is useless for now
				for(Element e: getElementsByTagName(results,"searchBean")){
					Element o = getElementByTagName(e,"ontologyDisplayLabel");
					Element n = getElementByTagName(e,"conceptIdShort");
					if(n != null && o != null){
						// get ontology uri
						String oname = o.getTextContent().trim();
						String ouri = BioPortalHelper.BIOPORTAL_URL+BioPortalHelper.deriveName(oname);
						
						// get resource uri
						String name = n.getTextContent().trim();
						//String uri =  ouri+"#"+name;
						//IClass cls = (IClass) repository.getResource(URI.create(uri));
						
						//if(cls == null){
						IOntology ont = repository.getOntology(URI.create(ouri));
						if(ont != null){
							IClass cls = new BClass((BOntology)ont,name);
							cls.getConcept().setTerminology(this);
							cls.getConcept().setSearchString(text);
							list.add(cls.getConcept());
						}
						//}
						
						//if(cls != null)
						//	list.add(cls.getConcept());
					}
				}
			}
			return list.toArray(new Concept [0]);
		}
		return new Concept [0];
	}
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#search(java.lang.String)
	 */
	public Concept[] search(String text) throws TerminologyException {
		Concept [] result =  search(text, "exactmatch");
		for(Concept c: result)
			c.setTerminology(this);
		return result;
	}

	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology#getSearchMethods()
	 */
	public String[] getSearchMethods() {
		return new String [] {BioPortalHelper.EXACT_MATCH,BioPortalHelper.CONTAINS_MATCH,BioPortalHelper.PROPERTIES_MATCH};
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#setSourceFilter(edu.pitt.dbmi.nlp.noble.terminology.Source[])
	 */
	public void setSourceFilter(Source[] srcs) {
		// NOOP
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getDescription()
	 */
	public String getDescription() {
		return ontology.getDescription();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getFormat()
	 */
	public String getFormat() {
		return ontology.getFormat();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getLocation()
	 */
	public String getLocation() {
		return ontology.getLocation();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getName()
	 */
	public String getName() {
		return "BioPortal";
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getURI()
	 */
	public URI getURI() {
		return URI.create(BioPortalRepository.DEFAULT_BIOPORTAL_URL);
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Describable#getVersion()
	 */
	public String getVersion() {
		return "2.0";
	}

	/**
	 * I ontologies to source.
	 *
	 * @param iontologies the iontologies
	 * @return the source[]
	 */
	private Source[] iOntologiesToSource(IOntology[] iontologies) {
		Source[] sources = new Source[iontologies.length];

		for (int i = 0; i < iontologies.length; i++)
			sources[i] = new Source(iontologies[i].getName());

		return sources;
	}

	
	/**
	 * get all root concepts. This makes sence if Terminology is in fact ontology
	 * that has heirchichal structure
	 *
	 * @return the root concepts
	 * @throws TerminologyException the terminology exception
	 */
	public Concept[] getRootConcepts() throws TerminologyException {
		IClass [] clss = ontology.getRootClasses();
		Concept [] list = new Concept [clss.length];
		for(int i=0;i<list.length;i++)
			list[i] = clss[i].getConcept();
		return list;
	}
	
	/**
	 * Gets the ontology.
	 *
	 * @return the ontology
	 */
	public IOntology getOntology(){
		return ontology;
	}
	
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String [] args) throws Exception{
		BioPortalTerminology term = new BioPortalTerminology();
		term.setOntology("NCIT");
		long time = System.currentTimeMillis();
		// ZFA_0001234 | C0025202
		System.out.println("--- lookup ---");
		Concept c = term.lookupConcept("C3224");
		if(c != null){
			c.printInfo(System.out);
		}
		
		System.out.println("lookup time "+(System.currentTimeMillis()-time));
		
		System.out.println("--- search ---");
		
		time = System.currentTimeMillis();
		for(String text: Arrays.asList("melanoma","melanoma")){
			System.out.println("- "+text+" -");
			Concept [] cs = term.search(text);
			for(Concept i: cs){
				i.printInfo(System.out);
			}
			System.out.println("--");
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
