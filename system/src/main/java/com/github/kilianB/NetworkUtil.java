package com.github.kilianB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

public class NetworkUtil {
	/**
	 * Resolves the first found link local address of the current machine 192.168.xxx.xxx.
	 * This is a fix for <code>InetAddress.getLocalHost().getHostAddress();</code> which 
	 * in some cases will resolve to a loopback address (127.0.01).
	 * <p>
	 * Site local addresses 192.168.xxx.xxx are available inside the same network.
	 * Same counts for 10.xxx.xxx.xxx addresses, and 172.16.xxx.xxx through 172.31.xxx.xxx
	 * <p>
	 * Link local addresses 169.254.xxx.xxx are for a single network segment
	 * <p>
	 * Addresses in the range 224.xxx.xxx.xxx through 239.xxx.xxx.xxx are multicast addresses.
	 * <p>
	 * Broadcast address 255.255.255.255.
	 * <p>
	 * Loopback addresses 127.xxx.xxx.xxx 
	 * 
	 * @return link local address of the machine or InetAddress.getLocalHost() if no address can be found.
	 * @throws IOException if address can not be resolved
	 */
	public static InetAddress resolveSiteLocalAddress() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		
		while(interfaces.hasMoreElements()) {
			NetworkInterface curInterface = interfaces.nextElement();
			Enumeration<InetAddress> addresses = curInterface.getInetAddresses();
			while(addresses.hasMoreElements()) {
				InetAddress curInetAddress = addresses.nextElement();
				if(curInetAddress.isSiteLocalAddress()) {
					return curInetAddress;
				}
			}
		}
		return InetAddress.getLocalHost();
	}
	
	
	/**
	 * Collect all content available in the reader and return it as a string 
	 * @param br Buffered Reader input source
	 * @return	Content of the reader as string
	 * @throws IOException Exception thrown during read operation.
	 */
	public static String dumpReader(BufferedReader br) throws IOException {
		StringBuilder response = new StringBuilder();
		String temp;
		while ((temp = br.readLine()) != null) {
			response.append(temp).append(System.lineSeparator());
		}
		return response.toString();
	}

	
	public static String collectSocketAndClose(Socket s) throws IOException {
		String result = collectSocket(s);
		s.close();
		return result;
	}
	
	public static String collectSocket(Socket s) throws IOException {
		
		InputStream is = s.getInputStream();
		
		StringBuilder sb = new StringBuilder();
		
		int b;
		
		while(is.available() > 0) {
			b = is.read();
			sb.append((char)b);
		}
		return sb.toString();
	}
	
	public static String collectSocketWithTimeout(Socket s, int msTimeout) throws IOException {
		
		int oldTimeout = s.getSoTimeout();
		
		s.setSoTimeout(msTimeout);
		
		InputStream is = s.getInputStream();
		
		StringBuilder sb = new StringBuilder();
			
		try {
			while(!s.isClosed()) {
				sb.append((char)is.read());
			}
		//TODO expensive each read an exception is thrown
		}catch(java.net.SocketTimeoutException io) {}
		
		s.setSoTimeout(oldTimeout);
		
		return sb.toString();
		
		
	}
	
}
