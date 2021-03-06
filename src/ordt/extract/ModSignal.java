/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

import ordt.output.OutputBuilder;
import ordt.output.SignalProperties;

public class ModSignal extends ModComponent {
	
	/** create a new indexed model instance of this component */
	@Override
	public ModIndexedInstance createNewInstance() {
		ModIndexedInstance newInst = new ModIndexedInstance();  // create the new instance
		newInst.setRegComp(this);  // set component type of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}

	// ------------------------------------ code gen templates ----------------------------------------
	
	/* generate output */
	@Override
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
		if (callingInst == null) return;
		
		SignalProperties signalProperties = new SignalProperties(callingInst);  // extract properties
		outputBuilder.pushInstance(signalProperties);  // instance path is valid after this

		outputBuilder.addSignal(signalProperties);  // add signal to verilog structures
		outputBuilder.popInstance();
	}
	

}
