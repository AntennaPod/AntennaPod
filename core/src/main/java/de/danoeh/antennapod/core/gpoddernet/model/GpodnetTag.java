package de.danoeh.antennapod.core.gpoddernet.model;

import org.apache.commons.lang3.Validate;

import java.util.Comparator;

public class GpodnetTag {

    private String name;
    private int usage;

    public GpodnetTag(String name, int usage) {
        Validate.notNull(name);

        this.name = name;
        this.usage = usage;
    }

    public GpodnetTag(String name) {
        super();
        this.name = name;
    }

    @Override
    public String toString() {
        return "GpodnetTag [name=" + name + ", usage=" + usage + "]";
    }

    public String getName() {
        return name;
    }

    public int getUsage() {
        return usage;
    }

    public static class UsageComparator implements Comparator<GpodnetTag> {

        @Override
        public int compare(GpodnetTag o1, GpodnetTag o2) {
            return o1.usage - o2.usage;
        }

    }

}
