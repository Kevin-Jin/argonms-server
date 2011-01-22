package argonms.loading.reactor;

import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjReactorDataLoader extends ReactorDataLoader {
	private static final Logger LOG = Logger.getLogger(KvjReactorDataLoader.class.getName());

	private static final byte
		LINK = 1,
		HIT_EVENT = 2,
		ITEM_EVENT = 3
	;

	private String dataPath;

	public KvjReactorDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	protected void load(int reactorid)  {
		String id = String.format("%07d", reactorid);

		try {
			ReactorStats stats = new ReactorStats();
			doWork(new LittleEndianByteArrayReader(new File(new StringBuilder(dataPath).append("Reactor.wz").append(File.separator).append(id).append(".img.kvj").toString())), stats);
			reactorStats.put(Integer.valueOf(reactorid), stats);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for reactor " + reactorid, e);
		}
	}

	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Reactor.wz");
			for (String kvj : root.list()) {
				ReactorStats stats = new ReactorStats();
				doWork(new LittleEndianByteArrayReader(new File(root.getAbsolutePath() + File.separatorChar + kvj)), stats);
				//InputStream is = new BufferedInputStream(new FileInputStream(root.getAbsolutePath() + File.separatorChar + kvj));
				//doWork(new LittleEndianStreamReader(is), stats);
				//is.close();
				reactorStats.put(Integer.valueOf(kvj.substring(0, kvj.lastIndexOf(".img.kvj"))), stats);
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all reactor data from KVJ files.", ex);
			return false;
		}
	}
	
	private void doWork(LittleEndianReader reader, ReactorStats stats) {
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case LINK:
					stats.setLink(reader.readInt());
					break;
				case HIT_EVENT:
					processHitEvent(reader, stats);
					break;
				case ITEM_EVENT:
					processItemEvent(reader, stats);
					break;
			}
		}
	}
	
	private State processHitEvent(LittleEndianReader reader, ReactorStats stats) {
		int stateid = reader.readInt();
		State s = new State();
		s.setType(reader.readInt());
		s.setNextState(reader.readInt());
		stats.addState(stateid, s);
		return s;
	}

	private void processItemEvent(LittleEndianReader reader, ReactorStats stats) {
		State s = processHitEvent(reader, stats);
		s.setItem(reader.readInt(), reader.readInt());
		s.setLt(reader.readInt(), reader.readInt());
		s.setRb(reader.readInt(), reader.readInt());
	}
}
