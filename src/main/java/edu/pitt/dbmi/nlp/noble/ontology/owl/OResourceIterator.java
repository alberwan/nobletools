package edu.pitt.dbmi.nlp.noble.ontology.owl;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import edu.pitt.dbmi.nlp.noble.ontology.DefaultResourceIterator;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.ontology.IResourceIterator;


/**
 * The Class OResourceIterator.
 */
public class OResourceIterator extends DefaultResourceIterator{
	private OOntology ont;
	
	
	/**
	 * Instantiates a new o resource iterator.
	 *
	 * @param list the list
	 * @param ont the ont
	 */
	public OResourceIterator(Collection list ,OOntology ont){
		super(list);
		this.ont =  ont;
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.DefaultResourceIterator#next()
	 */
	public Object next(){
		Object obj = null;
		do{
			obj = ont.convertOWLObject((OWLEntity)super.next());
			count --; // super.next() increments it, we need to decrement it
		}while(obj instanceof IResource && ((IResource)obj).isSystem());
		count ++;
		return obj;
	}

}
