package com.hoclamdev.store.datatype;

import java.io.Serializable;

public abstract class RedisDataType implements Serializable {
    protected long tll;

    protected RedisDataType(long tll) {
        this.tll = tll;
    }

    public long getTll() {
        return tll;
    }

    public void setTll(long tll) {
        this.tll = tll;
    }

    public String type() {
        throw new UnsupportedOperationException();
    }

    public Object get() {
        throw new UnsupportedOperationException();
    }
}
