package de.danoeh.antennapod.core.util.flattr;

import java.util.Calendar;

public class FlattrStatus {
	private static final int STATUS_UNFLATTERED = 0;
	public static final int STATUS_QUEUE = 1;
	private static final int STATUS_FLATTRED = 2;
	
	private int status = STATUS_UNFLATTERED;
	private Calendar lastFlattred;
	
	public FlattrStatus() {
        status = STATUS_UNFLATTERED;
		lastFlattred = Calendar.getInstance();
	}

	public FlattrStatus(long status) {
		lastFlattred = Calendar.getInstance();
		fromLong(status);
	}

	public void setFlattred() {
		status = STATUS_FLATTRED;
		lastFlattred = Calendar.getInstance();
	}
	
	public void setUnflattred() {
		status = STATUS_UNFLATTERED;
	}
	
	public boolean getUnflattred() {
		return status == STATUS_UNFLATTERED;
	}
	
	public void setFlattrQueue() {
		if (flattrable())
			status = STATUS_QUEUE;
	}
	
	private void fromLong(long status) {
		if (status == STATUS_UNFLATTERED || status == STATUS_QUEUE)
			this.status = (int) status;
		else {
			this.status = STATUS_FLATTRED;
			lastFlattred.setTimeInMillis(status);
		}
	}
	
	public long toLong() {
		if (status == STATUS_UNFLATTERED || status == STATUS_QUEUE)
			return status;
		else {
			return lastFlattred.getTimeInMillis();
		}
	}
		
	public boolean flattrable() {
		Calendar firstOfMonth = Calendar.getInstance();
		firstOfMonth.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().getActualMinimum(Calendar.DAY_OF_MONTH));
		
		return (status == STATUS_UNFLATTERED) || (status == STATUS_FLATTRED && firstOfMonth.after(lastFlattred) );
	}
	
	public boolean getFlattrQueue() {
		return status == STATUS_QUEUE;
	}
}
