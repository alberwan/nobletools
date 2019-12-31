package edu.pitt.dbmi.nlp.noble.ontology.owl;

import java.net.URI;

import org.semanticweb.owlapi.model.*;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;


/**
 * The Class ORestriction.
 */
public class ORestriction extends OClass implements IRestriction {
	private OWLRestriction rest;
	private IClass owner;
	private int tempType;
	private IProperty tempProp;
	private ILogicExpression tempExp;
	
	/**
	 * Instantiates a new o restriction.
	 *
	 * @param obj the obj
	 * @param ont the ont
	 */
	protected ORestriction(OWLRestriction obj, OOntology ont) {
		super(obj, ont);
		this.rest = obj;
	}
	
	/**
	 * Instantiates a new o restriction.
	 *
	 * @param type the type
	 * @param ont the ont
	 */
	protected ORestriction(int type,OOntology ont){
		super(ont.getOWLDataFactory().getOWLThing(),ont);
		tempType = type;
	}
	
	/**
	 * Gets the OWL restriction.
	 *
	 * @return the OWL restriction
	 */
	public OWLRestriction getOWLRestriction(){
		return rest;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IRestriction#getRestrictionType()
	 */
	public int getRestrictionType() {
		if(rest instanceof OWLObjectAllValuesFrom || rest instanceof OWLDataAllValuesFrom)
			return IRestriction.ALL_VALUES_FROM;
		else if(rest instanceof OWLObjectSomeValuesFrom || rest instanceof OWLDataSomeValuesFrom)
			return IRestriction.SOME_VALUES_FROM;
		else if(rest instanceof OWLDataHasValue || rest instanceof OWLObjectHasValue)
			return IRestriction.HAS_VALUE;
		else if(rest instanceof OWLObjectExactCardinality || rest instanceof OWLDataExactCardinality)
			return IRestriction.CARDINALITY;
		else if(rest instanceof OWLObjectMaxCardinality || rest instanceof OWLDataMaxCardinality)
			return IRestriction.MAX_CARDINALITY;
		else if(rest instanceof OWLObjectMinCardinality || rest instanceof OWLDataMinCardinality)
			return IRestriction.MIN_CARDINALITY;
		return 0;
	}
	
	/**
	 * Gets the operator.
	 *
	 * @return the operator
	 */
	private String getOperator() {
		if(rest instanceof OWLObjectAllValuesFrom || rest instanceof OWLDataAllValuesFrom)
			return "all";
		else if(rest instanceof OWLObjectSomeValuesFrom || rest instanceof OWLDataSomeValuesFrom)
			return "some";
		else if(rest instanceof OWLDataHasValue || rest instanceof OWLObjectHasValue)
			return "value";
		else if(rest instanceof OWLObjectExactCardinality || rest instanceof OWLDataExactCardinality)
			return "exactly";
		else if(rest instanceof OWLObjectMaxCardinality || rest instanceof OWLDataMaxCardinality)
			return "max";
		else if(rest instanceof OWLObjectMinCardinality || rest instanceof OWLDataMinCardinality)
			return "min";
		return "";
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IRestriction#getProperty()
	 */
	public IProperty getProperty() {
		return (IProperty) convertOWLObject(rest.getProperty());
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IRestriction#setProperty(edu.pitt.dbmi.nlp.noble.ontology.IProperty)
	 */
	public void setProperty(IProperty prop) {
		tempProp = prop;
		createRestriction();
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IRestriction#setParameter(edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression)
	 */
	public void setParameter(ILogicExpression exp) {
		tempExp = exp;
		createRestriction();
	}
	
	
	/**
	 * actually create a restriction once everything is in order.
	 */
	private void createRestriction() {
		if(tempProp != null && tempExp != null){
			OWLRestriction r = null;
			switch(tempType){
				case(IRestriction.ALL_VALUES_FROM):
					if(tempProp.isDatatypeProperty()){
						Object o = convertOntologyObject(tempExp);
						r = getOWLDataFactory().getOWLDataAllValuesFrom(
								(OWLDataPropertyExpression)convertOntologyObject(tempProp),
								(OWLDataRange) ((o instanceof OWLLiteral)?((OWLLiteral)o).getDatatype():o));
					}else if(tempProp.isObjectProperty())
						r = getOWLDataFactory().getOWLObjectAllValuesFrom(
								(OWLObjectPropertyExpression)convertOntologyObject(tempProp),
								(OWLClassExpression) convertOntologyObject(tempExp));
					break;
				case(IRestriction.SOME_VALUES_FROM):
					if(tempProp.isDatatypeProperty()){
						Object o = convertOntologyObject(tempExp);
						r = getOWLDataFactory().getOWLDataSomeValuesFrom(
								(OWLDataPropertyExpression)convertOntologyObject(tempProp),
								(OWLDataRange) ((o instanceof OWLLiteral)?((OWLLiteral)o).getDatatype():o));
					}else if(tempProp.isObjectProperty())
						r = getOWLDataFactory().getOWLObjectSomeValuesFrom(
								(OWLObjectPropertyExpression)convertOntologyObject(tempProp),
								(OWLClassExpression) convertOntologyObject(tempExp));
					break;
				case(IRestriction.HAS_VALUE):
					if(tempProp.isDatatypeProperty())
						r = getOWLDataFactory().getOWLDataHasValue(
								(OWLDataPropertyExpression)convertOntologyObject(tempProp),
								(OWLLiteral) convertOntologyObject(tempExp.get(0)));
					else if(tempProp.isObjectProperty())
						r = getOWLDataFactory().getOWLObjectHasValue(
								(OWLObjectPropertyExpression) convertOntologyObject(tempProp),
								(OWLIndividual) convertOntologyObject(tempExp.get(0)));
					break;
				case(IRestriction.CARDINALITY):
					if(tempProp.isDatatypeProperty())
						r = getOWLDataFactory().getOWLDataExactCardinality((Integer)tempExp.get(0),
								(OWLDataPropertyExpression)convertOntologyObject(tempProp));
					else if(tempProp.isObjectProperty())
						r = getOWLDataFactory().getOWLObjectExactCardinality((Integer)tempExp.get(0),
								(OWLObjectPropertyExpression)convertOntologyObject(tempProp));
					break;
				case(IRestriction.MAX_CARDINALITY):
					if(tempProp.isDatatypeProperty())
						r = getOWLDataFactory().getOWLDataMinCardinality((Integer)tempExp.get(0),
								(OWLDataPropertyExpression)convertOntologyObject(tempProp));
					else if(tempProp.isObjectProperty())
						r = getOWLDataFactory().getOWLObjectMinCardinality((Integer)tempExp.get(0),
								(OWLObjectPropertyExpression)convertOntologyObject(tempProp));
					break;
				case(IRestriction.MIN_CARDINALITY):
					if(tempProp.isDatatypeProperty())
						r = getOWLDataFactory().getOWLDataMaxCardinality((Integer)tempExp.get(0),
								(OWLDataPropertyExpression)convertOntologyObject(tempProp));
					else if(tempProp.isObjectProperty())
						r = getOWLDataFactory().getOWLObjectMaxCardinality((Integer)tempExp.get(0),
								(OWLObjectPropertyExpression)convertOntologyObject(tempProp));
					break;
			}
			// we've created a restriction
			if(r != null){
				this.rest = r;
				this.obj = r;
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IRestriction#getParameter()
	 */
	public ILogicExpression getParameter() {
		if(rest instanceof OWLCardinalityRestriction){
			return getOntology().createLogicExpression(ILogicExpression.EMPTY,((OWLCardinalityRestriction)rest).getCardinality());
		}else if(rest instanceof OWLQuantifiedRestriction){
			Object val = convertOWLObject(((OWLQuantifiedRestriction)rest).getFiller());
			return (val instanceof ILogicExpression)?(ILogicExpression)val:getOntology().createLogicExpression(ILogicExpression.EMPTY,val);
		}else if(rest instanceof OWLHasValueRestriction){
			Object val = convertOWLObject(((OWLHasValueRestriction)rest).getValue());
			return (val instanceof ILogicExpression)?(ILogicExpression)val:getOntology().createLogicExpression(ILogicExpression.EMPTY,val);
		}else if(rest instanceof OWLQuantifiedDataRestriction){
			
		}
		return null;
	}



	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.IRestriction#getOwner()
	 */
	public IClass getOwner() {
		return owner;
	}
	
	/**
	 * Sets the owner.
	 *
	 * @param o the new owner
	 */
	public void setOwner(IClass o){
		this.owner = o;
	}
	
	/**
	 * check if property is satisfied.
	 *
	 * @param prop the prop
	 * @param inst the inst
	 * @return true, if is property satisfied
	 */
	private boolean isPropertySatisfied(IProperty prop, IInstance inst){
		// if property is null, then not satisfied
		if(prop == null)
			return false;
		
		// if either parameter is null, then condition is sutisfied :)
		if(inst == null)
			return true;
		
		Object [] values = inst.getPropertyValues(prop,true);
		if(values == null)
			return false;
		
		// if any of values fits, that we are good
		boolean satisfied = false;
		ILogicExpression exp = getParameter();
		// check cardinality
		if(MIN_CARDINALITY == getRestrictionType()){
			int num = ((Integer) exp.getOperand()).intValue();
			return values.length >= num;
		}else if(MAX_CARDINALITY == getRestrictionType()){
			int num = ((Integer) exp.getOperand()).intValue();
			return values.length <= num;
		}else if(CARDINALITY == getRestrictionType()){
			int num = ((Integer) exp.getOperand()).intValue();
			return values.length == num;
		}
		
		// check every entry
		for(int i=0;i<values.length;i++){
			if(exp.evaluate(values[i])){
				satisfied = true;
				if(SOME_VALUES_FROM == getRestrictionType())
					break;
			}else if(ALL_VALUES_FROM == getRestrictionType()){
				satisfied = false;
				break;
			}
		}
		return satisfied;
	}
	
	
	/**
	 * is this restriction satisfied for the owner of this restriction.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	public boolean evaluate(Object obj){
		if(obj instanceof IInstance){
			IInstance inst = (IInstance) obj;
			// see if this instance has a value that fits this restriction
			IProperty prop = getProperty();
			
			// is property satisfied
			boolean satisfied = isPropertySatisfied(prop, inst);
			
			// check if there is evidence to contradict inverse property
			// then the instance becomes inconsistent again
			if(satisfied && isPropertySatisfied(prop.getInverseProperty(), inst)){
				satisfied = false;
			}
			return satisfied;
		}else{
			return super.evaluate(obj);
		}
	}
	
	/**
	 * how does this thing appear.
	 *
	 * @return the name
	 */
	public String getName(){
		return getProperty().getName()+" "+getOperator()+" "+getParameter();
	}

	/**
	 * Gets the signature.
	 *
	 * @return the signature
	 */
	private String getSignature(){
		return getProperty().getURI()+" "+getOperator()+" "+getParameter();
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.owl.OResource#hashCode()
	 */
	public int hashCode() {
		return getSignature().hashCode();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.owl.OResource#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof ORestriction) {
			return getSignature().equals(((ORestriction) obj).getSignature());
		}
		return super.equals(obj);
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.ontology.owl.OResource#getURI()
	 */
	public URI getURI(){
		return null;
	}
}
