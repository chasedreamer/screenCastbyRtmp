// IMyAidlInterface.aidl
package net.chr.screenrecorder;

// Declare any non-default types here with import statements
import net.chr.screenrecorder.model.DanmakuBean;

interface IScreenRecorderAidlInterface {

    void sendDanmaku(in List<DanmakuBean> danmakuBean);

    void startScreenRecord(in Intent bundleData);

}
