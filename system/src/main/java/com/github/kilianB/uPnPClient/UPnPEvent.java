package com.github.kilianB.uPnPClient;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * A UPnP 1.1 unicast event emitted by devices after subscribing to it's services.
 * 
 * @author Kilian
 * @see <a href="http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
 *
 */
public class UPnPEvent{
	
	private static final  Namespace upnpNamespace =  Namespace.getNamespace("e","urn:schemas-upnp-org:event-1-0");
	
	/**
	 * HTTP header of the event
	 * 200 OK
	 */
	private String httpHeader;
	/**
	 * IP Address of the device
	 */
	private String host;
	/**
	 * HTTP 1.1 header connection:close
	 */
	private String connection;
	/**
	 * length of the payload in bytes
	 */
	private int contentLength;
	/**
	 * Notification type has to be upnp:event
	 */
	private String nt;
	/**
	 * Notification sub type has to be upnp:propchange
	 */
	private String nts;
	/**
	 * subscription identifier. allows mapping of events to the subscription requests
	 */
	private String sid;
	/**
	 * sequence identifier. incremental to order events
	 */
	private int seq;
	
	/**
	 * Properties represent the values submitted during a upnp event
	 */
	private HashSet<Element> properties = new HashSet<Element>();
	
	
	//XML
	/**
	 * Raw xml body of the event
	 */
	private Document body;
	
	/**
	 * A UPnP 1.1 event
	 * @param httpHeader 		http response code of the event (200 OK)
	 * @param host				domain name or IP address and optional port components of delivery URL
	 * @param connection		HTTP 1.1 connection header 
	 * @param contentLength		package content in bytes
	 * @param nt				Notification type has to be upnp:event
	 * @param nts				Notification sub type has to be upnp:propchange
	 * @param sid				subscription identifier
	 * @param seq				sequence identifier
	 * @param body				body of the upnp event
	 * @see <a href="http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
	 */
	public UPnPEvent(String httpHeader, String host, String connection, int contentLength, String nt,
			String nts, String sid, int seq, Document body) {
		this.httpHeader = httpHeader;
		this.host = host;
		this.connection = connection;
		this.contentLength = contentLength;
		this.nt = nt;
		this.nts = nts;
		this.sid = sid;
		this.seq = seq;
		this.body = body;
		
		List<Element> propertyElements = body.getRootElement().getChildren("property", upnpNamespace);
		
		for(Element property : propertyElements) {
			properties.addAll(property.getChildren());
		}
	}


	public String getHttpHeader() {
		return httpHeader;
	}

	public String getHost() {
		return host;
	}

	public String getConnection() {
		return connection;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getNt() {
		return nt;
	}

	public String getNts() {
		return nts;
	}

	public String getSid() {
		return sid;
	}

	public int getSeq() {
		return seq;
	}

	public Document getBody() {
		return body;
	}

	/**
	 * @return	The body of the upnp event as well formated and idented xml
	 */
	public String getBodyAsString() {
		XMLOutputter xmlOut = new XMLOutputter();
		xmlOut.setFormat(Format.getPrettyFormat());
		return xmlOut.outputString(body);
	}

	@Override
	public String toString() {
		return "UPnPEvent [httpHeader=" + httpHeader + ", host=" + host + ", connection=" + connection
				+ ", contentLength=" + contentLength + ", nt=" + nt + ", nts=" + nts + ", sid=" + sid + ", seq=" + seq
				+ ", body=" + body + "]";
	}
	
	/**
	 * Get the properties of the upnp event.
	 * Properties represent the payload of an event used to transmit information.
	 * @return the properties of the xml body of the event. 
	 */
	public Set<Element> getProperties (){
		return Collections.unmodifiableSet(properties);
	}

}
