package edu.pitt.dbmi.nlp.noble.ontology.concept;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;

/**
 * The Class BioPortalConcept.
 */
public class BioPortalConcept extends Concept {
	
	/**
	 * Instantiates a new bio portal concept.
	 *
	 * @param cls the cls
	 */
	public BioPortalConcept(IClass cls) {
		super(cls);
		
		// not lets do NCI Thesaurus specifics
		IOntology ont = cls.getOntology();
		
		// do code
		IProperty code_p = ont.getProperty(BioPortalHelper.CODE);
		if(code_p != null){
			for(Object val : cls.getPropertyValues(code_p)){
				addCode(val.toString(),new Source(val.toString()));
			}
		}
		
		// do sem type
		IProperty sem_p = ont.getProperty(BioPortalHelper.SEMANTIC_TYPE);
		if(sem_p != null){
			Object [] val = cls.getPropertyValues(sem_p);
			SemanticType [] types = new SemanticType [val.length];
			for(int i=0;i<val.length;i++){
				types[i] = SemanticType.getSemanticType(val[i].toString());
			}
			setSemanticTypes(types);
		}
	}
}
