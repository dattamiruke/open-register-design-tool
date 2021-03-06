/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import ordt.extract.ModAddressableInstance;
import ordt.extract.ModInstance;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.parameters.ExtParameters;

/** extracted properties of an addressable instance (reg/regset properties) created during model walk */
public class AddressableInstanceProperties extends InstanceProperties {

	protected RegNumber baseAddress;
	protected RegNumber relativeBaseAddress;   // base address of reg relative to parent
	
	// external register group parameters
	protected int extAddressWidth = 0;   // width of word address range for this group
	protected int extLowBit = 0;  // low bit in external address range
	
	protected boolean addressMap = false;   // is an external address map
	private boolean externalDecode = false;   // inst declared as external decode

	public AddressableInstanceProperties(ModInstance extractInstance) {
		super(extractInstance);
	}

	public AddressableInstanceProperties(AddressableInstanceProperties oldInstance) {
		super(oldInstance);
		// set AddressableInstanceProperty info
		setRelativeBaseAddress(oldInstance.getRelativeBaseAddress());  
		setBaseAddress(oldInstance.getBaseAddress());  
		setExtAddressWidth(oldInstance.getExtAddressWidth());  
		setExtLowBit(oldInstance.getExtLowBit());  
		setAddressMap(oldInstance.isAddressMap());  
		setExternalDecode(oldInstance.isExternalDecode());  
	}
	
	/** display info AddressableInstanceProperties info */
	public void display() {
		super.display();
		System.out.println("  AddressableInstanceProperty info:" );  
		System.out.println("   base address=" + this.getBaseAddress());  		
		System.out.println("   relative base address=" + this.getRelativeBaseAddress());  		
		System.out.println("   ext addr width=" + this.getExtAddressWidth());  		
		System.out.println("   ext addr low bit=" + this.getExtLowBit());  		
		System.out.println("   is address map=" + this.isAddressMap());  		
		System.out.println("   external decode=" + this.isExternalDecode());  		
	}

	/** get baseAddress
	 *  @return the baseAddress
	 */
	public RegNumber getBaseAddress() {
		return baseAddress;
	}

	/** set baseAddress
	 *  @param baseAddress the baseAddress to set
	 */
	public void setBaseAddress(RegNumber baseAddress) {
		this.baseAddress = new RegNumber(baseAddress);  // use a copy, not reference
	}

	/** get full base address including base offset
	 */
	public RegNumber getFullBaseAddress() {
		RegNumber fullBase = new RegNumber(ExtParameters.getLeafBaseAddress());  
		fullBase.setVectorLen(ExtParameters.getLeafAddressSize());
		fullBase.setNumBase(NumBase.Hex);
		fullBase.setNumFormat(NumFormat.Address);
		fullBase.add(getBaseAddress());
		return fullBase;
	}

	/** get relativeBaseAddress
	 *  @return the relativeBaseAddress
	 */
	public RegNumber getRelativeBaseAddress() {
		return relativeBaseAddress;
	}

	/** get pre-computed aligned size of this instance (single rep - calls component.getAlignedSize) 
	 */
	public RegNumber getAlignedSize() {
		return getExtractInstance().getRegComp().getAlignedSize();
	}

	/** set relativeBaseAddress
	 *  @param relativeBaseAddress the relativeBaseAddress to set
	 */
	public void setRelativeBaseAddress(RegNumber relativeBaseAddress) {
		this.relativeBaseAddress = new RegNumber(relativeBaseAddress);  // use a copy, not reference;
	}
	/** get extAddressWidth
	 *  @return the extAddressWidth
	 */
	public int getExtAddressWidth() {
		return extAddressWidth;
	}

	/** set extRegWidth
	 *  @param extRegWidth the extRegWidth to set
	 */
	public void setExtAddressWidth(int extAddressWidth) {
		this.extAddressWidth = extAddressWidth;
	}

	/** get the low bit index of external address range
	 */
	public int getExtLowBit() {
		return extLowBit;
	}

	/** set extLowBit
	 *  @param extLowBit the extLowBit to set
	 */
	public void setExtLowBit(int extLowBit) {
		this.extLowBit = extLowBit;
	}

	/** get addressMap
	 *  @return the addressMap
	 */
	public boolean isAddressMap() {
		return addressMap;
	}

	/** set addressMap
	 *  @param addressMap the addressMap to set
	 */
	public void setAddressMap(boolean addressMap) {
		this.addressMap = addressMap;
	}

	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModAddressableInstance getExtractInstance() {
		return (ModAddressableInstance) extractInstance;
	}

	/** get externalDecode
	 *  @return the externalDecode
	 */
	public boolean isExternalDecode() {
		return externalDecode;
	}

	/** set externalDecode
	 *  @param externalDecode the externalDecode to set
	 */
	public void setExternalDecode(boolean externalDecode) {
		//System.out.println("AddressableInstanceProperties setExternalDecode: " + externalDecode);
		this.externalDecode = externalDecode;
	}

}
