package be.glever.anttest;

import be.glever.ant.message.AntMessage;
import be.glever.ant.message.data.BroadcastDataMessage;
import be.glever.ant.usb.AntUsbDevice;
import be.glever.ant.usb.AntUsbDeviceFactory;
import be.glever.antplus.common.datapage.AbstractAntPlusDataPage;
import be.glever.antplus.speedcadence.CadenceChannel;
import be.glever.antplus.speedcadence.SpeedChannel;
import be.glever.antplus.speedcadence.datapage.SpeedCadenceDataPageRegistry;
import be.glever.antplus.speedcadence.datapage.main.SpeedCadenceDataPage5Motion;
import be.glever.util.logging.Log;

import java.io.IOException;
import java.util.List;

public class SpeedCadenceTest_Main {
    private static final Log LOG = Log.getLogger(SpeedCadenceTest_Main.class);
    private SpeedCadenceDataPageRegistry registry = new SpeedCadenceDataPageRegistry();

    private int prevSpeedRevCount = 0;
    private int firstSpeedRevCount = 0;
    private long prevSpeedEventTime = 0;

    private int prevCadenceRevCount = 0;
    private int firstCadenceRevCount = 0;
    private long prevCadenceEventTime = 0;

    private SpeedCadenceTest_Main() throws IOException {
        try (AntUsbDevice device = AntUsbDeviceFactory.getAvailableAntDevices().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No devices found"))) {
            device.initialize();
            device.closeAllChannels(); // channels stay open on usb dongle even if program shuts down.

            SpeedChannel speedChannel = new SpeedChannel(device);
            speedChannel.getEvents().doOnNext(this::handle).subscribe();

            CadenceChannel cadenceChannel = new CadenceChannel(device);
            cadenceChannel.getEvents().doOnNext(this::handle).subscribe();

            System.in.read();
        }
    }

    public static void main(String[] args) throws Exception {
        new SpeedCadenceTest_Main();
    }

    private void handle(AntMessage antMessage) {
        if (antMessage instanceof BroadcastDataMessage) {
            BroadcastDataMessage msg = (BroadcastDataMessage) antMessage;
            byte[] payLoad = msg.getPayLoad();
            removeToggleBit(payLoad);
            AbstractAntPlusDataPage dataPage = registry.constructDataPage(payLoad);

            //LOG.debug(() -> "Received datapage " + dataPage.toString());
            if (dataPage instanceof SpeedCadenceDataPage5Motion) {
                calcSpeedDistance((SpeedCadenceDataPage5Motion) dataPage, 69);
                calcCadence((SpeedCadenceDataPage5Motion) dataPage);
            }
        } else {
            //LOG.warn(()->format("Ignoring message  %s", antMessage));
        }
    }

    private void calcSpeedDistance(SpeedCadenceDataPage5Motion dataPage, double diameter) {
        int curRevCount = dataPage.getCumulativeRevolutions();

        if (firstSpeedRevCount == 0)
            firstSpeedRevCount = curRevCount;

        // Skip this, if we get the same measurement as last time
        if (firstSpeedRevCount == curRevCount)
            return;

        double circumference = Math.PI * diameter;
        long speedEventTime = dataPage.getEventTime();
        boolean isMoving = dataPage.isMoving();

        // Can only calculate speed, if we've actually moved yet
        double speed = (prevSpeedEventTime == 0) ? 0 : calculateSpeed(circumference, prevSpeedRevCount, curRevCount, prevSpeedEventTime, speedEventTime);
        double kmhSpeed = speed * 3.6;
        double travelledDistance = calculateDistance(circumference, curRevCount, firstSpeedRevCount);

        System.out.println("The bike is currently" + (isMoving ? "" : " not") + " moving at " + kmhSpeed + " km/h and has travelled " + travelledDistance + " m.");

        prevSpeedRevCount = curRevCount;
        prevSpeedEventTime = speedEventTime;
    }

    /**
     * Calculate the speed in m/s from a current and a previous measurement
     *
     * @param circumference of the wheel
     * @param prevRevCount
     * @param curRevCount
     * @param prevTime
     * @param curTime
     * @return
     */
    private double calculateSpeed(double circumference, int prevRevCount, int curRevCount, long prevTime, long curTime) {
        double timeDiff = curTime - prevTime;
        double revDiff = curRevCount - prevRevCount;
        return 1000 * circumference * (revDiff / timeDiff);
    }

    private double calculateDistance(double circumference, int curRevCount, int firstRevCount) {
        return circumference * (curRevCount - firstRevCount);
    }

    private void calcCadence(SpeedCadenceDataPage5Motion dataPage) {
        int curCadenceRevCount = dataPage.getCumulativeRevolutions();

        if (firstCadenceRevCount == 0)
            firstCadenceRevCount = curCadenceRevCount;

        // Skip this, if we get the same measurement as last time
        if (prevCadenceRevCount == curCadenceRevCount)
            return;

        long speedEventTime = dataPage.getEventTime();

        // Can only calculate speed, if we've actually moved yet
        double cadence = (prevCadenceEventTime == 0) ? 0 : calculateCadence(prevCadenceRevCount, curCadenceRevCount, prevCadenceEventTime, speedEventTime);

        System.out.println("The crank is being rotated at " + cadence + " RPM.");

        prevCadenceRevCount = curCadenceRevCount;
        prevCadenceEventTime = speedEventTime;
    }

    private double calculateCadence(int prevRevCount, int curRevCount, long prevTime, long curTime) {
        double timeDiff = curTime - prevTime;
        int revDiff = curRevCount - prevRevCount;
        return 60 * (revDiff / timeDiff);
    }

    /**
     * For the moment not taking the legacy hrm devices into account.
     * Non-legacy devices swap the first bit of the pageNumber every 4 messages.
     *
     * @param payLoad
     */
    private void removeToggleBit(byte[] payLoad) {
        payLoad[0] = (byte) (0b01111111 & payLoad[0]);
    }
}
