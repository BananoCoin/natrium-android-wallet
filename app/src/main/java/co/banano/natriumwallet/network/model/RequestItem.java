package co.banano.natriumwallet.network.model;

import co.banano.natriumwallet.network.AccountService;

import java.util.Calendar;

/**
 * Request object for queue
 */

public class RequestItem<T> {
    private boolean isProcessing = false;
    private long expireTime;
    private T request;
    private boolean fromTransfer = false;

    public RequestItem(T request) {
        this.request = request;

        // set expire time to now plus timeout
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, AccountService.TIMEOUT_MILLISECONDS);
        this.expireTime = calendar.getTime().getTime();
        this.fromTransfer = false;
    }

    public T getRequest() {
        return request;
    }

    public void setRequest(T request) {
        this.request = request;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setProcessing(boolean processing) {
        isProcessing = processing;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean isFromTransfer() {
        return fromTransfer;
    }

    public void setFromTransfer(boolean fromTransfer) {
        this.fromTransfer = fromTransfer;
    }
}
