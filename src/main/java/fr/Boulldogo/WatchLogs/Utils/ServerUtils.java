package fr.Boulldogo.WatchLogs.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import fr.Boulldogo.WatchLogs.WatchLogsPlugin;

public class ServerUtils {
	
	private final WatchLogsPlugin plugin;
	
	public ServerUtils(WatchLogsPlugin plugin) {
		this.plugin = plugin;
	}

    public boolean isServerOnline(String ip, int port, int timeout, String serverName) {
        try(Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.setSoTimeout(timeout);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeByte(0xFE); 
            DataInputStream in = new DataInputStream(socket.getInputStream());

            if(in.readByte() == -1) {
            	plugin.getLogger().warning("Server " + serverName + " not pinged ! If the others servers mark this server in offline mode, this server will be exclude from the network !");
                return false;
            }
            return true;
        } catch(IOException e) {
            return false;
        }
    }

    public static String getLocalIPAddress() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            return localAddress.getHostAddress();
        } catch(UnknownHostException e) {
            e.printStackTrace();
            return "Unable to determine local IP address.";
        }
    }

    public String getExternalIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if(!address.isLoopbackAddress() && !address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch(SocketException e) {
            e.printStackTrace();
        }
        return "Unable to determine external IP address.";
    }
}

