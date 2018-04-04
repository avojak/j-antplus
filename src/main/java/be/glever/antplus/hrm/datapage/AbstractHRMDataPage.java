package be.glever.antplus.hrm.datapage;

import be.glever.ant.util.ByteUtils;
import be.glever.antplus.hrm.common.datapage.AbstractDataPage;

import java.util.Arrays;

public abstract class AbstractHRMDataPage extends AbstractDataPage {

	public AbstractHRMDataPage(byte[] dataPageBytes) {
		super(dataPageBytes);
	}

	/**
	 * Subclasses typically parse this byte to useful data.
	 */
	protected byte[] getPageSpecificBytes() {
		return Arrays.copyOfRange(getDataPageBytes(), 1, 4);
	}

	/**
	 * Time of last valid heartbeat event in milliseconds sinds "epoch".
	 * Note that the ANT+ field size for this is 16 bit and resolution is 1/1024 seconds,
	 * so the  value rolls over at 63.999 seconds ( ((1/1024)*(2^17)) - (1/1024)) ).
	 */
	public int getHeartBeatEventTime() {
		int timeAnt = ByteUtils.toInt(getDataPageBytes()[4], getDataPageBytes()[5]);
		return (timeAnt * 1024) / 1000;
	}

	/**
	 * Number of heartbeats since rollover.
	 * Ant+ field size is 1 byte so rollover = 256.
	 */
	public int getHeartBeatCount() {
		return getDataPageBytes()[6];
	}

	/**
	 * Heart rate computed by device. Meant for direct display without further interpretation.
	 * @return The heart rate computed by device or -1 if invalid.
	 */
	public int getComputedHeartRateInBpm() {
		return getDataPageBytes()[7];
	}
}