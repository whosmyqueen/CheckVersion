package com.allenliu.versioncheck.v2.callback

import com.allenliu.versioncheck.v2.builder.BuilderManager

/**
 *    @author : shengliu7
 *    @e-mail : shengliu7@iflytek.com
 *    @date   : 2020/12/19 2:18 PM
 *    @desc   :
 *
 */
interface LifecycleListener {
    fun isDisposed() =
            BuilderManager.getDownloadBuilder() == null

}