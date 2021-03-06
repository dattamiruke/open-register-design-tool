/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.parameters.ExtParameters;

/** system verilog module generation class
 *  
 * uses the following builder methods:
 *   writeStmt(s)
 *   static isLegacyVerilog()
 *   + SystemVerilogRegisters
 * */
public class SystemVerilogModule {
	
	protected SystemVerilogBuilder builder;  // builder creating this module
	protected String name;  // module name
	protected Integer insideLocs;
	protected boolean useInterfaces = false;  // will interfaces be used in module io
	protected boolean addBaseAddrParameter  = false;	// will base addr parm be created in module io
	protected List<Instance> instanceList = new ArrayList<Instance>();  // list of child instances
	
	protected List<SystemVerilogIOSignalList> ioList = new ArrayList<SystemVerilogIOSignalList>();  // list of IO lists in this module
	protected HashMap<Integer, SystemVerilogIOSignalList> ioHash = new HashMap<Integer, SystemVerilogIOSignalList>();  // set of writable IO lists in this module

	protected SystemVerilogSignalList wireDefList;    // list of wires   
	protected SystemVerilogSignalList regDefList;    // list of reg definitions	
	protected List<String> wireAssignList = new ArrayList<String>();    // list of wire assign statements
	protected SystemVerilogRegisters registers;   // set of register info for module
	protected HashSet<String> definedSignals = new HashSet<String>();   // set of all user defined reg/wire names for this module (to check for duplicates/resolve as valid)
	protected List<String> statements = new ArrayList<String>();    // list of free form verilog statements
    protected boolean showDuplicateSignalErrors = true;
    
	protected SystemVerilogCoverGroups coverGroups;   // set of cover group info for module
    
	/** create a module
	 * @param insideLocs - ORed Integer of locations in this module 
	 * @param defaultClkName - default clock name used for generated registers
	 */
	public SystemVerilogModule(SystemVerilogBuilder builder, int insideLocs, String defaultClkName) {
		this.builder = builder;  // save reference to calling builder
		this.insideLocs = insideLocs;  // locations inside this module
		registers = new SystemVerilogRegisters(builder, defaultClkName);
		wireDefList = new SystemVerilogSignalList();
		regDefList = new SystemVerilogSignalList();
		coverGroups = new SystemVerilogCoverGroups(builder, defaultClkName, builder.getDefaultReset());  // TODO - need to change cover reset if separate logic reset is being used
	}

	// ------------------- get/set -----------------------
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setShowDuplicateSignalErrors(boolean showDuplicateSignalErrors) {
		this.showDuplicateSignalErrors = showDuplicateSignalErrors;
	}

	public boolean useInterfaces() {
		return useInterfaces;
	}

	public boolean addBaseAddrParameter() {
		return addBaseAddrParameter;
	}

	public void setAddBaseAddrParameter(boolean addBaseAddrParameter) {
		this.addBaseAddrParameter = addBaseAddrParameter;
	}

	public void setUseInterfaces(boolean useInterfaces) {
		this.useInterfaces = useInterfaces;
	}

	// ------------------- wire/reg assign methods -----------------------

	/** add a wire assign statement */
	public void addWireAssign(String assignString) {
		wireAssignList.add(assignString);
	}

	/** add a list of wire assign statements */
	public void addWireAssigns(List<String> assignList) {
		wireAssignList.addAll(assignList);		
	}

	/** return the list of wire assigns */
	public List<String> getWireAssignList() {
		return wireAssignList;
	}

	/** add a combinatorial reg assign */
	public void addCombinAssign(String groupName, String assign) {
		registers.get(groupName).addCombinAssign(assign);  
	}

	/** add a list of combinatorial reg assigns */
	public void addCombinAssign(String groupName, List<String> assignList) {
		registers.get(groupName).addCombinAssign(assignList);  
	}

	/** add a combinatorial reg assign with specified precedence */
	public void addPrecCombinAssign(String groupName, boolean hiPrecedence, String assign) {
		registers.get(groupName).addPrecCombinAssign(hiPrecedence, assign);		
	}

	/** add a sequential reg assign */
	public void addRegAssign(String groupName, String assign) {
		 registers.get(groupName).addRegAssign(assign);
	}

	public SystemVerilogRegisters getRegisters() {
		return registers;
	}

	/** add a reset to this modules reg group */
	public void addReset(String resetName, boolean activeLow) {
		registers.addReset(resetName, activeLow);
	}

	/** add a reset assign to this modules reg group */
	public void addResetAssign(String groupName, String resetName, String assign) {
		registers.get(groupName).addResetAssign(resetName, assign);
	}

	/** add a wire define */
	public void addVectorWire(String name, int idx, Integer width) {
		if (addDefinedSignal(name)) wireDefList.addVector(name, idx, width);	
	}

	/** add a scalar wire define */
	public void addScalarWire(String name) {
		if (addDefinedSignal(name)) wireDefList.addScalar(name);	
	}

	public void addWireDefs(SystemVerilogSignalList wireList) {
		addWireDefs(wireList.getSignalList());		
	}

    /** add a list of signals to the wire def list - unroll the loop for uniqueness check */
	public void addWireDefs(List<SystemVerilogSignal> wireList) {
		for (SystemVerilogSignal sig : wireList) addVectorWire(sig.getName(), sig.getLowIndex(), sig.getSize());
	}
	
	/** return the list of defined wires */
	public SystemVerilogSignalList getWireDefList() {
		return wireDefList;
	}

	/** add a reg define */
	public void addVectorReg(String name, int idx, Integer width) {
		if (addDefinedSignal(name)) regDefList.addVector(name, idx, width);	
	}

	/** add a scalar reg define */
	public void addScalarReg(String name) {
		if (addDefinedSignal(name)) regDefList.addScalar(name);	
	}

	public void addRegDefs(SystemVerilogSignalList regList) {
		addRegDefs(regList.getSignalList());		
	}

    /** add a list of signals to the reg def list - unroll the loop for uniqueness check */
	public void addRegDefs(List<SystemVerilogSignal> regList) {
		for (SystemVerilogSignal sig : regList) addVectorReg(sig.getName(), sig.getLowIndex(), sig.getSize());
	}

	/** return the list of defined regs */
	public SystemVerilogSignalList getRegDefList() {
		return regDefList;
	}

	/** add a signal to list and check for uniqueness
	 *  returns true on success                          */
	public boolean addDefinedSignal(String name) {
		if (definedSignals.contains(name)) {
			if (showDuplicateSignalErrors) Ordt.errorMessage("Duplicate SystemVerilog signal " + name + " detected (possibly due to a repeated instance name)");
			//if (name.startsWith("leaf_dec")) System.out.println("SystemVerilogModule addDefinedSignal: not adding " + name + " to module " + getName() + " signal list");
		}
		else {
			definedSignals.add(name);
			/* if (name.startsWith("leaf_dec"))*/ //System.out.println("SystemVerilogModule addDefinedSignal: adding " + name + " to module " + getName() + " signal list");
			return true;
		}
		//System.out.println("SystemVerilogModule addDefinedSignal: adding " + name + " to module signal list");
		return false;
	}
	
	/** return true if specified signal name is in the definedSignal set */
	public boolean hasDefinedSignal(String name) {
		return definedSignals.contains(name);
	}
	
	/** create a coverpoint and add it to specified covergroup in this module
	 *  @param group - name of covergroup
	 *  @param name - name of new coverpoint
	 *  @param signal - signal to be sampled
	 */
	public void addCoverPoint(String group, String name, String signal, String condition) {
		coverGroups.addCoverPoint(group, name, signal, condition);
	}
	
	// ------------------- IO methods  -----------------------

	/** add an IO list to be used by this module
	 * 
	 * @param sigList - list to be added
	 * @param remoteLocation - optional location, created signals to/from this loc will be added to this list
	 */
	public void useIOList(SystemVerilogIOSignalList sigList, Integer remoteLocation) {
		//System.out.println("SystemVerilogModule useIOList: adding io siglist with " + sigList.size());
		ioList.add(sigList);
		if (remoteLocation != null) ioHash.put(remoteLocation, sigList);
	}

	/** return a single combined IO List for the module */
	public SystemVerilogIOSignalList getFullIOSignalList() {
		SystemVerilogIOSignalList retList = new SystemVerilogIOSignalList();	
		// add io lists
		for (SystemVerilogIOSignalList list : ioList)
		   retList.addList(list);
		return retList;
	}
	
	/** return inputs for this module */ 
	public List<SystemVerilogIOSignal> getInputList() {
		SystemVerilogIOSignalList fullList = getFullIOSignalList();	// start with the full list
		return useInterfaces ? fullList.getEncapsulatedIOSignalList(null, insideLocs) :
			                     fullList.getIOSignalList(null, insideLocs);
	}
	
	/** return inputs for this module */ 
	public List<SystemVerilogIOSignal> getOutputList() {
		SystemVerilogIOSignalList fullList = getFullIOSignalList();	// start with the full list
		return useInterfaces ? fullList.getEncapsulatedIOSignalList(insideLocs, null) :
                                 fullList.getIOSignalList(insideLocs, null);
	}
	
	/** return inputs for this module */ 
	public List<SystemVerilogIOSignal> getInputOutputList() {
		List<SystemVerilogIOSignal> retList = new ArrayList<SystemVerilogIOSignal>();
		retList.addAll(getInputList());
		retList.addAll(getOutputList());
		return retList;
	}

	/** return a list of non-interface signals in this module matching the from/to constraint
	 * @param from location
	 * @param to location
	 * @return - list of SystemVerilogSignals
	 */
	public List<SystemVerilogSignal> getSignalList(Integer from, Integer to) {
		SystemVerilogIOSignalList fullList = getFullIOSignalList();	// start with the full list
		return fullList.getSignalList(from, to);
	}
	
	/** return a list of strings defining this module's IO (systemverilog format) */ 
	public List<String> getIODefStrList() {
		List<String> outList = new ArrayList<String>();
		List<SystemVerilogIOSignal> inputList = getInputList();
		List<SystemVerilogIOSignal> outputList = getOutputList();
		Boolean hasOutputs = (outputList.size() > 0);
		outList.add("(");
		// generate input def list
		Iterator<SystemVerilogIOSignal> it = inputList.iterator();
		while (it.hasNext()) {
			SystemVerilogIOSignal elem = it.next();
			if (elem.isFirstRep()) {
				String suffix = (it.hasNext() || hasOutputs) ? "," : " );";
				if (elem.isIntfSig()) outList.add("  " + elem.getInterfaceDefName() + "  " + elem.getNoRepName() + elem.getIntfDefArray() + suffix);   // interface
				else outList.add("  input    " + elem + suffix);   // logic				
			}
		}		   	
		// generate output def list
		outList.add("");
		it = outputList.iterator();
		while (it.hasNext()) {
			SystemVerilogIOSignal elem = it.next();
			if (elem.isFirstRep()) {
				String suffix = (it.hasNext()) ? "," : " );";
				if (elem.isIntfSig()) outList.add("  " + elem.getInterfaceDefName() + "  " + elem.getNoRepName() + elem.getIntfDefArray() + suffix);   // interface
				else outList.add("  output    " + elem + suffix);   // logic				
			}
		}		   	
		return outList;
	}
	
	/** return a list of strings listing this module's IO (verilog compatible format) */ //TODO
	public List<String> getLegacyIOStrList() {
		List<String> outList = new ArrayList<String>();
		List<SystemVerilogIOSignal> inputList = getInputList();
		List<SystemVerilogIOSignal> outputList = getOutputList();
		Boolean hasOutputs = (outputList.size() > 0);
		outList.add("(");
		// generate input sig list
		Iterator<SystemVerilogIOSignal> it = inputList.iterator();
		while (it.hasNext()) {
			SystemVerilogIOSignal elem = it.next();
			if (elem.isFirstRep()) {
				String suffix = (it.hasNext() || hasOutputs) ? "," : " );";
				// ignore interfaces
				if (!elem.isIntfSig())  outList.add("  " + elem.getName() + suffix);   // logic				
			}
		}		   	
		// generate output sig list
		outList.add("");
		it = outputList.iterator();
		while (it.hasNext()) {
			SystemVerilogIOSignal elem = it.next();
			if (elem.isFirstRep()) {
				String suffix = (it.hasNext()) ? "," : " );";
				// ignore interfaces
				if (!elem.isIntfSig())   outList.add("  " + elem.getName() + suffix);   // logic				
			}
		}		   	
		return outList;
	}
	
	/** return a list of strings defining this module's IO (verilog compatible format) */ //TODO
	public List<String> getLegacyIODefStrList() {
		List<String> outList = new ArrayList<String>();
		List<SystemVerilogIOSignal> inputList = getInputList();
		List<SystemVerilogIOSignal> outputList = getOutputList();
		outList.add("");
		outList.add("  //------- inputs");
		// generate input def list
		Iterator<SystemVerilogIOSignal> it = inputList.iterator();
		while (it.hasNext()) {
			SystemVerilogIOSignal elem = it.next();
			if (elem.isFirstRep()) {
				// ignore interfaces
				if (!elem.isIntfSig())   outList.add("  input    " + elem + ";");   // logic				
			}
		}		   	
		// generate output def list
		outList.add("");
		outList.add("  //------- outputs");
		it = outputList.iterator();
		while (it.hasNext()) {
			SystemVerilogIOSignal elem = it.next();
			if (elem.isFirstRep()) {
				// ignore interfaces
				if (!elem.isIntfSig())   outList.add("  output    " + elem + ";");   // logic				
			}
		}		   	
		return outList;
	}
	
	/** add a new scalar IO signal to the specified external location */
	public void addScalarTo(Integer to, String name) {
		this.addVectorTo(to, name, 0, 1);
	}

	/** add a new scalar IO signal from the specified external location */
	public void addScalarFrom(Integer from, String name) {
		this.addVectorFrom(from, name, 0, 1);
	}

	/** add a new vector IO signal to the specified external location */
	public void addVectorTo(Integer to, String name, int lowIndex, int size) {
		SystemVerilogIOSignalList sigList = ioHash.get(to);  // get the siglist
		if (sigList == null) return;
		sigList.addVector(insideLocs, to, name, lowIndex, size); 
	}

	/** add a new vector IO signal from the specified external location */
	public void addVectorFrom(Integer from, String name, int lowIndex, int size) {
		SystemVerilogIOSignalList sigList = ioHash.get(from);  // get the siglist
		if (sigList == null) return;
		sigList.addVector(from, insideLocs, name, lowIndex, size); 
	}
	
    /** add a freeform statement to this module */
	public void addStatement(String stmt) { 
		statements.add(stmt);
	}

	// ------------------- child instance methods/classes  -----------------------

	public void addInstance(SystemVerilogModule mod, String name) {
		instanceList.add(new Instance(mod, name));
	}
	
	private class Instance { 
		private SystemVerilogModule mod;
		private String name;
		
		public Instance(SystemVerilogModule mod, String name) {
			this.mod=mod;
			this.name=name;
			//System.out.println("SystemVerilogModule addInstance: mod=" + mod.getName() + ", name=" + name);
		}

		public SystemVerilogModule getMod() {
			return mod;
		}
		public String getName() {
			return name;
		}
		
	}
	
	// ------------------- output write methods  -----------------------

	/** write module stmt */
	public  void writeModuleBegin(int indentLevel) {
		builder.writeStmt(indentLevel, "//");
		builder.writeStmt(indentLevel, "//---------- module " + getName());
		builder.writeStmt(indentLevel, "//");
		builder.writeStmt(indentLevel, "module " + getName());		
	}

	/** write module stmt w no io */
	public  void writeNullModuleBegin(int indentLevel) {
		builder.writeStmt(indentLevel, "//");
		builder.writeStmt(indentLevel, "//---------- module " + getName());
		builder.writeStmt(indentLevel, "//");
		builder.writeStmt(indentLevel, "module " + getName() + " ( );");		
	}
	
	/** write module end */
	public  void writeModuleEnd(int indentLevel) {
		builder.writeStmt(indentLevel, "endmodule\n");	
	}

	/** write wire define stmts */
	public  void writeWireDefs(int indentLevel) {
		List<String> defList = wireDefList.getDefNameList();
		if (defList.isEmpty()) return;
		builder.writeStmt(indentLevel, "//------- wire defines");
		Iterator<String> it = defList.iterator();
		while (it.hasNext()) {
			String elem = it.next();
			    if (SystemVerilogBuilder.isLegacyVerilog()) builder.writeStmt(indentLevel, "wire  " + elem + ";");  
			    else builder.writeStmt(indentLevel, "logic  " + elem + ";");  
		}		   	
		builder.writeStmt(indentLevel, "");  		
	}

	/** write reg define stmts */
	public  void writeRegDefs(int indentLevel) {
		List<String> defList = regDefList.getDefNameList();
		if (defList.isEmpty()) return;
		builder.writeStmt(indentLevel, "//------- reg defines");
		Iterator<String> it = defList.iterator();
		while (it.hasNext()) {
			String elem = it.next();
			if (SystemVerilogBuilder.isLegacyVerilog()) builder.writeStmt(indentLevel, "reg  " + elem + ";");
			else builder.writeStmt(indentLevel, "logic  " + elem + ";");  
		}		   	
		builder.writeStmt(indentLevel, "");  		
	}

	/** write assign stmts  */
	public  void writeWireAssigns(int indentLevel) {
		if (wireAssignList.isEmpty()) return;
		builder.writeStmt(indentLevel, "//------- assigns");
		Iterator<String> it = wireAssignList.iterator();
		while (it.hasNext()) {
			String elem = it.next();
			builder.writeStmt(indentLevel, "assign  " + elem);  
		}		   	
		builder.writeStmt(indentLevel, "");  		
	}
	
	/** write always block assign stmts  */
	public  void writeBlockAssigns(int indentLevel) {
		registers.writeVerilog(indentLevel);  // write always blocks for each group
	}
	
	/** write cover group stmts  */
	public  void writeCoverGroups(int indentLevel) {
		if (!SystemVerilogBuilder.isLegacyVerilog()) {
			coverGroups.write(indentLevel);  // write for each covergroup
		}
	}
	
	/** write write IO definitions for this module
	 * @param indentLevel
	 * @param showInterfaces - if true include interfaces in output, else output encapsulated logic signals
	 * @param addBaseAddressParameter
	 */
	public void writeIOs(int indentLevel) {
		// if legacy format, add the parm list
		if (!useInterfaces) builder.writeStmts(0, getLegacyIOStrList());
		
		// add base addr param if specified TODO - replace w generic parameter list
		if (addBaseAddrParameter) {
			RegNumber baseAddr = new RegNumber(ExtParameters.getLeafBaseAddress());
			baseAddr.setNumBase(RegNumber.NumBase.Hex);
			baseAddr.setNumFormat(RegNumber.NumFormat.Verilog);
			baseAddr.setVectorLen(ExtParameters.getLeafAddressSize());
		    builder.writeStmt(indentLevel, "");
		    builder.writeStmt(indentLevel, "parameter BASE_ADDR = " + baseAddr + ";");
		}
		// write IO definitions  // TODO - using legacy vlog format if no interfaces for compatibility
		if (useInterfaces) builder.writeStmts(indentLevel+1, getIODefStrList());   // sv format
		else builder.writeStmts(0, getLegacyIODefStrList());  //vlog format
		builder.writeStmt(0, "");
	}

	/** write each child instance in this module */
	public void writeChildInstances(int indentLevel) {
		for (Instance inst : instanceList) {
			//System.out.println("SystemVerilogModule writeChildInstances: inst=" + inst.getName());
			inst.getMod().writeInstance(indentLevel, inst.getName());
		}		
	}

	/** write an instance of this module */
	public void writeInstance(int indentLevel, String instName) {
		List<SystemVerilogIOSignal> childList = this.getInputOutputList();
		if (childList.isEmpty()) return;
	    String baseAddrStr = this.addBaseAddrParameter() ? " #(BASE_ADDR) " : " ";
	    if (SystemVerilogBuilder.isLegacyVerilog()) {
			builder.writeStmt(indentLevel++, this.getName() + baseAddrStr + instName + " (");   // more elements so use comma
			Iterator<SystemVerilogIOSignal> it = childList.iterator();
			Boolean anotherElement = it.hasNext();
			while (anotherElement) {
				SystemVerilogSignal elem = it.next();
				if (it.hasNext()) {
					builder.writeStmt(indentLevel, "." + elem.getName() + "(" + elem.getName() + "),");   // more elements so use comma
					anotherElement = true;
				}
				else {
					anotherElement = false;
					builder.writeStmt(indentLevel, "." + elem.getName() + "(" + elem.getName() + ") );");   // no more elements so close
				}
			}		   		    	
	    }
	    else {
			builder.writeStmt(indentLevel++, this.getName() + baseAddrStr + instName + " ( .* );");   // more elements so use comma	    	
	    }
		builder.writeStmt(indentLevel--, "");   
	}

	/** write any free form statements */
	public void writeStatements(int indentLevel) {
		for (String stmt : statements) builder.writeStmt(indentLevel, stmt);
		builder.writeStmt(indentLevel, "");	
	}

	/** write this module */
	public void write() {
		// start the module
		int indentLevel = 0;
		writeModuleBegin(indentLevel);
		indentLevel++;
		
		// write internal structures
		writeModuleInternals(indentLevel);

		indentLevel--;
		writeModuleEnd(indentLevel);
	}

	/** write module internal structures */
	protected void writeModuleInternals(int indentLevel) {
		
		// write inputs, outputs
		writeIOs(indentLevel);
		
		// write wire define stmts
		writeWireDefs(indentLevel);
		
		// write ff define stmts
		writeRegDefs(indentLevel);
		
		// write free form statements
		writeStatements(indentLevel);
		
		// write assign stmts
		writeWireAssigns(indentLevel);  
		
		// write block assign stmts
		writeBlockAssigns(indentLevel);  
		
		// write the child instances
		writeChildInstances(indentLevel);
		
		// write the coverage groups
		writeCoverGroups(indentLevel);
	}

}
