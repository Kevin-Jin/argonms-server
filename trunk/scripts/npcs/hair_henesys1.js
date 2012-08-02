/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

/**
 * Natalie: Hair Salon Owner (NPC 1012103)
 * Victoria Road: Henesys Hair Salon (Map 100000104)
 *
 * Henesys hair changer - VIP coupons.
 *
 * @author GoldenKevin (content from Vana r2111)
 */

function getStyleChoices(gender, currentHair) {
	let color = currentHair % 10;
	let styles;
	if (gender == 0)
		styles = [30030, 30020, 30000, 30310, 30330, 30060, 30150, 30410, 30210, 30140, 30120, 30200];
	else if (gender == 1)
		styles = [31050, 31040, 31000, 31150, 31310, 31300, 31160, 31100, 31410, 31030, 31080, 31070];
	for (let i = 0; i < styles.length; i++)
		styles[i] += color; //hair style choice colors are current hair color
	return styles;
}

function getColorChoices(currentHair) {
	let style = currentHair - currentHair % 10;
	let colors = new Array(8);
	for (let i = 0; i < 8; i++)
		colors[i] = style + i; //hair color choices use current hair style
	return colors;
}

let selection = npc.askMenu("I'm the head of this hair salon Natalie. If you have #b#t5150001##k, #b#t5151001##k or #b#t5420002##k, allow me to take care of your hairdo. Please choose the one you want.\r\n#b"
		+ "#L0# Haircut(VIP coupon)#l\r\n"
		+ "#L1# Dye your hair(VIP coupon)#l\r\n"
		+ "#L2# Change Hairstyle (VIP Membership Coupon)#l");
let item;
let take;
let styleChange;
let hairs;
switch (selection) {
	case 0:
		hairs = getStyleChoices(player.getGender(), player.getHair());
		selection = npc.askAvatar("I can totally change up your hairstyle and make it look so good. Why don't you change it up a bit? If you have #b#t5150001##k I'll change it for you. Choose the one to your liking~", hairs);
		item = 5150001;
		take = true;
		styleChange = true;
		break;
	case 1:
		hairs = getColorChoices(player.getHair());
		selection = npc.askAvatar("I can totally change your haircolor and make it look so good. Why don't you change it up a bit? With #b#t5151001##k I'll change it for you. Choose the one to your liking.", hairs);
		item = 5151001;
		take = true;
		styleChange = false;
		break;
	case 2:
		hairs = getStyleChoices(player.getGender(), player.getHair());
		selection = npc.askAvatar("I can totally change up your hairstyle and make it look so good. Why don't you change it up a bit? If you have #b#t5420002##k I'll change it for you. With this coupon, you have the power to change your hairstyle to something totally new, as often as you want, for ONE MONTH! Now, please choose the hairstyle of your liking.", hairs);
		item = 5420002;
		take = false;
		styleChange = true;
		break;
}
if (player.hasItem(item, 1)) {
	if (take)
		player.loseItem(item, 1);
	player.setHair(hairs[selection]);
	npc.say("Enjoy your new and improved hairstyle!");
} else {
	npc.sayNext("Hmmm...it looks like you don't have our designated coupon...I'm afraid I can't " + (styleChange ? "give you a haircut" : "dye your hair") + " without it. I'm sorry.");
}