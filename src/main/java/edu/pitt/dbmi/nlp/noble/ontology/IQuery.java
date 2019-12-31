package edu.pitt.dbmi.nlp.noble.ontology;

/**
 * generic interface for defining queries in some query language.
 *
 * @author tseytlin
 */
public interface IQuery {
	public static int SPARQL = 0;
	public static int SERQL = 1;

	/**
	 * get the language of this query.
	 *
	 * @return the language
	 */
	public int getLanguage();
	
	
	/**
	 * get the text of this query.
	 *
	 * @return the query
	 */
	public String getQuery();

}
