package de.podfetcher.syndication.handler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.atom.NSAtom;

/** Superclass for all SAX Handlers which process Syndication formats */
public abstract class SyndHandler extends DefaultHandler{
	protected HandlerState state;

	
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		state.tagstack.push(qName);
		
		String[] parts = qName.split(":");
		Namespace handler = state.namespaces.get(parts[0]);
		if (handler != null) {
			handler.handleElement(localName, state, attributes);
		}
	}
	
	

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		state.tagstack.pop();
	}



	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		state.namespaces.remove(prefix);
	}



	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// Find the right namespace
		if (prefix.equals(NSAtom.NSTAG) || uri.equals(NSAtom.NSURI)) {
			state.namespaces.put(prefix, new NSAtom());
		}
	}



	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	@Override
	public void startDocument() throws SAXException {
		state = new HandlerState();
	}

	public HandlerState getState() {
		return state;
	}
	
}
