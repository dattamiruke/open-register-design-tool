/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.ModComponent;
import ordt.extract.ModInstance;
import ordt.extract.PropertyList;
import ordt.extract.PropertyList.PropertyValue;

/** extracted properties of an instance created during model walk */
public class InstanceProperties {
	private String  id = "";           // current instance id (contains rep count suffix)
	private String instancePath = "";      // instance path
	//private boolean external = false;   // is instance set declared as external
	public enum ExtType { INTERNAL, DEFAULT, EXTERNAL_DECODE, BBV5, SRAM, SERIAL8, RING16 }
	private ExternalType externalType = new ExternalType(ExtType.INTERNAL);   // external interface type (init to internal)
	private boolean rootExternal = false;   // is instance root instance of an external reg set
	
	private String textName;  // text name of this instance
	private String textDescription;  // text description of this instance
	private String jspecSupersetCheck;  // jspec superset_check
	
	private boolean dontTest = false;  // default is test this component
	private boolean dontCompare = false;  // default is conpare this component
	
	private boolean useInterface = false;  // encapsulate logic IO in an interface?
	private String extInterfaceName;   // override name for logic IO intf encap
	
	protected ModInstance extractInstance;   // ptr to instance info in the extract model
	protected PropertyList instDefaultProperties;   // default properties for this instance

	private int repNum = 0;  // rep number of this instProperty if part of a replicated set
		
	/** create an instance and set basic info: id, extractInstance, external */
	public InstanceProperties(ModInstance extractInstance) {
		this.extractInstance = extractInstance;
		setId(extractInstance.getId());  // initialize id to that of extract instance
		instDefaultProperties = loadInstDefaultProperties();  // set default prop list
		//instDefaultProperties.updateProperties(getDefaultProperties());  // get default properties from extractInstance parent component
		//if (!instDefaultProperties.isEmpty()) System.out.println("--- InstanceProperties constructor:" + getId() + "\n" + instDefaultProperties);
		// set external status at object create (special case since it is used in model generateOutput and builder pushInstance)
		if (extractInstance.hasProperty("external"))  setExternal(extractInstance.getProperty("external"));
	}
	
	/** get default properties from extractInstance component and parent component
	 * @return 
	 */
	private PropertyList loadInstDefaultProperties() {
		//if (getId().equals("tx")) System.out.println("InstanceProperties loadInstDefaultProperties: " + getId() + "\n" + extractInstance.getProperties());
		// get parent and component of this instance
		ModComponent parent = extractInstance.getParent();
		ModComponent comp = extractInstance.getRegComp();
		PropertyList pList = new PropertyList();
		if (parent != null) pList.updateProperties(parent.getDefaultProperties());  
		if (comp != null) pList.updateProperties(comp.getDefaultProperties());  
		// since external default is assigned to an instance, add to default inst properties
		if (extractInstance.hasTrueProperty("external_decode")) {
			pList.setProperty("external_decode", "true");
		}
		return pList;		
	}

	public InstanceProperties(InstanceProperties oldInstance) {
		this.extractInstance = oldInstance.getExtractInstance();
		setId(oldInstance.getId());  
		setInstancePath(oldInstance.getInstancePath());  
		setExternalType(oldInstance.getExternalType());  
		setRootExternal(oldInstance.isRootExternal());  
		setTextName(oldInstance.getTextName());  
		setTextDescription(oldInstance.getTextDescription());  
		setJspecSupersetCheck(oldInstance.getJspecSupersetCheck());  
		setDontTest(oldInstance.isDontTest());  
		setDontCompare(oldInstance.isDontCompare());  
		setRepNum(oldInstance.getRepNum());  
		setUseInterface(oldInstance.useInterface());  
		setExtInterfaceName(oldInstance.getExtInterfaceName());  
		setInstDefaultProperties(oldInstance.getInstDefaultProperties());  
	}
	
	/** display info InstanceProperties info */
	public void display() {
		System.out.println("InstanceProperty, id=" + this.getId());  
		System.out.println("   path=" + this.getInstancePath());  
		System.out.println("   external=" + this.externalType);  
		System.out.println("   root external=" + this.isRootExternal());  
		System.out.println("   name=" + this.getTextName());  
		System.out.println("   description=" + this.getTextDescription());  
		System.out.println("   js_superset_check=" + this.getJspecSupersetCheck());  
		System.out.println("   donttest=" + this.isDontTest());  
		System.out.println("   dontcompare=" + this.isDontCompare());  
		System.out.println("   rep num=" + this.getRepNum());  		
		System.out.println("   use interface=" + this.useInterface());  		
		System.out.println("   external interface name=" + this.getExtInterfaceName());  		
		// display default properties
		System.out.println("    default properties:");
		System.out.println("        " + instDefaultProperties);
	}
	
	/** extract properties from the calling instance - this or overloaded child class method called from this.updateInstanceInfo */
	public void extractProperties(PropertyList pList) {
		// create name and description text
		if (pList.hasProperty("name")) setTextName(pList.getProperty("name")); 
		if (pList.hasProperty("desc")) setTextDescription(pList.getProperty("desc")); 
		if (pList.hasTrueProperty("use_new_interface"))  setUseInterface(true);
		else if (pList.hasProperty("use_interface") && !pList.hasBooleanProperty("use_interface")) {
			setUseInterface(true);
			setExtInterfaceName(pList.getProperty("use_interface"));
		}
		if (pList.hasProperty("js_superset_check")) setJspecSupersetCheck(pList.getProperty("js_superset_check"));
	}

	/** return true if this InstanceProperty is instanced by root
	 */
	public boolean isRootInstance() {
		return this.extractInstance.isRootInstance();
	}
	
	/** get id
	 *  @return the id
	 */
	public String getId() {
		return id;
	}

	/** set id
	 *  @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	} 
	
	/** get id with rep suffix removed
	 */
	public String getNoRepId() {
		return isReplicated() ? getId().replaceFirst("_\\d+$", "") : getId();
	}
	
	/** get id with rep as index
	 */
	public String getIndexedId() {
		if (!isReplicated()) return getId();
		return getNoRepId() + "[" + getRepNum() + "]";
	}

	/** get externalType
	 *  @return the externalType
	 */
	public ExternalType getExternalType() {
		return externalType;
	}

	/** set externalType
	 *  @param externalType the externalType to set
	 */
	public void setExternalType(ExternalType externalType) {
		this.externalType = externalType;
	}
	
	/* return true is instance externalType matches specified value */
	public boolean hasExternalType(ExtType eType) {
		return (externalType.getType() == eType);
	}

	/** get external
	 *  @return the external
	 */
	public boolean isExternal() {
		return ((externalType != null) && externalType.isExternal());
	}

	/** set external
	 *  @param external the external to set
	 */
	public void setExternal(String externalStr) {
		if (externalStr == null) this.externalType = new ExternalType(ExtType.INTERNAL);  // internal
		else if ("DEFAULT".equals(externalStr)) this.externalType = new ExternalType(ExtType.DEFAULT);
		else if ("EXTERNAL_DECODE".equals(externalStr)) this.externalType = new ExternalType(ExtType.EXTERNAL_DECODE);
		else if ("BBV5_8".equals(externalStr)) this.externalType = new ExternalType(ExtType.BBV5, 8);
		else if ("BBV5_16".equals(externalStr)) this.externalType = new ExternalType(ExtType.BBV5, 16);
		else if ("SRAM".equals(externalStr)) this.externalType = new ExternalType(ExtType.SRAM);
		else if (externalStr.matches("^SERIAL8_D\\d$")) {
			int delay = Integer.valueOf(externalStr.substring(externalStr.indexOf('_')+2));
			this.externalType = new ExternalType(ExtType.SERIAL8, delay);
		}
		else if (externalStr.matches("^RING16_D\\d$")) {
			int delay = Integer.valueOf(externalStr.substring(externalStr.indexOf('_')+2));
			this.externalType = new ExternalType(ExtType.RING16, delay);
		}
		else Ordt.errorExit("Invalid external interface type (" + externalStr + ") detected in instance " + getId());
		//System.out.println("InstanceProperties setExternal: input=" + externalStr + ", new val=" + this.externalType + ", inst=" + getId());
	}

	/** ExternalType class carrying parameters */
	public class ExternalType {
		private ExtType type = ExtType.INTERNAL;
		private Integer parm1;
		// constructors
		public ExternalType(ExtType type, Integer parm1) {
			this.type = type;
			this.parm1 = parm1;
		}
		public ExternalType(ExtType type) {
			this.type = type;
		}
		// getters setters
		public ExtType getType() {
			return type;
		}
		public boolean isExternal() {
			return (type != ExtType.INTERNAL);
		}
		public void setType(ExtType type) {
			this.type = type;
		}
		public Integer getParm1() {
			return parm1;
		}
		public void setParm1(Integer parm1) {
			this.parm1 = parm1;
		}
		@Override
		public String toString() {
			return type.toString() + "(" + parm1 + ")";
		}
	}
	
	/** get rootExternal (set by stack push into outputBuilder)
	 *  @return the rootExternal
	 */
	public boolean isRootExternal() {
		return rootExternal;
	}

	/** set rootExternal
	 *  @param rootExternal the rootExternal to set
	 */
	public void setRootExternal(boolean rootExternal) {
		this.rootExternal = rootExternal;
	}

	/** get total replication count
	 */
	public int getRepCount() {
		return extractInstance.getRepCount();
	}

	/** get repNum
	 *  @return the repNum
	 */
	public int getRepNum() {
		return repNum;
	}

	/** set repNum
	 *  @param repNum the repNum to set
	 */
	public void setRepNum(int repNum) {
		this.repNum = repNum;
	}

	/** return true if first rep in a set
	 */
	public boolean isFirstRep() {
		return (getRepNum() == 0);
	}

	/** return true if instance is last rep in a set
	 */
	public boolean isLastRep() {
		return ((getRepNum() + 1) == getRepCount());
	}

	/** return true if part of a replicated set
	 */
	public boolean isReplicated() {
		return (getRepCount() > 1);
	}

	/** get baseName
	 *  @return the baseName
	 */
	public String getBaseName() {
		return getInstancePath().replace('.', '_');
	}
	
	/** get instancePath
	 *  @return the instancePath
	 */
	public String getInstancePath() {
		return instancePath;
	}

	/** set instancePath
	 *  @param instancePath the instancePath to set
	 */
	private void setInstancePath(String instancePath) {
		this.instancePath = instancePath;
	}

	/** set instancePath, merge extract post property assigns, properties from extract ModInstance, and
	 *  default instance properties
	 *  Merged property list is then used to set component-specific instance info via extractProperties.
	 *  updateInstanceInfo method is called by the add* methods in OutputBuilder 
	 *  @param instancePath the instancePath
	 */
	public void updateInstanceInfo(String instancePath) {
		setInstancePath(instancePath);
		// before creating instance list update default instance properties 
		updateDefaultProperties(extractInstance.getDefaultProperties());
		// create a property list for holding combined info for this instance
		PropertyList mergedList = new PropertyList();
		// now add defined default instance properties
		mergedList.updateProperties(instDefaultProperties);   // start with instance defaults 
		//if (extractInstance.getRegComp().isReg() && getInstancePath().contains("stats.spin") && mergedList.hasProperty("category")) System.out.println("InstanceProperties extractInstance: inst=" + getInstancePath() + ", post inst defaults cat=" + mergedList.getProperty("category"));

		// now add the base property info
		mergedList.updateProperties(extractInstance.getProperties());   // TODO - need a similar call here to update instance defaults
		//if (extractInstance.getRegComp().isReg() && getInstancePath().contains("stats.spin") && mergedList.hasProperty("category")) System.out.println("InstanceProperties extractInstance: inst=" + getInstancePath() + ", post extractinst cat=" + mergedList.getProperty("category"));

		// extract post property assigns for this instance
		String fullInstancePath = Ordt.getModel().getRootInstance().getId() + "." + instancePath;  // include root instance in path
		PropertyList postAssignList = getPostPropertyAssigns(fullInstancePath);  // get assigns from all comps in inst path
		/*if (instancePath.endsWith("log_address1")) {
		//if (instancePath.endsWith("par_protect.interrupts.status.detected_hsl_req")) {
			System.out.println("InstanceProperties: i=" + instancePath);
			System.out.println("  old=" + mergedList);
			System.out.println("  dyn=" + postAssignList);		
		}*/
		
		mergedList.updateProperties(postAssignList);
		//if (extractInstance.getRegComp().isReg() && getInstancePath().contains("stats.spin") && mergedList.hasProperty("category")) System.out.println("InstanceProperties extractInstance: inst=" + getInstancePath() + ", post assigns cat=" + mergedList.getProperty("category"));
		extractProperties(mergedList);   // now that we have combined parameter list, extract instance info
		/*if (instancePath.contains("intr.poll_enable_")) {  
		//if (instancePath.endsWith("int_merge.int_status")) {  
			System.out.println("InstanceProperties updateInstanceInfo: i=" + instancePath);
			System.out.println("  final=" + mergedList);
			//System.out.println("  desc=" + getTextDescription());		
		}*/
	}

	/** extract post property assignments in all components in instance path */
	private PropertyList getPostPropertyAssigns(String instPathStr) {
		// split the path string
		List<String> instPath = new ArrayList<String>();
		String[] strArray = instPathStr.split("\\.");
		for (int i=0; i<=strArray.length-1; i++) {
			instPath.add(strArray[i]);
		}
		// get the list of components in the instance path
		List<String> iPath = new ArrayList<String>();  // save path since recursive getPath call is destructive
		iPath.addAll(instPath);
		//List<ModComponent> pathComps = extractInstance.getRegComp().getAncestorComponents(iPath, true);  // add root
		List<ModComponent> pathComps = Ordt.getModel().getRoot().getPathComponents(iPath);  // add all path components including root
		// check for postProperty assigns in each component and add to list
		PropertyList postAssignList = new PropertyList(); 
		//if (pathComps.size() != instPath.size())
		//   System.out.println("InstanceProperties getPostPropertyAssigns: path=" + instPathStr + ", components=" + pathComps.size() + ", path size=" + instPath.size());
		// find assigns matching instance for each component in instance path (root to leaf)
		for (ModComponent comp: pathComps) { 
			PropertyList assignList = comp.getPostPropertyAssigns(instPath);
			postAssignList.updateProperties(assignList, true);  // update but keep old key values
			instPath.remove(0);
		}
		return postAssignList;
	}

	/** get textName
	 *  @return the textName
	 */
	public String getTextName() {
		return textName;
	}

	/** set textName
	 *  @param textName the textName to set
	 */
	public void setTextName(String textName) {
		this.textName = resolveTextMacros(textName);
	}

	/** get textDescription
	 *  @return the textDescription
	 */
	public String getTextDescription() {
		return textDescription;
	}

	/** set textDescription
	 *  @param textDescription the textDescription to set
	 */
	public void setTextDescription(String textDescription) {
		this.textDescription = resolveTextMacros(textDescription);
	}
	
	/** resolve text macro embedded in name/descriptions */
	private String resolveTextMacros(String text) {
		if ((text == null) || !text.contains("%")) return text;
		String newText = text;
		newText = newText.replace("%instance", this.getId());
		return newText;
	}

	/** get jspecSupersetCheck
	 *  @return the jspecSupersetCheck
	 */
	public String getJspecSupersetCheck() {
		return jspecSupersetCheck;
	}

	/** set jspecSupersetCheck
	 *  @param jspecSupersetCheck the jspecSupersetCheck to set
	 */
	public void setJspecSupersetCheck(String jspecSupersetCheck) {
		this.jspecSupersetCheck = jspecSupersetCheck;
	}

	/** get dontTest
	 *  @return the dontTest
	 */
	public boolean isDontTest() {
		return dontTest;
	}

	/** set dontTest
	 *  @param dontTest the dontTest to set
	 */
	public void setDontTest(boolean dontTest) {
		this.dontTest = dontTest;
	}

	/** get dontCompare
	 *  @return the dontCompare
	 */
	public boolean isDontCompare() {
		return dontCompare;
	}

	/** set dontCompare
	 *  @param dontCompare the dontCompare to set
	 */
	public void setDontCompare(boolean dontCompare) {
		this.dontCompare = dontCompare;
	}

	/** get useInterface */
	public boolean useInterface() {
		return useInterface;
	}

	/** set useInterface
	 *  @param useInterface the useInterface to set
	 */
	public void setUseInterface(boolean useInterface) {
		this.useInterface = useInterface;
	}

	/** get extInterfaceName
	 *  @return the extInterfaceName
	 */
	public String getExtInterfaceName() {
		return extInterfaceName;
	}

	/** set extInterfaceName
	 *  @param extInterfaceName the extInterfaceName to set
	 */
	public void setExtInterfaceName(String extInterfaceName) {
		this.extInterfaceName = extInterfaceName;
	}

	/** get extractInstance
	 *  @return the extractInstance
	 */
	public ModInstance getExtractInstance() {
		return extractInstance;
	}
	
    /** get instDefaultProperties
	 *  @return the instDefaultProperties
	 */
	public PropertyList getInstDefaultProperties() {
		return instDefaultProperties;
	}

	/** set instDefaultProperties
	 *  @param instDefaultProperties the instDefaultProperties to set
	 */
	public void setInstDefaultProperties(PropertyList instDefaultProperties) {
		this.instDefaultProperties = instDefaultProperties;
	}

	/** update default properties of this instance with property list 
	 * called from pushInstance in OutputBuilder to pass these to child insts- does not overrride existing properties */
	public void updateDefaultProperties(PropertyList instDefaultProperties) {
		this.instDefaultProperties.updateProperties(instDefaultProperties, true);		
	}
	
	/** update default properties using values in supplied hashmap (override existing props)
	 *  @param hashmap of properties to be set
	 */
	public void updateDefaultProperties(HashMap<String, PropertyValue> updates) {
		instDefaultProperties.updateProperties(updates);
	}	

	public boolean hasDefaultProperty(String propName) {
		return instDefaultProperties.hasProperty(propName);
	}

	/** return true is true value of property is in instDefaultProperties */
	public boolean hasTrueDefaultProperty(String propName) {
		return instDefaultProperties.hasTrueProperty(propName);
	}

	public String getDefaultProperty(String propName) {
		return instDefaultProperties.getProperty(propName);
	}
    
	public static void main(String[] args) throws Exception {
    	ModInstance modInst = new ModInstance();
    	modInst.setId("padoody_10");
    	modInst.setRepCount(10);
    	InstanceProperties inst = new InstanceProperties(modInst);
    	System.out.println("orig=" + inst.getId() + ", norep=" + inst.getNoRepId() + ", indexed=" + inst.getIndexedId());
    }

}
