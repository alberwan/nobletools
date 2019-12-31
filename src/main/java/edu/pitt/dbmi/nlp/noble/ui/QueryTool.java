package edu.pitt.dbmi.nlp.noble.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.ontology.IResourceIterator;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.ui.widgets.ResourceCellRenderer;
import edu.pitt.dbmi.nlp.noble.util.PathHelper;

/**
 * displays a query .
 *
 * @author tseytlin
 */
public class QueryTool extends JPanel implements ActionListener, ListSelectionListener {
	private JTextField search;
	private JList results;
	private JEditorPane info;
	private Terminology terminology;
	private PathHelper pathHelper;
	private IOntology ontology;
	private JProgressBar progress;
	private JLabel status;
	
	/**
	 * create new query tool.
	 */
	public QueryTool(){
		super();
		setLayout(new BorderLayout());
		
		// search box
		search = new JTextField();
		//search.setActionCommand("search");
		search.addActionListener(this);
		JButton b = new JButton("search");
		//b.setActionCommand("search");
		b.addActionListener(this);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		//p.add(new JLabel("search terminology"),BorderLayout.WEST);
		p.add(search,BorderLayout.CENTER);
		p.add(b,BorderLayout.EAST);
		
		//bottom panel
		// results
		results = new JList();
		results.addListSelectionListener(this);
		results.setCellRenderer(new ResourceCellRenderer());
		
		JScrollPane s = new JScrollPane(results);
		s.setPreferredSize(new Dimension(300,500));
		// info panel
		info = new JEditorPane();
		info.setContentType("text/html");
		info.setPreferredSize(new Dimension(500,500));
		info.setEditable(false);
		info.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())){
					doViewRelationships(e.getDescription());
				}
			}
		});
		
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setLeftComponent(s);
		split.setRightComponent(new JScrollPane(info));
		split.setDividerLocation(200);
		
		status = new JLabel(" ");
		progress = new JProgressBar();
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		progress.setIndeterminate(true);
		
		
		add(p,BorderLayout.NORTH);
		add(split,BorderLayout.CENTER);
		add(status,BorderLayout.SOUTH);
	}
	
	/**
	 * Gets the path helper.
	 *
	 * @return the path helper
	 */
	private PathHelper getPathHelper(){
		if(pathHelper == null && terminology != null){
			pathHelper = new PathHelper(terminology);
		}
		return pathHelper;
	}
	
	/**
	 * Do view relationships.
	 *
	 * @param code the code
	 */
	protected void doViewRelationships(String code) {
		try {
			Concept c = terminology.lookupConcept(code);
			if(c != null){
				StringBuffer str = new StringBuffer();
				// narrower concepts
				str.append("<h3>Direct Children of <font color=green>"+c.getName()+" ("+c.getCode()+")</font></h3>");
				str.append("<ul>");
				for(Concept a: c.getChildrenConcepts()){
					String h = "<a href=\""+a.getCode()+"\">"+a.getCode()+"</a>";
					str.append("<li><b>"+a.getName()+"</b> ("+h+") "+Arrays.toString(a.getSemanticTypes())+"</li>");
				}
				str.append("</ul>");
				str.append("<h3>Direct Parents of <font color=green>"+c.getName()+" ("+c.getCode()+")</font></h3>");
				for(Concept a: c.getParentConcepts()){
					String h = "<a href=\""+a.getCode()+"\">"+a.getCode()+"</a>";
					str.append("<li><b>"+a.getName()+"</b> ("+h+") "+Arrays.toString(a.getSemanticTypes())+"</li>");
				}
				str.append("</ul>");
				// ancestry
				str.append("<h3>All Ancestors of <font color=green>"+c.getName()+" ("+c.getCode()+") </font></h3>");
				Map<Concept,Integer> ancestors = getPathHelper().getAncestors(c);
				if(ancestors != null && !ancestors.isEmpty()){
					for(Concept a: ancestors.keySet()){
						try {
							a.initialize();
						} catch (TerminologyException e) {
							e.printStackTrace();
						}
						String h = "<a href=\""+a.getCode()+"\">"+a.getCode()+"</a>";
						str.append("<li>{"+ancestors.get(a)+"} ... <b>"+a.getName()+"</b> ("+h+") "+
									Arrays.toString(a.getSemanticTypes())+"</li>");
					}
				}
				str.append("</ul>");
				
				// other relationshipts
				for(Relation r: c.getRelatedConcepts().keySet()){
					if(Relation.BROADER.equals(r) || Relation.NARROWER.equals(r))
						continue;
					str.append("<h3>"+r.getName()+" of <font color=green>"+c.getName()+" ("+c.getCode()+") </font></h3>");
					str.append("<ul>");
					for(Concept a: c.getRelatedConcepts(r)){
						String h = "<a href=\""+a.getCode()+"\">"+a.getCode()+"</a>";
						str.append("<li><b>"+a.getName()+"</b> ("+h+") "+Arrays.toString(a.getSemanticTypes())+"</li>");
					}
					str.append("</ul>");
				}
				
				
				JEditorPane info2 = new JEditorPane();
				info2.setContentType("text/html");
				info2.setPreferredSize(new Dimension(700,500));
				info2.setEditable(false);
				info2.addHyperlinkListener(new HyperlinkListener() {
					public void hyperlinkUpdate(HyperlinkEvent e) {
						if(HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())){
							Window w = SwingUtilities.getWindowAncestor((Component)e.getSource());
							if(w != null){
								w.dispose();
							}
							search.setText(e.getDescription());
							doSearch();
							try {
								show(terminology.lookupConcept(e.getDescription()));
							} catch (TerminologyException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
				info2.setText("<html>"+str);
				JOptionPane.showMessageDialog(this,new JScrollPane(info2),"Related Concepts",JOptionPane.PLAIN_MESSAGE);
			}		
		} catch (TerminologyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * perform search.
	 *
	 * @param text the text
	 */
	private void doTerminologySearch(String text){
		try{
			long time = System.currentTimeMillis();
			final Concept [] c = doLookup(text);
			final long delta = System.currentTimeMillis()-time;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					results.setListData(c);
					status.setText("search time: "+delta+" ms");
				}
			});
		
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * Do lookup.
	 *
	 * @param text the text
	 * @return the concept[]
	 * @throws Exception the exception
	 */
	private Concept[] doLookup(String text) throws Exception{
		// do lookup first
		if(text.matches(".*[\\d#:]+.*")){
			Concept c = terminology.lookupConcept(text);
			if(c != null){
				//if(isCode(text)){
				return c != null? new Concept []{c}:new Concept [0];
			}
		}
		return terminology.search(text);
	}

	/**
	 * Checks if is code.
	 *
	 * @param text the text
	 * @return true, if is code
	 */
	private boolean isCode(String text) {
		return text != null && (text.matches("CL?\\d{3,8}") || text.matches("http://.*"));
	}

	/**
	 * perform search.
	 *
	 * @param text the text
	 */
	private void doOntologySearch(String text){
		IResourceIterator it = ontology.getMatchingResources(text);
		Vector v = new Vector();
		while(it.hasNext())
			v.add(it.next());
		results.setListData(v);
	}
	
	/**
	 * display busy.
	 *
	 * @param busy the new busy
	 */
	public void setBusy(boolean busy){
		if(busy){
			remove(status);
			add(progress,BorderLayout.SOUTH);
		}else{
			remove(progress);
			add(status,BorderLayout.SOUTH);
		}
		revalidate();
		repaint();
	}
	
	/**
	 * handle actions.
	 *
	 * @param e the e
	 */
	public void actionPerformed(ActionEvent e){
		doSearch();
	}
	
	/**
	 * preset search term.
	 *
	 * @param text the new search term
	 */
	public void setSearchTerm(String text){
		search.setText(text);
	}
	
	/**
	 * Sets the selected concept.
	 *
	 * @param c the new selected concept
	 */
	public void setSelectedConcept(Concept c){
		search.setText(c.getCode());
		doSearch();
		show(c);
	}
	
	/**
	 * perform search.
	 */
	public void doSearch(){
		info.setText("");
		results.setListData(new String [0]);
		if(terminology != null)
			doTerminologySearch(search.getText());
		else if(ontology != null)
			doOntologySearch(search.getText());
	}
	
	/**
	 * get selected concepts.
	 *
	 * @return the selected concepts
	 */
	public Concept [] getSelectedConcepts(){
		return (Concept []) results.getSelectedValuesList().toArray(new Concept [0]);
	}
	
	
	/**
	 * get selected concept.
	 *
	 * @return the selected concept
	 */
	public Concept getSelectedConcept(){
		return (Concept) results.getSelectedValue();
	}
	
	/**
	 * keep track of browsing of result set.
	 *
	 * @param e the e
	 */
	public void valueChanged(ListSelectionEvent e){
		if(!e.getValueIsAdjusting()){
			Object o = results.getSelectedValue();
			if(o instanceof Concept){
				show((Concept) o);
			}else if(o instanceof IResource){
				show((IResource) o);
			}
		}
	}

	/**
	 * show lexical entry in info panel.
	 *
	 * @param e the e
	 */ 
	private void show(Concept e){
		StringBuffer buffer = new StringBuffer();
		if(e != null){
			try {
				e.initialize();
			} catch (TerminologyException e1) {
				e1.printStackTrace();
			}
			buffer.append("<b>"+e.getName()+"</b> &nbsp;&nbsp;");
			buffer.append("(<i>"+e.getCode()+"</i>)<br>");
			buffer.append(""+Arrays.asList(e.getSemanticTypes()));
			buffer.append("<hr>");
			buffer.append("<b>Definition: </b> "+e.getDefinition()+"<br>");
			buffer.append("<b>Synonyms: </b> "+Arrays.asList(e.getSynonyms())+"<br>");
			buffer.append("<b>Sources: </b> "+Arrays.asList(e.getSources())+"<br>");
			buffer.append("<b>Codes: </b> "+e.getCodes()+"<br>");
			
			buffer.append("<b>Relations: </b> [<a href=\""+e.getCode()+"\">view related concepts</a>]");
			
			
		}
		info.setText("<html>"+buffer.toString());
	}
	
	/**
	 * show lexical entry in info panel.
	 *
	 * @param e the e
	 */ 
	private void show(IResource e){
		StringBuffer buffer = new StringBuffer();
		if(e != null){
			buffer.append("<b>"+e.getName()+"</b><br>");
			buffer.append("(<i>"+e.getURI()+"</i>)<br>");
			buffer.append("<hr>");
			buffer.append("<b>Definition: </b> "+e.getDescription()+"<br>");
			if(e instanceof IClass){
				IClass cls = (IClass) e;
				buffer.append("<b>Equivalent: </b> "+Arrays.asList(cls.getEquivalentClasses())+"<br>");
				buffer.append("<b>Disjoint: </b> "+Arrays.asList(cls.getDisjointClasses())+"<br>");
			}else if(e instanceof IProperty){
				IProperty cls = (IProperty) e;
				buffer.append("<b>Domain: </b> "+Arrays.asList(cls.getDomain())+"<br>");
				buffer.append("<b>Range: </b> "+Arrays.asList(cls.getRange())+"<br>");
			}else if(e instanceof IInstance){
				IInstance cls = (IInstance) e;
				buffer.append("<b>Types: </b> "+Arrays.asList(cls.getTypes())+"<br>");
			}
			buffer.append("<b>Properties:</b>");
			IProperty[] p = e.getProperties();
			if(p.length > 0){
				buffer.append("<ul>");
				for(int i=0;i<p.length;i++){
					buffer.append("<li>"+p[i]+" = "+Arrays.toString(e.getPropertyValues(p[i]))+"</li>");
				}
				buffer.append("</ul>");
			}
			
		}
		info.setText("<html>"+buffer.toString());
	}
	
	
	/**
	 * Sets the ontology.
	 *
	 * @param ontology the ontology to set
	 */
	public void setOntology(IOntology ontology) {
		this.ontology = ontology;
	}

	/**
	 * Sets the terminology.
	 *
	 * @param terminology the terminology to set
	 */
	public void setTerminology(Terminology terminology) {
		this.terminology = terminology;
	}

}
