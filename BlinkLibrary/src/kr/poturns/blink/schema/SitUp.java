package kr.poturns.blink.schema;

/** category : fitness */
public class SitUp extends DefaultSchema.Base {
	public int count;

	public SitUp() {
	}

	public SitUp(int count, String dateTime) {
		this.count = count;
		this.DateTime = dateTime;
	}
}
