package wxdgaming.backends;

import wxdgaming.boot.starter.AppContext;

public class BackendsStart {

    public static void main(String[] args) {
        AppContext.boot(BackendsStart.class);
        AppContext.start(true, 1, "backends", "backends");
    }

}