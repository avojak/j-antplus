package be.glever.ant.message;

import java.util.Arrays;

import be.glever.ant.AntException;
import be.glever.ant.util.ByteUtils;

public abstract class AbstractAntMessage {

	private byte sync = (byte) 0xa4; // TODO synchronous mode (0xa5 used for WRITE mode) is currently unsupported

	public abstract byte getMessageId();

	public abstract byte[] getMessageContent();

	public abstract void setMessageBytes(byte[] messageContentBytes) throws AntException;

	public byte getCheckSum() {
		byte checksum = (byte) (sync ^ getMsgLength() ^ getMessageId());
		for (byte b : getMessageContent()) {
			checksum ^= b;
		}
		return checksum;
	}

	private byte getMsgLength() {
		return (byte) getMessageContent().length;
	}

	public void parse(byte[] bytes) throws AntException {
		validateNumberDataBytes(bytes);
		validateChecksum(bytes);
		setMessageBytes(Arrays.copyOfRange(bytes, 3, bytes.length - 1));
	}

	private void validateChecksum(byte[] bytes) throws AntException {
		byte calculatedChecksum = bytes[0];
		for (int i = 1; i < bytes.length - 1; i++) {
			calculatedChecksum ^= bytes[i];
		}

		byte checkSum = bytes[bytes.length - 1];
		if (calculatedChecksum != checkSum) {
			throw new AntException("Checksum doesnt match. Given: [" + checkSum + "]. Calculated: ["
					+ calculatedChecksum + "]. Message: " + ByteUtils.hexString(bytes));
		}
	}

	private void validateNumberDataBytes(byte[] bytes) throws AntException {
		byte nrDataBytes = bytes[1];
		if (nrDataBytes + 4 != bytes.length) {
			throw new AntException("Incorrect message length given [" + nrDataBytes + "] for byte array "
					+ ByteUtils.hexString(bytes));
		}
	}

}