package com.apps.swapyx.channelize.Utils;

import com.squareup.otto.Bus;

/**
 * Created by SwapyX on 29-06-2017.
 */

public final class BusProvider {
    private static Bus BUS;

    public static Bus getInstance() {
        if(BUS == null){
            BUS = new Bus();
        }
        return BUS;
    }

    private BusProvider(){

    }
}
