package io.github.noodle1983;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;

//解决so热更后覆盖安装闪退的问题
//问题1. 热更过so后，覆盖安装有可能会闪退，原因是Application.persistentdatapath/il2cpp目录下的文件与新的so不匹配
    //解决方案：启动时删除il2cpp目录，让unity自动生成新的
//问题2. 热更过so后，覆盖安装会导致patch失败，原因是覆盖安装后user.db中存的datapath有可能跟新的datapath（Application.persistentdatapath，重装后目录有可能会变）
//对不上，见bootstrap.cpp中“can't read file”报错
    //解决方案：启动时删除user.db，当成此包没有热更过so，进游戏后如有更新，重新更so
public class BootstrapBugFix{
    public static void Fix(Activity activity)
    {
        String tag = "BBF";
        String saveKey = "BootstrapBugFix_lastUpdateTime";
        try
        {
            long firstInstallTime = 0;
            long lastUpdateTime = 0;

            PackageManager packMgr = activity.getApplicationContext().getPackageManager();
            PackageInfo packInfo = packMgr.getPackageInfo(activity.getPackageName(), 0);
            firstInstallTime = packInfo.firstInstallTime;
            lastUpdateTime = packInfo.lastUpdateTime;
            Log.d(tag, "firstInstallTime: " + firstInstallTime + " lastUpdateTime: " + lastUpdateTime);

            if(lastUpdateTime == 0)
                return;
            String strLastUpdateTime = lastUpdateTime + "";

            SharedPreferences prefs = activity.getPreferences(Activity.MODE_PRIVATE);
            String oldLastUpdateTime = prefs.getString(saveKey, "");
            if(oldLastUpdateTime.equals(""))
            {
                //首次启动
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(saveKey, strLastUpdateTime);
                editor.commit();
                return;
            }

            if(oldLastUpdateTime.equals(strLastUpdateTime))
            {
                //非重装
                return;
            }

            //重装后第一个打开
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(saveKey, strLastUpdateTime);
            editor.commit();

            //删除user.db
            File file = new File(activity.getApplicationContext().getFilesDir().getPath()+"/user.db");
            if(file.exists())
            {
                file.delete();
            }

            //删除Application.persistentdatapath/il2cpp，可能在sd卡
            File il2cpp = new File(activity.getApplicationContext().getFilesDir().getPath() + "/il2cpp");
            if(il2cpp.exists())
                DeleteFileEx(il2cpp);

            il2cpp = new File(activity.getApplicationContext().getExternalFilesDir(null).getPath() + "/il2cpp");
            if(il2cpp.exists())
                DeleteFileEx(il2cpp);

        }
        catch (Exception e)
        {
            Log.d(tag, "Fix过程中发生异常，" + e);
            e.printStackTrace();
        }
    }

    public static boolean DeleteFileEx(File file)
    {
        if(!file.exists())
            return true;

        boolean result = false;
        if(file.isDirectory())
        {
            File[] childrenFiles = file.listFiles();
            for(File childFile:childrenFiles)
            {
                result = DeleteFileEx(childFile);
                if(!result)
                {
                    return false;
                }
            }
        }

        result = file.delete();
        return result;
    }
}