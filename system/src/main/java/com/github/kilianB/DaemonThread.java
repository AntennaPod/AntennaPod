package com.github.kilianB;

public class DaemonThread extends Thread{
	
	public DaemonThread() {
		this.setDaemon(true);
	}
	
	public DaemonThread(Runnable r) {
		super(r);
		this.setDaemon(true);
	}
	
	public DaemonThread(String name) {
		super(name);
		this.setDaemon(true);
	}
	
	public DaemonThread(Runnable r,String name) {
		super(r,name);
		this.setDaemon(true);
	}
	
	
	
	
	
}
