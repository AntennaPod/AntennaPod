package de.danoeh.antennapod.core.util.flattr;

/* SimpleFlattrThing is a trivial implementation of the FlattrThing interface */
public class SimpleFlattrThing implements FlattrThing {
    public SimpleFlattrThing(String title, String url, FlattrStatus status)
    {
        this.title = title;
        this.url = url;
        this.status = status;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String getPaymentLink()
    {
        return this.url;
    }

    public FlattrStatus getFlattrStatus()
    {
        return this.status;
    }

    private final String title;
    private final String url;
    private final FlattrStatus status;
}
