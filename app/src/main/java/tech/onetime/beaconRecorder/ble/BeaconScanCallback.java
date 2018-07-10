package tech.onetime.beaconRecorder.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tech.onetime.beaconRecorder.schema.BeaconObject;


/**
 * Created by Alexandro on 2016/7/5.
 */

public class BeaconScanCallback implements KitkatScanCallback.iKitkatScanCallback, LollipopScanCallback.iLollipopScanCallback {

    private final String TAG = "OPBeaconScanCallback";

    private iBeaconScanCallback scanCallback;

    private LollipopScanCallback lollipopScanCallback;
    private KitkatScanCallback kitkatLeScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private ScanFilter.Builder _filterBuilder;
    private long lastScannedTime = 0;
    private Timer timer = null;
    private Boolean flag = true;
    private long firstBeaconTime;
    public BeaconScanCallback(Context ctx, iBeaconScanCallback scanCallback) {

        this.scanCallback = scanCallback;

        BluetoothManager bm = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bm.getAdapter();
        startTimerTask();

    }
    class timerTask extends TimerTask{

        @Override
        public void run() {
            scanCallback.getNearestBeacon(syncBeacons.getIns().getNearest());
            scanCallback.getCurrentRoundBeacon(syncBeacons.getIns().getBeacons());
        }
    }

    public void startTimerTask(){
        Log.d(TAG, "********startTimerTask***********" + System.currentTimeMillis());

        timer = new Timer();
        timer.schedule(new timerTask(), 100L,100L);
    }


    public void closeTimerTask(){
        if(timer!=null){
            timer.cancel();
        }
        Log.d(TAG, "closeTimerTask---------" + System.currentTimeMillis());

    }
    public interface iBeaconScanCallback {

        void scannedBeacons(BeaconObject beaconObject);

        void getNearestBeacon(BeaconObject beaconObject);

        void getCurrentRoundBeacon(ArrayList<BeaconObject> BeaconObjectArray);

        void testInterface();

    }

    public void startScan() {

        int apiVersion = Build.VERSION.SDK_INT;

        if (apiVersion > Build.VERSION_CODES.KITKAT)
            scan_lollipop();
        else scan_kitkat();

    }

    /**
     * android 4.4
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void scan_kitkat() {

        Log.d(TAG, "scan_kitkat");

        kitkatLeScanCallback = new KitkatScanCallback(this);
        mBluetoothAdapter.startLeScan(kitkatLeScanCallback);

//        scanning = true;

    }

    /**
     * android 5.0
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void scan_lollipop() {

        Log.d(TAG, "scan_lollipop");

        lollipopScanCallback = new LollipopScanCallback(this);

        List<ScanFilter> scanFilters = new ArrayList<>();
        if(_filterBuilder == null) _filterBuilder = new ScanFilter.Builder();
        _filterBuilder.setDeviceName("USBeacon");
        scanFilters.add(_filterBuilder.build());

        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
//        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
//        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        scanner.startScan(scanFilters, scanSettingsBuilder.build(), lollipopScanCallback);

    }

    @TargetApi(Build.VERSION_CODES.M)
    public void setScanFilter_address(String deviceAddress) {

        if(_filterBuilder == null) _filterBuilder = new ScanFilter.Builder();
        _filterBuilder.setDeviceAddress(deviceAddress);

    }


    /**
     * kitkat - 偵測到 beacon
     *
     * @param beaconObject is the object include beacon attribute
     */
    @Override
    public void kitkat_beaconScanned(BeaconObject beaconObject) {
        if(flag){
            firstBeaconTime = beaconObject.time;
            flag = false;
        }
        syncBeacons.getIns().addBeacon(beaconObject,firstBeaconTime);
        scanCallback.scannedBeacons(beaconObject);
//        returnCallback();

    }

    /**
     * lollipop - 偵測到 beacon
     *
     * @param beaconObject  is the object include beacon attribute
     */
    public long preTime = 0;
    @Override
    public void lollipop_beaconScanned(BeaconObject beaconObject) {
        if(flag){
            firstBeaconTime = beaconObject.time;
            flag = false;
        }
        long delta = beaconObject.time - preTime;
        preTime = beaconObject.time;
        Log.d(TAG, "[delta] = " + delta + "------" + beaconObject.getMajorMinorString() + "---" + beaconObject.rssi);
        syncBeacons.getIns().addBeacon(beaconObject,firstBeaconTime);
//        scanCallback.scannedBeacons(beaconObject);
//        returnCallback();

    }

    public void stopScan() {

        Log.d(TAG, "stopScan");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
                scanner.stopScan(lollipopScanCallback);
            } else {
                if (mBluetoothAdapter != null)
                    mBluetoothAdapter.stopLeScan(kitkatLeScanCallback);
                if (kitkatLeScanCallback != null)
                    kitkatLeScanCallback.stopDetect();
            }

//            scanning = false;

        } catch (Exception e) {

            e.printStackTrace();

        }

    }


//    private  void returnCallback(){
//        if (!canReturnCallback()){
////            System.out.println("[delay]");
//            return;
//        }
//        if(syncBeacons.getIns().getNearest()!=null){
//            scanCallback.getNearestBeacon(syncBeacons.getIns().getNearest());
//            scanCallback.getCurrentRoundBeacon(syncBeacons.getIns().getBeacons());
//        }
//        syncBeacons.getIns().removeAllBeacons();
//    }
    public void clearAllbeacons(){
        syncBeacons.getIns().removeAllBeacons();
    }

    private static class syncBeacons {

        private static syncBeacons ins = null;

        private ArrayList<BeaconObject> beacons = new ArrayList<>();

        public static synchronized syncBeacons getIns() {
            if (ins == null)
                ins = new syncBeacons();
            return ins;
        }

        public ArrayList<BeaconObject> getBeacons() {
            return (ArrayList<BeaconObject>) beacons.clone();
        }

        public synchronized void addBeacon(BeaconObject beaconObject,long firstTime) {

            for (int i = beacons.size() - 1; i >= 0; i--) {

                if (beacons.get(i).mac.equals(beaconObject.mac)) {
//                    if (beacons.get(i).rssi <= beaconObject.rssi) { // 避免收到散反射的
//                    Log.d("OPBeaconScanCallback", "[Cover] = " + beacons.get(i).getMajorMinorString() + "[pre]" + beacons.get(i).rssi + "[post]" + beaconObject.rssi + "[delta]" + (beaconObject.time - beacons.get(i).time));
                    beacons.remove(i);
                    beaconObject.time = beaconObject.time - firstTime;
                    beacons.add(beaconObject);
                    return;
                }
            }
            beaconObject.time = beaconObject.time - firstTime;
            beacons.add(beaconObject);
        }

        public synchronized void addBeacon(BeaconObject beaconObject) {

            for (int i = beacons.size() - 1; i >= 0; i--) {

                if (beacons.get(i).mac.equals(beaconObject.mac)) {

//                    if (beacons.get(i).rssi <= beaconObject.rssi) { // 避免收到散反射的
//                        Log.d("OPBeaconScanCallback", "[Cover] = " + beacons.get(i).getMajorMinorString() + "[pre]" + beacons.get(i).rssi + "[post]" + beaconObject.rssi + "[delta]" + (beaconObject.time - beacons.get(i).time));
                        beacons.remove(i);
                        beacons.add(beaconObject);
                        return;

//                    }else{
//                        beacons.get(i).time = beaconObject.time;
//                    }
//                    }
//                    return;

                }
            }
            beacons.add(beaconObject);
            Log.d("OPBeaconScanCallback", "[Add]" + beaconObject.getMajorMinorString() + "/" + beaconObject.rssi);


        }
        public synchronized void removeAllBeacons() {
//            long currentTime = System.currentTimeMillis();
//            for(int i = beacons.size() - 1 ; i >= 0; i--){
//                Log.d("OPBeaconScanCallback", "[R_delta] = " + (currentTime - beacons.get(i).time));
//                if(currentTime - beacons.get(i).time > 2000){ // 因應每隻手機更新頻率
//                    Log.d("OPBeaconScanCallback", "[Remove] = " + beacons.get(i).getMajorMinorString());
//                    beacons.remove(i);
//                }
//            }
//            if(beacons.size()==4)
                beacons.clear();
        }

        public synchronized BeaconObject getNearest() {
            int maxRSSI = -1000;
            BeaconObject nearest = null;
            for (BeaconObject object : beacons) {
                if (maxRSSI < object.rssi) {
                    nearest = object;
                    maxRSSI = object.rssi;
                }
            }

            return nearest;
        }
    }

    private boolean canReturnCallback() {
        long currentScannedTime = System.currentTimeMillis();
        if (lastScannedTime == 0) {
            lastScannedTime = currentScannedTime;
            return false;
        }

        if (currentScannedTime - lastScannedTime > 100) { /**   delay 0.1s */
            lastScannedTime = currentScannedTime;
            return true;
        } else {
            return false;
        }
    }

}
