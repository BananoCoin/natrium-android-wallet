package co.banano.natriumwallet.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import co.banano.natriumwallet.BuildConfig;
import co.banano.natriumwallet.KaliumUtil;
import co.banano.natriumwallet.bus.RxBus;
import co.banano.natriumwallet.bus.SocketError;
import co.banano.natriumwallet.model.Address;
import co.banano.natriumwallet.model.Credentials;
import co.banano.natriumwallet.model.KaliumWallet;
import co.banano.natriumwallet.model.PreconfiguredRepresentatives;
import co.banano.natriumwallet.network.model.BaseResponse;
import co.banano.natriumwallet.network.model.BlockTypes;
import co.banano.natriumwallet.network.model.RequestItem;
import co.banano.natriumwallet.network.model.request.AccountHistoryRequest;
import co.banano.natriumwallet.network.model.request.GetBlocksInfoRequest;
import co.banano.natriumwallet.network.model.request.PendingTransactionsRequest;
import co.banano.natriumwallet.network.model.request.ProcessRequest;
import co.banano.natriumwallet.network.model.request.SubscribeRequest;
import co.banano.natriumwallet.network.model.request.WorkRequest;
import co.banano.natriumwallet.network.model.request.block.Block;
import co.banano.natriumwallet.network.model.request.block.OpenBlock;
import co.banano.natriumwallet.network.model.request.block.ReceiveBlock;
import co.banano.natriumwallet.network.model.request.block.SendBlock;
import co.banano.natriumwallet.network.model.request.block.StateBlock;
import co.banano.natriumwallet.network.model.response.BlockInfoItem;
import co.banano.natriumwallet.network.model.response.BlockItem;
import co.banano.natriumwallet.network.model.response.BlocksInfoResponse;
import co.banano.natriumwallet.network.model.response.CurrentPriceResponse;
import co.banano.natriumwallet.network.model.response.PendingTransactionResponseItem;
import co.banano.natriumwallet.network.model.response.ProcessResponse;
import co.banano.natriumwallet.network.model.response.SubscribeResponse;
import co.banano.natriumwallet.network.model.response.TransactionResponse;
import co.banano.natriumwallet.network.model.response.WarningResponse;
import co.banano.natriumwallet.network.model.response.WorkResponse;
import co.banano.natriumwallet.ui.common.ActivityWithComponent;
import co.banano.natriumwallet.util.ExceptionHandler;
import co.banano.natriumwallet.util.NumberUtil;
import co.banano.natriumwallet.util.SharedPreferencesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import io.realm.Realm;
import timber.log.Timber;

/**
 * Methods for calling the account service
 */

public class AccountService {
    public static final int TIMEOUT_MILLISECONDS = 8000;
    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;
    @Inject
    KaliumWallet wallet;
    @Inject
    Gson gson;
    @Inject
    Realm realm;
    @Inject
    @Named("encryption_key")
    byte[] encryption_key;
    private WebSocketClient websocket;
    private LinkedList<RequestItem> requestQueue = new LinkedList<>();
    private String private_key;
    private Address address;

    // Map previous hash to the block request
    private HashMap<String, StateBlock> previousPendingMap = new HashMap<>();
    private HashMap<String, StateBlock> pendingResponseBlockMap = new HashMap<>();

    public AccountService(Context context) {
        // init dependency injection
        if (context instanceof ActivityWithComponent) {
            ((ActivityWithComponent) context).getActivityComponent().inject(this);
        }
    }

    public boolean isRequestQueueEmpty() {
        return requestQueue.size() == 0;
    }

    public void open() {
        wallet.setBlockCount(-1);

        private_key = getPrivateKey();
        address = getAddress();
        wallet.setPublicKey(getPublicKey());

        // initialize the web socket
        if (wsDisconnected()) {
            initWebSocket();
        } else {
            requestUpdate();
        }
    }

    /**
     * Initialize websocket and event listeners
     */
    private void initWebSocket() {
        if (websocket != null && websocket.isOpen()) {
            processQueue();
            return;
        }
        // create websocket
        URI wssUri;
        try {
            wssUri = new URI(BuildConfig.CONNECTION_URL);
        } catch (URISyntaxException use) {
            Timber.e(use);
            return;
        }
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("X-Client-Version", Integer.toString(BuildConfig.VERSION_CODE));
        websocket = new WebSocketClient(wssUri, httpHeaders) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Timber.d("OPENED");
                requestUpdate();
            }

            @Override
            public void onMessage(String message) {
                Timber.d("RECEIVED %s", message);
                handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                switch (code) {
                    case 1000: // CLOSE_NORMAL
                        Timber.d("CLOSED");
                        break;
                    default: // Abnormal closure
                        break;
                }
            }

            @Override
            public void onError(Exception ex) {
                ExceptionHandler.handle(ex);
                if (wsDisconnected()) {
                    post(new SocketError(ex));
                    close();
                    if (requestQueue != null) {
                        requestQueue.clear();
                    }
                    checkState();
                }
            }
        };
        websocket.setConnectionLostTimeout(5);
        websocket.connect();
    }

    /**
     * Generic message handler. Convert to an object and process or post to bus.
     *
     * @param message String message
     */
    private void handleMessage(String message) {
        // deserialize message if possible
        BaseResponse event = null;
        try {
            event = gson.fromJson(message, BaseResponse.class);
        } catch (JsonSyntaxException e) {
            ExceptionHandler.handle(e);
        }

        if (event != null && event.getMessageType() == null) {
            // try parsing to a linked tree map object if event type is null
            // for now, these are the blocks that come back from a pending request
            handleNullMessageTypes(message);
        } else if (event != null && event instanceof WorkResponse) {
            // process a work response
            handleWorkResponse((WorkResponse) event);
        } else if (event != null && event instanceof TransactionResponse) {
            // a transaction was pushed to the app via the socket
            TransactionResponse transactionResponse = (TransactionResponse) event;
            PendingTransactionResponseItem pendingTransactionResponseItem = new PendingTransactionResponseItem(
                    transactionResponse.getAccount(), transactionResponse.getAmount(), transactionResponse.getHash());
            if (transactionResponse.getIs_send().equals("true")) {
                handleTransactionResponse(pendingTransactionResponseItem);
            }
        } else if (event != null && event instanceof ProcessResponse) {
            handleProcessResponse((ProcessResponse) event);
        } else if (event != null &&
                event instanceof BlocksInfoResponse) {
            handleBlocksInfoResponse((BlocksInfoResponse) event);
        } else {
            // update block count on subscribe request
            if (event instanceof SubscribeResponse) {
                if (((SubscribeResponse) event).getBlock_count() != null) {
                    updateBlockCount(((SubscribeResponse) event).getBlock_count());
                }
                if (((SubscribeResponse) event).getFrontier() != null) {
                    updateFrontier(((SubscribeResponse) event).getFrontier());
                }
            }

            // post whatever the response type is to the bus
            if (event != null) {
                post(event);
            }

            // remove item from queue and process
            // current price response is sent without a request and warnings are
            // sent in addition to the actual response
            if (!(event instanceof CurrentPriceResponse) &&
                    !(event instanceof WarningResponse)) {
                requestQueue.poll();
            }
            processQueue();
        }
    }

    /**
     * Post event to bus on UI thread
     *
     * @param event Object to post to bus
     */
    private void post(Object event) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> RxBus.get().post(event));
    }

    /**
     * Handle a transaction response by create an open or a receive block
     *
     * @param item Pending transaction response item
     */
    private void handleTransactionResponse(PendingTransactionResponseItem item) {
        Timber.d(item.toString());
        if (!queueContainsRequestWithHash(item.getHash())) {
            // set balance to just the amount,
            // it will be added to total later when we verify the hash of the last transaction
            // BigInteger balance = wallet.getAccountBalanceBananoRaw().toBigInteger().add(new BigInteger(item.getAmount()));
            BigInteger balance = new BigInteger(item.getAmount());
            if (wallet.getOpenBlock() == null && !queueContainsOpenBlock()) {
                requestOpen("0", item.getHash(), balance);
            } else {
                requestReceive(wallet.getFrontierBlock(), item.getHash(), balance);
            }
        }
    }

    /**
     * When block info comes back. We need to verify the hash, amount, etc...
     *
     * @param blocksInfo BlocksInfoResponse Response
     */
    private void handleBlocksInfoResponse(BlocksInfoResponse blocksInfo) {
        HashMap<String, BlockInfoItem> blocks = blocksInfo.getBlocks();
        if (blocks.size() != 1) {
            ExceptionHandler.handle(new Exception("unexpected amount of blocks in blocks_info response"));
            requestQueue.poll();
            return;
        }
        String hash = blocks.keySet().iterator().next();
        BlockInfoItem blockInfo = blocks.get(hash);
        BlockItem block = gson.fromJson(blockInfo.getContents(), BlockItem.class);

        if (block.getType().equals(BlockTypes.STATE.toString())) {
            String calculatedHash = KaliumUtil.computeStateHash(
                    block.getAccount(),
                    block.getPrevious(),
                    block.getRepresentative(),
                    block.getBalance(),
                    block.getLink());
            if (!blockInfo.getBalance().equals(block.getBalance())) {
                ExceptionHandler.handle(new Exception("balance in state block doesn't match balance in block info"));
                requestQueue.poll();
                return;
            }
            if (!hash.equals(calculatedHash)) {
                ExceptionHandler.handle(new Exception("balance in state block doesn't match balance in block info"));
                ExceptionHandler.handle(new Exception("state block hash doesn't match hash from block info"));
                requestQueue.poll();
                return;
            }
        } else if (block.getType().equals(BlockTypes.SEND.toString())) {
            String calculatedHash = KaliumUtil.computeSendHash(
                    block.getPrevious(),
                    KaliumUtil.addressToPublic(block.getDestination()),
                    block.getBalance());
            if (!blockInfo.getBalance().equals(NumberUtil.getRawFromHex(block.getBalance()))) {
                ExceptionHandler.handle(new Exception("balance in send block doesn't match balance in block info"));
                requestQueue.poll();
                return;
            }
            if (!hash.equals(calculatedHash)) {
                ExceptionHandler.handle(new Exception("send block hash doesn't match hash from block info"));
                requestQueue.poll();
                return;
            }
        } else if (block.getType().equals(BlockTypes.RECEIVE.toString())) {
            String calculatedHash = KaliumUtil.computeReceiveHash(block.getPrevious(), block.getSource());
            if (!hash.equals(calculatedHash)) {
                ExceptionHandler.handle(new Exception("receive block hash doesn't match hash from block info"));
                requestQueue.poll();
                return;
            }
        } else if (block.getType().equals(BlockTypes.OPEN.toString())) {
            String calculatedHash = KaliumUtil.computeOpenHash(
                    block.getSource(),
                    KaliumUtil.addressToPublic(block.getRepresentative()),
                    KaliumUtil.addressToPublic(block.getAccount()));
            if (!hash.equals(calculatedHash)) {
                ExceptionHandler.handle(new Exception("open block hash doesn't match hash from block info"));
                requestQueue.poll();
                return;
            }
        } else if (block.getType().equals(BlockTypes.CHANGE.toString())) {
            String calculatedHash = KaliumUtil.computeChangeHash(
                    block.getPrevious(),
                    KaliumUtil.addressToPublic(block.getRepresentative()));
            if (!hash.equals(calculatedHash)) {
                ExceptionHandler.handle(new Exception("change block hash doesn't match hash from block info"));
                requestQueue.poll();
                return;
            }
        } else {
            ExceptionHandler.handle(new Exception("unexpected block type " + block.getType()));
            requestQueue.poll();
            return;
        }

        requestQueue.poll();
        StateBlock nextBlock = previousPendingMap.get(hash);
        if (nextBlock != null) {
            if (block.getRepresentative() != null) {
                nextBlock.setRepresentative(block.getRepresentative());
            }
            nextBlock.setPrevious(hash);
            if (nextBlock.getInternal_block_type() == BlockTypes.SEND) {
                if (nextBlock.getSendAmount().equals("0")) {
                    nextBlock.setBalance("0");
                } else {
                    nextBlock.setBalance(
                            new BigInteger(blockInfo.getBalance())
                                    .subtract(new BigInteger(nextBlock.getSendAmount()))
                                    .toString()
                    );
                }
            } else {
                nextBlock.setBalance(
                        new BigInteger(blockInfo.getBalance())
                                .add(new BigInteger(nextBlock.getSendAmount()))
                                .toString()
                );
            }
            previousPendingMap.remove(hash);
            ProcessRequest prq = new ProcessRequest(gson.toJson(nextBlock), true);
            pendingResponseBlockMap.put(nextBlock.getPrevious(), nextBlock);
            requestQueue.add(new RequestItem<>(prq));
        }

        processQueue();
    }


    /**
     * Here is where we handle any work response that comes back
     *
     * @param workResponse Work response
     */
    private void handleWorkResponse(WorkResponse workResponse) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            // work response received so remove that work item from the queue
            requestQueue.poll();

            // make sure the next item is a Block type and update the work on that type
            RequestItem nextBlockRequest = requestQueue.peek();
            if (nextBlockRequest != null && nextBlockRequest.getRequest() instanceof Block) {
                ((Block) nextBlockRequest.getRequest()).setWork(workResponse.getWork());
            } else {
                if (requestQueue.size() > 1) {
                    nextBlockRequest = requestQueue.get(1);
                    if (nextBlockRequest != null && nextBlockRequest.getRequest() instanceof Block) {
                        ((Block) nextBlockRequest.getRequest()).setWork(workResponse.getWork());
                    } else {
                        // Work was submitted without a block request following - should never happen
                        ExceptionHandler.handle(new Exception("Queue Error: work was submitted without a block request following"));
                    }
                } else {
                    // Work was submitted without a block request following - should never happen
                    ExceptionHandler.handle(new Exception("Queue Error: work was submitted without a block request following"));
                }
            }
            processQueue();
        });
    }

    /**
     * When an OPEN, SEND, or RECEIVE block comes back successfully with a hash
     *
     * @param processResponse Process Response
     */
    private void handleProcessResponse(ProcessResponse processResponse) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            // see what type of request sent this response
            RequestItem requestItem = requestQueue.peek();
            if (requestItem != null) {
                StateBlock blockRequest = null;
                if (requestItem.getRequest() instanceof ProcessRequest) {
                    blockRequest = gson.fromJson(((ProcessRequest) requestItem.getRequest()).getBlock(), StateBlock.class);
                    if (blockRequest != null) {
                        StateBlock previous = pendingResponseBlockMap.get(blockRequest.getPrevious());
                        if (previous != null) {
                            requestItem.setRequest(previous);
                        }
                    }
                }
                if (requestItem.getRequest() instanceof Block) {
                    if (requestItem.getRequest() instanceof OpenBlock ||
                            (requestItem.getRequest() instanceof StateBlock &&
                                    ((StateBlock) requestItem.getRequest()).getInternal_block_type().equals(BlockTypes.OPEN))) {
                        updateFrontier(processResponse.getHash());
                        updateBlockCount(1);
                    } else if (requestItem.getRequest() instanceof ReceiveBlock ||
                            (requestItem.getRequest() instanceof StateBlock &&
                                    ((StateBlock) requestItem.getRequest()).getInternal_block_type().equals(BlockTypes.RECEIVE))) {
                        updateFrontier(processResponse.getHash());
                        updateBlockCount(wallet.getBlockCount() + 1);
                    } else if (requestItem.getRequest() instanceof SendBlock ||
                            (requestItem.getRequest() instanceof StateBlock &&
                                    ((StateBlock) requestItem.getRequest()).getInternal_block_type().equals(BlockTypes.SEND))) {
                        updateBlockCount(wallet.getBlockCount() + 1);
                        post(processResponse);
                    } else if (requestItem.getRequest() instanceof StateBlock &&
                            ((StateBlock) requestItem.getRequest()).getInternal_block_type().equals(BlockTypes.CHANGE)) {
                        updateBlockCount(wallet.getBlockCount() + 1);
                        post(processResponse);
                    } else {
                        // something is out of sync if this wasn't a block - should never happen
                        ExceptionHandler.handle(new Exception("Queue Error: something is out of sync if this wasn't a block"));
                    }

                    requestSubscribe();
                    requestAccountHistory();
                } else {
                    // something is out of sync if this wasn't a block - should never happen
                    ExceptionHandler.handle(new Exception("Queue Error: something is out of sync if this wasn't a block"));
                }
            }
            requestQueue.poll();
            processQueue();
        });
    }

    /**
     * Objects that are not mapped to a known response can be processed here
     *
     * @param message Websocket Message
     */
    private void handleNullMessageTypes(String message) {
        try {
            Object o = gson.fromJson(message, Object.class);
            if (o instanceof LinkedTreeMap) {
                processLinkedTreeMap((LinkedTreeMap) o);
            } else {
                requestQueue.poll();
                processQueue();
            }
        } catch (JsonSyntaxException e) {
            ExceptionHandler.handle(e);
            requestQueue.poll();
            processQueue();
        }
    }

    /**
     * Process a linked tree map to see if there are pending blocks to handle
     *
     * @param linkedTreeMap Linked Tree Map
     */
    private void processLinkedTreeMap(LinkedTreeMap linkedTreeMap) {
        if (linkedTreeMap.containsKey("blocks")) {
            // this is a set of blocks
            Object blocks = linkedTreeMap.get("blocks");
            if (blocks instanceof LinkedTreeMap) {
                // blocks is not empty
                Set keys = ((LinkedTreeMap) blocks).keySet();
                for (Object key : keys) {
                    try {
                        PendingTransactionResponseItem pendingTransactionResponseItem = new Gson().fromJson(String.valueOf(((LinkedTreeMap) blocks).get(key)), PendingTransactionResponseItem.class);
                        pendingTransactionResponseItem.setHash(key.toString());
                        handleTransactionResponse(pendingTransactionResponseItem);
                    } catch (Exception e) {
                        ExceptionHandler.handle(e);
                    }
                }
            }
        }
        requestQueue.poll();
        processQueue();
    }

    /**
     * Process the next item in the queue if item is not currently processing
     */
    private void processQueue() {
        if (requestQueue != null && requestQueue.size() > 0) {
            if (wsDisconnected()) {
                requestQueue.clear();
                return;
            }
            RequestItem requestItem = requestQueue.peek();
            if (requestItem != null && !requestItem.isProcessing()) {
                // process item
                requestItem.setProcessing(true);

                Timber.d("SEND: %s", gson.toJson(requestItem.getRequest()));
                wsSend(gson.toJson(requestItem.getRequest()));
            } else if (requestItem != null && (requestItem.isProcessing() && System.currentTimeMillis() > requestItem.getExpireTime())) {
                // expired request on the queue so remove and go to the next
                requestQueue.poll();
                processQueue();
            }
        }
    }

    /**
     * Request all the account info
     */
    public void requestUpdate() {
        if (address != null && address.getAddress() != null) {
            requestQueue.add(new RequestItem<>(new SubscribeRequest(address.getAddress(), getLocalCurrency(), wallet.getUuid(), sharedPreferencesUtil.getFcmToken())));
            requestQueue.add(new RequestItem<>(new AccountHistoryRequest(address.getAddress(), wallet.getBlockCount() != null ? wallet.getBlockCount() : 10)));
            requestQueue.add(new RequestItem<>(new PendingTransactionsRequest(address.getAddress(), true, wallet.getBlockCount())));
            processQueue();
        }
    }

    /**
     * Request subscribe
     */
    public void requestSubscribe() {
        if (address != null && address.getAddress() != null) {
            requestQueue.add(new RequestItem<>(new SubscribeRequest(address.getAddress(), getLocalCurrency(), wallet.getUuid(), sharedPreferencesUtil.getFcmToken())));
            processQueue();
        }
    }

    /**
     * Request Pending Blocks
     */
    public void requestPending() {
        if (address != null && address.getAddress() != null) {
            requestQueue.add(new RequestItem<>(new PendingTransactionsRequest(address.getAddress(), true, wallet.getBlockCount())));
            processQueue();
        }
    }

    /**
     * Request AccountHistory
     */
    private void requestAccountHistory() {
        if (address != null && address.getAddress() != null) {
            requestQueue.add(new RequestItem<>(new AccountHistoryRequest(address.getAddress(), wallet.getBlockCount() != null ? wallet.getBlockCount() : 10)));
            processQueue();
        }
    }

    /**
     * Make an open block request (state)
     *
     * @param previous Previous hash
     * @param source   Destination
     * @param balance  Remaining balance after a send
     */
    private void requestOpen(String previous, String source, BigInteger balance) {
        // If user has set a custom representative, use it
        String representative = sharedPreferencesUtil.hasCustomRepresentative() ? sharedPreferencesUtil.getCustomRepresentative() : PreconfiguredRepresentatives.getRepresentative();

        // Create open block
        StateBlock openBlock = new StateBlock(
                BlockTypes.OPEN,
                private_key,
                previous,
                representative,
                balance.toString(),
                source
        );
        pendingResponseBlockMap.put(previous, openBlock);

        // Create process request
        ProcessRequest prq = new ProcessRequest(gson.toJson(openBlock), true);
        requestQueue.add(new RequestItem<>(prq));

        processQueue();
    }

    /**
     * Make a receive block request (state)
     *
     * @param previous Previous hash
     * @param source   Destination
     * @param balance  Remaining balance after a send
     */
    private void requestReceive(String previous, String source, BigInteger balance) {
        StateBlock receiveBlock = new StateBlock(
                BlockTypes.RECEIVE,
                private_key,
                previous,
                wallet.getRepresentative(),
                balance.toString(),
                source
        );
        previousPendingMap.put(previous, receiveBlock);

        // Request block info for previous
        requestQueue.add(new RequestItem<>(new GetBlocksInfoRequest(new String[]{previous})));

        processQueue();
    }

    /**
     * Make a send request
     *
     * @param previous    Previous hash
     * @param destination Destination
     * @param amount     Amount to send in RAW
     */
    public void requestSend(String previous, Address destination, BigInteger amount) {

        StateBlock sendBlock = new StateBlock(
                BlockTypes.SEND,
                private_key,
                previous,
                wallet.getRepresentative(),
                amount.toString(),
                destination.getAddress()
        );
        previousPendingMap.put(previous, sendBlock);

        // Request block info for previous
        requestQueue.add(new RequestItem<>(new GetBlocksInfoRequest(new String[]{previous})));

        processQueue();
    }

    /**
     * Make a no-op request
     *
     * @param previous       Previous hash
     * @param balance        Current Wallet Balance
     * @param representative Representative
     */
    public void requestChange(String previous, BigInteger balance, String representative) {
        // Create change block
        StateBlock changeBlock = new StateBlock(
                BlockTypes.CHANGE,
                private_key,
                previous,
                representative,
                balance.toString(),
                "0000000000000000000000000000000000000000000000000000000000000000"
        );
        pendingResponseBlockMap.put(previous, changeBlock);

        // Create process request
        ProcessRequest prq = new ProcessRequest(gson.toJson(changeBlock), true);
        requestQueue.add(new RequestItem<>(prq));

        processQueue();
    }

    /**
     * Get credentials from realm and return address
     *
     * @return Address object
     */
    private Address getAddress() {
        Credentials _credentials = realm.where(Credentials.class).findFirst();
        if (_credentials == null) {
            return null;
        } else {
            Credentials credentials = realm.copyFromRealm(_credentials);
            return new Address(credentials.getAddressString());
        }
    }

    /**
     * Get local currency from shared preferences
     *
     * @return Local Currency
     */
    public String getLocalCurrency() {
        return sharedPreferencesUtil.getLocalCurrency().toString();
    }

    /**
     * Get private key from realm
     *
     * @return Private Key
     */
    private String getPrivateKey() {
        Credentials _credentials = realm.where(Credentials.class).findFirst();
        if (_credentials == null) {
            return null;
        } else {
            Credentials credentials = realm.copyFromRealm(_credentials);
            return credentials.getPrivateKey();
        }
    }

    /**
     * Get private key from realm
     *
     * @return Private Key
     */
    private String getPublicKey() {
        Credentials _credentials = realm.where(Credentials.class).findFirst();
        if (_credentials == null) {
            return null;
        } else {
            Credentials credentials = realm.copyFromRealm(_credentials);
            return credentials.getPublicKey();
        }
    }

    /**
     * Check to see if queue already contains an open block
     *
     * @return true if queue has an open block in it already
     */
    private boolean queueContainsOpenBlock() {
        if (requestQueue == null) {
            return false;
        }
        for (RequestItem item : requestQueue) {
            if (item.getRequest() instanceof OpenBlock ||
                    (item.getRequest() instanceof StateBlock &&
                            ((StateBlock) item.getRequest()).getInternal_block_type().equals(BlockTypes.OPEN))) {
                return true;
            }
        }
        return false;
    }

    /**
     * See if this block is already in the queue
     *
     * @param source Source hash
     * @return true if block is already in the queue with the same source
     */
    private boolean queueContainsRequestWithHash(String source) {
        if (requestQueue == null) {
            return false;
        }
        for (RequestItem item : requestQueue) {
            if ((item.getRequest() instanceof OpenBlock && ((OpenBlock) item.getRequest()).getSource().equals(source)) ||
                    (item.getRequest() instanceof ReceiveBlock && ((ReceiveBlock) item.getRequest()).getSource().equals(source)) ||
                    (item.getRequest() instanceof StateBlock && ((StateBlock) item.getRequest()).getInternal_block_type().equals(BlockTypes.RECEIVE) && ((StateBlock) item.getRequest()).getLink().equals(source)) ||
                    (item.getRequest() instanceof StateBlock && ((StateBlock) item.getRequest()).getInternal_block_type().equals(BlockTypes.OPEN) && ((StateBlock) item.getRequest()).getLink().equals(source))
                    ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update block count in wallet and on pending requests
     *
     * @param blockCount Block count
     */
    private void updateBlockCount(int blockCount) {
        wallet.setBlockCount(blockCount);
        if (requestQueue != null) {
            for (RequestItem item : requestQueue) {
                if (item.getRequest() instanceof AccountHistoryRequest && !item.isProcessing()) {
                    ((AccountHistoryRequest) item.getRequest()).setCount(blockCount);
                } else if (item.getRequest() instanceof PendingTransactionsRequest && !item.isProcessing()) {
                    ((PendingTransactionsRequest) item.getRequest()).setCount(blockCount);
                }
            }
        }
    }


    /**
     * Update frontier block in wallet and on any pending receive requests
     *
     * @param frontier Frontier hash
     */
    private void updateFrontier(String frontier) {
        wallet.setFrontierBlock(frontier);
        List<Object> objectsToUpdate = new ArrayList<>();
        if (requestQueue != null) {
            for (RequestItem item : requestQueue) {
                Object o = item.getRequest();
                if (((o instanceof ReceiveBlock ||
                        (o instanceof StateBlock &&
                                ((StateBlock) o).getInternal_block_type().equals(BlockTypes.RECEIVE))) ||
                        o instanceof WorkRequest ||
                        o instanceof GetBlocksInfoRequest
                ) && !item.isProcessing()) {
                    objectsToUpdate.add(o);
                }
            }
        }

        for (Object o : objectsToUpdate) {
            if (o != null && o instanceof ReceiveBlock) {
                ((ReceiveBlock) o).setPrevious(frontier);
            } else if ((o instanceof StateBlock &&
                    ((StateBlock) o).getInternal_block_type().equals(BlockTypes.RECEIVE))) {
                ((StateBlock) o).setPrevious(frontier);
            } else if (o != null && o instanceof WorkRequest) {
                ((WorkRequest) o).setHash(frontier);
            } else if (o != null && o instanceof GetBlocksInfoRequest) {
                ((GetBlocksInfoRequest) o).setHashes(new String[]{frontier});
            }
        }
    }


    /**
     * Close the web socket
     */
    public void close() {
        if (wsDisconnected()) {
            return;
        }
        try {
            websocket.close(1000, "Closed");
        } catch (Exception e) {
            ExceptionHandler.handle(e);
        }
    }

    private boolean wsDisconnected() {
        return websocket == null || websocket.isClosing() || websocket.isClosed() || !websocket.isOpen();
    }

    private void checkState() {
        if (wsDisconnected()) {
            initWebSocket();
        }
    }

    private void wsSend(String message) {
        checkState();
        if (websocket.isOpen()) {
            websocket.send(message);
        }
    }
}