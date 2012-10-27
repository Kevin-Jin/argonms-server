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
 * Mani: Lead Hair Stylist (NPC 9201064)
 * New Leaf City Town Street: NLC Mall (Map 600000001)
 *
 * New Leaf City hair changer - VIP coupons.
 *
 * @author GoldenKevin (content from Vana r3171)
 */

function getStyleChoices(gender, currentHair) {
	let color = currentHair % 10;
	let styles;
	if (gender == 0)
		styles = [30730, 30280, 30220, 30410, 30200, 30050, 30230, 30160, 30110, 30250];
	else if (gender == 1)
		styles = [31730, 31310, 31470, 31150, 31160, 31300, 31260, 31220, 31410, 31270];
	for (let i = 0; i < styles.length; i++)
		if (npc.isHairValid(styles[i] + color)) //prefer current hair color
			styles[i] += color;
	return styles;
}

let selection = npc.askMenu("I'm the head of this hair salon Mani. If you have #b#t5150031##k, #b#t5151031##k or #b#t5420001##k, allow me to take care of your hairdo. Please choose the one you want.\r\n#b"
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
		selection = npc.askAvatar("I can totally change up your hairstyle and make it look so good. Why don't you change it up a bit? If you have #b#t5150031##k I'll change it for you. Choose the one to your liking~", hairs);
		item = 5150031;
		take = true;
		styleChange = true;
		break;
	case 1:
		hairs = npc.getAllHairColors();
		selection = npc.askAvatar("I can totally change your haircolor and make it look so good. Why don't you change it up a bit? With #b#t5151026##k I'll change it for you. Choose the one to your liking.", hairs);
		item = 5151026;
		take = true;
		styleChange = false;
		break;
	case 2:
		hairs = getStyleChoices(player.getGender(), player.getHair());
		selection = npc.askAvatar("I can totally change up your hairstyle and make it look so good. Why don't you change it up a bit? If you have #b#t5420001##k I'll change it for you. With this coupon, you have the power to change your hairstyle to something totally new, as often as you want, for ONE MONTH! Now, please choose the hairstyle of your liking.", hairs);
		item = 5420001;
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