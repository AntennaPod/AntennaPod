package com.github.kilianB.sonos;

import com.github.kilianB.StringUtil;
import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.exception.UPnPSonosControllerException;

import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vmichalak
 */
class CommandBuilder {
	private static final int SOAP_PORT = 1400;
	
	
	private static final String TRANSPORT_ENDPOINT = "/MediaRenderer/AVTransport/Control";
	private static final String TRANSPORT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1";
	private static final String RENDERING_ENDPOINT = "/MediaRenderer/RenderingControl/Control";
	private static final String RENDERING_SERVICE = "urn:schemas-upnp-org:service:RenderingControl:1";
	private static final String DEVICE_ENDPOINT = "/DeviceProperties/Control";
	private static final String DEVICE_SERVICE = "urn:schemas-upnp-org:service:DeviceProperties:1";
	private static final String CONTENT_DIRECTORY_ENDPOINT = "/MediaServer/ContentDirectory/Control";
	private static final String CONTENT_DIRECTORY_SERVICE = "urn:schemas-upnp-org:service:ContentDirectory:1";
	private static final String ZONE_GROUP_TOPOLOGY_ENDPOINT = "/ZoneGroupTopology/Control";
	private static final String ZONE_GROUP_TOPOLOGY_SERVICE = "urn:upnp-org:serviceId:ZoneGroupTopology";

	private static final HashMap<Integer, String> ERROR_DESCRIPTION_MAP = new HashMap<Integer, String>();

	static {
		ERROR_DESCRIPTION_MAP.put(400, "Bad Request");
		ERROR_DESCRIPTION_MAP.put(401, "Invalid Action");
		ERROR_DESCRIPTION_MAP.put(402, "Invalid Args");
		ERROR_DESCRIPTION_MAP.put(404, "Invalid Var");
		ERROR_DESCRIPTION_MAP.put(412, "Precondition Failed");
		ERROR_DESCRIPTION_MAP.put(501, "Action Failed");
		ERROR_DESCRIPTION_MAP.put(600, "Argument Value Invalid");
		ERROR_DESCRIPTION_MAP.put(601, "Argument Value Out of Range");
		ERROR_DESCRIPTION_MAP.put(602, "Option Action Not Implemented");
		ERROR_DESCRIPTION_MAP.put(603, "Out Of Memory");
		ERROR_DESCRIPTION_MAP.put(604, "Human Intervention Required");
		ERROR_DESCRIPTION_MAP.put(605, "String Argument Too Long");
		ERROR_DESCRIPTION_MAP.put(606, "Action Not Authorized");
		ERROR_DESCRIPTION_MAP.put(607, "Signature Failure");
		ERROR_DESCRIPTION_MAP.put(608, "Signature Missing");
		ERROR_DESCRIPTION_MAP.put(609, "Not Encrypted");
		ERROR_DESCRIPTION_MAP.put(610, "Invalid Sequence");
		ERROR_DESCRIPTION_MAP.put(611, "Invalid Control Url");
		ERROR_DESCRIPTION_MAP.put(612, "No Such Session");
		ERROR_DESCRIPTION_MAP.put(701, "Invalid transition");
		ERROR_DESCRIPTION_MAP.put(702, "No content");
		ERROR_DESCRIPTION_MAP.put(712, "Unsupported Play Mode");
		ERROR_DESCRIPTION_MAP.put(714, "Illegal MIME-Type");
	}

	private static OkHttpClient httpClient;

	private final String endpoint;
	private final String service;
	private final String action;
	private final HashMap<String, String> bodyEntries = new HashMap<String, String>();

	public CommandBuilder(String endpoint, String service, String action) {
		this.endpoint = endpoint;
		this.service = service;
		this.action = action;
	}

	public static CommandBuilder transport(String action) {
		return new CommandBuilder(TRANSPORT_ENDPOINT, TRANSPORT_SERVICE, action);
	}

	public static CommandBuilder rendering(String action) {
		return new CommandBuilder(RENDERING_ENDPOINT, RENDERING_SERVICE, action);
	}

	public static CommandBuilder device(String action) {
		return new CommandBuilder(DEVICE_ENDPOINT, DEVICE_SERVICE, action);
	}

	public static CommandBuilder contentDirectory(String action) {
		return new CommandBuilder(CONTENT_DIRECTORY_ENDPOINT, CONTENT_DIRECTORY_SERVICE, action);
	}

	public static CommandBuilder zoneGroupTopology(String action) {
		return new CommandBuilder(ZONE_GROUP_TOPOLOGY_ENDPOINT, ZONE_GROUP_TOPOLOGY_SERVICE, action);
	}

	public static String download(String ip, String url) throws IOException, SonosControllerException {
		String uri = "http://" + ip + ":" + SOAP_PORT + "/" + url;
		Request request = new Request.Builder().url(uri).get().build();
		String response = getHttpClient().newCall(request).execute().body().string();
		handleError(ip, response);
		return response;
	}

	public CommandBuilder put(String key, String value) {
		if (!StringUtil.isEscaped(value)) {
			value = StringEscapeUtils.escapeXml11(value);
		}
		this.bodyEntries.put(key, value);
		return this;
	}

	public String executeOn(String ip) throws IOException, SonosControllerException {
		String uri = "http://" + ip + ":" + SOAP_PORT + this.endpoint;
		String content = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
				+ " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body>" + "<u:" + this.action
				+ " xmlns:u=\"" + this.service + "\">" + this.getBody() + "</u:" + this.action + ">"
				+ "</s:Body></s:Envelope>";
		//RequestBody body = RequestBody.create(MediaType.parse("application/text"), content.getBytes("UTF-8"));
        RequestBody body = RequestBody.create(content.getBytes("UTF-8"), MediaType.parse("application/text"));
        Request request = new Request.Builder().url(uri).addHeader("Content-Type", "text/xml")
				.addHeader("SOAPACTION", this.service + "#" + this.action).post(body).build();
		String response = getHttpClient().newCall(request).execute().body().string();
		response = unescape(response);
		handleError(ip, response);
		return response;
	}

	protected static void handleError(String ip, String response) throws SonosControllerException {
		if (!response.contains("errorCode")) {
			return;
		}
		int errorCode = Integer.parseInt(ParserHelper.findOne("<errorCode>([0-9]*)</errorCode>", response));
		String desc = ERROR_DESCRIPTION_MAP.get(errorCode);
		throw new UPnPSonosControllerException("UPnP Error " + errorCode + " (" + desc + ") received from " + ip,
				errorCode, desc, response);
	}

	protected String getBody() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : bodyEntries.entrySet()) {
			sb.append("<").append(entry.getKey()).append(">").append(entry.getValue()).append("</")
					.append(entry.getKey()).append(">");
		}
		return sb.toString();
	}

	private static OkHttpClient getHttpClient() {
		if (httpClient == null) {
			httpClient = new OkHttpClient();
		}
		return httpClient;
	}

	// This method correct some strange behaviour (multiple escaped string) with the
	// getQueue method.
	private static String unescape(String s) {
		String tmp = s;
		while (StringUtil.isEscaped(tmp)) {
			tmp = StringEscapeUtils.unescapeXml(tmp);
		}
		return tmp;
	}
}
