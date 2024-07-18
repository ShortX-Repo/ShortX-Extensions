package autojs.api;

/**
 * Created by Stardust on Oct 28, 2017.
 */
public class VolatileDispose<T> {

    private volatile T mValue;

    public T blockedGet() {
        synchronized (this) {
            return blockedGet(0);
        }
    }

    public T blockedGet(long timeout) {
        synchronized (this) {
            if (mValue != null) {
                return mValue;
            }
            try {
                this.wait(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return mValue;
    }

    public T blockedGetOrThrow(Class<? extends RuntimeException> exception) {
        synchronized (this) {
            if (mValue != null) {
                return mValue;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
                try {
                    throw exception.newInstance();
                } catch (InstantiationException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
        return mValue;
    }

    public T blockedGetOrThrow(Class<? extends RuntimeException> exception, long timeout, T defaultValue) {
        synchronized (this) {
            if (mValue != null) {
                return mValue;
            }
            try {
                this.wait(timeout);
            } catch (InterruptedException e) {
                try {
                    throw exception.newInstance();
                } catch (InstantiationException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
            }
            if (mValue == null) {
                return defaultValue;
            }
        }
        return mValue;
    }

    public void setAndNotify(T value) {
        synchronized (this) {
            mValue = value;
            notify();
        }
    }

}
