/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;

/** class describing a rhs expression which includes operators and rhs references */
public class RhsExpression {
	
	private String baseExpression; 
	private List<RhsReference> refs = new ArrayList<RhsReference>();

   public RhsExpression(String rawExpression, int depth) {
	   parseRawExpression(rawExpression, depth);  // extract instance and deref  
   }
   
   /** extract baseExpression and list of references from rawExpression */
   private void parseRawExpression(String rawExpression, int depth) { 
	   baseExpression = "";
	   boolean matchFail= false;
	   int refId = 0;
	   Pattern p = Pattern.compile("^([\\s\\&\\|\\~\\(\\)]*)([\\w\\.]+(\\s*->\\s*\\w+)?)([\\s\\&\\|\\~\\(\\)].*)?$");
	   Matcher m;
	   String expression = rawExpression;  // start with full expression
	   while (!matchFail) {
		   m = p.matcher(expression);
		   if (m.matches()) {
			   /*for (int idx=1; idx<=m.groupCount(); idx++)
				   System.out.println("exp " + idx + ": " + m.group(idx));*/
			   baseExpression += m.group(1) + "$" + refId++ + " ";
			   refs.add(new RhsReference(m.group(2), depth));
			   expression = m.group(4);
			   if (expression == null) matchFail = true;
		   }
		   else {
			   matchFail = true;
			   if (refId>0) {
				   baseExpression += expression;
				   baseExpression = baseExpression.trim();
			   }
			   else Ordt.errorMessage("parse of rhs expression (" + rawExpression + ") failed.");
		   }   
	   }  // while
	   /*System.out.println("rawExpression=" + rawExpression);
	   System.out.println("baseExpression=" + baseExpression);
	   for (RhsReference ref: refs) {
		   System.out.println("ref=" + ref.getRawReference());
	   }*/
   }

   /** return the raw expression */
   public String getRawExpression() {
	   String retExpression = baseExpression;
	   for (int idx=0; idx<refs.size(); idx++) retExpression = retExpression.replaceFirst("\\$" + idx, refs.get(idx).getRawReference());
	   return retExpression;   
   }
	
   /** return a list of references in this expression */
   public List<RhsReference> getRefList() {
	   return refs;
   }
	
   /** return list of raw references in this expression */
   public List<String> getRawRefNameList() {
	   List<String> retList = new ArrayList<String>();
	   for (RhsReference ref: refs) retList.add(ref.getRawReference());
	   return retList;
   }
	
   /** return list of resolved references in this expression */
   public List<String> getResolvedRefNameList(InstanceProperties instProperties, HashMap<String, SignalProperties> userDefinedSignals) {
	   List<String> retList = new ArrayList<String>();
	   for (RhsReference ref: refs) {
		   String fullRef = ref.getReferenceName(instProperties, false);
		   if ((fullRef != null) && fullRef.startsWith("rg_")) {  // if this is a field/signal reference, resolve it
			   String sigRef = fullRef.replaceFirst("rg_", "sig_");
			   if  (userDefinedSignals.containsKey(sigRef)) fullRef=sigRef;
		   }
		   retList.add(fullRef);
	   }
	   return retList;
   }

   /** return the resolved rtl expression */
   public String getResolvedExpression(InstanceProperties instProperties, HashMap<String, SignalProperties> userDefinedSignals) {
	   String retExpression = baseExpression;
	   //System.out.println("RhsExpression getResolvedExpression: baseExpression=" + baseExpression);
	   for (int idx=0; idx<refs.size(); idx++) {
		   String fullRef = refs.get(idx).getReferenceName(instProperties, false);
		   if ((fullRef != null) && fullRef.startsWith("rg_")) {  // if this is a field/signal reference, resolve it
			   String sigRef = fullRef.replaceFirst("rg_", "sig_");
			   if  (userDefinedSignals.containsKey(sigRef)) fullRef=sigRef;
		   }
		   retExpression = retExpression.replaceFirst("\\$" + idx, fullRef);
	   }
	   return retExpression;   
   }
   
   public static void main(String[] args) {
	   RhsExpression expr = new RhsExpression("bla.bla->we & rhs3.rhs2.rhs1->next",3);
	   System.out.println("raw expr=" + expr.getRawExpression());
   }


}
