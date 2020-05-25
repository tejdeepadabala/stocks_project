package simulate;

import java.util.LinkedList;

public class QuoteList{
	private LinkedList<Quote> list=new LinkedList<Quote>();
	public synchronized void enque(Quote q){
		this.list.add(q);
	}
	
	public synchronized void remove(int a){
		this.list.remove(a);
	}
	
	public synchronized void remove(Quote q){
		this.list.remove(q);
	}
	
	public synchronized String toString(){
		String str="";
		for (int i=0;i<this.list.size();i++){
			str+=this.list.get(i).toString();
			str+="\n";
		}
		return str;
	}
	
	public synchronized Quote readQuote(int i){
	return this.list.get(i);	
	}
	
	public synchronized Quote pullQuote(){
		Quote q=this.list.get(0);
		this.list.remove(q);
		return q;
	}
	
	public synchronized boolean isEmpty(){
		synchronized(Simulate.lock){
			return (this.list.size()==0);
		}
	}
	
	public synchronized int size(){
		return this.list.size();
	}
}
