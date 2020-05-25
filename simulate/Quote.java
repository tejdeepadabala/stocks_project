package simulate;
public class Quote {
	private	String name, type, stock;
	private boolean partial;
	private int t0, texp, qty, price;
	private long tstart=Simulate.tstart;
	public Quote(String ordr) throws NumberFormatException,IndexOutOfBoundsException{
	
		String[] ord = ordr.split("\t");
		t0 = Integer.parseInt(ord[0]);
		name = ord[1];
		texp = Integer.parseInt(ord[2]);
		type = ord[3].toLowerCase();
		qty = Integer.parseInt(ord[4]);
		stock = ord[5];
		price = Integer.parseInt(ord[6]);
		if(ord[7].equals("1")) partial=true;
		else if(ord[7].equalsIgnoreCase("true")) partial=true;
		else if(ord[7].equalsIgnoreCase("t")) partial=true;
		else partial=false;
		
	}
	public String getType() {
		return type;
	}
	public int getQty() {
		return qty;
	}
	public void setQty(int qty) {
		this.qty = qty;
	}
	public String getName() {
		return name;
	}
	public String getStock() {
		return stock;
	}
	public boolean isPartial() {
		return partial;
	}
	public int getT0() {
		return t0;
	}
	public int getTexp() {
		return texp;
	}
	public int getPrice() {
		return price;
	}
	public boolean isExpired() {
		return ((t0 + texp) < (System.currentTimeMillis() - tstart)/1000);
	}
	public boolean isBuy() {
		return type.matches("buy");
	}
	public String toString() {
		String str = "";
		str += t0 + "\t" + name + "\t" + texp + "\t" + type + "\t" + qty + "\t"
				+ stock + "\t" + price + "\t" + partial;

		return str;
	}
}

