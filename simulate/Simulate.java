package simulate;

import java.io.*;

public class Simulate {
	// Variable declarations
	static final Long tstart = System.currentTimeMillis();
	static String[] a = null;
	static Thread t1 = new Order();
	static Thread t2 = new Exchange();
	static Thread t3 = new Cleanup();
	static Object lock=new Object();
	static Object bslock=new Object();

	static long telapsed() {
		return ((System.currentTimeMillis() - tstart) / 1000);
	}

	public static void main(String[] args) {
		a = args;
		t1.start();
		t2.start();
		t3.start();
	}
}

class Log {
	public static void makeLog(String fname, String msg) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(fname, true);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(msg);
			out.newLine();
			// Close the output stream
			out.close();
		} catch (Exception e) {
			System.out.println("Exception encountered while logging. Program quits");
		}
	}

	public static void clearLog(String fname) {
		try {
			FileWriter fstream = new FileWriter(fname + ".out");
			BufferedWriter out = new BufferedWriter(fstream);
			out.close();
		} catch (Exception e) {
			System.out.println("Exception encountered while logging. Delete existing log files before re-running");
		}

	}

	public static void logOrder(String msg) {
		msg = Simulate.telapsed() + "\t" + msg;
		Log.makeLog("order.out", msg);
	}

	public static void logExchange(Quote q,char c,int status) {

		String msg=q.toString();
		msg= c+"\t"+Simulate.telapsed()+"\t"+status+"\t"+msg;
		makeLog("exchange.out", msg);
	}

	public static void logCleanup(String msg) {
		msg = Simulate.telapsed() + "\t" + msg;
		Log.makeLog("cleanup.out", msg);
	}

	public static void logException(String msg) {
		msg = "EXCEPTION" + "\t" + Simulate.telapsed() + "\t" + msg;
		Log.makeLog("order.out", msg);
	}
}

class Order extends Thread implements Runnable {
	static QuoteList queue = new QuoteList();

	public void run() {
		Log.clearLog("order");
		try {
			// Open the file that is the first
			// command line parameter
			if (Simulate.a.length == 0) {
				System.out.println("Error : Input file Expected as First parameter");
				return;
			} else {

				FileInputStream fstream = new FileInputStream(Simulate.a[0]);
				// Get the object of DataInputStream
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String strLine;
				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					try {
						Quote quote = new Quote(strLine);
						while (true) {
							if (quote.getT0() > Simulate.telapsed()) {
								sleep(1000);
							}else break;
						}
						if (!quote.isExpired()) {
							queue.enque(quote);
							//TODO
							Log.logOrder(quote.toString());
							synchronized (queue) {
								queue.notify();
							}
						}
					} catch (NumberFormatException e) {// Catch exception if any
						Log.logException(e.toString() + strLine);
					} catch (IndexOutOfBoundsException e) {
						Log.logException(e.toString() + strLine);
					} catch (InterruptedException e) {
						// ignore
					}
				}
				// Close the input stream
				in.close();
				Exchange.setnotified();
//				Simulate.t2.interrupt();
//				System.out.println("********************************");
//				System.out.println(queue);
			}

		} catch (FileNotFoundException e) {// Catch exception if any
			System.out
					.println("Error in reading file. Input file doesn't exist. Check first argument.");
		} catch (IOException e) {
			System.out
					.println("IOException caught. please check the input file");
		}

	}
}

class Exchange extends Thread implements Runnable {
	private QuoteList queue = Order.queue;
	static QuoteList buy = new QuoteList();
	static QuoteList sell = new QuoteList();
	private static boolean notified = false;
	static int profit = 0, tprofit = 0, j = -1;

	static void setnotified(){
		synchronized(Simulate.lock){
			notified=true;
		}
	}
	static boolean getnotified(){
		synchronized(Simulate.lock){
			return(notified);
		}
	}
	public void run() {
		Log.clearLog("exchange");

		while (true) {
			if (queue.isEmpty()) {
				try {
					if (Exchange.getnotified()) {
						Log.makeLog("exchange.out", "The total profit made is:"+Exchange.tprofit);
						Cleanup.setNotified(-1);
						return;
						
					}
					synchronized (queue) {
						queue.wait();
					}
				} catch (InterruptedException e) {
					continue; // doesn't matter
				}
			}while(!queue.isEmpty()){  //queue not empty...
			Quote q = queue.pullQuote(),q1 = null;
			// TODO
				if (!q.isExpired()) { //q is still valid...
					if (q.isBuy()) {  //q is a buy order...
						if (sell.isEmpty()) {
							synchronized (Simulate.bslock) {
								buy.enque(q);
								Log.logExchange(q, 'P', q.getQty());
							}
						} else { //q is buy order and sell orders pending
							j=-1;
							for (int i = 0; i < sell.size(); i++) {
								synchronized (Simulate.bslock) {
									q1 = sell.readQuote(i);
									if (match(q1, q)) {
										if ((q.getPrice() - q1.getPrice()) >= profit) {
											j = i;
										}
									}
								}
							}
							if (j!=-1) {
								synchronized (Simulate.bslock) {
									q1 = sell.readQuote(j);
								}
								if (q.getQty() > q1.getQty()) {
									Log.logExchange(q, 'T', q1.getQty());
									tprofit+=q1.getQty()*(q.getPrice()-q1.getPrice());
									Cleanup.setNotified(2 * j + 1);
									q.setQty(q.getQty() - q1.getQty());
									profit=0;
									continue;
								} else if (q1.getQty() > q.getQty()) {
									Log.logExchange(q, 'T', q.getQty());
									tprofit+=q.getQty()*(q.getPrice()-q1.getPrice());
									synchronized (Simulate.bslock) {
										//discard q
										q1.setQty(q1.getQty() - q.getQty());
									}
										profit=0;
									break;
								} else if (q.getQty() == q1.getQty()) {
									Log.logExchange(q, 'T', q.getQty());
									tprofit+=q.getQty()*(q.getPrice()-q1.getPrice());
									//discard q
									Cleanup.setNotified(2 * j + 1);
									profit=0;
									break;
								}
								j=-1;
							} else {
								synchronized (Simulate.bslock) {
									buy.enque(q);
								}
								Log.logExchange(q, 'P', q.getQty());
							}
						}
					} else { // q is a sell order
						j=-1;
						if (buy.isEmpty()) {
//							System.out.println("good");
							synchronized (Simulate.bslock) {
								sell.enque(q);
							}
							Log.logExchange(q, 'S', q.getQty());
						} else { // q is sell order and buy orders pending
							synchronized (Simulate.bslock) {
							for (int i = 0; i < buy.size(); i++) {
									q1 = buy.readQuote(i);
									if (match(q, q1)) {
										if ((q1.getPrice() - q.getPrice()) >= profit) {
											j = i;
										}
									}
								}
							}
							if (j!=-1) {
								synchronized (Simulate.bslock) {
									try {sleep(1000);}
									catch(InterruptedException e){continue;}
									System.out.println(j);
									System.out.println(buy.size());
									q1 = buy.readQuote(j);
								}
								if (q.getQty() > q1.getQty()) {
									Log.logExchange(q, 'T', q1.getQty());
									tprofit+=q1.getQty()*(q1.getPrice()-q.getPrice());
									Cleanup.setNotified(2 * j);
									q.setQty(q.getQty() - q1.getQty());
									profit=0;
									continue;
								} else if (q1.getQty() > q.getQty()) {
									Log.logExchange(q, 'T', q.getQty());
									tprofit+=q.getQty()*(q1.getPrice()-q.getPrice());
									synchronized (Simulate.bslock) {
										//discard q
										q1.setQty(q1.getQty() - q.getQty());
									}
										profit=0;
									break;
								} else if (q.getQty() == q1.getQty()) {
									Log.logExchange(q, 'T', q.getQty());
									tprofit+=q.getQty()*(q1.getPrice()-q.getPrice());
									//discard q
									Cleanup.setNotified(2 * j);
									profit=0;
									break;
								}
								j=-1;
							}else{
									synchronized (Simulate.bslock) {
										sell.enque(q);
									}
									Log.logExchange(q, 'S', q.getQty());
							}
						}
					}
				}
			}
		}
	}

	public boolean match(Quote qsell, Quote qbuy) {
		if (!(qsell.getStock().equals(qbuy.getStock())))
			return false;
		else if (qsell.getPrice() >= qbuy.getPrice())
			return false;
		else if (!qsell.isPartial() && (qbuy.getQty() < qsell.getQty()))
			return false;
		else if (!qbuy.isPartial() && (qsell.getQty() < qbuy.getQty()))
			return false;
		else
			return true;
	}
}

class Cleanup extends Thread implements Runnable {
	static int notified=0;
	private final static Object lock=new Object(); //only used for wait notify communication.
	private QuoteList sell=Exchange.sell, buy=Exchange.buy;
	public void run() {
		Log.clearLog("cleanup");
		synchronized (lock) {
			while (true){
				if (notified==0){
					try {lock.wait(3000);}
					catch(InterruptedException e){
						continue; //doesn't matter
					}
				}else if(notified>0){
					if(((notified+1)/2)>(notified/2)){
						synchronized (Simulate.bslock) {
						Log.logCleanup(sell.readQuote(notified/2).toString());
							sell.remove(notified / 2);
							
						}
						notified=0;
					}else{
						synchronized (Simulate.bslock) {
						Log.logCleanup(buy.readQuote(notified/2).toString());
							buy.remove(notified / 2);
							notified = 0;
						}
					}
				}else if(notified<0){
					while(true){
						if (sell.isEmpty() && buy.isEmpty()) return;
						if (!sell.isEmpty()){
							for (int i=0;i<sell.size();i++){
								synchronized (Simulate.bslock) {
									Quote q = sell.readQuote(i);
									if (q.isExpired()){
										sell.remove(q);
									Log.logCleanup(q.toString());
									}
								}
							}
						}
						if (!buy.isEmpty()){
							for (int i=0;i<buy.size();i++){
								synchronized (Simulate.bslock) {
									Quote q = buy.readQuote(i);
									if (q.isExpired()){
										buy.remove(q);
									Log.logCleanup(q.toString());
									}
								}
							}
						}
//						try {sleep(1000);}
//						catch(InterruptedException e){continue;}
					}
				}
			}
		}
	}
	
	static void setNotified(int i){
		synchronized (lock) {
			notified = i;
			lock.notify();
			return;
		}
	}
}