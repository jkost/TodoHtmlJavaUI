package todo.viewmodel;

import javax.swing.JComponent;
import org.netbeans.api.htmlui.HTMLComponent;
//import org.openide.modules.OnStart;

//@OnStart
public final class Installer implements Runnable {

    @Override
    public void run() {
//        BrowserBuilder.newBrowser().
//            loadPage("Tasks.html").
//            loadClass(TasksCntrl.class).
//            invoke("onPageLoad").
//            showAndWait();
////        TasksCntrl.onPageLoad();
        Installer.start();
    }
    @HTMLComponent(url = "Tasks.html", type = JComponent.class)
    public static void start() {
        
    }
}
