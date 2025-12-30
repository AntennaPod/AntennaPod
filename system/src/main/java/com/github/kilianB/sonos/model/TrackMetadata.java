package com.github.kilianB.sonos.model;

import com.github.kilianB.sonos.ParserHelper;

public class TrackMetadata {
    private final String title;
    private final String creator;
    private final String albumArtist;
    private final String album;
    private final String albumArtURI;

    public TrackMetadata(String title, String creator, String albumArtist, String album, String albumArtURI) {
        this.title = title;
        this.creator = creator;
        this.albumArtist = albumArtist;
        this.album = album;
        this.albumArtURI = albumArtURI;
    }

    public static TrackMetadata parse(String metadata) {
        return new TrackMetadata(
                ParserHelper.findOne("<dc:title>(.*)</dc:title>", metadata),
                ParserHelper.findOne("<dc:creator>(.*)</dc:creator>", metadata),
                ParserHelper.findOne("<r:albumArtist>(.*)</r:albumArtist>", metadata),
                ParserHelper.findOne("<upnp:album>(.*)</upnp:album>", metadata),
                ParserHelper.findOne("<upnp:albumArtURI>(.*)</upnp:albumArtURI>", metadata)
        );
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public String getAlbum() {
        return album;
    }

    public String getAlbumArtURI() {
        return albumArtURI;
    }

    @Override
    public String toString() {
        return "TrackMetadata{" +
                "title='" + title + '\'' +
                ", creator='" + creator + '\'' +
                ", albumArtist='" + albumArtist + '\'' +
                ", album='" + album + '\'' +
                ", albumArtURI='" + albumArtURI + '\'' +
                '}';
    }

    public String toDIDL() {
        return "<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns:r=\"urn:schemas-rinconnetworks-com:metadata-1-0/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\">" +
                    "<item>" +
                        "<dc:title>" + title + "</dc:title>" +
                        "<dc:creator>" + creator + "</dc:creator>" +
                        "<dc:albumArtist>" + albumArtist + "</dc:albumArtist>" +
                        "<upnp:album>" + album + "</upnp:album>" +
                        "<upnp:albumArtURI>" + albumArtURI + "</upnp:albumArtURI>" +
                    "</item>" +
                "</DIDL-Lite>";
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((album == null) ? 0 : album.hashCode());
		result = prime * result + ((albumArtURI == null) ? 0 : albumArtURI.hashCode());
		result = prime * result + ((albumArtist == null) ? 0 : albumArtist.hashCode());
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackMetadata other = (TrackMetadata) obj;
		if (album == null) {
			if (other.album != null)
				return false;
		} else if (!album.equals(other.album))
			return false;
		if (albumArtURI == null) {
			if (other.albumArtURI != null)
				return false;
		} else if (!albumArtURI.equals(other.albumArtURI))
			return false;
		if (albumArtist == null) {
			if (other.albumArtist != null)
				return false;
		} else if (!albumArtist.equals(other.albumArtist))
			return false;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
    
    
}
