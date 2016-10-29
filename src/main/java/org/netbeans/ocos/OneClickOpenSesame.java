package org.netbeans.ocos;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JOptionPane;
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
    final Duration timeout = Duration.ofSeconds(5000);

//    @Override
//    public void run() {
//        openableResult = Utilities.actionsGlobalContext().lookupResult(Openable.class);
//        openableResult.addLookupListener(WeakListeners.create(LookupListener.class, this, openableResult));
//    }

    final Runnable stuffToDo = new Thread() {
        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, "Test");
            for (final Openable o : openableResult.allInstances()) {
                o.open();
            }
        }
    };

    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Future future = executor.submit(stuffToDo);

    @Override
    public void resultChanged(LookupEvent le) {
        executor.shutdown(); // This does not cancel the already-scheduled task.

        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            /* Handle the interruption. Or ignore it. */
        } catch (ExecutionException ee) {
            /* Handle the error. Or ignore it. */
        } catch (TimeoutException te) {
            /* Handle the timeout. Or ignore it. */
        }
        if (!executor.isTerminated()) {
            executor.shutdownNow(); // If you want to stop the code that hasn't finished.
        }
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
