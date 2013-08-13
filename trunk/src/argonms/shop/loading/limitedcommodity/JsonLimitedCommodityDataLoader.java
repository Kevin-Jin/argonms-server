/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.shop.loading.limitedcommodity;

import argonms.shop.ShopServer;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class JsonLimitedCommodityDataLoader extends LimitedCommodityDataLoader {
	private static final Logger LOG = Logger.getLogger(JsonLimitedCommodityDataLoader.class.getName());

	protected JsonLimitedCommodityDataLoader() {

	}

	@Override
	public boolean loadAll() {
		Context cx = Context.enter();
		FileReader fr = null;
		try {
			fr = new FileReader(ShopServer.getInstance().getLimitedCommodityPath());
			final Scriptable globalScope = cx.initStandardObjects();
			Object json = NativeJSON.parse(cx, globalScope, Kit.readReader(fr), new Callable() {
				@Override
				public Object call(Context cntxt, Scriptable s, Scriptable s1, Object[] os) {
					return os[1];
				}
			});
			for (Map.Entry<Object, Object> limitedCommodity : ((NativeObject) json).entrySet()) {
				Integer itemId = (Integer) limitedCommodity.getKey();
				LimitedCommodity lc = new LimitedCommodity();
				NativeObject properties = (NativeObject) limitedCommodity.getValue();
				lc.addSerialNumbers((NativeArray) properties.get("serials"));
				lc.setStartAmount(((Number) properties.get("startAmount")).intValue());
				lc.setUsedAmount(getUsed(itemId.intValue()));
				lc.setBeginDate(((Number) properties.get("beginDate")).intValue());
				lc.setEndDate(((Number) properties.get("endDate")).intValue());
				lc.setBeginHour(((Number) properties.get("beginHour")).intValue());
				lc.setEndHour(((Number) properties.get("endHour")).intValue());
				limitedCommodities.put(itemId, lc);
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error with reading cashshoplimitedcommodities.txt", ex);
			return false;
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException ex) {
					LOG.log(Level.WARNING, "Error with reading cashshoplimitedcommodities.txt", ex);
				}
			}
			Context.exit();
		}
	}
}
