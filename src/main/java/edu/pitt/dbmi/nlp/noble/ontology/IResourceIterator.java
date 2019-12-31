package edu.pitt.dbmi.nlp.noble.ontology;

import java.util.Iterator;

/**
 * mecanism for iterating over resources.
 *
 * @author tseytlin
 */
public interface IResourceIterator extends Iterator {
	
	/**
	 * get the current count
	 * number of next() calls.
	 *
	 * @return the count
	 */
	public int getCount();
	
	/**
	 * get offset from which resources should be fetched.
	 *
	 * @return the offset
	 */
	public int getOffset();
	
	/**
	 * if possible start fetching resources from given offset
	 * default 0 .
	 *
	 * @param offset index
	 */
	public void setOffset(int offset);
	
	/**
	 * get limit. Iterator could be limited to N iterations
	 * @return N
	 */
	public int getLimit();
	
	/**
	 * set limit. Iterator could be limited to N iterations
	 *
	 * @param limit the new limit
	 */
	public void setLimit(int limit);
	
	
	/**
	 * if possible to know the total size.
	 *
	 * @return the total
	 */
	public int getTotal();
	
}
