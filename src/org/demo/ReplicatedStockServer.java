package org.demo;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

/**
 * @author Bela Ban
 * @version $Id$
 */
public class ReplicatedStockServer extends ReceiverAdapter {

    private final Map<String, Integer> quotes = new HashMap<String, Integer>();
    JChannel ch = null;

    private void start(String props)
    {

        try {
            ch = new JChannel("./conf/udp.xml");
            //ch = new JChannel("/Users/neptyn/Dropbox/HSLU/5.Semester/TA.ENAPP.H1001/Lab/JGroups/lab/conf/udp.xml");

            ch.setReceiver(this);
            ch.connect("enapp");
            ch.getState(null, 5000);

            while (true) {
                int c = input();
                switch (c) {
                    case '1':
                        showStocks();
                        break;
                    case '2':
                        getQuote();
                        break;
                    case '3':
                        setQuote();
                        break;
                    case '4':
                        removeQuote();
                        break;
                    case 'x':
                        return;
                }
            }
        } catch (Exception ex) {

            Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (ch != null) {

                ch.close();
            }
        }
    }

    @Override
    public byte[] getState()
    {
        synchronized (quotes) {
            byte[] state;
            try {
                state = Util.objectToByteBuffer(quotes);
                return state;
            } catch (Exception ex) {
                Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            // serialize hashmap into byte[] buffer
            return null;
        }
    }

    public void setState(byte[] new_state)
    {
        try {
            Map new_map = (Map) Util.objectFromByteBuffer(new_state);
            synchronized (quotes) {
                quotes.clear();
                quotes.putAll(new_map);
            }
        } catch (Exception ex) {
            Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getQuote() throws IOException
    {
        String ticker = readString("Symbol");
        Integer val = quotes.get(ticker);
        System.out.println(ticker + " is " + val);
    }

    private void setQuote() throws IOException
    {
        String ticker, val;
        ticker = readString("Symbol");
        val = readString("Value");
        //quotes.put(ticker, Integer.parseInt(val));

        UpdateRequest ur = new UpdateRequest(UpdateRequest.SET, ticker, Integer.valueOf(val));
        Message m = new Message(null, null, ur);
        try {
            ch.send(m);
        } catch (ChannelNotConnectedException ex) {
            Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ChannelClosedException ex) {
            Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void removeQuote() throws IOException
    {
        String ticker = readString("Symbol");

        UpdateRequest ur = new UpdateRequest(UpdateRequest.REMOVE, ticker, 0);
        Message m = new Message(null, null, ur);
        try {
            ch.send(m);
        } catch (ChannelNotConnectedException ex) {
            Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ChannelClosedException ex) {
            Logger.getLogger(ReplicatedStockServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*Integer val = quotes.remove(ticker);
        if (val == null) {
        System.err.println("Quote for " + ticker + " was not present");
        } else {
        System.out.println("Removed " + ticker + " (val was " + val + ")");
        }*/
    }

    @Override
    public void receive(Message msg)
    {
        System.out.println("update request from " + msg.getSrc() + ": " + msg.getObject());
        UpdateRequest ur = (UpdateRequest) msg.getObject();
        switch (ur.type) {
            case UpdateRequest.SET:
                quotes.put(ur.ticker, ur.value);
                System.out.println("Added quote " + ur.ticker + " with val " + ur.value);
                break;
            case UpdateRequest.REMOVE:
                Integer val = quotes.remove(ur.ticker);
                if (val == null) {
                    System.err.println("Quote for " + ur.ticker + " was not present");
                } else {
                    System.out.println("Removed " + ur.ticker + " (val was " + val + ")");
                }
                break;
        }
    }

    @Override
    public void viewAccepted(View new_view)
    {
        super.viewAccepted(new_view);
        System.out.println("View added " + new_view);
    }

    private void showStocks()
    {
        System.out.println("Stocks:");
        for (Map.Entry<String, Integer> entry : quotes.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private static int input()
    {
        int c = 0;
        try {
            System.out.println("[1] Show stocks [2] Get quote [3] Set quote [4] Remove quote [x] Exit");
            System.out.flush();
            c = System.in.read();
            System.in.skip(System.in.available());
        } catch (IOException e) {
        }
        return c;
    }

    private static String readString(String s) throws IOException
    {
        int c;
        boolean looping = true;
        StringBuilder sb = new StringBuilder();
        System.out.print(s + ": ");
        System.out.flush();
        System.in.skip(System.in.available());

        while (looping) {
            c = System.in.read();
            switch (c) {
                case -1:
                case '\n':
                case 13:
                    looping = false;
                    break;
                default:
                    sb.append((char) c);
                    break;
            }
        }

        return sb.toString();
    }

    public static void main(String[] args)
    {
        String props = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-props")) {
                props = args[++i];
                continue;
            }
            System.out.println("StockServer [-props <XML config file>]");
            return;
        }

        new ReplicatedStockServer().start(props);
    }

    private static class UpdateRequest implements Serializable {

        public static final short SET = 1;
        public static final short REMOVE = 2;
        private short type = 0;
        private String ticker = null;
        private int value = 0;
        private static final long serialVersionUID = 7530569601671042214L;

        private UpdateRequest()
        {
        }

        private UpdateRequest(short type, String ticker, int value)
        {
            this.type = type;
            this.ticker = ticker;
            this.value = value;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            switch (type) {
                case SET:
                    sb.append("SET(").append(ticker).append(", ").append(value).append(")");
                    break;
                case REMOVE:
                    sb.append("REMOVE(").append(ticker).append(")");
                    break;
                default:
                    sb.append("invalid request");
            }
            return sb.toString();
        }
    }
}
