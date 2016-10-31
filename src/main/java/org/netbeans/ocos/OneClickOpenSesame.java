package org.netbeans.ocos;

import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.modules.OnStart;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@OnStart
public class OneClickOpenSesame implements Runnable {

    private PropertyChangeListener listener;
    private DataObjectLookupListener myLookupListener;
    private final Lookup.Result<DataObject> openableResult;

    public OneClickOpenSesame() {
	openableResult = Utilities.actionsGlobalContext().lookupResult(DataObject.class);
    }

    @Override
    public void run() {
	myLookupListener = new DataObjectLookupListener();
	// FIXME unregister at module shutdown
	openableResult.addLookupListener(WeakListeners.create(LookupListener.class, myLookupListener, openableResult));

	listener = new EditorTabListener();
	// FIXME unregister at module shutdown
	EditorRegistry.addPropertyChangeListener(listener);
    }

    /**
     *
     * @param result
     * @return
     */
    private void removeReuseableFlagFromEditor() {
	/*
	 * But it is module-private, so call it via reflections
	 */
	ClassLoader loader = Lookup.getDefault().lookup(ClassLoader.class);
	if (loader == null) {
	    loader = this.getClass().getClassLoader();
	}
	try {
	    Class claszz = loader.loadClass("org.openide.text.CloneableEditorSupport");
	    Field field = claszz.getDeclaredField("lastReusable");
	    field.setAccessible(true);
	    // lastReusable=new WeakReference<CloneableTopComponent>(null)
	    // param 0 = null (because it is static)
	    field.set(null, new WeakReference<CloneableTopComponent>((CloneableTopComponent) null));
	} catch (Exception ex) {
	    // ignore
	    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot clear ", ex);
	}
    }

    private class DataObjectLookupListener implements LookupListener {

	private static final int DELAY = 400;
	private Timer timer;

	@Override
	public void resultChanged(LookupEvent le) {

	    // throw away old timer, if neccessary
	    if (null != timer) {
//		System.out.println("Cancelling timer for " + openableResult.allInstances());
		timer.cancel();
		timer.purge();
	    }
	    TopComponent tc = WindowManager.getDefault().getRegistry().getActivated();
	    // Filterung notwendig, damit ...
	    // FIXME support favorites and files tab too
	    if (!tc.toString().startsWith("org.netbeans.modules.project.ui.ProjectTab[Projects,")) {
		return;
	    }

	    timer = new Timer();

	    // create new timer
	    if (!openableResult.allInstances().isEmpty()) {
		final DataObject dataObject = openableResult.allInstances().iterator().next();
		TimerTask task = new TimerTask() {
		    @Override
		    public void run() {

			int caretPosition = findCurrentCaret(dataObject);
			// NOTE temporaeres Oeffnen des setzt das Reuse-Flag in
			// CloneableEditorSupport
			NbDocument.openDocument(dataObject, caretPosition, Line.ShowOpenType.REUSE,
				Line.ShowVisibilityType.FRONT);

		    }

		    /**
		     * 
		     * @return 0 or the caret of the currently opened editor
		     */
		    private int findCurrentCaret(DataObject dataObject) {
			EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
			if (editorCookie != null) {
			    StyledDocument expectedDocument = editorCookie.getDocument();
			    List<? extends JTextComponent> componentList = EditorRegistry.componentList();
			    for (JTextComponent jtc : componentList) {
				Document document = jtc.getDocument();
				if (expectedDocument == document) {
				    return jtc.getCaretPosition();
				}
			    }
			}
			// default
			return 0;
		    }
		};
		timer.schedule(task, DELAY);
	    }

	}
    }

    private class EditorTabListener implements PropertyChangeListener {

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	    Object start = evt.getOldValue();

	    if ("focusGained".equals(evt.getPropertyName())) {
		TopComponent oldTC = getNearestTopComponentFromHierarchy(start);
		/**
		 * org.netbeans.modules.project.ui.ProjectTab[Projects,
		 * org.netbeans.modules.project.ui.ProjectTab[Files,
		 * org.netbeans.modules.favorites.Tab[Favorites,
		 *
		 */
		if (null != oldTC) {
                    // Beim focusGained vom Projekttab zum (z.B.) Editor soll das Flag geloescht werden.
		    // Filterung damit es auch mit dem Reuseable-Verhalten
		    // beim Debugging funktioniert.
		    if (oldTC.toString().startsWith("org.netbeans.modules.project.ui.ProjectTab[Projects,")) {
			removeReuseableFlagFromEditor();
		    }
		}
	    }
	}

	private TopComponent getNearestTopComponentFromHierarchy(Object start) {
	    TopComponent tc = null;
	    if (start instanceof JComponent) {
		JComponent comp = (JComponent) start;
		while (comp != null) {

		    if (comp instanceof TopComponent) {
			tc = (TopComponent) comp;
			break;
		    }
		    Container parent = comp.getParent();
		    if (parent instanceof JComponent) {
			comp = (JComponent) parent;
		    } else {
			comp = null;
		    }
		}
	    }
	    return tc;
	}
    }
}
