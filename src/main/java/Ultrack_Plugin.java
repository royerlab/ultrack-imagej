import ij.plugin.PlugIn;

public class Ultrack_Plugin implements PlugIn {

    @Override
    public void run(String arg) {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        //System.setProperty("prism.order", "sw");
        MainApp.main(new String[]{});
    }

}