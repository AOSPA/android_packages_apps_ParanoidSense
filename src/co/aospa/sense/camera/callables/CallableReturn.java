package co.aospa.sense.camera.callables;

public class CallableReturn {
    public Exception exception;
    public Object value;

    public CallableReturn(Object value) {
        this.value = value;
        exception = null;
    }

    public CallableReturn(Exception exc) {
        exception = exc;
        value = null;
    }

}
