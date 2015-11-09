package org.netbeans.ocos;

import org.netbeans.api.actions.Openable;
import org.openide.modules.OnStart;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

@OnStart
public class OneClickOpenSesame implements Runnable, LookupListener {
    Lookup.Result<Openable> openableResult = null;
    @Override
    public void run() {
        openableResult = Utilities.actionsGlobalContext().lookupResult(Openable.class);
        openableResult.addLookupListener(
                WeakListeners.create(LookupListener.class, this, openableResult));
    }
    @Override
    public void resultChanged(LookupEvent le) {
        for(Openable o : openableResult.allInstances()){
            o.open();
        }
    }
}