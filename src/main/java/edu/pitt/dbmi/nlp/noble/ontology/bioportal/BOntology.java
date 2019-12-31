package edu.pitt.dbmi.nlp.noble.ontology.bioportal;

import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.ALL;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.CONCEPTS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.DISJOINT_CLASS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.EQUIVALENT_CLASS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.EXACT_MATCH;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.GET_VERSIONS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.HAS_PART;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.MAX_SEARCH_HITS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.ONTOLOGIES;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.ONTOLOGYIDS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.PART_OF;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.PROPERTIES_MATCH;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.ROOT;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.SEARCH;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.SUB_CLASS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.SUPER_CLASS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.TYPE_CLASS;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.deriveName;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.getElementByTagName;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.getElementsByTagName;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.openURL;
import static edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper.parseXML;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.ontology.DefaultResourceIterator;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyError;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IQuery;
import edu.pitt.dbmi.nlp.noble.ontology.IQueryResults;
import edu.pitt.dbmi.nlp.noble.ontology.IRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.ontology.IResourceIterator;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.LogicExpression;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

/**
 * this class represents BioPortal ontology.
 *
 * @author Eugene Tseytlin
 */
public class BOntology extends BResource implements IOntology, Terminology {
	private static final String LAST_PROCESSED_PAGE = "LAST_PROCESSED_PAGE";
	private static final String LAST_PAGE_COUNT = "LAST_PAGE_COUNT";
	private static final String PROPERTIES_FILE_LOCATION = "PROPERTIES_FILE_LOCATION";
	private static final String TEMPORARY_ONTOLOGY_LOCATION = "TEMPORARY_ONTOLOGY_LOCATION";
	
	private BioPortalRepository repository;
	private Set<IClass> roots;
	private Map<String,BClass> classMap;
	private Map<String, BProperty> propertyMap; 
	private Map<String, BOntology> ontologyMap; 
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private boolean loaded;
	private long time;
	
	public static final String ONTOLOGY_PAGE_SIZE = "Page Size";
	public static final String ONTOLOGY_PAGE_URL = "Page URL";
	public static final String ONTOLOGY_PAGE_COUNT = "Page Count";
	public static final String ONTOLOGY_PROCESSED_PAGE = "Processed Page";
	public static final String ONTOLOGY_PROCESSED_CLASS = "Processed Class";
	
	public static final String ONTOLOGY_CLASS_COUNT = "Class Count";
	public static final String ONTOLOGY_LOAD_STAGE = "Ontology Loading Stage";
	public static final String ONTOLOGY_LOAD_STAGE_GETALLCLASSES = "Getting All Classes";
	public static final String ONTOLOGY_LOAD_STAGE_BUILDHIERARCHY = "Building Hierarchy";
	
	public static final int BIOPORTAL_PAGE_SIZE = 500;
	
	/**
	 * create new BioPortal ontology.
	 *
	 * @param r the r
	 * @param id the id
	 */
	BOntology(BioPortalRepository r, String id) {
		super();
		setRepository(r);
		setOntology(this);
		properties.setProperty("id",id);
		
		// setup url
		properties.setProperty("location",r.getURL()+CONCEPTS+getId()+"/"+getId());
		
		// init maps
		classMap = new HashMap<String, BClass>();
		propertyMap = new HashMap<String,BProperty>();
	}
	
	/**
	 * create new BioPortal ontology.
	 *
	 * @param r the r
	 * @param elem the elem
	 */
	public BOntology(BioPortalRepository r, Element elem) {
		super();
		setRepository(r);
		setOntology(this);
		// load element
		load(elem);
		
		// init maps
		classMap = new HashMap<String, BClass>();
		propertyMap = new HashMap<String,BProperty>();
	}

	/**
	 * load ontology content.
	 *
	 * @param elem the elem
	 */
	public void load(Element elem){
		// pull in all ontology data
		NodeList list = elem.getChildNodes();
		for(int i=0;i<list.getLength();i++){
			if(list.item(i) instanceof Element){
				Element e = (Element) list.item(i);
				String text = e.getTextContent();
				// if text content, then simply add it
				// if inside there is an array of children
				// else just put in an empty property
				if(text != null && text.trim().length() > 0){
					properties.put(e.getTagName(),text.trim());
				}else if(e.getChildNodes().getLength() > 0){
					NodeList l = e.getChildNodes();
					List<String> content = new ArrayList<String>();
					for(int j=0;j<l.getLength();j++){
						if(l.item(j) instanceof Element){
							content.add(l.item(j).getTextContent().trim());
						}
					}
					properties.put(e.getTagName(),content);
				}else{
					properties.put(e.getTagName(),"");
				}
			}
		}
		// now set some common elements in standard way
		if(properties.containsKey("description")){
			addComment((String) properties.get("description"));
		}
		if(properties.containsKey("name")){
			String name = (String) properties.get("name");
			setName(name);
			addLabel(name);
			String acronym = "";
			if(properties.containsKey("acronym")){
				acronym = (String) properties.get("acronym");
				properties.setProperty("ontologyId",acronym);
				acronym = " ("+acronym+")";
			}
			addComment(name+acronym);
		}
		if(properties.containsKey("versionNumber")){
			addVersion((String) properties.get("versionNumber"));
		}
		properties.setProperty("uri",BioPortalHelper.BIOPORTAL_URL+properties.getProperty("ontologyId"));
		properties.setProperty("location",getId());
		properties.setProperty("namespace",getURI()+"#");
		properties.setProperty("prefix","");
	}
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.bioportal.BResource#getVersion()
	 */
	public String getVersion(){
		loadMetaInfo();
		return super.getVersion();
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.bioportal.BResource#getFormat()
	 */
	public String getFormat() {
		loadMetaInfo();
		return super.getFormat();
	}

	/**
	 * Load meta info.
	 */
	public void loadMetaInfo(){
		// if we got a format, then it is OK
		if(super.getFormat().length() > 0)
			return;
		
		// get document
		Document doc = parseXML(openURL(getLocation()+"/latest_submission?"+getAPIKey()));
		if(doc != null){
			// add description
			Element e = BioPortalHelper.getElementByTagName(doc.getDocumentElement(),"description");
			if(e != null){
				properties.setProperty("description",e.getTextContent().trim());
				for(String s: getComments()){
					removeComment(s);
				}
				addComment(properties.getProperty("description"));
			}
			// add format
			e = BioPortalHelper.getElementByTagName(doc.getDocumentElement(),"version");
			if(e != null){
				properties.setProperty("version",e.getTextContent().trim());
				addVersion(properties.getProperty("version"));
			}
			// get released format
			e = BioPortalHelper.getElementByTagName(doc.getDocumentElement(),"hasOntologyLanguage");
			if(e != null){
				properties.setProperty("format",e.getTextContent().trim());
			}
			// add format
			e = BioPortalHelper.getElementByTagName(doc.getDocumentElement(),"released");
			if(e != null){
				properties.setProperty("released",e.getTextContent().trim());
			}
		}
	}
	
	
	/**
	 * Register class.
	 *
	 * @param cls the cls
	 */
	public void registerClass(BClass cls){
		// do not replace existing classes, they maybe better defined
		if(!classMap.containsKey(cls.getName()))
			classMap.put(cls.getName(),cls);
	}
	
	/**
	 * Unregister class.
	 *
	 * @param cls the cls
	 */
	public void unregisterClass(BClass cls){
		classMap.remove(cls.getName());
	}
	
	
	/**
	 * add listener to listen to misc ontology events.
	 *
	 * @param listener the listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener){
		pcs.addPropertyChangeListener(listener);
	}
	
	
	/**
	 * remove listener to listen to misc ontology events.
	 *
	 * @param listener the listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener){
		pcs.removePropertyChangeListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#addImportedOntology(edu.pitt.dbmi.nlp.noble.ontology.IOntology)
	 */
	public void addImportedOntology(IOntology o) throws IOntologyException {
		throw new IOntologyException("BioPortal is read-only");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#createLogicExpression(int, java.lang.Object)
	 */
	public ILogicExpression createLogicExpression(int type, Object param) {
		if (param instanceof Collection)
			return new LogicExpression(type, (Collection) param);
		else if (param instanceof Object[])
			return new LogicExpression(type, (Object[]) param);
		else
			return new LogicExpression(type, param);
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#createLogicExpression()
	 */
	public ILogicExpression createLogicExpression() {
		return new LogicExpression(ILogicExpression.EMPTY);
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#createProperty(java.lang.String, int)
	 */
	public IProperty createProperty(String name, int type) {
		BProperty p = new BProperty();
		p.setName(name);
		p.setOntology(this);
		propertyMap.put(name,p);
		return p;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#createRestriction(int)
	 */
	public IRestriction createRestriction(int type) {
		throw new IOntologyError("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.bioportal.BResource#dispose()
	 */
	public void dispose() {
		classMap = null;
		propertyMap = null;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#executeQuery(edu.pitt.dbmi.nlp.noble.ontology.IQuery)
	 */
	public IQueryResults executeQuery(IQuery query) {
		throw new IOntologyError("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getAllClasses()
	 */
	public IResourceIterator getAllClasses() {
		return new DefaultResourceIterator(classMap.values().iterator());
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getAllProperties()
	 */
	public IResourceIterator getAllProperties() {
		throw new IOntologyError("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getAllResources()
	 */
	public IResourceIterator getAllResources() {
		return new DefaultResourceIterator(classMap.values().iterator());
	}

	/**
	 * get existing class.
	 *
	 * @param name the name
	 * @return the class
	 */
	public IClass getClass(String name) {
		// if we got a uri as a code
		int i = name.lastIndexOf("#");
		if(i > -1)
			name = name.substring(i+1);
		// fetch class
		IClass cls = classMap.get(name);
		
		// try some derivative of a name
		if(cls == null){
			cls = classMap.get(deriveName(name));
		}

		// if class still null, then look it up and load it
		/*
		if (cls == null) {
			BClass bc = new BClass(this,name);
			bc.load();
			return (bc.isLoaded())?bc:null;
		}*/
		
		return cls;
	}
	
	/**
	 * download ontology.
	 *
	 * @return the download URL
	 */
	public URL getDownloadURL(){
		String url = null;
		try {
			url = getLocation()+BioPortalHelper.DOWNLOAD+"?"+getAPIKey();
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new IOntologyError("Malformed URL: "+url,e);
		}
	}
	

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getImportedOntologies()
	 */
	public IOntology[] getImportedOntologies() {
		return new IOntology[0];
	}

	/**
	 * no instances supported.
	 *
	 * @param name the name
	 * @return single instance of BOntology
	 */
	public IInstance getInstance(String name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getMatchingResources(edu.pitt.dbmi.nlp.noble.ontology.IProperty, java.lang.Object)
	 */
	public IResourceIterator getMatchingResources(IProperty p, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getMatchingResources(java.lang.String)
	 */
	public IResourceIterator getMatchingResources(String regex) {
		List list = new ArrayList();
		try{
			for(Concept c: search(regex)){
				list.add(c.getConceptClass());
			}
		}catch(TerminologyException ex){
			ex.printStackTrace();
		}
		return new DefaultResourceIterator(list.listIterator());
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getProperty(java.lang.String)
	 */
	public IProperty getProperty(String name) {
		// convert a name to valid property
		String oname = name;
		name = deriveName(name);
		BProperty p = propertyMap.get(name);
		if(p == null){
			p = (BProperty) createProperty(name,IProperty.ANNOTATION_DATATYPE);
			p.setOrignalName(oname);
			propertyMap.put(name,p);
		}
		return p;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.bioportal.BResource#getProperties()
	 */
	public IProperty [] getProperties(){
		return propertyMap.values().toArray(new IProperty [0]);
	}
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getRepository()
	 */
	public IRepository getRepository() {
		return repository;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getResource(java.lang.String)
	 */
	public IResource getResource(String name) {
		IClass c = getClass(name);
		// search if not in caseh, can't rely on lookup
		// cause class name and location are not always the same things
		if(c == null){
			
			return new BClass(ontology,name);
			/*try{
				Concept [] result = search(BioPortalHelper.getName(name));
				return result.length > 0?result[0].getConceptClass():null;
			}catch(Exception ex){}*/
		}
		return c;
	}
	
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getRoot()
	 */
	public IClass getRoot() {
		throw new IOntologyError("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#getRootClasses()
	 */
	public IClass[] getRootClasses() {
		if(roots == null){
			roots = BioPortalHelper.getClassList(this, getId()+CONCEPTS+ROOT+"?"+repository.getAPIKey());
		}
		return roots.toArray(new IClass [0]);
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#hasResource(java.lang.String)
	 */
	public boolean hasResource(String path) {
		return classMap.containsKey(path) || propertyMap.containsKey(path);
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#isLoaded()
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#isModified()
	 */
	public boolean isModified() {
		return false;
	}

	
	/**
	 * load an entire ontology.
	 *
	 * @throws IOntologyException the i ontology exception
	 */
	public void load() throws IOntologyException {
		if(isLoaded())
			return;
		
		//http://rest.bioontology.org/bioportal/concepts/40644/all?pagesize=50&pagenum=1
		
		// pull all classes into 
		int pagesize = BIOPORTAL_PAGE_SIZE, i=1;
		int pagecount = 1,total = 0;
		pcs.firePropertyChange(ONTOLOGY_LOAD_STAGE,null,ONTOLOGY_LOAD_STAGE_GETALLCLASSES);
		pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Loading "+getName()+" ...");
		do {
			Document doc = parseXML(openURL(repository.getURL()+CONCEPTS+"/"+getId()+ALL+"?pagesize="+pagesize+"&pagenum="+i+"&"+repository.getAPIKey()));
			if(doc != null){
				// if pagecount is in default state, figure the pagecount
				if(pagecount == 1){
					Element page = getElementByTagName(doc.getDocumentElement(),"page");
					Element npages = getElementByTagName(page,"numPages");
					pagecount = Integer.parseInt(npages.getTextContent().trim());
					Element ntotal = getElementByTagName(page,"numResultsTotal");
					total = Integer.parseInt(ntotal.getTextContent().trim());
					
					
					pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Page Size: "+pagesize+", Total Number of Classes: "+total+" ...");
					pcs.firePropertyChange(ONTOLOGY_PAGE_COUNT,null,""+pagecount);
					pcs.firePropertyChange(ONTOLOGY_PAGE_SIZE,null,""+pagesize);
					pcs.firePropertyChange(ONTOLOGY_CLASS_COUNT,null,""+total);
					
					page = null;
					npages = null;
					ntotal = null;
				}
				
				// now go over result set
				Element result = getElementByTagName(doc.getDocumentElement(),"classBeanResultList");
				for(Element e: getElementsByTagName(result,"classBean")){
					// if we init the class it gets registered in the constructor
					new BClass(this,e);
				}
				result = null;
				doc = null;
				System.gc();
				pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Processed Page "+i+"/"+pagecount+" ...");
				pcs.firePropertyChange(ONTOLOGY_PROCESSED_PAGE,null,new Integer(i));
			}
		}while(++i <= pagecount);
	
		
		pcs.firePropertyChange(ONTOLOGY_LOAD_STAGE,null,ONTOLOGY_LOAD_STAGE_BUILDHIERARCHY);
		pcs.firePropertyChange(ONTOLOGY_LOADED_EVENT,null,"Creating Hierarchy ...");
		
		// now that we have all classes, build a tree, since classes only have the superclasses set
		IResourceIterator it = getAllClasses();
		while(it.hasNext()){
			BClass cls = (BClass) it.next();
			// setup direct subclasses
			for(String clsName: cls.getList(SUPER_CLASS)){
				BClass parent = (BClass) getClass(clsName);
				if(parent != null){
					parent.getList(SUB_CLASS).add(cls.getName());
				}
			}
		}
		
		
		loaded = true;
	}

	/**
	 * get import properties for the target ontology.
	 *
	 * @param ont the ont
	 * @return the properties
	 */
	private Properties loadImportProperties(IOntology ont){
		Properties p = new Properties();
		// get temp directory
		File temp =  new File(System.getProperty("java.io.tmpdir"));;
		/*if(ont.getRepository() instanceof ProtegeRepository){
			temp = new File(((ProtegeRepository)ont.getRepository()).getDatabaseRepositoryDirectory());
		}else{
			temp = new File(System.getProperty("java.io.tmpdir"));
		}*/
		File props = new File(temp,ont.getName()+".properties");
		FileInputStream fos = null;
		try {
			if(props.exists()){
				fos =  new FileInputStream(props);
				p.load(fos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		// save location of the file
		p.setProperty(PROPERTIES_FILE_LOCATION,props.getAbsolutePath());
		
		
		// now check if default ontology location does not exists
		if(!(new File(ont.getLocation())).exists()){
			String tablePrefix = null;
			//if(ont.getRepository() instanceof ProtegeRepository)
			//	tablePrefix = ((ProtegeRepository)ont.getRepository()).getOntologyPrefix();
			
			// if location is not already a table name, then set location to a file
			if(tablePrefix == null || !ont.getLocation().startsWith(tablePrefix)){
				//if(ont instanceof POntology)
				//	((POntology)ont).setFileLocation(temp);
			}
		}
		
		// save temp file location
		p.setProperty(TEMPORARY_ONTOLOGY_LOCATION,ont.getLocation());
		
		return p;
	}
	
	/**
	 * store import properties.
	 *
	 * @param ont the ont
	 * @param p the p
	 */
	private void storeImportProperties(IOntology ont, Properties p){
		// get file location
		File props = new File(p.getProperty(PROPERTIES_FILE_LOCATION));
		FileOutputStream fos = null;
		try{
			// store last processed page
			fos = new FileOutputStream(props);
			p.store(fos,"");
			
			// save temporary ontology
			ont.save();
		}catch(Exception e){
			e.printStackTrace();
		} finally{
			if(fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * clean temporary files.
	 *
	 * @param p the p
	 */
	private void cleanImportProperties(Properties p){
		// remove temp ontology
		if(p.containsKey(TEMPORARY_ONTOLOGY_LOCATION)){
			String fileName = p.getProperty(TEMPORARY_ONTOLOGY_LOCATION);
			URI uri = null;
			try{
				// is it really URI, if not then it don't delete anything
				uri = new URI(fileName);
			}catch(Exception ex){}
			if(uri != null){
				try{
					File f = new File(uri);
					if(f.exists())
						f.delete();
				}catch(Exception ex){
					
				}
			}
		}
		//remove prop file
		if(p.containsKey(PROPERTIES_FILE_LOCATION))
			(new File(p.getProperty(PROPERTIES_FILE_LOCATION))).delete();
	}
	
	
	/**
	 * try to do a memory efficiently copy this ontology into a target ontology
	 * This method goes over BioPortal paged access and copies stuff directly into it.
	 *
	 * @param target the target
	 */
	public void copy(IOntology target){
		
		//Load the properties file, to check if this import can be resumed.
		Properties importProps = loadImportProperties(target);
		
		// pull all classes into 
		int pagesize = BIOPORTAL_PAGE_SIZE, i=1;
		int pagecount = 1,total = 0;
		
		pcs.firePropertyChange(ONTOLOGY_LOAD_STAGE,null,ONTOLOGY_LOAD_STAGE_GETALLCLASSES);
		pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Loading "+getName()+" ...");
		
		// load ontology metadata
		for(String s : getLabels())
			target.addLabel(s);
		for(String s : getComments())
			target.addComment(s);
		target.addVersion(getVersion());
		
		// if loaded then get all classes
		if(isLoaded()){
			IResourceIterator it = getAllClasses();
			while(it.hasNext()){
				BClass b = (BClass) it.next();
				// copy content
				copyClass(b,target);
				
				// do not dispose of content
			}
		}else{
			
			// get last processed page
			int lastPageCount = 0;
			if(importProps.containsKey(LAST_PROCESSED_PAGE)){
				i = Integer.parseInt(importProps.getProperty(LAST_PROCESSED_PAGE,"1"))+1;
				lastPageCount = Integer.parseInt(importProps.getProperty(LAST_PAGE_COUNT,"1"));
				
				// if next page is greater then pagecount, it looks like we are done already
				if(i > lastPageCount)
					return;
			}
				
			do {
				pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Requesting Page "+i+" ...");
				long time = System.currentTimeMillis();
				Document doc = parseXML(openURL(repository.getURL()+CONCEPTS+"/"+getId()+ALL+"?pagesize="+pagesize+"&pagenum="+i+"&"+repository.getAPIKey()));
				if(doc != null){
					// if pagecount is in default state, figure the pagecount
					if(pagecount == 1){
						Element page = getElementByTagName(doc.getDocumentElement(),"page");
						Element npages = getElementByTagName(page,"numPages");
						pagecount = Integer.parseInt(npages.getTextContent().trim());
						Element ntotal = getElementByTagName(page,"numResultsTotal");
						total = Integer.parseInt(ntotal.getTextContent().trim());
						pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Page Size: "+pagesize+", Total Number of Classes: "+total+" ...");
						pcs.firePropertyChange(ONTOLOGY_PAGE_COUNT,null,new Integer(pagecount));
						pcs.firePropertyChange(ONTOLOGY_PAGE_SIZE,null,new Integer(pagesize));
						pcs.firePropertyChange(ONTOLOGY_CLASS_COUNT,null,new Integer(total));
						
						page = null;
						npages = null;
						ntotal = null;
					}
					
					// if last page count does not match current, we have a problem
					if(lastPageCount > 0 && lastPageCount != pagecount){
						// we should ignore results and start again
						i = 1;
						continue;
					}
				
					// now go over result set
					Element result = getElementByTagName(doc.getDocumentElement(),"classBeanResultList");
					for(Element e: getElementsByTagName(result,"classBean")){
						// get top level class that was returned
						BClass b = new BClass(this,e);
						
						// check resource type (don't do properties etc...)
						if(TYPE_CLASS.equalsIgnoreCase(b.getResourceType())){
							// copy content
							copyClass(b,target);
						}
						
						// dispose to save memory
						disposeClass(b);
					}
					
					// remove any outstanding classes (should not be many)
					for(BClass b: new ArrayList<BClass>(classMap.values())){
						b.dispose();
					}
					classMap.clear();
					result = null;
					doc = null;
					target.flush();
					System.gc();
					pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Processed Page "+i+"/"+pagecount+"  ("+(System.currentTimeMillis()-time)+" ms) ...");
					pcs.firePropertyChange(ONTOLOGY_PROCESSED_PAGE,null,new Integer(i));
					
					
					// store the page that was just processed
					importProps.setProperty(LAST_PROCESSED_PAGE, ""+i);
					importProps.setProperty(LAST_PAGE_COUNT, ""+pagecount);
					storeImportProperties(target,importProps);
				}
				else{
					throw new IOntologyError("Bioportal did what it does best");
				}
			}while(++i <= pagecount);
		}
		
	
		pcs.firePropertyChange(ONTOLOGY_LOAD_STAGE,null,ONTOLOGY_LOAD_STAGE_BUILDHIERARCHY);
		pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Creating Hierarchy ...");
		
		
		// create some important properties
		IProperty partOf = target.getProperty(PART_OF);
		if(partOf == null){
			partOf = target.createProperty(PART_OF,IProperty.OBJECT);
			partOf.setDomain(new IClass [] {target.getRoot()});
			partOf.setRange(new IClass [] {target.getRoot()});
		}
		
		IProperty hasPart = target.getProperty(HAS_PART);
		if(hasPart == null){
			hasPart = target.createProperty(HAS_PART,IProperty.OBJECT);
			hasPart.setDomain(new IClass [] {target.getRoot()});
			hasPart.setRange(new IClass [] {target.getRoot()});
		}
		// make them inverses of eachother
		hasPart.setInverseProperty(partOf);
		partOf.setInverseProperty(hasPart);
		
		// now that we have all classes, build a tree, since classes only have the superclasses set
		IResourceIterator it = target.getAllClasses();
		while(it.hasNext()){
			IClass cls = (IClass) it.next();
			
			long time = System.currentTimeMillis();
			
			// now copy equivalent classes classes
			IProperty p = target.getProperty(EQUIVALENT_CLASS);
			if(p != null){
				for(IClass sibling: getClassList(target,cls.getPropertyValues(p))){
					cls.addEquivalentClass(sibling);
				}
				// remove temporary property
				cls.removePropertyValues(p);
			}
			
			// setup direct subclasses and superclasses
			p = target.getProperty(SUPER_CLASS);
			if(p != null){
				for(IClass parent: getClassList(target,cls.getPropertyValues(p))){
					if(!cls.hasDirectSuperClass(parent)){  
						// add string as a true superclass
						cls.addSuperClass(parent);
						// if superclass is not root, then add this class
						// as a child and remove root as parent
						if(!parent.equals(target.getRoot())){
							parent.addSubClass(cls);
							
							// since everything was added as root, cleanup
							cls.removeSuperClass(target.getRoot());
						}
					}			
				}
				
				// remove temporary property
				cls.removePropertyValues(p);
			}
			
			// setup direct subclasses and superclasses
			p = target.getProperty(SUB_CLASS);
			if(p != null){
				for(IClass child: getClassList(target,cls.getPropertyValues(p))){
					if(!cls.hasDirectSubClass(child)){
						// add string as a true superclass
						cls.addSubClass(child);
						child.addSuperClass(cls);
						
						// if superclass is not root, then add this class
						// as a child and remove root as parent
						if(child.hasDirectSuperClass(target.getRoot())){
							// since everything was added as root, cleanup
							child.removeSuperClass(target.getRoot());
						}
					}
				}
				
				// remove temporary property
				cls.removePropertyValues(p);
			}
			
			
			// now copy disjoint classes
			p = target.getProperty(DISJOINT_CLASS);
			if(p != null){
				for(IClass sibling: getClassList(target,cls.getPropertyValues(p))){
					cls.addDisjointClass(sibling);
				}
				// remove temporary property
				cls.removePropertyValues(p);
			}
		
			// now copy other relationships relationships
			for(String relation: new String [] {PART_OF, HAS_PART}){
				p = target.getProperty("annotation_"+relation);
				if(p != null){
					for(IClass sibling: getClassList(target,cls.getPropertyValues(p))){
						
						IRestriction r = target.createRestriction(IRestriction.SOME_VALUES_FROM);
						r.setProperty(target.getProperty(relation));
						r.setParameter(sibling.getLogicExpression());
						cls.addNecessaryRestriction(r);
					}
					// remove temporary property
					cls.removePropertyValues(p);
				}
			}
			
			
			
			// fire event every 1000 actions
			if(it.getCount() % pagesize == 1){
				pcs.firePropertyChange(ONTOLOGY_PROCESSED_CLASS,null,new Integer(it.getCount()));
				pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Processing Hierarchy "+it.getCount()+"/"+total+" ...");
			}
			
			//System.out.println(".......... processing hierarchy "+cls.getName()+" "+(System.currentTimeMillis()-time));
		}

		
		pcs.firePropertyChange(ONTOLOGY_PROCESSED_CLASS,null,new Integer(it.getCount()));
		pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Processing Hierarchy "+it.getCount()+"/"+total+" ...");

		// remove helper annotations
		for(String p: new String []{SUB_CLASS,SUPER_CLASS,DISJOINT_CLASS,EQUIVALENT_CLASS,
									"annotation_"+PART_OF,"annotation_"+HAS_PART}){
			disposeProperty(target,p);
		}
		
		
		// now clean temporary files
		cleanImportProperties(importProps);
	}
	

	/**
	 * try to do a memory efficiently copy this ontology into a target ontology
	 * This method goes over BioPortal paged access and copies stuff directly into it.
	 *
	 * @param target the target
	 */
	public void copy2(IOntology target){
		
		//Load the properties file, to check if this import can be resumed.
		Properties importProps = loadImportProperties(target);
		
		// pull all classes into 
		int pagesize = BIOPORTAL_PAGE_SIZE, i=1;
		int pagecount = 1,total = 0;
		
		pcs.firePropertyChange(ONTOLOGY_LOAD_STAGE,null,ONTOLOGY_LOAD_STAGE_GETALLCLASSES);
		pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Loading "+getName()+" ...");
		
		// load ontology metadata
		for(String s : getLabels())
			target.addLabel(s);
		for(String s : getComments())
			target.addComment(s);
		target.addVersion(getVersion());
		
		// if loaded then get all classes
		if(isLoaded()){
			IResourceIterator it = getAllClasses();
			while(it.hasNext()){
				BClass b = (BClass) it.next();
				
				// copy content
				copyClass2(b,target.getRoot());
				
				// do not dispose of content
			}
		}else{
			
			// create some important properties
			IProperty partOf = target.getProperty(PART_OF);
			if(partOf == null){
				partOf = target.createProperty(PART_OF,IProperty.OBJECT);
				partOf.setDomain(new IClass [] {target.getRoot()});
				partOf.setRange(new IClass [] {target.getRoot()});
			}
			
			IProperty hasPart = target.getProperty(HAS_PART);
			if(hasPart == null){
				hasPart = target.createProperty(HAS_PART,IProperty.OBJECT);
				hasPart.setDomain(new IClass [] {target.getRoot()});
				hasPart.setRange(new IClass [] {target.getRoot()});
			}
			// make them inverses of eachother
			hasPart.setInverseProperty(partOf);
			partOf.setInverseProperty(hasPart);
			
			
			// if we have last processed page
			int lastPageCount = 0;
			if(importProps.containsKey(LAST_PROCESSED_PAGE)){
				i = Integer.parseInt(importProps.getProperty(LAST_PROCESSED_PAGE,"1"))+1;
				lastPageCount = Integer.parseInt(importProps.getProperty(LAST_PAGE_COUNT,"1"));
				
				// if next page is greater then pagecount, it looks like we are done already
				if(i > lastPageCount)
					return;
			}
				
			do {
				pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Requesting Page "+i+" ...");
				pcs.firePropertyChange(ONTOLOGY_PAGE_URL,null,repository.getURL()+CONCEPTS+"/"+getId()+ALL+"?pagesize="+pagesize+"&pagenum="+i);
				long time = System.currentTimeMillis();
				Document doc = parseXML(openURL(repository.getURL()+CONCEPTS+"/"+getId()+ALL+"?pagesize="+pagesize+"&pagenum="+i+"&"+repository.getAPIKey()));
				if(doc != null){
					// if pagecount is in default state, figure the pagecount
					if(pagecount == 1){
						Element page = getElementByTagName(doc.getDocumentElement(),"page");
						Element npages = getElementByTagName(page,"numPages");
						pagecount = Integer.parseInt(npages.getTextContent().trim());
						Element ntotal = getElementByTagName(page,"numResultsTotal");
						total = Integer.parseInt(ntotal.getTextContent().trim());
						pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Page Size: "+pagesize+", Total Number of Classes: "+total+" ...");
						pcs.firePropertyChange(ONTOLOGY_PAGE_COUNT,null,new Integer(pagecount));
						pcs.firePropertyChange(ONTOLOGY_PAGE_SIZE,null,new Integer(pagesize));
						pcs.firePropertyChange(ONTOLOGY_CLASS_COUNT,null,new Integer(total));
						
						page = null;
						npages = null;
						ntotal = null;
					}
					
					// if last page count does not match current, we have a problem
					if(lastPageCount > 0 && lastPageCount != pagecount){
						// we should ignore results and start again
						i = 1;
						continue;
					}
					
					// now go over result set
					Element result = getElementByTagName(doc.getDocumentElement(),"classBeanResultList");
					for(Element e: getElementsByTagName(result,"classBean")){
						// get top level class that was returned
						BClass b = new BClass(this,e);
						
						// check resource type (don't do properties etc...)
						if(TYPE_CLASS.equalsIgnoreCase(b.getResourceType())){
							// copy content
							copyClass2(b,target.getRoot());
						}
						
						// dispose to save memory
						disposeClass(b);
					}
					
					// remove any outstanding classes (should not be many)
					for(BClass b: new ArrayList<BClass>(classMap.values())){
						b.dispose();
					}
					classMap.clear();
					result = null;
					doc = null;
					target.flush();
					System.gc();
					pcs.firePropertyChange(ONTOLOGY_LOADING_EVENT,null,"Processed Page "+i+"/"+pagecount+"  ("+(System.currentTimeMillis()-time)+" ms) ...");
					pcs.firePropertyChange(ONTOLOGY_PROCESSED_PAGE,null,new Integer(i));
					
					
					// store the page that was just processed
					importProps.setProperty(LAST_PROCESSED_PAGE, ""+i);
					importProps.setProperty(LAST_PAGE_COUNT, ""+pagecount);
					storeImportProperties(target,importProps);
				}
				else{
					throw new IOntologyError("Bioportal did what it does best");
				}
			}while(++i <= pagecount);
		}
		
	
		pcs.firePropertyChange(ONTOLOGY_LOAD_STAGE,null,ONTOLOGY_LOAD_STAGE_BUILDHIERARCHY);
		
		// now clean temporary files
		cleanImportProperties(importProps);
	}
	
	
	/**
	 * dispose of helper property.
	 *
	 * @param ont the ont
	 * @param name the name
	 */
	private void disposeProperty(IOntology ont, String name){
		if(ont.hasResource(name)){
			ont.getProperty(name).delete();
		}
	}
	
	
	/**
	 * dispose of class and dependencies.
	 *
	 * @param cls the cls
	 */
	private void disposeClass(BClass cls){
		// clear superclasses
		for(String r : new String [] {SUPER_CLASS, SUB_CLASS}){
			for(String name : cls.getList(r)){
				BClass c = classMap.get(name);
				if(c != null){
					unregisterClass(c);
					c.dispose();
				}
			}
		}
		unregisterClass(cls);
		cls.dispose();
	}
	
	
	/**
	 * get list of classes from list of objects.
	 *
	 * @param ont the ont
	 * @param obj the obj
	 * @return the class list
	 */
	private List<IClass> getClassList(IOntology ont, Object [] obj){
		List<IClass> list = new ArrayList<IClass>();
		for(Object s: obj){
			String name = (String) s;
			// check for valid names
			if(name.matches("[\\w-]+")){
				IClass cls = ont.getClass(name);
				if(cls != null)
					list.add(cls);
			}
		}
		return list;
	}
	
	
	/**
	 * copy class content to target class.
	 *
	 * @param src the src
	 * @param target the target
	 */
	private void copyClass(BClass src, IOntology target){
		// if no such resource, then add it
		if(!target.hasResource(src.getName())){
			long time = System.currentTimeMillis();
			
			IClass dst = target.createClass(src.getName());
		
			// copy superficial stuff
			for(String lbl: src.getLabels())
				dst.addLabel(lbl);
			for(String com: src.getComments())
				dst.addComment(com);
			if(src.getVersion() != null)
				dst.addVersion(src.getVersion());
		
			// copy properties
			for(IProperty sp : src.getProperties()){
				// skip PartOf, and has_PART
				if(HAS_PART.equals(sp.getName()) || PART_OF.equals(sp.getName()))
					continue;
				
				IProperty dp = target.getProperty(sp.getName());
				
				// create unknown property for the first time
				if(dp == null && !target.hasResource(sp.getName())){
					dp = target.createProperty(sp.getName(),IProperty.ANNOTATION_DATATYPE);
					dp.setRange(new String [0]);
				}
				
				// copy string values
				dst.setPropertyValues(dp,src.getPropertyValues(sp));
				
			}
			
			// copy class information into special fields
			copyClassRelations(src,dst,SUPER_CLASS);
			copyClassRelations(src,dst,SUB_CLASS);
			copyClassRelations(src,dst,"annotation_"+PART_OF);
			copyClassRelations(src,dst,"annotation_"+HAS_PART);
			copyClassRelations(src,dst,DISJOINT_CLASS);
			copyClassRelations(src,dst,EQUIVALENT_CLASS);
			
			//if(src.getList(SUPER_CLASS).isEmpty()){
			//	System.out.println(src+" | "+src.getResourceProperties());
			//}
			//System.out.println(".......... processing "+dst.getName()+" "+(System.currentTimeMillis()-time));
		}	
	}
	
	/**
	 * copy class content to target class.
	 *
	 * @param src the src
	 * @param parent the parent
	 * @return the class 2
	 */
	private IClass getClass2(BClass src,IClass parent){
		IClass dst = null;
		IOntology target = parent.getOntology();
		
		// if no such resource, then add it
		if(!target.hasResource(src.getName())){
			dst = parent.createSubClass(src.getName());
		}else{
			IResource r = target.getResource(src.getName());
			if(r instanceof IClass){
				dst = (IClass) r;
			}else{
				System.err.println("Error: expecting a class "+src.getName()+" but got a "+r);
			}
		}
		return dst;
	}
	
	/**
	 * copy class content to target class.
	 *
	 * @param src the src
	 * @param parent the parent
	 * @return the i class
	 */
	private IClass copyClass2(BClass src,IClass parent){
		long time = System.currentTimeMillis();
		IOntology target = parent.getOntology();
		IClass dst = getClass2(src,parent);
		
		// if no such resource, then we are fucked
		if(dst == null)
			return null;
		
		
		// add superclasses if needed (avoid infinite loops)
		for(IClass cls : src.getDirectSuperClasses()){
			if(cls.getName().equals(parent.getName()) || cls.equals(src))
				continue;
			// add superclass
			IClass p = getClass2((BClass)cls,target.getRoot());
			if(p != null && !p.hasSuperClass(p))
				dst.addSuperClass(p);
		}
		
		// remove root class as parent if it is not in source, but in destination
		if(src.getDirectSuperClasses().length > 0 && dst.hasDirectSuperClass(target.getRoot()) && !src.hasDirectSuperClass(src.getOntology().getRoot())){
			dst.removeSuperClass(target.getRoot());
		}
			
		// copy superficial stuff
		for(String lbl: src.getLabels())
			dst.addLabel(lbl);
		
		for(String com: src.getComments())
			dst.addComment(com);
		if(src.getVersion() != null && dst.getVersion() != src.getVersion())
			dst.addVersion(src.getVersion());
	
		// copy properties values
		for(IProperty sp : src.getProperties()){
			// skip PartOf, and has_PART
			if(HAS_PART.equals(sp.getName()) || PART_OF.equals(sp.getName()))
				continue;
		
			IProperty dp = target.getProperty(sp.getName());
			
			// create unknown property for the first time
			if(dp == null && !target.hasResource(sp.getName())){
				dp = target.createProperty(sp.getName(),IProperty.ANNOTATION_DATATYPE);
				dp.setRange(new String [0]);
			}
			
			// copy string values if not there
			dst.setPropertyValues(dp,src.getPropertyValues(sp));
		}
		
		// now create sub class relationships
		for(IClass cls : src.getDirectSubClasses()){
			// if child class does not exists, it will be created under this parent
			// if it does, this should be added as superclass anyways, we will see
			getClass2((BClass)cls,dst);
		}
		
		/*
		// now copy disjoing classes
		for(IClass cls: src.getDisjointClasses()){
			IClass dc = getClass2((BClass)cls,target.getRoot());
			if(dc != null)
				dst.addDisjointClass(dc);
		}
	
		// now copy equivalent classes
		for(IClass cls: src.getEquivalentClasses()){
			IClass dc = getClass2((BClass)cls,target.getRoot());
			if(dc != null)
				dst.addEquivalentClass(dc);
		}
		// now copy other relationships relationships
		for(String relation: new String [] {PART_OF, HAS_PART}){
			for(IClass sibling: getClassList(target,src.getList(relation).toArray())){
				IRestriction r = target.createRestriction(IRestriction.SOME_VALUES_FROM);
				r.setProperty(target.getProperty(relation));
				r.setParameter(sibling.getLogicExpression());
				dst.addNecessaryRestriction(r);
			}
		}
		*/
		//System.out.println(".......... processing "+dst.getName()+" "+(System.currentTimeMillis()-time));
		return dst;	
	}
	
	/**
	 * save relations from source class into some tempoprary property of destination class.
	 *
	 * @param src the src
	 * @param dst the dst
	 * @param relation the relation
	 */
	private void copyClassRelations(BClass src, IClass dst, String relation){
		// get or create temp property
		IProperty dp = dst.getOntology().getProperty(relation);
		if(dp == null){
			dp = dst.getOntology().createProperty(relation,IProperty.ANNOTATION_DATATYPE);
			dp.setRange(new String [0]);
		}
		
		// remove prefix
		if(relation.startsWith("annotation_")){
			relation = relation.substring("annotation_".length());
		}
		
		// copy class information into special fields
		for(String s: src.getList(relation)){
			dst.addPropertyValue(dp,s);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#reload()
	 */
	public void reload() throws IOntologyException {
		// NOOP
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#removeImportedOntology(edu.pitt.dbmi.nlp.noble.ontology.IOntology)
	 */
	public void removeImportedOntology(IOntology o) {
		throw new IOntologyError("BioPortal is read-only");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#flush()
	 */
	public void flush() {
		// no-op
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#save()
	 */
	public void save() throws IOntologyException {
		throw new IOntologyException("BioPortal is read-only");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#setRepository(edu.pitt.dbmi.nlp.noble.ontology.IRepository)
	 */
	public void setRepository(IRepository r) {
		repository = (BioPortalRepository) r;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#write(java.io.OutputStream, int)
	 */
	public void write(OutputStream out, int format) throws IOntologyException {
		throw new IOntologyException("writing BioPortal is not supported yet");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.bioportal.BResource#getOntology()
	 */
	public IOntology getOntology() {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IOntology#createClass(edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression)
	 */
	public IClass createClass(ILogicExpression exp) {
		throw new IOntologyError("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#addConcept(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public boolean addConcept(Concept c) throws TerminologyException {
		throw new TerminologyException("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSourceFilter()
	 */
	public Source[] getSourceFilter() {
		return getSources();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getLanguages()
	 */
	public String[] getLanguages() {
		return new String[] { "en" };
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getRelatedConcepts(edu.pitt.dbmi.nlp.noble.terminology.Concept, edu.pitt.dbmi.nlp.noble.terminology.Relation)
	 */
	public Concept[] getRelatedConcepts(Concept c, Relation r)
			throws TerminologyException {
		IClass cls = c.getConceptClass();
		if (r == Relation.BROADER) {
			// IClass cls = ontology.getClass(c.getCode());
			if (cls != null) {
				return convertConcepts(cls.getDirectSuperClasses());
			}
		} else if (r == Relation.NARROWER) {
			// IClass cls = ontology.getClass(c.getCode());
			if (cls != null) {
				return convertConcepts(cls.getDirectSubClasses());
			}
		} else if (r == Relation.SIMILAR) {
			// IClass cls = ontology.getClass(c.getCode());
			if (cls != null) {
				List<IClass> clses = new ArrayList<IClass>();
				for (IClass eq : cls.getEquivalentClasses()) {
					if (!eq.isAnonymous()) {
						clses.add(eq);
					}
				}
				return convertConcepts(clses);
			}
		}
		return new Concept[0];
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getRelatedConcepts(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public Map getRelatedConcepts(Concept c) throws TerminologyException {
		Map<Relation, List<Concept>> map = new HashMap<Relation, List<Concept>>();
		map.put(Relation.BROADER, Arrays.asList(getRelatedConcepts(c,
				Relation.BROADER)));
		map.put(Relation.NARROWER, Arrays.asList(getRelatedConcepts(c,
				Relation.NARROWER)));
		map.put(Relation.SIMILAR, Arrays.asList(getRelatedConcepts(c,
				Relation.SIMILAR)));
		return map;
	}

	/**
	 * Convert concepts.
	 *
	 * @param clses the clses
	 * @return the concept[]
	 */
	private Concept[] convertConcepts(IClass[] clses) {
		Concept[] concepts = new Concept[clses.length];
		for (int i = 0; i < concepts.length; i++)
			concepts[i] = clses[i].getConcept();
		return concepts;
	}

	/**
	 * Convert concepts.
	 *
	 * @param clses the clses
	 * @return the concept[]
	 */
	private Concept[] convertConcepts(Collection<IClass> clses) {
		Concept[] concepts = new Concept[clses.size()];
		int i = 0;
		for (IClass cls : clses)
			concepts[i++] = cls.getConcept();
		return concepts;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getRelations()
	 */
	public Relation[] getRelations() throws TerminologyException {
		return new Relation[] { Relation.BROADER, Relation.NARROWER,
				Relation.SIMILAR };
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getRelations(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public Relation[] getRelations(Concept c) throws TerminologyException {
		return getRelations();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getRootConcepts()
	 */
	public Concept[] getRootConcepts() throws TerminologyException {
		IClass[] cls = getRootClasses();
		Concept[] c = new Concept[cls.length];
		for (int i = 0; i < c.length; i++)
			c[i] = cls[i].getConcept();
		return c;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSearchMethods()
	 */
	public String[] getSearchMethods() {
		return new String[] { BioPortalHelper.EXACT_MATCH };
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSources()
	 */
	public Source[] getSources() {
		return new Source[] { new Source(getName(), getDescription(), ""
				+ getURI()) };
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#getSources(java.lang.String)
	 */
	public Source[] getSources(String matchtext) {
		return getSources();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#lookupConcept(java.lang.String)
	 */
	public Concept lookupConcept(String cui) throws TerminologyException {
		//IClass cls = getClass(cui);
		//return (cls != null) ? cls.getConcept() : null;
		IResource cls = ontology.getResource(cui);
		return (cls != null && cls instanceof IClass)?((IClass)cls).getConcept():null;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#removeConcept(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public boolean removeConcept(Concept c) throws TerminologyException {
		throw new TerminologyException("Operation not supported");
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#search(java.lang.String)
	 */
	public Concept[] search(String text) throws TerminologyException {
		return search(text,EXACT_MATCH);
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
	public Concept[] search(String text, String method) throws TerminologyException {
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
		String ctext = TextTools.escapeURL(text);
		String url = repository.getURL()+SEARCH+"?q="+ctext+"&"+ONTOLOGYIDS+properties.getProperty("ontologyId")+BioPortalHelper.BIOPORTAL_OPTIONS+"&require_exact_match=true&"+repository.getAPIKey();
		
		List<Concept> result = new ArrayList<Concept>();
		for(IClass cls: BioPortalHelper.getClassList(this, url)){
			cls.getConcept().setSearchString(text);
			cls.getConcept().setTerminology(this);
			result.add(cls.getConcept());
		}
		return result.toArray(new Concept [0]);
	}

	
	
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#setSourceFilter(edu.pitt.dbmi.nlp.noble.terminology.Source[])
	 */
	public void setSourceFilter(Source[] srcs) {
		// NOOP
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.terminology.Terminology#updateConcept(edu.pitt.dbmi.nlp.noble.terminology.Concept)
	 */
	public boolean updateConcept(Concept c) throws TerminologyException {
		throw new TerminologyException("Operation not supported");
	}

	/**
	 * create new concepts.
	 *
	 * @param name the name
	 * @return the i class
	 */
	public IClass createClass(String name) {
		throw new IOntologyError("Operation not supported");
	}
	
	/**
	 * get all available version of this ontolgy
	 * the first entry is the current version, entries after that are previous versions
	 * If given IRepository supports multiple versions of ontologies, you can append this version
	 * number to ontology URL to get the desired version
	 * Example: IRepository.getOntology("http://wwww.ontologies.com/TestOntology.owl#1.1")
	 * @return list of version
	 */
	public Map<String,BOntology> getOntologyVersions(){
		if(ontologyMap == null){
			ontologyMap = new LinkedHashMap<String,BOntology>();
			
			// get document
			Document doc = parseXML(openURL(repository.getURL()+ONTOLOGIES+GET_VERSIONS+properties.get("ontologyId")+"?"+repository.getAPIKey()));
			if(doc != null){
				// since ontologyBean are not nested we can simple
				// get their list
				NodeList list = doc.getDocumentElement().getElementsByTagName("ontologyBean");
				for(int i=0;i<list.getLength();i++){
					BOntology ont = new BOntology(repository,(Element)list.item(i));
					ontologyMap.put(ont.getVersion(),ont);
				}
			}
		}
		return ontologyMap;
	}
	
	/**
	 * get all available concept objects in terminology. Only sensible for small terminologies
	 *
	 * @return the concepts
	 * @throws TerminologyException the terminology exception
	 */
	public Collection<Concept> getConcepts()  throws TerminologyException{
		List<Concept> concepts = new ArrayList<Concept>();
		for(IResourceIterator it = getAllClasses();it.hasNext();){
			Object r = it.next();
			if(r instanceof IClass){
				concepts.add(((IClass)r).getConcept());
			}
		}
		return concepts;
	}
	
	/**
	 * convert Template to XML DOM object representation.
	 *
	 * @param doc the doc
	 * @return the element
	 * @throws TerminologyException the terminology exception
	 */
	public Element toElement(Document doc)  throws TerminologyException{
		Element root = doc.createElement("Terminology");
		
		root.setAttribute("name",getName());
		root.setAttribute("version",getVersion());
		root.setAttribute("location",getLocation());
		root.setAttribute("format",getFormat());
		root.setAttribute("uri",""+getURI());
		
		Element desc = doc.createElement("Description");
		desc.setTextContent(getDescription());
		root.appendChild(desc);
		
		Element sources = doc.createElement("Sources");
		root.appendChild(sources);
		for(Source c: getSources()){
			sources.appendChild(c.toElement(doc));
		}
		
		Element relations = doc.createElement("Relations");
		root.appendChild(relations);
		for(Relation c: getRelations()){
			relations.appendChild(c.toElement(doc));
		}
		
		Element concepts = doc.createElement("Concepts");
		root.appendChild(concepts);
		for(Concept c: getConcepts()){
			concepts.appendChild(c.toElement(doc));
		}
		
		return root;
	}
	
	/**
	 * convert Template to XML DOM object representation.
	 *
	 * @param element the element
	 * @throws TerminologyException the terminology exception
	 */
	public void fromElement(Element element) throws TerminologyException{
		throw new TerminologyException("Not implemented");
	}
	
	/**
	 * process sentence.
	 *
	 * @param s the s
	 * @return the sentence
	 * @throws TerminologyException the terminology exception
	 */
	public Sentence process(Sentence s) throws TerminologyException {
		time = System.currentTimeMillis();
		List<Mention> mentions = new ArrayList<Mention>();
		for(Concept c: search(s.getText())){
			mentions.addAll(Mention.getMentions(c));
		}
		s.setMentions(mentions);
		time = System.currentTimeMillis() - time;
		return s;
	}


	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Processor#getProcessTime()
	 */
	public long getProcessTime() {
		return time;
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
