package be.glever.antplus.hrm.datapage.background;

import be.glever.antplus.hrm.datapage.AbstractHRMDataPage;

public class HrmDataPage3ProductInformation extends AbstractHRMDataPage {

	public HrmDataPage3ProductInformation(byte[] dataPageBytes) {
		super(dataPageBytes);
	}

	public byte getHardwareVersion() {
		return super.getPageSpecificBytes()[0];
	}

	public byte getSoftwareVersion() {
		return super.getPageSpecificBytes()[1];
	}

	public byte getModelNumber() {
		return super.getPageSpecificBytes()[2];
	}

	@Override
	public byte getPageNumber() {
		return 0x3;
	}
}