package autocoupons.com.webviewapp.receivers;

import java.util.Observable;

/**
 * Created by sHIVAM on 5/25/2017.
 */

public class WebviewObservable extends Observable {
    private static WebviewObservable instance;

    public static WebviewObservable getInstance() {
        if(instance == null)
            instance = new WebviewObservable();
        return instance;
    }

    public WebviewObservable() {
    }

    public void updateValue(Object data) {
        synchronized (this) {
            setChanged();
            notifyObservers(data);
        }
    }
}
