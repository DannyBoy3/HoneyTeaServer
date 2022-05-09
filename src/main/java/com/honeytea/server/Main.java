package com.honeytea.server;

public class Main {

	public static void main(String[] args) {
		int port = 8080;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		int refreshPeriod = 1000 * 60 * 60;
		if (args.length > 1) {
			refreshPeriod = Integer.parseInt(args[1]);
		}
		System.out.println("Starting server on port " + port);
		new HoneyTeaServer(refreshPeriod, port).start();
		System.out.println("Server started");
	}

}