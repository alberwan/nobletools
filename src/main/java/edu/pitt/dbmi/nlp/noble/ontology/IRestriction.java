package edu.pitt.dbmi.nlp.noble.ontology;

/**
 * This class describes a restriction on a class.
 *
 * @author tseytlin
 */
public interface IRestriction extends IClass {
	public static final int ALL_VALUES_FROM = 0;
	public static final int SOME_VALUES_FROM = 1;
	public static final int HAS_VALUE = 2;
	public static final int CARDINALITY = 3;
	public static final int MAX_CARDINALITY = 4;
	public static final int MIN_CARDINALITY = 5;
		
	/**
	 * get restriction type.
	 *
	 * @return a type that can be either one of:
	 * ALL_VALUES_FROM,SOME_VALUES_FROM,HAS_VALUE,CARDINALITY,MAX_CARDINALITY,MIN_CARDINALITY
	 */
	public int getRestrictionType();
	
	
	/**
	 * Gets the property that is restricted by this restriction.
	 *
	 * @return the property
	 */
	public IProperty getProperty();
	
	
	/**
	 * Sets the property that is restricted by this restriction.
	 *
	 * @param prop the new property
	 */
	public void setProperty(IProperty prop);
	
	/**
	 * get restriction parameter. Filler. 
	 * Parameter is a logic expression that 
	 * could be a Integer, String, Boolean, Float for CARDINALITY and HAS_VALUE restrictions
	 * It could be IResource for ALL_VALUES_FROM, SOME_VALUES_FROM and HAS_VALUE
	 *
	 * @return the parameter
	 */
	public ILogicExpression getParameter();
	
	/**
	 * set restriction parameter. Filler. 
	 * Parameter is a logic expression that 
	 * could be a Integer, String, Boolean, Float for CARDINALITY and HAS_VALUE restrictions
	 * It could be IResource for ALL_VALUES_FROM, SOME_VALUES_FROM and HAS_VALUE
	 *
	 * @param exp the new parameter
	 */	
	public void setParameter(ILogicExpression exp);
	
	
	/**
	 * get owner of this restriction.
	 *
	 * @return the owner
	 */
	public IClass getOwner();
}
