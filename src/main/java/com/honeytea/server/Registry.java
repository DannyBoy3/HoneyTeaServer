package com.honeytea.server;

import static java.util.Collections.newSetFromMap;

import java.io.*;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Registry {

	private final File file;

	//possible memory leak
	private final Set<String> memory = newSetFromMap(new ConcurrentHashMap<>());

	public Collection<String> getAll() {
		return memory;
	}

	public Registry() {
		file = init();
		try (
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				memory.add(line);
			}
			System.out.println("Read stored cache " + memory.size() + " lines from " + file.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void save(Collection<String> links) {
		if (links.isEmpty()) {
			return;
		}
		int count = 0;
		try (FileWriter fileWriter = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter(fileWriter);
				PrintWriter out = new PrintWriter(bw)) {

			for (String link : links) {
				if (memory.contains(link)) {
					continue;
				}
				memory.add(link);
				count++;
				out.println(link);
			}

			fileWriter.flush();
			System.out.println("Blacklisted " + count + " new links");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean contains(String link) {
		return memory.contains(link);
	}

	private File init() {
		File home = new File(System.getProperty("user.home"));
		File honeyTeaDir = new File(home, ".honeytea");
		if (!honeyTeaDir.exists()) {
			honeyTeaDir.mkdir();
		}
		File honeyTeaDb = new File(honeyTeaDir, "maliciouslinks.db");
		if (!honeyTeaDb.exists()) {
			try {
				honeyTeaDb.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return honeyTeaDb;
	}

}