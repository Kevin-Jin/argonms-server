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

package argonms.shop.loading.commodityoverride;

import argonms.shop.ShopServer;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class JsonCommodityOverrideDataLoader extends CommodityOverrideDataLoader {
	private static final Logger LOG = Logger.getLogger(JsonCommodityOverrideDataLoader.class.getName());

	protected JsonCommodityOverrideDataLoader() {

	}

	@Override
	public boolean loadAll() {
		Context cx = Context.enter();
		FileReader fr = null;
		try {
			fr = new FileReader(ShopServer.getInstance().getCommodityOverridePath());
			final Scriptable globalScope = cx.initStandardObjects();
			Object json = NativeJSON.parse(cx, globalScope, Kit.readReader(fr), new Callable() {
				@Override
				public Object call(Context cntxt, Scriptable s, Scriptable s1, Object[] os) {
					return os[1];
				}
			});
			for (Map.Entry<Object, Object> commodity : ((NativeObject) json).entrySet()) {
				Integer sn = (Integer) commodity.getKey();
				Map<CommodityMod, Object> properties = new EnumMap<CommodityMod, Object>(CommodityMod.class);
				for (Map.Entry<Object, Object> property : ((NativeObject) commodity.getValue()).entrySet()) {
					String propKey = (String) property.getKey();
					if (propKey.equals("itemId")) {
						assert property.getValue() instanceof Number;
						properties.put(CommodityMod.ITEM_ID, property.getValue());
					} else if (propKey.equals("count")) {
						assert property.getValue() instanceof Number;
						properties.put(CommodityMod.COUNT, property.getValue());
					} else if (propKey.equals("price")) {
						assert property.getValue() instanceof Number;
						properties.put(CommodityMod.SALE_PRICE, property.getValue());
					} else if (propKey.equals("priority")) {
						assert property.getValue() instanceof Number;
						properties.put(CommodityMod.PRIORITY, property.getValue());
					} else if (propKey.equals("onSale")) {
						assert property.getValue() instanceof Number;
						properties.put(CommodityMod.ON_SALE, property.getValue());
					} else if (propKey.equals("class")) {
						assert property.getValue() instanceof Number;
						properties.put(CommodityMod.CLASS, property.getValue());
					}
				}
				mods.put(sn, properties);
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Error with reading cashshopcommodityoverrides.txt", ex);
			return false;
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException ex) {
					LOG.log(Level.WARNING, "Error with reading cashshopcommodityoverrides.txt", ex);
				}
			}
			Context.exit();
		}
	}
}
