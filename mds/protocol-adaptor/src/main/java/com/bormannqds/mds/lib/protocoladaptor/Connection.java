package com.bormannqds.mds.lib.protocoladaptor;

import com.bormannqds.mds.lib.gateway.ApplicationContext;
import org.zeromq.ZMQ;

/**
 * Created by bormanng on 24/06/15.
 */
public class Connection {
//public:
    public static ZMQ.Socket createSocket(int socketType) {
        ZMQ.Socket socket = ApplicationContext.getInstance().getZmqContext().createSocket(socketType);
        socket.setLinger(0);

        return socket;
    }

    public Connection(final String address) {
        this.address = address;
    }

    public String getAddress() { return address; }

//private:
    private final String address;
}
